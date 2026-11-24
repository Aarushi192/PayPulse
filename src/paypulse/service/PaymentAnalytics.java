package paypulse.service;

import paypulse.model.Transaction;
import paypulse.model.TransactionStatus;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Person B, lambdas and streams: statistics over a list of payments. */
public class PaymentAnalytics {

    private final List<Transaction> payments;

    public PaymentAnalytics(List<Transaction> payments) {
        this.payments = payments;
    }

    private static final Predicate<Transaction> SUCCESSFUL = t -> t.getStatus() == TransactionStatus.SUCCESS;

    /** Total money that was actually sent. filter -> mapToDouble -> sum. */
    public double totalSent() {
        return payments.stream()
                .filter(SUCCESSFUL)
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    public double totalFees() {
        return payments.stream()
                .filter(SUCCESSFUL)
                .mapToDouble(Transaction::calculateFee)
                .sum();
    }

    public OptionalDouble averageAmount() {
        return payments.stream()
                .filter(SUCCESSFUL)
                .mapToDouble(Transaction::getAmount)
                .average();                          // empty when there are no payments
    }

    public Optional<Transaction> biggest() {
        return payments.stream()
                .filter(SUCCESSFUL)
                .max(Comparator.comparingDouble(Transaction::getAmount));
    }

    /** The n biggest successful payments. sorted -> limit -> collect. */
    public List<Transaction> topPayments(int n) {
        return payments.stream()
                .filter(SUCCESSFUL)
                .sorted(Comparator.comparingDouble(Transaction::getAmount).reversed())
                .limit(n)
                .collect(Collectors.toList());
    }

    /** Money sent to each receiver (groupingBy + summingDouble). TreeMap keeps the names sorted. */
    public Map<String, Double> sentPerReceiver() {
        return payments.stream()
                .filter(SUCCESSFUL)
                .collect(Collectors.groupingBy(Transaction::getReceiverUpi, TreeMap::new,
                        Collectors.summingDouble(Transaction::getAmount)));
    }

    /** How many payments of each type (P2P, BILL). */
    public Map<String, Long> countByType() {
        return payments.stream()
                .collect(Collectors.groupingBy(Transaction::getTxnType, TreeMap::new, Collectors.counting()));
    }

    /** true -> how many succeeded, false -> how many did not. */
    public Map<Boolean, Long> successVersusOthers() {
        return payments.stream()
                .collect(Collectors.partitioningBy(SUCCESSFUL, Collectors.counting()));
    }

    /** Distinct receivers in alphabetical order as one text: "bescom@upi, priya@upi". */
    public String receiversText() {
        return payments.stream()
                .map(Transaction::getReceiverUpi)
                .distinct()
                .sorted()
                .collect(Collectors.joining(", "));
    }

    /** Any condition you like, passed in as a lambda. */
    public List<Transaction> matching(Predicate<Transaction> condition) {
        return payments.stream().filter(condition).collect(Collectors.toList());
    }

    public boolean anyOver(double amount) {
        return payments.stream().anyMatch(t -> t.getAmount() > amount);
    }

    public boolean allSuccessful() {
        return payments.stream().allMatch(SUCCESSFUL);
    }
}