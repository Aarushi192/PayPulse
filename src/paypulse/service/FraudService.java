package paypulse.service;

import paypulse.exception.SuspiciousTransactionException;
import paypulse.model.FraudFlag;
import paypulse.model.Transaction;
import paypulse.util.FraudLog;
import paypulse.util.FraudRulesFile;
import paypulse.util.FraudStats;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Person C's main class. check(transaction) runs every fraud rule on a payment.
 * The methods are synchronized because the FraudMonitor thread and the admin menu use this object together.
 */
public class FraudService {

    private static final int MAX_RECENT_ALERTS = 5;

    /** One rule + its reason text + its risk score. */
    private static class NamedRule {
        final String reason;
        final int risk;
        final FraudRule rule;

        NamedRule(String reason, int risk, FraudRule rule) {
            this.reason = reason;
            this.risk = risk;
            this.rule = rule;
        }
    }

    // Thresholds (can be changed from rules.txt)
    private double largeAmount = 10000;
    private double roundAmount = 5000;
    private int rapidCount = 2;               // this many EARLIER transfers inside the window => flag
    private long rapidSeconds = 60;

    private final List<NamedRule> rules = new ArrayList<>();                        // List of lambda rules
    private final PriorityQueue<FraudFlag> flagQueue = new PriorityQueue<>();       // highest risk first (Comparable)
    private final Deque<String> recentAlerts = new ArrayDeque<>();                  // newest alert at the front
    private final HashMap<String, Integer> fraudCount = new HashMap<>();            // sender -> number of flags
    private final Set<String> flaggedAccounts = new HashSet<>();                    // no duplicates
    private final List<Transaction> checked = new ArrayList<>();                    // every transaction checked so far
    private final Map<Integer, String> senderOfTxn = new HashMap<>();               // transaction id -> sender
    private String logPath = null;                                                  // null = do not write a log file

    public FraudService() {
        buildRules();
    }

    // ------------------------------------------------------------------
    // Rules (lambdas)
    // ------------------------------------------------------------------
    private void buildRules() {
        rules.clear();
        // Each rule is a lambda: t -> (true when suspicious). They read the thresholds when they run,
        // so changing a threshold changes the rule at once.
        rules.add(new NamedRule("Large amount", FraudFlag.HIGH,
                t -> t.getAmount() > largeAmount));
        rules.add(new NamedRule("Round big amount", FraudFlag.MEDIUM,
                t -> t.getAmount() >= roundAmount && t.getAmount() <= largeAmount
                        && t.getAmount() % 1000 == 0));          // above largeAmount the HIGH rule already flags it
        rules.add(new NamedRule("Rapid repeated transfers", FraudFlag.MEDIUM,
                t -> earlierTransfersInWindow(t) >= rapidCount));
    }

    /** How many EARLIER transfers did the same sender make inside the time window? */
    private int earlierTransfersInWindow(Transaction t) {
        int count = 0;
        for (Transaction old : checked) {
            boolean sameSender = old.getSenderUpi().equals(t.getSenderUpi());
            long seconds = Math.abs(Duration.between(old.getTimestamp(), t.getTimestamp()).getSeconds());
            if (sameSender && seconds <= rapidSeconds) {
                count++;
            }
        }
        return count;
    }

    /** Reads rules.txt (see FraudRulesFile) and applies the numbers. */
    public synchronized void loadThresholds(String path) throws IOException {
        Map<String, Double> values = FraudRulesFile.load(path);     // may throw InvalidRuleException
        for (Map.Entry<String, Double> e : values.entrySet()) {
            double v = e.getValue();
            switch (e.getKey()) {
                case "LARGE_AMOUNT"  -> largeAmount = v;
                case "ROUND_AMOUNT"  -> roundAmount = v;
                case "RAPID_COUNT"   -> rapidCount = (int) v;
                case "RAPID_SECONDS" -> rapidSeconds = (long) v;
                default -> { }
            }
        }
        buildRules();
    }

    public synchronized String describeRules() {
        return String.format("Large amount > %.0f | Round amount >= %.0f | Rapid: more than %d earlier in %d s",
                largeAmount, roundAmount, rapidCount, rapidSeconds);
    }

    public synchronized void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    // ------------------------------------------------------------------
    // Checking transactions
    // ------------------------------------------------------------------

