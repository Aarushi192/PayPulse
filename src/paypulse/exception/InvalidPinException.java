package paypulse.exception;

/** Checked exception: the PIN is not valid or does not match. */
public class InvalidPinException extends Exception {
    public InvalidPinException(String message){
        super(message);
    }

    public InvalidPinException(String message, Throwable cause){
        super(message, cause);
    }
}
