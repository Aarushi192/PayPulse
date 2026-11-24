package paypulse.ui;

import paypulse.exception.DailyLimitExceededException;
import paypulse.exception.InsufficientBalanceException;
import paypulse.exception.InvalidAmountException;
import paypulse.exception.InvalidUPIException;
import paypulse.model.BillPayment;
import paypulse.model.P2PTransaction;
import paypulse.model.Transaction;
import paypulse.model.User;
import paypulse.repository.TransactionRepository;
import paypulse.service.BackupService;
import paypulse.service.HistoryService;
import paypulse.service.MonthlySpendTable;
import paypulse.service.PaymentAnalytics;
import paypulse.service.StatementExporter;
import paypulse.service.UserService;
import paypulse.service.FraudHub;
import paypulse.util.PaymentLog;
import paypulse.util.ReceiptFormatter;
import paypulse.util.ValidationUtil;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.TreeSet;




/** Console menu for Person B: send money, pay bills, history, receipts, monthly spend. */
public class PaymentMenu {


    private final UserService service;
    private final User sender;                       // the logged-in user
    private final Scanner sc;
    private final HistoryService history = new HistoryService();
    private final MonthlySpendTable spendTable;
    private final TransactionRepository txnRepo = new TransactionRepository();   // used only when MySQL is connected

    public PaymentMenu(UserService service, User sender, Scanner sc) {
        this.service = service;
        this.sender = sender;
        this.sc = sc;
        this.spendTable = new MonthlySpendTable(new ArrayList<>(new TreeSet<>(service.getUpiIds())));
        if (!service.isMemoryOnly()) {
            try {
                txnRepo.syncIdCounter();             // new payment IDs continue after the stored ones
            } catch (SQLException e) {
                System.out.println("Could not read stored payments: " + e.getMessage());
            }
        }
    }

