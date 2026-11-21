package paypulse.model;

import paypulse.exception.InsufficientBalanceException;
import paypulse.exception.InvalidAmountException;

import java.time.LocalDateTime;

public abstract class Transaction implements Processable {

    private static int nextId = 1;        // made thread-safe later (synchronization topic)
    private static int totalCount = 0;

    private final int id;
    private final String senderUpi;
    private final String receiverUpi;
    private final double amount;
    private final String note;
    private final LocalDateTime timestamp;
    private TransactionStatus status;

    protected Transaction(String senderUpi, String receiverUpi, double amount, String note) {
        this.id = nextId++;
        this.senderUpi = senderUpi;
        this.receiverUpi = receiverUpi;
        this.amount = amount;
        this.note = note;
        this.timestamp = LocalDateTime.now();
        this.status = TransactionStatus.PENDING;
        totalCount++;
    }

    protected Transaction(String senderUpi, String receiverUpi, double amount) {
        this(senderUpi, receiverUpi, amount, "");
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
    public void process(User sender, User receiver) throws InsufficientBalanceException {
        try {
            validate();
        } catch (InvalidAmountException e) {
            status = TransactionStatus.FAILED;
            throw e;
        }
        try {
            sender.withdraw(totalDebit());      // sender pays amount + fee
        } catch (InsufficientBalanceException e) {
            status = TransactionStatus.FAILED;
            throw e;
        }
        receiver.deposit(amount);               // receiver gets the amount, not the fee
        status = TransactionStatus.SUCCESS;
    }

    public static int getTotalCount() { return totalCount; }

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