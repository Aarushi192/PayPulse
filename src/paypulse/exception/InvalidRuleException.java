package paypulse.exception;

/** UNCHECKED exception (extends RuntimeException): a fraud rule or filter text is wrong. */
public class InvalidRuleException extends RuntimeException {
    public InvalidRuleException(String message) {
        super(message);
    }

    public InvalidRuleException(String message, Throwable cause) {
        super(message, cause);
    }
}
