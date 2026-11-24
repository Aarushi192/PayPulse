package paypulse.ui;

import paypulse.model.P2PTransaction;
import paypulse.service.AlertLogger;
import paypulse.service.FraudInbox;
import paypulse.service.FraudMonitor;
import paypulse.service.FraudService;

import java.util.concurrent.atomic.AtomicInteger;

/** Person C threads demo: start vs run, sleep/interrupt, lifecycle, wait/notify, AtomicInteger. */
public class FraudThreadsDemo {

    public static void main(String[] args) throws Exception {
        startVsRunDemo();
        lifecycleDemo();
        waitNotifyDemo();
        atomicDemo();
    }

    // ---------- 1. start() vs run() ----------
    private static void startVsRunDemo() throws InterruptedException {
        System.out.println("=== 1. start() vs run() ===");
        Runnable task = () -> System.out.println("  running in thread: " + Thread.currentThread().getName());

        Thread direct = new Thread(task, "direct-thread");
        direct.run();                          // NOT a new thread: runs inside main
        Thread real = new Thread(task, "real-thread");
        real.start();                          // a new thread runs the task
        real.join();
    }

    // ---------- 2. Lifecycle: NEW, RUNNABLE, WAITING, TIMED_WAITING, TERMINATED ----------
    private static void lifecycleDemo() throws InterruptedException {
        System.out.println("\n=== 2. Thread lifecycle ===");
        FraudService service = new FraudService();
        FraudInbox inbox = new FraudInbox();

        Thread monitor = new Thread(new FraudMonitor(inbox, service, true), "fraud-monitor");
        System.out.println("  monitor before start()      : " + monitor.getState());   // NEW
        monitor.start();
        Thread.sleep(200);
        System.out.println("  monitor waiting for payments: " + monitor.getState());   // WAITING (inside wait())

        Thread logger = new Thread(new AlertLogger(service, 300), "alert-logger");
        logger.start();
        Thread.sleep(100);
        System.out.println("  logger while sleeping       : " + logger.getState());    // TIMED_WAITING (inside sleep())

        inbox.submit(new P2PTransaction("ravi@upi", "priya@upi", 20000, "big one"));
        Thread.sleep(500);                                                             // let both threads react

        monitor.interrupt();                   // ask both threads to stop
        logger.interrupt();
        monitor.join();                        // wait until they are really finished
        logger.join();
        System.out.println("  monitor after interrupt+join: " + monitor.getState());   // TERMINATED
        System.out.println("  logger after interrupt+join : " + logger.getState());
    }

    // ---------- 3. wait() and notifyAll(): producer and consumer ----------
    private static void waitNotifyDemo() throws InterruptedException {
        System.out.println("\n=== 3. wait / notifyAll (producer-consumer) ===");
        FraudService service = new FraudService();
        FraudInbox inbox = new FraudInbox();
        FraudMonitor monitorTask = new FraudMonitor(inbox, service, true);
        Thread monitor = new Thread(monitorTask, "fraud-monitor");
        monitor.start();

        // The producer plays the part of Person B's payment workers
        Thread producer = new Thread(() -> {
            double[] amounts = {500, 12000, 7000, 90};
            try {
                for (double amount : amounts) {
                    inbox.submit(new P2PTransaction("worker@upi", "shop@upi", amount));
                    System.out.println("  [producer] submitted " + amount);
                    Thread.sleep(150);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "payment-worker");
        producer.start();
        producer.join();

        Thread.sleep(300);                     // give the monitor time to finish the queue
        monitor.interrupt();
        monitor.join();
        System.out.println("  payments checked: " + monitorTask.getProcessed()
                + ", flags raised: " + service.getFlagCount());
    }

    // ---------- 4. AtomicInteger vs plain int ----------
    private static int plainCounter = 0;
    private static final AtomicInteger atomicCounter = new AtomicInteger();

    private static void atomicDemo() throws InterruptedException {
        System.out.println("\n=== 4. Plain int vs AtomicInteger (4 threads x 5000 increments) ===");
        Runnable work = () -> {
            for (int i = 0; i < 5_000; i++) {
                int temp = plainCounter;              // 1. read
                Thread.yield();                       // lets another thread sneak in (makes the race visible)
                plainCounter = temp + 1;              // 2. write back: may overwrite another thread's update
                atomicCounter.incrementAndGet();      // one safe step
            }
        };
        Thread[] pool = new Thread[4];
        for (int i = 0; i < pool.length; i++) {
            pool[i] = new Thread(work);
            pool[i].start();
        }
        for (Thread t : pool) {
            t.join();
        }
        System.out.println("  expected     : 20000");
        System.out.println("  plain int    : " + plainCounter + "   (usually LESS: lost updates)");
        System.out.println("  AtomicInteger: " + atomicCounter.get());
    }
}
