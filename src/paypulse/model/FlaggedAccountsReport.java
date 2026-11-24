package paypulse.model;

import java.util.List;
import java.util.Set;

/** Lists every flag and the accounts that were flagged. */
public class FlaggedAccountsReport extends Report {

    private final List<FraudFlag> flags;
    private final Set<String> accounts;

    public FlaggedAccountsReport(List<FraudFlag> flags, Set<String> accounts) {
        super("Flagged Accounts Report");
        this.flags = flags;
        this.accounts = accounts;
    }

    @Override
    public String generate() {
        StringBuilder sb = new StringBuilder();
        sb.append(title).append("\n");
        sb.append("=".repeat(50)).append("\n");
        sb.append(String.format("%-6s %-8s %s%n", "Txn", "Risk", "Reason"));
        sb.append("-".repeat(50)).append("\n");
        for (FraudFlag f : flags) {
            sb.append(String.format("%-6d %-8s %s%n", f.getTransactionId(),
                    FraudFlag.riskLabel(f.getRiskScore()), f.getReason()));
        }
        sb.append("-".repeat(50)).append("\n");
        sb.append("Flagged accounts: ");
        for (String a : accounts) {
            sb.append(a).append(",");
        }
        if (!accounts.isEmpty()) {
            sb.deleteCharAt(sb.length() - 1);        // remove the last extra comma
        }
        return sb.toString();
    }

    @Override
    public String toCsv() {
        StringBuilder sb = new StringBuilder("transaction_id,risk,reason\n");
        for (FraudFlag f : flags) {
            sb.append(f.getTransactionId()).append(",")
              .append(FraudFlag.riskLabel(f.getRiskScore())).append(",")
              .append(f.getReason()).append("\n");
        }
        return sb.toString();
    }
}
