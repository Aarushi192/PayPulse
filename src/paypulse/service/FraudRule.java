package paypulse.service;

import paypulse.model.Transaction;

/** Functional interface: only ONE abstract method, so a rule can be written as a lambda. */
@FunctionalInterface
public interface FraudRule {
    boolean isSuspicious(Transaction t);
}
