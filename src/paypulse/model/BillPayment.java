package paypulse.model;

import java.time.LocalDateTime;

public class BillPayment extends Transaction {

    private static final double FEE_RATE = 0.01;
    private static final double MAX_FEE = 20.0;
    private static final String NOTE_PREFIX = "Bill: ";

    private final String billerName;

    public BillPayment(String senderUpi, String receiverUpi, double amount, String billerName) {
        super(senderUpi, receiverUpi, amount, NOTE_PREFIX + billerName);
        this.billerName = billerName;
    }

    /** Rebuilds a stored bill payment (used by TransactionRepository). */
    public BillPayment(int id, String senderUpi, String receiverUpi, double amount, String note,
                       LocalDateTime timestamp, TransactionStatus status) {
        super(id, senderUpi, receiverUpi, amount, note, timestamp, status);
        this.billerName = note.startsWith(NOTE_PREFIX) ? note.substring(NOTE_PREFIX.length()) : note;
    }

    @Override
    public double calculateFee() { return Math.min(getAmount() * FEE_RATE, MAX_FEE); }

    @Override
    public String getTxnType() { return "BILL"; }

    public String getBillerName() { return billerName; }
}