    // ---------------- Main menu ----------------
    public void run() {
        try {
            while (true) {
                System.out.println("\n=== PayPulse: Payments (" + sender.getMaskedUpi() + ") ===");
                System.out.printf("Balance: %.2f%n", sender.getBalance());
                System.out.println("1. Send money");
                System.out.println("2. Pay a bill");
                System.out.println("3. Payment history");
                System.out.println("4. Show a receipt");
                System.out.println("5. Search notes");
                System.out.println("6. Monthly spend table");
                System.out.println("7. Recent receivers");
                System.out.println("8. Queue a payment for later");
                System.out.println("9. Send queued payments");
                System.out.println("10. Remove failed payments from history");
                System.out.println("11. Statistics");
                System.out.println("12. Export statement to a file");
                System.out.println("13. Exit");
                String choice = ask("Choose: ");
                switch (choice) {
                    case "1" -> sendMoney();
                    case "2" -> payBill();
                    case "3" -> showHistory();
                    case "4" -> showReceipt();
                    case "5" -> searchNotes();
                    case "6" -> System.out.println("\n" + spendTable.render());
                    case "7" -> showRecentReceivers();
                    case "8" -> queuePayment();
                    case "9" -> sendQueued();
                    case "10" -> System.out.println("Removed " + history.removeFailed() + " failed payment(s).");
                    case "11" -> showStatistics();
                    case "12" -> exportStatement();
                    case "13" -> {
                        System.out.println("Goodbye!");
                        return;
                    }
                    default -> System.out.println("Please enter a number from 1 to 13.");
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println("\nInput closed. Goodbye!");
        }
    }

    // ---------------- Send money (asks: amount, then UPI ID, then an optional message) ----------------
    private void sendMoney() {
        System.out.println("\n--- Send money --- (type 'cancel' at any question to go back)");
        Transaction t = askP2P();
        if (t != null) {
            execute(t);
        }
    }

    /** Asks the three questions and builds the payment. Returns null if the user cancels. */
    private Transaction askP2P() {
        Double amount = askAmount();
        if (amount == null) {
            return null;
        }
        User receiver = askReceiver("Send to (UPI ID): ");
        if (receiver == null) {
            return null;
        }
        String note = "";
        if (askYesNo("Add a message? (y/n): ")) {
            note = ask("Message: ");
        }
        return new P2PTransaction(sender.getUpiId(), receiver.getUpiId(), amount, note);
    }

    // ---------------- Queue: send later ----------------
    private void queuePayment() {
        System.out.println("\n--- Queue a payment --- (type 'cancel' at any question to go back)");
        Transaction t = askP2P();
        if (t != null) {
            history.queuePending(t);
            System.out.println("Queued. Payments waiting: " + history.pendingCount());
        }
    }

    private void sendQueued() {
        if (history.pendingCount() == 0) {
            System.out.println("No queued payments.");
            return;
        }
        Transaction t;
        while ((t = history.nextPending()) != null) {        // oldest first
            System.out.println("\nSending queued payment #" + t.getId() + " ...");
            execute(t);
        }
    }

    private void showRecentReceivers() {
        List<String> recent = history.recentReceivers(5);
        if (recent.isEmpty()) {
            System.out.println("You have not paid anyone yet.");
            return;
        }
        System.out.println("\nRecent receivers (latest first):");
        for (String upi : recent) {
            System.out.println(" - " + upi);
        }
    }

    // ---------------- Pay a bill (asks: amount, then biller UPI ID, then what the bill is for) ----------------
    private void payBill() {
        System.out.println("\n--- Pay a bill --- (type 'cancel' at any question to go back)");
        Double amount = askAmount();
        if (amount == null) {
            return;
        }
        User biller = askReceiver("Biller UPI ID (e.g. bescom@upi): ");
        if (biller == null) {
            return;
        }
        String billerName = "";
        while (billerName.isEmpty()) {
            billerName = ask("Bill for (e.g. Electricity): ");
        }
        execute(new BillPayment(sender.getUpiId(), biller.getUpiId(), amount, billerName));
    }

    // ---------------- Input helpers with validation loops ----------------
    /** Keeps asking until the amount is a number above zero. Returns null if the user cancels. */
    private Double askAmount() {
        while (true) {
            String raw = ask("Amount: ");
            if (raw.equalsIgnoreCase("cancel")) {
                return null;
            }
            try {
                return ValidationUtil.parseAmount(raw);          // double is boxed into a Double
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid amount: " + e.getMessage());
            }
        }
    }

    /** Keeps asking until the UPI ID is valid and belongs to another user. Returns null if cancelled. */
    private User askReceiver(String prompt) {
        while (true) {
            String upi = ask(prompt);
            if (upi.equalsIgnoreCase("cancel")) {
                return null;
            }
            try {
                ValidationUtil.validateUpi(upi);
            } catch (InvalidUPIException e) {
                System.out.println("Invalid UPI ID: " + e.getMessage());
                continue;
            }
            User receiver = service.getUser(upi);
            if (receiver == null) {
                System.out.println("No such user: " + upi);
            } else if (receiver == sender) {
                System.out.println("You cannot pay yourself.");
            } else {
                System.out.println("Paying " + receiver.getName() + " (" + receiver.getMaskedUpi() + ")");
                return receiver;
            }
        }
    }

    private boolean askYesNo(String prompt) {
        while (true) {
            String answer = ask(prompt).toLowerCase();
            if (answer.equals("y") || answer.equals("yes")) {
                return true;
            }
            if (answer.equals("n") || answer.equals("no")) {
                return false;
            }
            System.out.println("Please answer y or n.");
        }
    }

    /** Carries out a payment and keeps a record of the attempt, even when it fails. */
    private void execute(Transaction t) {
        User receiver = service.getUser(t.getReceiverUpi());
        if (receiver == null) {
            System.out.println("No such user: " + t.getReceiverUpi());
            return;
        }
        if (receiver == sender) {
            System.out.println("You cannot pay yourself.");
            return;
        }
        try {
            t.process(sender, receiver);
            spendTable.add(t);
            System.out.println("Payment successful.\n");
            System.out.println(ReceiptFormatter.receipt(t));
        } catch (InsufficientBalanceException | DailyLimitExceededException e) {
            System.out.println("Payment failed: " + e.getMessage());
        } catch (InvalidAmountException e) {
            System.out.println("Invalid amount: " + e.getMessage());
        } catch (IllegalStateException e) {
            System.out.println("Database problem: " + e.getMessage());
        } finally {
        boolean saved = saveToDatabase(t, receiver);
        history.record(t);

        if (saved) {
            FraudHub.report(t);
        }

        PaymentLog.add(t.getTxnType() + " " + t.getSenderUpi() + " -> " + t.getReceiverUpi()
                + " " + t.getStatus());
    }
    }

    /** Stores the payment and both balances in MySQL as one all-or-nothing step. */
    private boolean saveToDatabase(Transaction t, User receiver) {
        if (service.isMemoryOnly()) {
            return true;
        }

        try {
            txnRepo.savePayment(t, sender, receiver);
            return true;
        } catch (SQLException e) {
            System.out.println("Could not save payment #" + t.getId()
                    + " to the database: " + e.getMessage());
            return false;
        }
    }

    // ---------------- Statistics (streams) and export (files) ----------------
    private void showStatistics() {
        List<Transaction> all = history.getAll();
        if (all.isEmpty()) {
            System.out.println("No payments yet.");
            return;
        }
        PaymentAnalytics stats = new PaymentAnalytics(all);
        System.out.println("\n--- Statistics ---");
        System.out.printf("Total sent     : %.2f%n", stats.totalSent());
        System.out.printf("Fees paid      : %.2f%n", stats.totalFees());
        System.out.printf("Average amount : %.2f%n", stats.averageAmount().orElse(0));
        System.out.println("Biggest        : " + stats.biggest().map(Transaction::toString).orElse("none"));
        System.out.println("Per receiver   : " + stats.sentPerReceiver());
        System.out.println("By type        : " + stats.countByType());
        System.out.println("Success/other  : " + stats.successVersusOthers());
    }

    private void exportStatement() {
        if (history.size() == 0) {
            System.out.println("No payments to export.");
            return;
        }
        String handle = sender.getUpiId().substring(0, sender.getUpiId().indexOf('@'));
        File csv = new File("statements", handle + "_statement.csv");
        File txt = new File("statements", handle + "_statement.txt");
        StatementExporter exporter = new StatementExporter();
        try {
            int rows = exporter.exportCsv(history.getAll(), csv);
            exporter.exportText(history.getAll(), txt);
            File copy = new BackupService(new File("backups")).backup(csv);
            System.out.println("Saved " + rows + " payment(s) to " + csv.getPath() + " and " + txt.getPath());
            System.out.println("Backup copy: " + copy.getPath());
        } catch (IOException e) {
            System.out.println("Could not write the statement: " + e.getMessage());
        }
    }

    // ---------------- History, receipt, search ----------------
    private void showHistory() {
        System.out.println("\n--- Payment history ---");
        if (history.size() == 0) {
            System.out.println("No payments yet.");
            return;
        }
        System.out.println("1. Oldest first  2. Newest first  3. Biggest amount first");
        List<Transaction> list;
        switch (ask("Sort by (1-3, default 1): ")) {
            case "2" -> list = history.newestFirst();
            case "3" -> list = history.sortedByAmount();
            default -> list = history.oldestFirst();
        }
        System.out.println(ReceiptFormatter.tableHeader());
        for (Transaction t : list) {
            System.out.println(ReceiptFormatter.tableRow(t));
        }
    }

    private void showReceipt() {
        String rawId = ask("Payment ID: ");
        try {
            int id = Integer.parseInt(rawId);
            Transaction t = history.findById(id);             // HashMap lookup
            if (t == null) {
                System.out.println("No payment with ID " + id + " in this session.");
            } else {
                System.out.println("\n" + ReceiptFormatter.receipt(t));
            }
        } catch (NumberFormatException e) {
            System.out.println("The ID must be a number.");
        }
    }

    private void searchNotes() {
        String keyword = ask("Search notes for: ");
        List<Transaction> found = ReceiptFormatter.searchNotes(history.getAll(), keyword);
        if (found.isEmpty()) {
            System.out.println("No notes contain \"" + keyword + "\".");
            return;
        }
        System.out.println(ReceiptFormatter.tableHeader());
        for (Transaction t : found) {
            System.out.println(ReceiptFormatter.tableRow(t));
        }
    }

    // ---------------- Input helper ----------------
    private String ask(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }
}