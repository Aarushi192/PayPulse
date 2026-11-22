package paypulse.util;

import paypulse.model.Transaction;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class ReceiptFormatter {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");

    private ReceiptFormatter() { }

    public static String receipt(Transaction t) {
        StringBuilder sb = new StringBuilder();
        sb.append("======== PAYPULSE RECEIPT ========\n");
        sb.append(String.format("%-8s: %d%n", "Txn ID", t.getId()));
        sb.append(String.format("%-8s: %s%n", "Type", t.getTxnType()));
        sb.append(String.format("%-8s: %s%n", "From", t.getSenderUpi()));
        sb.append(String.format("%-8s: %s%n", "To", t.getReceiverUpi()));
        sb.append(String.format("%-8s: %10.2f%n", "Amount", t.getAmount()));
        sb.append(String.format("%-8s: %10.2f%n", "Fee", t.calculateFee()));
        sb.append(String.format("%-8s: %10.2f%n", "Total", t.totalDebit()));
        if (!t.getNote().isEmpty()) {
            sb.append(String.format("%-8s: %s%n", "Note", t.getNote()));
        }
        sb.append(String.format("%-8s: %s%n", "Time", t.getTimestamp().format(TIME_FORMAT)));
        sb.append(String.format("%-8s: %s%n", "Status", t.getStatus()));
        sb.append("==================================");
        return sb.toString();
    }

    public static String tableHeader() {
        return String.format("%-4s %-5s %-12s %-12s %9s %-8s", "ID", "Type", "From", "To", "Amount", "Status")
                + "\n" + "-".repeat(55);
    }

    public static String tableRow(Transaction t) {
        return String.format("%-4d %-5s %-12s %-12s %9.2f %-8s",
                t.getId(), t.getTxnType(), t.getSenderUpi(), t.getReceiverUpi(),
                t.getAmount(), t.getStatus());
    }

    public static List<Transaction> searchNotes(List<Transaction> payments, String keyword) {
        List<Transaction> found = new ArrayList<>();
        String key = keyword.toLowerCase();
        for (Transaction t : payments) {
            if (t.getNote().toLowerCase().indexOf(key) >= 0) {
                found.add(t);
            }
        }
        return found;
    }

    // Strings are immutable: replace() returns a NEW String, the original note is unchanged
    public static String maskWord(String note, String word) {
        return note.replace(word, "*".repeat(word.length()));
    }
}