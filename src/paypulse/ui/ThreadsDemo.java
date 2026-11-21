package paypulse.ui;

import paypulse.model.RegularUser;
import paypulse.model.User;
import paypulse.service.AccountNotifier;
import paypulse.service.DailyLimitReset;
import paypulse.service.UserService;

/** Phase 3 test: synchronization, thread lifecycle, interrupt/join, wait/notify. */
public class ThreadsDemo {

    /** A wallet with NO synchronization, used only to show the race condition. */
    static class UnsafeWallet {
        double balance = 0;

        void deposit(double amount) {
            double temp = balance;         // 1. read
            Thread.yield();                // lets another thread sneak in (makes the race visible)
            balance = temp + amount;       // 2. write back: may overwrite another thread's deposit
        }
    }

    public static void main(String[] args) throws Exception {
        raceConditionDemo();
        startVsRunDemo();
        lifecycleDemo();
        notifierDemo();
    }

    // ---------- 1. Race condition vs synchronized ----------
    private static void raceConditionDemo() throws InterruptedException {
        System.out.println("=== 1. Race condition ===");
        final int threads = 10;
        final int deposits = 10_000;

        UnsafeWallet unsafe = new UnsafeWallet();
        User safe = new RegularUser("race@upi", "Race Test", "1234");

        Thread[] pool = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            pool[i] = new Thread(() -> {
                for (int j = 0; j < deposits; j++) {
                    unsafe.deposit(1);
                    safe.deposit(1);
                }
            });
            pool[i].start();
        }
        for (Thread t : pool) {
            t.join();                      // wait for all workers
        }
        System.out.println("Expected          : " + (threads * deposits));
        System.out.println("Unsafe wallet     : " + (long) unsafe.balance + "  (usually less: lost updates)");
        System.out.println("Synchronized User : " + (long) safe.getBalance());
    }

    // ---------- 2. start() vs run() ----------
    private static void startVsRunDemo() throws InterruptedException {
        System.out.println("\n=== 2. start() vs run() ===");
        Runnable task = () ->
                System.out.println("  task running on thread: " + Thread.currentThread().getName());

        Thread t1 = new Thread(task, "worker-1");
        System.out.println("Calling run():");
        t1.run();                          // plain method call, runs on main
        Thread t2 = new Thread(task, "worker-2");
        System.out.println("Calling start():");
        t2.start();                        // new thread
        t2.join();
    }

    // ---------- 3. DailyLimitReset lifecycle ----------
    private static void lifecycleDemo() throws InterruptedException {
        System.out.println("\n=== 3. DailyLimitReset thread lifecycle ===");
        UserService service = new UserService();
        User ravi = new RegularUser("ravi@upi", "Ravi Kumar", "1234", 5000);
        service.register(ravi);
        ravi.setDailySpent(2500);
        System.out.println("Daily spent before reset: " + ravi.getDailySpent());

        DailyLimitReset reset = new DailyLimitReset(service, 400);
        Thread t = new Thread(reset, "reset-thread");

        System.out.println("State after creating : " + t.getState());   // NEW
        t.start();
        System.out.println("State right after start: " + t.getState()); // RUNNABLE (or already sleeping)
        Thread.sleep(150);
        System.out.println("State while sleeping : " + t.getState());   // TIMED_WAITING
        Thread.sleep(600);
        System.out.println("Daily spent after reset : " + ravi.getDailySpent()
                + " (cycles: " + reset.getCycles() + ")");

        t.interrupt();                     // ask the thread to stop
        t.join();                          // wait until it is finished
        System.out.println("State after join : " + t.getState());       // TERMINATED
    }

    // ---------- 4. AccountNotifier: wait / notifyAll ----------
    private static void notifierDemo() throws InterruptedException {
        System.out.println("\n=== 4. AccountNotifier (capacity 2) ===");
        AccountNotifier notifier = new AccountNotifier(2);
        final int total = 6;

        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= total; i++) {
                    notifier.post("event-" + i);
                    System.out.println("  posted   event-" + i + " (queue size " + notifier.size() + ")");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "producer");

        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= total; i++) {
                    Thread.sleep(150);                 // slow consumer, so the queue fills up
                    String e = notifier.take();
                    System.out.println("  consumed " + e);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "consumer");

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
        System.out.println("Producer and consumer finished.");
    }
}
