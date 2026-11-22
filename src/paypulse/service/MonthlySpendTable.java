package paypulse.service;

import paypulse.model.Transaction;
import paypulse.model.TransactionStatus;

import java.util.List;

public class MonthlySpendTable {

    private static final String[] MONTHS =
            {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    private final String[] upiIds;
    private final double[][] spend;

    public MonthlySpendTable(List<String> users) {
        this.upiIds = users.toArray(new String[0]);
        this.spend = new double[upiIds.length][12];
    }


    public boolean record(String upiId, int month, double amount) {
        int row = rowOf(upiId);
        if (row < 0 || month < 1 || month > 12) {
            return false;
        }
        spend[row][month - 1] += amount;
        return true;
    }


    public boolean add(Transaction t) {
        if (t.getStatus() != TransactionStatus.SUCCESS) {
            return false;
        }
        return record(t.getSenderUpi(), t.getTimestamp().getMonthValue(), t.totalDebit());
    }

    private int rowOf(String upiId) {
        for (int r = 0; r < upiIds.length; r++) {
            if (upiIds[r].equals(upiId)) {
                return r;
            }
        }
        return -1;
    }

    public double rowTotal(int row) {
        double sum = 0;
        for (double value : spend[row]) {
            sum += value;
        }
        return sum;
    }


    public double columnTotal(int monthIndex) {
        double sum = 0;
        for (int r = 0; r < spend.length; r++) {
            sum += spend[r][monthIndex];
        }
        return sum;
    }

    public double grandTotal() {
        double sum = 0;
        for (int r = 0; r < spend.length; r++) {
            sum += rowTotal(r);
        }
        return sum;
    }


    public String render() {
        int width = 14 + 12 * 8 + 10;
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-14s", "User"));
        for (String month : MONTHS) {
            sb.append(String.format("%8s", month));
        }
        sb.append(String.format("%10s", "Total")).append('\n');
        sb.append("-".repeat(width)).append('\n');
        for (int r = 0; r < spend.length; r++) {
            sb.append(String.format("%-14s", upiIds[r]));
            for (int m = 0; m < 12; m++) {
                sb.append(String.format("%8.0f", spend[r][m]));
            }
            sb.append(String.format("%10.0f", rowTotal(r))).append('\n');
        }
        sb.append("-".repeat(width)).append('\n');
        sb.append(String.format("%-14s", "TOTAL"));
        for (int m = 0; m < 12; m++) {
            sb.append(String.format("%8.0f", columnTotal(m)));
        }
        sb.append(String.format("%10.0f", grandTotal()));
        return sb.toString();
    }
}