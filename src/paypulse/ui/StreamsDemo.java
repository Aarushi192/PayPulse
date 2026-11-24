package paypulse.ui;

import paypulse.model.BillPayment;
import paypulse.model.P2PTransaction;
import paypulse.model.RegularUser;
import paypulse.model.Transaction;
import paypulse.model.User;
import paypulse.service.FeeRule;
import paypulse.service.PaymentAnalytics;
import paypulse.util.ReceiptFormatter;
import paypulse.util.SortUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Person B demo: lambdas, functional interfaces, streams and generics. No database needed. */
public class StreamsDemo {

    public static void main(String[] args) {
        lambdaDemo();
        genericsDemo();
        streamsDemo();
    }

    // ---------- 1. Lambdas and functional interfaces ----------
    private static void lambdaDemo() {
        System.out.println("=== 1. Lambdas ===");
        FeeRule bill = FeeRule.percent(0.01, 20);                // lambda built inside FeeRule
        FeeRule weekendOffer = amount -> amount * 0.005;         // our own lambda
        FeeRule noFee = FeeRule.flat(0);
        System.out.printf("Bill 5000: fee %.2f, total %.2f%n", bill.feeFor(5000), bill.totalFor(5000));
        System.out.printf("Weekend offer on 1000: fee %.2f%n", weekendOffer.feeFor(1000));
        System.out.printf("No fee on 1000: total %.2f%n", noFee.totalFor(1000));

        Predicate<Transaction> big = t -> t.getAmount() >= 1000;           // Predicate: Transaction -> boolean
        Function<Transaction, String> label =                              // Function: Transaction -> String
                t -> t.getTxnType() + " #" + t.getId();
        Supplier<String> title = () -> "--- big payments ---";             // Supplier: nothing -> value
        Consumer<String> show = System.out::println;                       // Consumer: value -> nothing (method reference)

        List<Transaction> list = sample();
        show.accept(title.get());
        for (Transaction t : list) {
            if (big.test(t)) {
                show.accept(label.apply(t) + " : " + t.getAmount());
            }
        }
    }

    // ---------- 2. Generics ----------
    private static void genericsDemo() {
        System.out.println("\n=== 2. Generics: SortUtil<T extends Comparable<T>> ===");
        SortUtil<Integer> forInts = new SortUtil<>();
        SortUtil<String> forNames = new SortUtil<>();
        SortUtil<Double> forAmounts = new SortUtil<>();

        List<Integer> ids = Arrays.asList(30, 5, 12, 9);
        List<String> names = Arrays.asList("priya@upi", "asha@upi", "ben@upi");
        List<Double> amounts = Arrays.asList(250.0, 40.5, 1800.0);

        System.out.println("Integers : " + forInts.sorted(ids) + "  max " + forInts.max(ids));
        System.out.println("Strings  : " + forNames.sorted(names) + "  max " + forNames.max(names));
        System.out.println("Doubles  : " + forAmounts.sorted(amounts) + "  sorted? " + forAmounts.isSorted(amounts));
        System.out.println("Original list is unchanged: " + ids);

        System.out.println("Generic methods : filter amounts over 100 -> "
                + SortUtil.filter(amounts, a -> a > 100));
        System.out.println("                  map names to upper    -> "
                + SortUtil.map(names, String::toUpperCase));
        System.out.println("Wildcard sum    : " + SortUtil.sum(ids) + " (ints), " + SortUtil.sum(amounts) + " (doubles)");
    }

    // ---------- 3. Streams ----------
    private static void streamsDemo() {
        System.out.println("\n=== 3. Streams over payments ===");
        List<Transaction> list = sample();
        PaymentAnalytics stats = new PaymentAnalytics(list);

        System.out.println("Payments:");
        list.forEach(t -> System.out.println("  " + ReceiptFormatter.tableRow(t)));

        System.out.printf("Total sent        : %.2f%n", stats.totalSent());
        System.out.printf("Total fees        : %.2f%n", stats.totalFees());
        System.out.println("Average amount    : " + stats.averageAmount().orElse(0));
        System.out.println("Biggest payment   : " + stats.biggest().map(Transaction::toString).orElse("none"));
        System.out.println("Top 2             : " + stats.topPayments(2).stream().map(Transaction::getId).toList());
        System.out.println("Sent per receiver : " + stats.sentPerReceiver());
        System.out.println("Count by type     : " + stats.countByType());
        System.out.println("Success vs others : " + stats.successVersusOthers());
        System.out.println("Receivers         : " + stats.receiversText());
        System.out.println("Any over 2000?    : " + stats.anyOver(2000));
        System.out.println("All successful?   : " + stats.allSuccessful());
        System.out.println("Notes with 'rent' : " + stats.matching(t -> t.getNote().toLowerCase().contains("rent")).size());
    }

    /** Four payments made for real: 3 succeed, 1 fails (not enough balance). */
    private static List<Transaction> sample() {
        User ravi = new RegularUser("ravi@upi", "Ravi", "1234", 6000);
        User priya = new RegularUser("priya@upi", "Priya", "4321", 0);
        User bescom = new RegularUser("bescom@upi", "Bescom", "1111", 0);
        List<Transaction> list = new ArrayList<>();
        list.add(run(new P2PTransaction("ravi@upi", "priya@upi", 1500, "Rent share"), ravi, priya));
        list.add(run(new BillPayment("ravi@upi", "bescom@upi", 2000, "Electricity"), ravi, bescom));
        list.add(run(new P2PTransaction("ravi@upi", "priya@upi", 300, "Lunch"), ravi, priya));
        list.add(run(new P2PTransaction("ravi@upi", "priya@upi", 9000, "Too much"), ravi, priya));
        return list;
    }

    private static Transaction run(Transaction t, User from, User to) {
        try {
            t.process(from, to);
        } catch (Exception e) {
            // process() already marked it FAILED
        }
        return t;
    }
}