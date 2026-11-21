package paypulse.model;

import paypulse.exception.AccountLockedException;
import paypulse.exception.InsufficientBalanceException;
import paypulse.exception.InvalidPinException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Abstract base class for every account in PayPulse. */
public abstract class User implements Authenticatable, Comparable<User> {

    public static final int MAX_FAILED_ATTEMPTS = 3;
    private static int totalUsers = 0;

    private String upiId;
    private String name;
    private String pinHash;          // plain PIN for now; hashing is an optional extra
    private double balance;
    private int failedAttempts;
    private boolean locked;
    private double dailySpent;
    private final List<String> contacts = new ArrayList<>();   // saved contacts (UPI IDs)

    // Overloaded constructors chained with this(...)
    public User(String upiId, String name, String pin) {
        this(upiId, name, pin, 0.0);
    }

    public User(String upiId, String name, String pin, double balance) {
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

    // ---------------- Authentication ----------------
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

    // ---------------- Wallet (Phase 3: synchronized) ----------------
    /** Person B calls this when crediting the receiver. */
    public synchronized void deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        }
        balance += amount;
    }

    /** Person B calls this when debiting the sender. Also records daily spending. */
    public synchronized void withdraw(double amount) throws InsufficientBalanceException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        }
        if (amount > balance) {
            throw new InsufficientBalanceException(
                    "Balance " + balance + " is less than " + amount);
        }
        balance -= amount;
        dailySpent += amount;
    }

    public synchronized void resetDailySpent() {
        dailySpent = 0;
    }

    // ---------------- Contacts ----------------
    public void addContact(String contactUpi) {
        if (!contacts.contains(contactUpi)) {
            contacts.add(contactUpi);
        }
    }

    public List<String> getContacts() {
        return Collections.unmodifiableList(contacts);
    }

    // ---------------- Helpers ----------------
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

    /** Natural order: by name (ignoring case), then UPI ID so two equal names still differ. */
    @Override
    public int compareTo(User other) {
        int byName = this.name.compareToIgnoreCase(other.name);
        return byName != 0 ? byName : this.upiId.compareTo(other.upiId);
    }

    public String getUpiId() { return upiId; }
    public String getName() { return name; }
    public String getPinHash() { return pinHash; }
    public synchronized double getBalance() { return balance; }
    public int getFailedAttempts() { return failedAttempts; }
    public boolean isLocked() { return locked; }
    public synchronized double getDailySpent() { return dailySpent; }

    public void setName(String name) { this.name = name; }
    public synchronized void setBalance(double balance) { this.balance = balance; }
    public synchronized void setDailySpent(double dailySpent) { this.dailySpent = dailySpent; }

    @Override
    public String toString() {
        return String.format("%s[%s, %s, balance=%.2f, locked=%b]",
                getUserType(), getMaskedUpi(), name, getBalance(), locked);
    }
}
