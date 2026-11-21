package paypulse.exception;

/** Checked exception: the UPI ID format is wrong. */
public class InvalidUPIException extends Exception{
    public InvalidUPIException(String message){
        super(message);
    }
}
