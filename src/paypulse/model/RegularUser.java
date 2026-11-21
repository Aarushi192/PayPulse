package paypulse.model;

public class RegularUser extends User {

    public RegularUser(String upiId, String name, String pin) {
        super(upiId, name, pin);
    }

    public RegularUser(String upiId, String name, String pin, double balance) {
        super(upiId, name, pin, balance);
    }

    public RegularUser(String upiId, String name, String pin, double balance,
                       int failedAttempts, boolean locked, double dailySpent) {
        super(upiId, name, pin, balance, failedAttempts, locked, dailySpent);
    }

    @Override
    public double getDailyLimit() {
        return 50_000.0;
    }

    @Override
    public String getUserType() {
        return "REGULAR";
    }
}
