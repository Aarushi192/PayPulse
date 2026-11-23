package paypulse.ui;

import paypulse.model.BillPayment;
import paypulse.model.P2PTransaction;
import paypulse.model.RegularUser;
import paypulse.model.Transaction;
import paypulse.model.User;
import paypulse.service.HistoryService;
import paypulse.util.ReceiptFormatter;

import java.util.List;

/** Shows each collection in HistoryService working. No database or typing needed. */
public class HistoryDemo {

    public static void main(String[] args) {
        User ravi = new RegularUser("ravi@upi", "Ravi", "1234", 100000);
        User priya = new RegularUser("priya@upi", "Priya", "4321", 0);
        User bescom = new RegularUser("bescom@upi", "Bescom", "1111", 0);
        HistoryService history = new HistoryService();

        run(history, new P2PTransaction("ravi@upi", "priya@upi", 500, "Lunch"), ravi, priya);
        run(history, new BillPayment("ravi@upi", "bescom@upi", 2000, "Electricity"), ravi, bescom);
        run(history, new P2PTransaction("ravi@upi", "priya@upi", 90000, "Too big"), ravi, priya);   // fails: daily limit
        run(history, new P2PTransaction("ravi@upi", "priya@upi", 50, "Chai"), ravi, priya);

        System.out.println("1) ArrayList, in order made:");
        print(history.getAll());
        System.out.println("\n2) Sorted by amount (Comparator class):");
        print(history.sortedByAmount());
        System.out.println("\n3) Newest first (anonymous Comparator):");
        print(history.newestFirst());
        System.out.println("\n4) Collections.reverse:");
        print(history.reversed());

        System.out.println("\n5) HashMap lookup by ID: findById(2) -> " + history.findById(2));
        System.out.println("   findById(99) -> " + history.findById(99));

        System.out.println("\n6) LinkedHashSet, recent receivers (no repeats, newest first): "
                + history.recentReceivers(5));

        System.out.println("\n7) ListIterator removes failed payments: removed " + history.removeFailed());
        print(history.getAll());
        System.out.println("   findById(3) after removal -> " + history.findById(3));

        System.out.println("\n8) Queue, first in = first out:");
        history.queuePending(new P2PTransaction("ravi@upi", "priya@upi", 10, "first"));
        history.queuePending(new P2PTransaction("ravi@upi", "priya@upi", 20, "second"));
        System.out.println("   waiting: " + history.pendingCount());
        Transaction t;
        while ((t = history.nextPending()) != null) {
            System.out.println("   processing: " + t.getNote());
        }
        System.out.println("   waiting now: " + history.pendingCount());
    }

    private static void run(HistoryService history, Transaction t, User from, User to) {
        try {
            t.process(from, to);
        } catch (Exception e) {
            System.out.println("(" + t.getNote() + " failed: " + e.getClass().getSimpleName() + ")");
        }
        history.record(t);
    }

    private static void print(List<Transaction> list) {
        for (Transaction t : list) {
            System.out.println("   " + ReceiptFormatter.tableRow(t));
        }
    }
}