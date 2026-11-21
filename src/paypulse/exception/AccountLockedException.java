package paypulse.exception;

/** Unchecked exception: the account is locked after 3 wrong PINs. */
public class AccountLockedException extends RuntimeException{
    public AccountLockedException(String message){
        super(message);
    }
}
