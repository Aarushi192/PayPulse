package paypulse.service;

import paypulse.model.Transaction;
import paypulse.util.ReceiptFormatter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Person B, file I/O: writes the payment history to files and reads it back. */
public class StatementExporter {

    private static final String HEADER = "id,type,from,to,amount,fee,status,time,note";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Writes one CSV line per payment. try-with-resources closes the file even if something fails. */
    public int exportCsv(List<Transaction> payments, File file) throws IOException {
        makeParentFolder(file);
        try (BufferedWriter out = new BufferedWriter(new FileWriter(file))) {
            out.write(HEADER);
            out.newLine();
            for (Transaction t : payments) {
                out.write(t.getId() + "," + t.getTxnType() + "," + t.getSenderUpi() + "," + t.getReceiverUpi() + ","
                        + t.getAmount() + "," + t.calculateFee() + "," + t.getStatus() + ","
                        + t.getTimestamp().format(TIME) + "," + quote(t.getNote()));
                out.newLine();
            }
        }
        return payments.size();
    }

    /** A human-readable statement: the same table the menu shows, plus a total line. */
    public void exportText(List<Transaction> payments, File file) throws IOException {
        makeParentFolder(file);
        try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
            out.println("PAYPULSE STATEMENT");
            out.println(ReceiptFormatter.tableHeader());
            double total = 0;
            for (Transaction t : payments) {
                out.println(ReceiptFormatter.tableRow(t));
                total += t.getAmount();
            }
            out.printf("%d payment(s), total amount %.2f%n", payments.size(), total);
        }
    }

    /** Reads a CSV written by exportCsv: one String[] of 9 fields per payment (header skipped). */
    public List<String[]> readCsv(File file) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            String line = in.readLine();                   // the header line
            while ((line = in.readLine()) != null) {
                if (!line.isBlank()) {
                    rows.add(split(line));
                }
            }
        }
        return rows;
    }

    /** Adds one line at the end of a text file (FileWriter in append mode). */
    public void appendLine(File file, String line) throws IOException {
        makeParentFolder(file);
        try (FileWriter out = new FileWriter(file, true)) {
            out.write(line + System.lineSeparator());
        }
    }

    // ---------- helpers ----------
    private void makeParentFolder(File file) throws IOException {
        File folder = file.getAbsoluteFile().getParentFile();
        if (folder != null && !folder.exists() && !folder.mkdirs()) {
            throw new IOException("Cannot create folder " + folder);
        }
    }

    /** A note may contain commas, so it is wrapped in quotes and inner quotes are doubled. */
    private String quote(String text) {
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    /** Splits a CSV line into 9 fields; only the last field (note) can be quoted. */
    private String[] split(String line) {
        String[] parts = line.split(",", 9);
        String note = parts[8];
        if (note.length() >= 2 && note.startsWith("\"") && note.endsWith("\"")) {
            note = note.substring(1, note.length() - 1).replace("\"\"", "\"");
        }
        parts[8] = note;
        return parts;
    }
}
