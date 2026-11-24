package paypulse.ui;

import paypulse.model.BillPayment;
import paypulse.model.P2PTransaction;
import paypulse.model.RegularUser;
import paypulse.model.Transaction;
import paypulse.model.TransactionStatus;
import paypulse.model.User;
import paypulse.repository.TransactionRepository;
import paypulse.repository.UserRepository;
import paypulse.util.ReceiptFormatter;

import java.util.ArrayList;
import java.util.List;


public class JdbcDemo {

    public static void main(String[] args) throws Exception {
        UserRepository users = new UserRepository();
        TransactionRepository repo = new TransactionRepository();
        List<Integer> created = new ArrayList<>();

        cleanup(users, repo);                                  // in case an earlier run stopped half-way
        User asha = new RegularUser("jdbc_asha@upi", "Asha", "1234", 10000);
        User ben = new RegularUser("jdbc_ben@upi", "Ben", "4321", 500);
        users.save(asha);
        users.save(ben);

        try {
            System.out.println("=== 0. Table information (metadata) ===");
            repo.describeTable().forEach(System.out::println);
            System.out.println("Payments already stored: " + repo.syncIdCounter());

            System.out.println("\n=== 1. Create: a successful payment saved with balances (COMMIT) ===");
            Transaction t1 = new P2PTransaction(asha.getUpiId(), ben.getUpiId(), 1500, "Rent share");
            t1.process(asha, ben);
            repo.savePayment(t1, asha, ben);
            created.add(t1.getId());
            System.out.println("Saved #" + t1.getId() + ". In MySQL now: Asha="
                    + balance(users, "jdbc_asha@upi") + "  Ben=" + balance(users, "jdbc_ben@upi"));

            System.out.println("\n=== 2. Read: findById and findByUser ===");
            Transaction loaded = repo.findById(t1.getId()).orElseThrow();
            System.out.println(ReceiptFormatter.receipt(loaded));
            System.out.println("Payments of Ben: " + repo.findByUser("jdbc_ben@upi").size());

            System.out.println("\n=== 3. Update: change the status/note, then delete ===");
            Transaction t2 = new BillPayment(asha.getUpiId(), ben.getUpiId(), 200, "Water");
            t2.process(asha, ben);
            repo.savePayment(t2, asha, ben);
            created.add(t2.getId());
            System.out.println("Bill loaded back: " + repo.findById(t2.getId()).orElseThrow());
            t2.setStatus(TransactionStatus.PENDING);
            repo.update(t2);                                   // UPDATE ... SET status = ?
            System.out.println("After update : " + repo.findById(t2.getId()).orElseThrow());
            System.out.println("Deleted #" + t2.getId() + ": " + repo.delete(t2.getId())
                    + "   findById again: " + repo.findById(t2.getId()));
            created.remove((Integer) t2.getId());

            System.out.println("\n=== 4. Rollback: saving the SAME payment twice ===");
            double before = balance(users, "jdbc_asha@upi");
            try {
                repo.savePayment(t1, asha, ben);               // balances update, then the insert hits a duplicate ID
            } catch (java.sql.SQLException e) {
                System.out.println("Second save failed : " + e.getClass().getSimpleName());
            }
            double after = balance(users, "jdbc_asha@upi");
            System.out.println("Asha before: " + before + "  after: " + after
                    + (before == after ? "   -> rolled back, nothing changed" : "   -> PROBLEM"));

            System.out.println("\n=== 5. Batch insert: 3 payments in one go ===");
            List<Transaction> batch = new ArrayList<>();
            for (int i = 1; i <= 3; i++) {
                Transaction t = new P2PTransaction("jdbc_ben@upi", "jdbc_asha@upi", i * 10, "batch " + i);
                batch.add(t);
                created.add(t.getId());
            }
            repo.saveAll(batch);
            System.out.println("Payments of Asha now: " + repo.findByUser("jdbc_asha@upi").size());
        } finally {
            cleanup(users, repo);
            System.out.println("\nDemo data removed.");
        }
    }

    private static double balance(UserRepository users, String upi) throws Exception {
        return users.findById(upi).orElseThrow().getBalance();
    }

    private static void cleanup(UserRepository users, TransactionRepository repo) throws Exception {
        for (String upi : new String[]{"jdbc_asha@upi", "jdbc_ben@upi"}) {
            for (Transaction t : repo.findByUser(upi)) {
                repo.delete(t.getId());
            }
            users.delete(upi);
        }
    }
}