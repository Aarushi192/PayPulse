package paypulse.model;

public class MerchantUser extends User {
    public MerchantUser(String upiId, String name, String pin) {
        super(upiId, name, pin);
    }

    public MerchantUser(String upiId, String name, String pin, double balance) {
        super(upiId, name, pin, balance);
    }

    public MerchantUser(String upiId, String name, String pin, double balance,
                        int failedAttempts, boolean locked, double dailySpent) {
        super(upiId, name, pin, balance, failedAttempts, locked, dailySpent);
    }

    @Override
    public double getDailyLimit() {
        return 200_000.0;
    }

    @Override
    public String getUserType() {
        return "MERCHANT";
    }
}
