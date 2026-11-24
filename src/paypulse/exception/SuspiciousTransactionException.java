package paypulse.exception;

/** CHECKED exception: the caller must catch it or declare "throws". Raised when a payment looks like fraud. */
public class SuspiciousTransactionException extends Exception {
    public SuspiciousTransactionException(String message) {
        super(message);
    }
}
