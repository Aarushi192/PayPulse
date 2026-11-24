package paypulse.ui;

import paypulse.model.P2PTransaction;
import paypulse.model.RegularUser;
import paypulse.model.Transaction;
import paypulse.model.TransactionStatus;
import paypulse.model.User;
import paypulse.service.TransactionQueue;
import paypulse.service.TransactionWorker;
import paypulse.service.UserService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Person B threads demo: synchronized IDs, a protected limit check, wait/notify queue, thread states. */
public class PaymentThreadsDemo {

    public static void main(String[] args) throws Exception {
        uniqueIdDemo();
        dailyLimitDemo();
        queueDemo();
        statesDemo();
    }

    // ---------- 1. synchronized ID generator ----------
    private static int unsafeCounter = 0;     // NO synchronization, used only to show the problem

    private static void uniqueIdDemo() throws InterruptedException {
        System.out.println("=== 1. Unique payment IDs across threads ===");
        final int threads = 8;
        final int each = 2000;
        Set<Integer> ids = Collections.synchronizedSet(new HashSet<>());
        Thread[] pool = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            pool[i] = new Thread(() -> {
                for (int j = 0; j < each; j++) {
                    ids.add(new P2PTransaction("a@upi", "b@upi", 1).getId());
                    unsafeCounter++;                   // plain ++ is read, add, write: updates get lost
                }
            });
            pool[i].start();
        }
        for (Thread t : pool) {
            t.join();                                  // wait until every thread has finished
        }
        System.out.println("Payments created           : " + (threads * each));
        System.out.println("Different IDs handed out   : " + ids.size() + "  (synchronized: must be equal)");
        System.out.println("Unsynchronized counter     : " + unsafeCounter + "  (usually less: lost updates)");
    }

    // ---------- 2. daily limit under many threads ----------
    private static void dailyLimitDemo() throws InterruptedException {
        System.out.println("\n=== 2. Daily limit with 30 payments at once ===");
        UserService service = new UserService();
        service.register(new RegularUser("ravi@upi", "Ravi", "1234", 1_000_000));
        service.register(new RegularUser("priya@upi", "Priya", "4321", 0));
        User ravi = service.getUser("ravi@upi");
        System.out.println("Ravi's daily limit: " + ravi.getDailyLimit() + ", each payment: 5000");

        List<Transaction> finished = Collections.synchronizedList(new ArrayList<>());
        TransactionQueue queue = new TransactionQueue(50);
        TransactionWorker[] workers = new TransactionWorker[5];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new TransactionWorker("worker-" + (i + 1), queue, service, finished);
            workers[i].start();
        }
        for (int i = 0; i < 30; i++) {
            queue.put(new P2PTransaction("ravi@upi", "priya@upi", 5000, "p" + i));
        }
        queue.close();
        for (TransactionWorker w : workers) {
            w.join();
        }
        long ok = finished.stream().filter(t -> t.getStatus() == TransactionStatus.SUCCESS).count();
        System.out.println("Succeeded: " + ok + "   Failed: " + (finished.size() - ok));
        System.out.println("Ravi spent today: " + ravi.getDailySpent() + "   Priya received: "
                + service.getUser("priya@upi").getBalance());
        System.out.println("Work done by each worker: " + workers[0].getProcessed() + ", "
                + workers[1].getProcessed() + ", " + workers[2].getProcessed() + ", "
                + workers[3].getProcessed() + ", " + workers[4].getProcessed()
                + "  (total " + finished.size() + ")");
    }

    // ---------- 3. wait/notify with a small queue ----------
    private static void queueDemo() throws InterruptedException {
        System.out.println("\n=== 3. wait/notify: producer fills a queue of size 2, one worker empties it ===");
        UserService service = new UserService();
        service.register(new RegularUser("ravi@upi", "Ravi", "1234", 100000));
        service.register(new RegularUser("priya@upi", "Priya", "4321", 0));
        List<Transaction> finished = Collections.synchronizedList(new ArrayList<>());
        TransactionQueue queue = new TransactionQueue(2);
        TransactionWorker worker = new TransactionWorker("consumer", queue, service, finished);
        worker.start();
        for (int i = 1; i <= 6; i++) {
            Transaction t = new P2PTransaction("ravi@upi", "priya@upi", i * 10, "n" + i);
            queue.put(t);                                  // waits whenever 2 are already queued
            System.out.println("  produced payment #" + t.getId() + "  (waiting in queue: " + queue.size() + ")");
        }
        queue.close();
        worker.join();
        System.out.println("Consumer processed " + worker.getProcessed() + " payments, Priya now has "
                + service.getUser("priya@upi").getBalance());
    }

    // ---------- 4. thread states ----------
    private static void statesDemo() throws InterruptedException {
        System.out.println("\n=== 4. Thread states ===");
        UserService service = new UserService();
        TransactionQueue queue = new TransactionQueue(5);
        TransactionWorker worker = new TransactionWorker("state-worker", queue, service,
                Collections.synchronizedList(new ArrayList<>()));
        System.out.println("After creating          : " + worker.getState());     // NEW
        worker.start();
        Thread.sleep(200);
        System.out.println("Empty queue, waiting    : " + worker.getState());     // WAITING (inside wait())

        // BLOCKED: main holds the queue's lock, so another thread cannot enter queue.size()
        Thread blocked = new Thread(queue::size, "blocked-thread");
        synchronized (queue) {
            blocked.start();
            Thread.sleep(200);
            System.out.println("Lock held by someone else: " + blocked.getState()); // BLOCKED
        }
        blocked.join();

        queue.close();
        worker.join();                                                             // wait for it to end
        System.out.println("After join()            : " + worker.getState());     // TERMINATED
    }
}