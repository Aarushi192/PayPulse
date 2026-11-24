package paypulse.model;

import java.time.LocalDateTime;

public class P2PTransaction extends Transaction {

    public P2PTransaction(String senderUpi, String receiverUpi, double amount) {
        super(senderUpi, receiverUpi, amount);
    }

    public P2PTransaction(String senderUpi, String receiverUpi, double amount, String note) {
        super(senderUpi, receiverUpi, amount, note);
    }

    /** Rebuilds a stored payment (used by TransactionRepository). */
    public P2PTransaction(int id, String senderUpi, String receiverUpi, double amount, String note,
                          LocalDateTime timestamp, TransactionStatus status) {
        super(id, senderUpi, receiverUpi, amount, note, timestamp, status);
    }

    @Override
    public double calculateFee() { return 0.0; }

    @Override
    public String getTxnType() { return "P2P"; }
}