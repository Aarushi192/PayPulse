package paypulse.ui;

import paypulse.model.Transaction;
import paypulse.model.TransactionStatus;

/** Person B's quick test: run this class to check the Transaction class. */
public class TransactionDemo {
    public static void main(String[] args) {
        Transaction t1 = new Transaction("ravi@upi", "priya@upi", 500);
        Transaction t2 = new Transaction("priya@upi", "ravi@upi", 120.50, "Lunch");

        System.out.println(t1);
        System.out.println(t2);

        t1.setStatus(TransactionStatus.SUCCESS);
        System.out.println("After processing: " + t1);
        System.out.println("Note on t2: " + t2.getNote());
        System.out.println("Total created: " + Transaction.getTotalCount());
    }
}
