package paypulse.ui;

import paypulse.exception.InvalidRuleException;
import paypulse.exception.ReportExportException;
import paypulse.model.FlaggedAccountsReport;
import paypulse.model.FraudFlag;
import paypulse.model.P2PTransaction;
import paypulse.model.Report;
import paypulse.model.TopSendersReport;
import paypulse.model.Transaction;
import paypulse.service.FraudRule;
import paypulse.service.FraudService;
import paypulse.util.FlagFilter;
import paypulse.util.FraudStats;
import paypulse.util.JaggedFlagTable;
import paypulse.util.ReportGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Person C: one small demo per syllabus topic (run main() and walk the evaluator through it). */
public class FraudDemo {

    public static void main(String[] args) {
        FraudService service = new FraudService();
        List<Transaction> txns = sampleTransactions();
        for (Transaction t : txns) {
            service.check(t);
        }

        classesDemo(service);
        polymorphismDemo(service, txns);
        stringsDemo();
        wrappersDemo(service);
        exceptionsDemo();
        collectionsDemo(service);
        jaggedArrayDemo(service);
        lambdaDemo(txns);
        streamsDemo(service, txns);
        genericsDemo(service, txns);
    }

    private static List<Transaction> sampleTransactions() {
        List<Transaction> list = new ArrayList<>();
        list.add(new P2PTransaction("ravi@upi", "priya@upi", 500));
        list.add(new P2PTransaction("ravi@upi", "priya@upi", 15000));
        list.add(new P2PTransaction("ravi@upi", "shop@upi", 80));
        list.add(new P2PTransaction("ravi@upi", "shop@upi", 90));      // 4th quick payment of Ravi: rapid
        list.add(new P2PTransaction("priya@upi", "ravi@upi", 6000));
        list.add(new P2PTransaction("amit@upi", "ravi@upi", 120));
        return list;
    }

    private static void classesDemo(FraudService service) {
        System.out.println("=== 1. Classes, constructors, this, static ===");
        FraudFlag a = new FraudFlag(1, "Manual flag", FraudFlag.LOW);          // 3-argument constructor
        FraudFlag b = new FraudFlag(7, 2, "From database", FraudFlag.HIGH);    // 4-argument constructor
        System.out.println("  " + a + "  |  " + b);
        System.out.println("  static counter FraudFlag.getTotalFlags() = " + FraudFlag.getTotalFlags());
    }

    private static void polymorphismDemo(FraudService service, List<Transaction> txns) {
        System.out.println("\n=== 2/3. Inheritance, abstract class, interface ===");
        List<Report> reports = new ArrayList<>();                              // one list, different child classes
        reports.add(new FlaggedAccountsReport(service.getFlagsReadOnly(), service.getFlaggedAccounts()));
        reports.add(new TopSendersReport(txns, 3));
        for (Report r : reports) {
            r.print();                                                         // generate() differs per class
        }
    }

    private static void stringsDemo() {
        System.out.println("\n=== 5. Strings ===");
        String filter = "risk>=2";
        String[] parts = filter.split(">=");                                   // "risk>=2" becomes ["risk", "2"]
        System.out.println("  split: " + parts[0] + " | " + parts[1]);
        System.out.println("  startsWith(\"risk\") = " + filter.startsWith("risk")
                + ", endsWith(\"2\") = " + filter.endsWith("2"));
        System.out.println("  equalsIgnoreCase: " + "HIGH".equalsIgnoreCase("high"));
        System.out.println("  compareTo: " + "apple".compareTo("banana") + ", compareToIgnoreCase: "
                + "Apple".compareToIgnoreCase("apple"));
        System.out.println("  join: " + String.join(" | ", "LOW", "MEDIUM", "HIGH"));
        System.out.println("  " + "=".repeat(20));
    }

