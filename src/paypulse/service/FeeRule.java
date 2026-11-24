package paypulse.service;

/**
 * A functional interface: exactly one abstract method, so a lambda can be used wherever a FeeRule is needed.
 * Example: FeeRule oneOnePercent = amount -> amount * 0.01;
 */
@FunctionalInterface
public interface FeeRule {

    double feeFor(double amount);

    /** A default method can use the abstract one. */
    default double totalFor(double amount) {
        return amount + feeFor(amount);
    }

    /** Ready-made rules built with lambdas. */
    static FeeRule flat(double fee) {
        return amount -> fee;
    }

    static FeeRule percent(double rate, double cap) {
        return amount -> Math.min(amount * rate, cap);
    }
}