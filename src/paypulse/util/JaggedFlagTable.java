package paypulse.util;

import paypulse.model.FraudFlag;

import java.util.List;
import java.util.Map;

/** A jagged 2D array: one row per user, and each row has as many columns as that user has flags. */
public class JaggedFlagTable {

    private final String[] users;
    private final int[][] scores;

    public JaggedFlagTable(Map<String, List<FraudFlag>> flagsByUser) {
        users = new String[flagsByUser.size()];
        scores = new int[flagsByUser.size()][];          // only the rows exist now; every row is created below
        int row = 0;
        for (Map.Entry<String, List<FraudFlag>> entry : flagsByUser.entrySet()) {
            users[row] = entry.getKey();
            List<FraudFlag> list = entry.getValue();
            scores[row] = new int[list.size()];          // each row can have a different length
            for (int col = 0; col < list.size(); col++) {
                scores[row][col] = list.get(col).getRiskScore();
            }
            row++;
        }
    }

    public String render() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-16s %-18s %s%n", "User", "Risk scores", "Total"));
        sb.append("-".repeat(40)).append("\n");
        for (int i = 0; i < scores.length; i++) {
            int total = 0;
            StringBuilder row = new StringBuilder();
            for (int value : scores[i]) {
                total += value;
                row.append(value).append(" ");
            }
            sb.append(String.format("%-16s %-18s %d%n", users[i], row.toString().trim(), total));
        }
        if (scores.length == 0) {
            sb.append("No flags yet.\n");
        }
        return sb.toString();
    }
}
