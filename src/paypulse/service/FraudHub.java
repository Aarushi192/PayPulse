package paypulse.service;

import paypulse.model.Transaction;
import paypulse.model.TransactionStatus;

/**
 * One shared FraudService and one shared FraudInbox for the whole program.
 * Person B's payment code calls FraudHub.report(t) after every payment.
 */
public final class FraudHub {

    public static final FraudService SERVICE = new FraudService();
    public static final FraudInbox INBOX = new FraudInbox();

    private FraudHub() { }

    /** Sends a finished payment (SUCCESS or FAILED) to the fraud monitor. */
    public static void report(Transaction t) {
        if (t.getStatus() != TransactionStatus.PENDING) {
            INBOX.submit(t);
        }
    }
}
