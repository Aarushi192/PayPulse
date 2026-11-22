package paypulse.ui;

import paypulse.exception.DailyLimitExceededException;
import paypulse.exception.InsufficientBalanceException;
import paypulse.exception.InvalidAmountException;
import paypulse.exception.InvalidUPIException;
import paypulse.model.BillPayment;
import paypulse.model.P2PTransaction;
import paypulse.model.Transaction;
import paypulse.model.User;
import paypulse.service.MonthlySpendTable;

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
    private final User sender;
    private final Scanner sc;
    private final List<Transaction> history = new ArrayList<>();
    private final MonthlySpendTable spendTable;

    public PaymentMenu(UserService service, User sender, Scanner sc) {
        this.service = service;
        this.sender = sender;
        this.sc = sc;
        this.spendTable = new MonthlySpendTable(new ArrayList<>(new TreeSet<>(service.getUpiIds())));
    }


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


    private void sendMoney() {
        System.out.println("\n--- Send money --- (type 'cancel' at any question to go back)");
        Double amount = askAmount();
        if(amount == null){
            return;
        }
        User receiver = askReceiver("Send to (UPI ID): ");
        if(receiver == null){
            return;
        }
        String note = "";
        if(askYesNo("Add a message? (y/n): ")){
            note = ask("Message: ");
        }
        execute(new P2PTransaction(sender.getUpiId(), receiver.getUpiId(),amount,note));


    }


    private void payBill() {
        System.out.println("\n--- Pay a bill ---(type 'cancel' at any question to go back)");
        Double amount = askAmount();
        if(amount == null){
            return;
        }
        User biller = askReceiver("Biller UPI ID (e.g bescom@upi): ");
        if(biller == null)
            return;
        String billerName = "";
        while(billerName.isEmpty()){
            billerName = ask("Bill for(e.g Electricity): ");
        }
        execute(new BillPayment(sender.getUpiId(), biller.getUpiId(),amount,billerName));
    }

    private Double askAmount(){
        while(true){
            String raw = ask("Amount: ");
            if(raw.equalsIgnoreCase("cancel")){
                return null;
            }
            try{
                return ValidationUtil.parseAmount(raw);
            }
            catch (IllegalArgumentException e){
                System.out.println("Invalid amount: "+e.getMessage());
            }
        }
    }

    private User askReceiver(String prompt){
        while(true){
            String upi = ask(prompt);
            if(upi.equalsIgnoreCase("cancel")){
                return null;
            }
            try{
                ValidationUtil.validateUpi(upi);
            }
            catch (InvalidUPIException e){
                System.out.println("Invalid UPI ID: "+ e.getMessage());
                continue;
            }
            User receiver = service.getUser(upi);
            if(receiver == null){
                System.out.println("No such user: "+upi);
            }
            else if(receiver == sender){
                System.out.println("You cannot pay yourself.");
            }
            else {
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