    /** Runs all rules. Returns the new flags (an empty list means the payment looks fine). */
    public synchronized List<FraudFlag> check(Transaction t) {
        List<FraudFlag> newFlags = new ArrayList<>();
        for (NamedRule nr : rules) {
            if (nr.rule.isSuspicious(t)) {
                FraudFlag flag = new FraudFlag(t.getId(), nr.reason, nr.risk);
                newFlags.add(flag);
                flagQueue.add(flag);
                flaggedAccounts.add(t.getSenderUpi());
                fraudCount.put(t.getSenderUpi(), fraudCount.getOrDefault(t.getSenderUpi(), 0) + 1);

                recentAlerts.addFirst(nr.reason + " -> " + t.getSenderUpi() + " (txn " + t.getId() + ")");
                if (recentAlerts.size() > MAX_RECENT_ALERTS) {
                    recentAlerts.removeLast();               // keep only the latest few
                }
                writeLog(flag, t.getSenderUpi());
            }
        }
        checked.add(t);
        senderOfTxn.put(t.getId(), t.getSenderUpi());
        return newFlags;
    }

    /** Strict version: a HIGH risk payment is rejected with a checked exception. */
    public synchronized void checkStrict(Transaction t) throws SuspiciousTransactionException {
        for (FraudFlag f : check(t)) {
            if (f.getRiskScore() == FraudFlag.HIGH) {
                throw new SuspiciousTransactionException(
                        "Transaction " + t.getId() + " blocked: " + f.getReason());
            }
        }
    }

    private void writeLog(FraudFlag flag, String sender) {
        if (logPath == null) {
            return;
        }
        try {
            FraudLog.append(logPath, flag, sender);
        } catch (IOException e) {
            System.out.println("Could not write fraud log: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Reading results
    // ------------------------------------------------------------------

    /** All open flags, highest risk first. Works on a COPY so the real queue is untouched. */
    public synchronized List<FraudFlag> flagsByPriority() {
        PriorityQueue<FraudFlag> copy = new PriorityQueue<>(flagQueue);
        List<FraudFlag> sorted = new ArrayList<>();
        while (!copy.isEmpty()) {
            sorted.add(copy.poll());                  // poll() always gives the highest risk first
        }
        return sorted;
    }

    /** Read-only view for the menus. */
    public synchronized List<FraudFlag> getFlagsReadOnly() {
        return Collections.unmodifiableList(flagsByPriority());
    }

    public synchronized int getFlagCount() {
        return flagQueue.size();
    }

    public synchronized Set<String> getFlaggedAccounts() {
        return new TreeSet<>(flaggedAccounts);        // sorted copy
    }

    public synchronized Map<String, Integer> getFraudCounts() {
        return new TreeMap<>(fraudCount);
    }

    /** Unboxing: the Integer from the map becomes an int. */
    public synchronized int getFraudCountFor(String upi) {
        Integer count = fraudCount.get(upi);
        return count == null ? 0 : count;
    }

    public synchronized List<String> getRecentAlerts() {
        return new ArrayList<>(recentAlerts);
    }

    public synchronized List<Transaction> getCheckedTransactions() {
        return new ArrayList<>(checked);
    }

    /** Sender -> list of that sender's flags (used by the jagged table). */
    public synchronized TreeMap<String, List<FraudFlag>> flagsByUser() {
        TreeMap<String, List<FraudFlag>> map = new TreeMap<>();
        for (FraudFlag f : flagsByPriority()) {
            String sender = senderOfTxn.get(f.getTransactionId());
            map.computeIfAbsent(sender, k -> new ArrayList<>()).add(f);
        }
        return map;
    }

    /** Flags per user, counted with a stream (groupingBy + counting). */
    public synchronized Map<String, Long> flagsPerUserStream() {
        return flagsByPriority().stream()
                .collect(Collectors.groupingBy(f -> senderOfTxn.get(f.getTransactionId()),
                        TreeMap::new, Collectors.counting()));
    }

    public synchronized String senderOf(int transactionId) {
        return senderOfTxn.get(transactionId);
    }

    // ------------------------------------------------------------------
    // Cleaning up
    // ------------------------------------------------------------------

    /** Removes resolved flags (risk <= maxRisk). An Iterator is the safe way to remove while looping. */
    public synchronized int clearResolved(int maxRisk) {
        int removed = 0;
        Iterator<FraudFlag> it = flagQueue.iterator();
        while (it.hasNext()) {
            if (it.next().getRiskScore() <= maxRisk) {
                it.remove();
                removed++;
            }
        }
        return removed;
    }

    public synchronized List<FraudFlag> page(int pageNo, int pageSize) {
        return FraudStats.page(flagsByPriority(), pageNo, pageSize);
    }
}
