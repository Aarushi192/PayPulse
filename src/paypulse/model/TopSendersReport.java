package paypulse.model;

import paypulse.util.FraudStats;

import java.util.List;
import java.util.Map;

/** The senders who moved the most money (built with streams in FraudStats). */
public class TopSendersReport extends Report {

    private final List<Map.Entry<String, Double>> top;

    public TopSendersReport(List<Transaction> transactions, int n) {
        super("Top Senders Report");
        this.top = FraudStats.topSenders(transactions, n);
    }

    @Override
    public String generate() {
        StringBuilder sb = new StringBuilder();
        sb.append(title).append("\n");
        sb.append("=".repeat(36)).append("\n");
        sb.append(String.format("%-4s %-18s %12s%n", "No.", "Sender", "Total sent"));
        sb.append("-".repeat(36)).append("\n");
        int rank = 1;
        for (Map.Entry<String, Double> e : top) {
            sb.append(String.format("%-4d %-18s %12.2f%n", rank++, e.getKey(), e.getValue()));
        }
        if (top.isEmpty()) {
            sb.append("No transactions yet.\n");
        }
        return sb.toString();
    }

    @Override
    public String toCsv() {
        StringBuilder sb = new StringBuilder("sender,total_sent\n");
        for (Map.Entry<String, Double> e : top) {
            sb.append(e.getKey()).append(",").append(String.format("%.2f", e.getValue())).append("\n");
        }
        return sb.toString();
    }
}
