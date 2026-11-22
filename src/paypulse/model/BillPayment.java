package paypulse.model;

public class BillPayment extends Transaction {

    private static final double FEE_RATE = 0.01;
    private static final double MAX_FEE = 20.0;

    private final String billerName;

    public BillPayment(String senderUpi, String receiverUpi, double amount, String billerName) {
        super(senderUpi, receiverUpi, amount, "Bill: " + billerName);
        this.billerName = billerName;
    }

    @Override
    public double calculateFee() { return Math.min(getAmount() * FEE_RATE, MAX_FEE); }

    @Override
    public String getTxnType() { return "BILL"; }

    public String getBillerName() { return billerName; }
}