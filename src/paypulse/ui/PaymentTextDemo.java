package paypulse.ui;

import paypulse.exception.DailyLimitExceededException;
import paypulse.exception.InsufficientBalanceException;
import paypulse.exception.InvalidAmountException;
import paypulse.exception.InvalidUPIException;
import paypulse.model.RegularUser;
import paypulse.model.Transaction;
import paypulse.model.User;
import paypulse.service.PaymentCommandParser;
import paypulse.util.PaymentLog;
import paypulse.util.ReceiptFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PaymentTextDemo {
    public static void main(String[] args) {
        Map<String, User> users = new HashMap<>();
        users.put("ravi@upi", new RegularUser("ravi@upi", "Ravi", "1234", 80000));
        users.put("priya@upi", new RegularUser("priya@upi", "Priya", "4321", 0));
        User ravi = users.get("ravi@upi");

        String[] commands = {
                "send 500 to priya@upi Lunch with team",
                "send 250.75 to priya@upi Movie ticket",
                "send abc to priya@upi",              // not a number
                "send 100",                           // too short
                "pay 100 to priya@upi",               // wrong command word
                "send 100 to PRIYA",                  // bad UPI ID
                "send 60000 to priya@upi Big gift"    // over the daily limit
        };

        List<Transaction> done = new ArrayList<>();
        for (String command : commands) {
            System.out.println("> " + command);
            try {
                Transaction t = PaymentCommandParser.parse("ravi@upi", command);
                t.process(ravi, users.get(t.getReceiverUpi()));
                done.add(t);
                System.out.println("  OK  " + t);
            } catch (InvalidUPIException | InsufficientBalanceException | DailyLimitExceededException e) {
                System.out.println("  FAILED (" + e.getClass().getSimpleName() + "): " + e.getMessage());
            } catch (InvalidAmountException | IllegalArgumentException e) {
                System.out.println("  REJECTED (" + e.getClass().getSimpleName() + "): " + e.getMessage());
                if (e.getCause() != null) {
                    System.out.println("  caused by: " + e.getCause());
                }
            }
        }

        System.out.println();
        System.out.println(ReceiptFormatter.receipt(done.get(0)));

        System.out.println();
        System.out.println(ReceiptFormatter.tableHeader());
        for (Transaction t : done) {
            System.out.println(ReceiptFormatter.tableRow(t));
        }

        System.out.println();
        System.out.println("Notes containing 'MOVIE': " + ReceiptFormatter.searchNotes(done, "MOVIE").size());
        String note = "Lunch with Ravi";
        String masked = ReceiptFormatter.maskWord(note, "Ravi");
        System.out.println("Original note: " + note + "   (unchanged: Strings are immutable)");
        System.out.println("Masked note:   " + masked);

        // Wrapper classes: autoboxing (double -> Double) and unboxing (Double -> double)
        List<Double> amounts = new ArrayList<>();
        for (Transaction t : done) {
            amounts.add(t.getAmount());
        }
        double biggest = Collections.max(amounts);
        System.out.println("Amounts: " + amounts + "  biggest: " + biggest
                + "  compare(500, 250.75) = " + Double.compare(500, 250.75));

        System.out.println();
        System.out.println("--- PaymentLog ---");
        System.out.print(PaymentLog.dump());
    }
}
