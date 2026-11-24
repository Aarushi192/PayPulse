package paypulse.model;

import java.time.LocalDateTime;

/** One fraud alert for one transaction. Natural order = highest risk first (used by the PriorityQueue). */
public class FraudFlag implements Comparable<FraudFlag> {

    // Constants for the risk levels
    public static final int LOW = 1;
    public static final int MEDIUM = 2;
    public static final int HIGH = 3;

    private static int totalFlags = 0;          // static: one counter shared by all flags

    private int id;                             // 0 until the flag is saved in MySQL
    private final int transactionId;
    private final String reason;
    private final int riskScore;
    private final LocalDateTime flaggedAt;

    // Overloaded constructors chained with this(...)
    /** A brand-new flag (no database id yet). */
    public FraudFlag(int transactionId, String reason, int riskScore) {
        this(0, transactionId, reason, riskScore);
    }

    public FraudFlag(int id, int transactionId, String reason, int riskScore) {
        this(id, transactionId, reason, riskScore, LocalDateTime.now());
    }

    /** Full constructor (also used when loading rows from MySQL). */
    public FraudFlag(int id, int transactionId, String reason, int riskScore, LocalDateTime flaggedAt) {
        this.id = id;
        this.transactionId = transactionId;
        this.reason = reason;
        this.riskScore = riskScore;
        this.flaggedAt = flaggedAt;
        totalFlags++;
    }

    public static int getTotalFlags() { return totalFlags; }

    /** 1 -> LOW, 2 -> MEDIUM, 3 -> HIGH. */
    public static String riskLabel(int score) {
        switch (score) {
            case LOW:    return "LOW";
            case MEDIUM: return "MEDIUM";
            default:     return "HIGH";
        }
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getTransactionId() { return transactionId; }
    public String getReason() { return reason; }
    public int getRiskScore() { return riskScore; }
    public LocalDateTime getFlaggedAt() { return flaggedAt; }

    /** Higher risk comes FIRST. Same risk: smaller transaction ID first. */
    @Override
    public int compareTo(FraudFlag other) {
        int byRisk = Integer.compare(other.riskScore, this.riskScore);
        return byRisk != 0 ? byRisk : Integer.compare(this.transactionId, other.transactionId);
    }

    @Override
    public String toString() {
        return String.format("Flag#%d txn=%d risk=%s (%s)", id, transactionId, riskLabel(riskScore), reason);
    }
}
