package paypulse.model;

public class P2PTransaction extends Transaction {

    public P2PTransaction(String senderUpi, String receiverUpi, double amount) {
        super(senderUpi, receiverUpi, amount);
    }

    public P2PTransaction(String senderUpi, String receiverUpi, double amount, String note) {
        super(senderUpi, receiverUpi, amount, note);
    }

    @Override
    public double calculateFee() { return 0.0; }

    @Override
    public String getTxnType() { return "P2P"; }
}
