package paypulse.model;

import paypulse.exception.AccountLockedException;
import paypulse.exception.InvalidPinException;

/** Abstract base class for every account in PayPulse. */

public class User implements Authenticatable{

    public static final int MAX_FAILED_ATTEMPTS = 3;
    private static int totalUsers = 0;

    private String upiId;
    private String name;
    private String pinHash;

    private double balance;
    private int failedAttempts ;
    private boolean locked;
    private double dailySpent;

    // Overloaded constructors chained with this(...)

    public User(String upiId,String name, String pin){
        this(upiId, name, pin, 0.0);
    }

    public User(String upiId,String name, String pin, double balance){
        this(upiId, name, pin, balance, 0, false, 0.0);
    }

    // Full constructor (also used later when loading rows from MySQL)

    public User(String upiId, String name, String pin, double balance,
                int failedAttempts, boolean locked, double dailySpent) {
        this.upiId = upiId;
        this.name = name;
        this.pinHash = pin;
        this.balance = balance;
        this.failedAttempts = failedAttempts;
        this.locked = locked;
        this.dailySpent = dailySpent;
        totalUsers++;
    }

    /** Each user type has its own daily sending limit. */
    public abstract double getDailyLimit();

    /** Short label such as REGULAR or MERCHANT (will become user_type in the DB). */
    public abstract String getUserType();

    @Override
    public boolean login(String pin) throws InvalidPinException {
        if (locked) {
            throw new AccountLockedException("Account " + getMaskedUpi() + " is locked.");
        }
        if (pinHash.equals(pin)) {
            failedAttempts = 0;
            return true;
        }
        failedAttempts++;
        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            lock();
            throw new AccountLockedException("Too many wrong PINs. Account locked.");
        }
        throw new InvalidPinException("Wrong PIN. Attempts left: "
                + (MAX_FAILED_ATTEMPTS - failedAttempts));
    }

    @Override
    public void lock() {
        this.locked = true;
    }

    /** ravi@upi becomes ra****@upi, built with StringBuilder. */

    public String getMaskedUpi() {
        int at = upiId.indexOf('@');
        String handle = upiId.substring(0, at);
        String bank = upiId.substring(at);
        StringBuilder sb = new StringBuilder();
        sb.append(handle, 0, Math.min(2, handle.length()));
        sb.append("****");
        sb.append(bank);
        return sb.toString();
    }

    public static int getTotalUsers() {
        return totalUsers;
    }

    public String getUpiId() { return upiId; }
    public String getName() { return name; }
    public String getPinHash() { return pinHash; }
    public double getBalance() { return balance; }
    public int getFailedAttempts() { return failedAttempts; }
    public boolean isLocked() { return locked; }
    public double getDailySpent() { return dailySpent; }

    public void setName(String name) { this.name = name; }
    public void setBalance(double balance) { this.balance = balance; }
    public void setDailySpent(double dailySpent) { this.dailySpent = dailySpent; }

    @Override
    public String toString() {
        return String.format("%s[%s, %s, balance=%.2f, locked=%b]",
                getUserType(), getMaskedUpi(), name, balance, locked);
    }

}
