package paypulse.ui;

import paypulse.exception.DailyLimitExceededException;
import paypulse.exception.InsufficientBalanceException;
import paypulse.exception.InvalidAmountException;
import paypulse.exception.InvalidUPIException;
import paypulse.model.BillPayment;
import paypulse.model.Transaction;
import paypulse.model.User;
import paypulse.service.MonthlySpendTable;
import paypulse.service.PaymentCommandParser;
import paypulse.service.UserService;
import paypulse.util.PaymentLog;
import paypulse.util.ReceiptFormatter;
import paypulse.util.ValidationUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.TreeSet;

public class PaymentMenu {

    private final UserService service;
    private final User sender;                       // the logged-in user
    private final Scanner sc;
    private final List<Transaction> history = new ArrayList<>();
    private final MonthlySpendTable spendTable;

    public PaymentMenu(UserService service, User sender, Scanner sc) {
        this.service = service;
        this.sender = sender;
        this.sc = sc;
        this.spendTable = new MonthlySpendTable(new ArrayList<>(new TreeSet<>(service.getUpiIds())));
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
                System.out.println("7. Exit");
                String choice = ask("Choose: ");
                switch (choice) {
                    case "1" -> sendMoney();
                    case "2" -> payBill();
                    case "3" -> showHistory();
                    case "4" -> showReceipt();
                    case "5" -> searchNotes();
                    case "6" -> System.out.println("\n" + spendTable.render());
                    case "7" -> {
                        System.out.println("Goodbye!");
                        return;
                    }
                    default -> System.out.println("Please enter a number from 1 to 7.");
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println("\nInput closed. Goodbye!");
        }
    }

    // ---------------- Send money (typed command) ----------------
    private void sendMoney() {
        System.out.println("\n--- Send money ---");
        String command = ask("Type: send <amount> to <upi> [note]  > ");
        try {
            Transaction t = PaymentCommandParser.parse(sender.getUpiId(), command);
            execute(t);
        } catch (InvalidUPIException | InvalidAmountException | IllegalArgumentException e) {
            System.out.println("Invalid input: " + e.getMessage());
        }
    }

    // ---------------- Pay a bill ----------------
    private void payBill() {
        System.out.println("\n--- Pay a bill ---");
        String biller = ask("Biller UPI ID (e.g. bescom@upi): ");
        String billerName = ask("Bill for (e.g. Electricity): ");
        String rawAmount = ask("Amount: ");
        try {
            ValidationUtil.validateUpi(biller);
            double amount = ValidationUtil.parseAmount(rawAmount);
            execute(new BillPayment(sender.getUpiId(), biller, amount, billerName));
        } catch (InvalidUPIException | IllegalArgumentException e) {
            System.out.println("Invalid input: " + e.getMessage());
        }
    }


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
            service.saveUser(sender);
            service.saveUser(receiver);
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
            history.add(t);                           // finally: every attempt is kept
            PaymentLog.add(t.getTxnType() + " " + t.getSenderUpi() + " -> " + t.getReceiverUpi()
                    + " " + t.getStatus());
        }
    }

    // ---------------- History, receipt, search ----------------
    private void showHistory() {
        System.out.println("\n--- Payment history ---");
        if (history.isEmpty()) {
            System.out.println("No payments yet.");
            return;
        }
        System.out.println(ReceiptFormatter.tableHeader());
        for (Transaction t : history) {
            System.out.println(ReceiptFormatter.tableRow(t));
        }
    }

    private void showReceipt() {
        String rawId = ask("Payment ID: ");
        try {
            int id = Integer.parseInt(rawId);
            for (Transaction t : history) {
                if (t.getId() == id) {
                    System.out.println("\n" + ReceiptFormatter.receipt(t));
                    return;
                }
            }
            System.out.println("No payment with ID " + id + " in this session.");
        } catch (NumberFormatException e) {
            System.out.println("The ID must be a number.");
        }
    }

    private void searchNotes() {
        String keyword = ask("Search notes for: ");
        List<Transaction> found = ReceiptFormatter.searchNotes(history, keyword);
        if (found.isEmpty()) {
            System.out.println("No notes contain \"" + keyword + "\".");
            return;
        }
        System.out.println(ReceiptFormatter.tableHeader());
        for (Transaction t : found) {
            System.out.println(ReceiptFormatter.tableRow(t));
        }
    }


    private String ask(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }
}