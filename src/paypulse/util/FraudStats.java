package paypulse.util;

import paypulse.model.FraudFlag;
import paypulse.model.Transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Report calculations. Most of them use the Stream API (groupingBy, sorted, limit, skip). */
public final class FraudStats {

    private FraudStats() { }

    /** A Comparator written as a lambda: biggest total first. */
    public static final Comparator<Map.Entry<String, Double>> BY_TOTAL_DESC =
            (a, b) -> Double.compare(b.getValue(), a.getValue());

    /** Top n senders by total amount sent. */
    public static List<Map.Entry<String, Double>> topSenders(List<Transaction> txns, int n) {
        List<Map.Entry<String, Double>> top = txns.stream()
                // groupingBy: sender -> sum of all amounts they sent
                .collect(Collectors.groupingBy(Transaction::getSenderUpi,
                        Collectors.summingDouble(Transaction::getAmount)))
                .entrySet().stream()
                .sorted(BY_TOTAL_DESC)
                .limit(n)
                .collect(Collectors.toList());
        // Read-only list: the menu can look at it but cannot change it.
        return Collections.unmodifiableList(top);
    }

    /** Pagination: skip the earlier pages, then take one page. pageNo starts at 1. */
    public static <T> List<T> page(List<T> items, int pageNo, int pageSize) {
        return items.stream()
                .skip((long) (pageNo - 1) * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());
    }

    /** Distinct sender UPI IDs, sorted. */
    public static List<String> distinctSenders(List<Transaction> txns) {
        return txns.stream().map(Transaction::getSenderUpi).distinct().sorted().collect(Collectors.toList());
    }

    /** Total sent per sender in a TreeMap, so the output is sorted by UPI ID. */
    public static TreeMap<String, Double> senderTotals(List<Transaction> txns) {
        TreeMap<String, Double> totals = new TreeMap<>();
        for (Transaction t : txns) {
            totals.merge(t.getSenderUpi(), t.getAmount(), Double::sum);   // Double::sum is a method reference
        }
        return totals;
    }

    /** Lowest risk score in a list. Starts from Integer.MAX_VALUE so any real score is smaller. */
    public static int lowestRisk(List<FraudFlag> flags) {
        int min = Integer.MAX_VALUE;
        for (FraudFlag f : flags) {
            min = Math.min(min, f.getRiskScore());
        }
        return flags.isEmpty() ? 0 : min;
    }

    /** Copies a list into a new sorted list (the original is not changed). */
    public static List<FraudFlag> sortedByRisk(List<FraudFlag> flags) {
        List<FraudFlag> copy = new ArrayList<>(flags);
        Collections.sort(copy);                       // uses FraudFlag.compareTo (Comparable)
        return copy;
    }
}