    private static void wrappersDemo(FraudService service) {
        System.out.println("\n=== 6. StringBuilder and wrapper classes ===");
        StringBuilder sb = new StringBuilder();
        for (String upi : service.getFlaggedAccounts()) {
            sb.append(upi).append(", ");
        }
        sb.deleteCharAt(sb.length() - 1).deleteCharAt(sb.length() - 1);        // remove the trailing ", "
        System.out.println("  accounts: " + sb);
        int threshold = Integer.parseInt("2");                                 // String -> int
        System.out.println("  parsed threshold = " + threshold + ", Integer.MAX_VALUE = " + Integer.MAX_VALUE);
        Map<String, Integer> counts = new HashMap<>();
        counts.put("ravi@upi", 3);                                             // autoboxing int -> Integer
        int n = counts.get("ravi@upi");                                        // unboxing Integer -> int
        System.out.println("  unboxed count = " + n);
        System.out.println("  lowest risk score now = " + FraudStats.lowestRisk(service.flagsByPriority()));
    }

    private static void exceptionsDemo() {
        System.out.println("\n=== 7/8. try / catch / finally, custom exceptions ===");
        try {
            int[] arr = new int[2];
            arr[Integer.parseInt("5")] = 1;                                    // index 5 does not exist
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {   // multi-catch
            System.out.println("  caught: " + e.getClass().getSimpleName());
        } finally {
            System.out.println("  finally always runs (closing the report section)");
        }
        try {
            FlagFilter.parse("risk>=abc");                                     // throws InvalidRuleException (unchecked)
        } catch (InvalidRuleException e) {
            System.out.println("  " + e.getMessage() + " | cause: " + e.getCause().getClass().getSimpleName());
        }
        try {
            new TopSendersReport(new ArrayList<>(), 3).export("report.pdf");   // wrong extension: checked exception
        } catch (ReportExportException e) {
            System.out.println("  " + e.getMessage());
        }
    }

    private static void collectionsDemo(FraudService service) {
        System.out.println("\n=== 10/11/12. Collections ===");
        System.out.println("  HashSet of flagged accounts (sorted copy): " + service.getFlaggedAccounts());
        System.out.println("  HashMap of flag counts                  : " + service.getFraudCounts());
        System.out.println("  Deque of recent alerts (newest first)   : " + service.getRecentAlerts());
        System.out.println("  PriorityQueue order (highest risk first):");
        for (FraudFlag f : service.flagsByPriority()) {
            System.out.println("    " + f);
        }
        int removed = service.clearResolved(FraudFlag.LOW);                    // Iterator removal
        System.out.println("  Iterator removed " + removed + " LOW flag(s)");
        try {
            service.getFlagsReadOnly().add(new FraudFlag(1, "hack", 1));       // read-only list
        } catch (UnsupportedOperationException e) {
            System.out.println("  unmodifiableList blocked the change");
        }
    }

    private static void jaggedArrayDemo(FraudService service) {
        System.out.println("\n=== 4. Jagged 2D array ===");
        System.out.println(new JaggedFlagTable(service.flagsByUser()).render());
    }

    private static void lambdaDemo(List<Transaction> txns) {
        System.out.println("=== 20. Lambdas and method references ===");
        FraudRule big = t -> t.getAmount() > 5000;                             // FraudRule as a lambda
        Predicate<Transaction> fromRavi = t -> t.getSenderUpi().equals("ravi@upi");
        for (Transaction t : txns) {
            if (big.isSuspicious(t) && fromRavi.test(t)) {
                System.out.println("  big payment from Ravi: " + t);
            }
        }
        txns.stream().map(Transaction::getSenderUpi).distinct().forEach(System.out::println);   // method references
    }

    private static void streamsDemo(FraudService service, List<Transaction> txns) {
        System.out.println("\n=== 21. Streams ===");
        System.out.println("  top 2 senders      : " + FraudStats.topSenders(txns, 2));
        System.out.println("  page 2, size 2     : " + FraudStats.page(txns, 2, 2).size() + " item(s)");
        System.out.println("  flags per user     : " + service.flagsPerUserStream());
        System.out.println("  totals (TreeMap)   : " + FraudStats.senderTotals(txns));
    }

    private static void genericsDemo(FraudService service, List<Transaction> txns) {
        System.out.println("\n=== 22. Generics ===");
        ReportGenerator<TopSendersReport> only = new ReportGenerator<>();      // only TopSendersReport allowed
        only.add(new TopSendersReport(txns, 2));
        only.printAll();
        List<FlaggedAccountsReport> list = new ArrayList<>();
        list.add(new FlaggedAccountsReport(service.getFlagsReadOnly(), service.getFlaggedAccounts()));
        ReportGenerator.printAll(list);                                        // wildcard method
    }
}
