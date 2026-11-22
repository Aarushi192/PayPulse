package paypulse.ui;

import paypulse.exception.DailyLimitExceededException;
import paypulse.exception.InsufficientBalanceException;
import paypulse.exception.InvalidAmountException;
import paypulse.model.BillPayment;
import paypulse.model.P2PTransaction;
import paypulse.model.RegularUser;
import paypulse.model.Transaction;
import paypulse.model.User;

import java.util.ArrayList;
import java.util.List;

public class TransactionDemo {
    public static void main(String[] args) {
        User ravi = new RegularUser("ravi@upi", "Ravi", "1234", 1000);
        User priya = new RegularUser("priya@upi", "Priya", "4321", 0);
        User bescom = new RegularUser("bescom@upi", "Bescom", "1111", 0);

        List<Transaction> payments = new ArrayList<>();
        payments.add(new P2PTransaction("ravi@upi", "priya@upi", 500, "Lunch"));
        payments.add(new BillPayment("ravi@upi", "bescom@upi", 200, "Electricity"));
        payments.add(new BillPayment("ravi@upi", "bescom@upi", 5000, "Rent"));   // too much
        payments.add(new P2PTransaction("ravi@upi", "priya@upi", -5));           // bad amount

        System.out.println("Before: Ravi=" + ravi.getBalance() + " Priya=" + priya.getBalance()
                + " Bescom=" + bescom.getBalance());

        for (Transaction t : payments) {
            User receiver = t.getReceiverUpi().equals("priya@upi") ? priya : bescom;
            try {
                t.process(ravi, receiver);
            } catch (InsufficientBalanceException |DailyLimitExceededException e) {
                System.out.println("Failed: " + e.getMessage());
            } catch (InvalidAmountException e) {
                System.out.println("Rejected: " + e.getMessage());
            }
            System.out.println(t);
        }

        System.out.println("After:  Ravi=" + ravi.getBalance() + " Priya=" + priya.getBalance()
                + " Bescom=" + bescom.getBalance());
        System.out.println("Total created: " + Transaction.getTotalCount());
    }
}