package paypulse.model;
import paypulse.exception.DailyLimitExceededException;
import paypulse.exception.InsufficientBalanceException;
import paypulse.exception.InvalidAmountException;

import java.time.LocalDateTime;

public abstract class Transaction implements Processable {

    // Shared by every thread, so access goes through synchronized static methods (one lock: the class).
    private static int nextId = 1;
    private static int totalCount = 0;

    private final int id;
    private final String senderUpi;
    private final String receiverUpi;
    private final double amount;
    private final String note;
    private final LocalDateTime timestamp;
    private volatile TransactionStatus status;   // volatile: other threads see the change straight away

    protected Transaction(String senderUpi, String receiverUpi, double amount, String note) {
        this.id = takeNextId();
        this.senderUpi = senderUpi;
        this.receiverUpi = receiverUpi;
        this.amount = amount;
        this.note = note;
        this.timestamp = LocalDateTime.now();
        this.status = TransactionStatus.PENDING;
        countOne();
    }

    protected Transaction(String senderUpi, String receiverUpi, double amount) {
        this(senderUpi, receiverUpi, amount, "");
    }

    /** Used when a payment is loaded back from MySQL: keeps its stored ID, time and status. */
    protected Transaction(int id, String senderUpi, String receiverUpi, double amount, String note,
                          LocalDateTime timestamp, TransactionStatus status) {
        this.id = id;
        this.senderUpi = senderUpi;
        this.receiverUpi = receiverUpi;
        this.amount = amount;
        this.note = note;
        this.timestamp = timestamp;
        this.status = status;
        ensureNextIdAbove(id);                   // new payments must never reuse a stored ID
    }

    /** Two threads can never get the same ID: only one thread at a time may run this method. */
    private static synchronized int takeNextId() {
        return nextId++;
    }

    /** Makes sure the next new payment gets an ID bigger than every ID already stored. */
    public static synchronized void ensureNextIdAbove(int existingId) {
        if (existingId >= nextId) {
            nextId = existingId + 1;
        }
    }

    private static synchronized void countOne() {
        totalCount++;
    }

    public abstract double calculateFee();

    public abstract String getTxnType();     // P2P or BILL, becomes txn_type in the database

    public double totalDebit() {
        return amount + calculateFee();
    }

    @Override
    public void validate() {
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero: " + amount);
        }
    }

    @Override
    public void process(User sender, User receiver)
            throws InsufficientBalanceException, DailyLimitExceededException {
        // Check-then-withdraw must be ONE step per sender, otherwise two threads can both pass the
        // daily-limit check before either has withdrawn. The lock is the sender object itself.
        // The receiver is credited OUTSIDE this block, so two payments in opposite directions
        // (A pays B while B pays A) never hold two locks at once and cannot deadlock.
        synchronized (sender) {
            try {
                validate();
                if (sender.getDailySpent() + totalDebit() > sender.getDailyLimit()) {
                    throw new DailyLimitExceededException(String.format(
                            "Daily limit %.2f would be exceeded (already spent %.2f)",
                            sender.getDailyLimit(), sender.getDailySpent()));
                }
                sender.withdraw(totalDebit());      // sender pays amount + fee
            } catch (InvalidAmountException | InsufficientBalanceException
                     | DailyLimitExceededException e) {      // multi-catch: same handling for all three
                status = TransactionStatus.FAILED;
                throw e;
            }
        }
        receiver.deposit(amount);               // receiver gets the amount, not the fee
        status = TransactionStatus.SUCCESS;
    }


    public static synchronized int getTotalCount() { return totalCount; }

    public int getId() { return id; }
    public String getSenderUpi() { return senderUpi; }
    public String getReceiverUpi() { return receiverUpi; }
    public double getAmount() { return amount; }
    public String getNote() { return note; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    @Override
    public String toString() {
        return String.format("#%d %s %s -> %s : %.2f (fee %.2f) [%s]",
                id, getTxnType(), senderUpi, receiverUpi, amount, calculateFee(), status);
    }
}