package paypulse.service;

import paypulse.model.Transaction;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Producer-consumer queue with wait() and notifyAll().
 * Payment code (producer) calls submit(); the FraudMonitor thread (consumer) calls take().
 */
public class FraudInbox {

    private final Queue<Transaction> waiting = new LinkedList<>();

    /** Producer: adds a completed payment and wakes the monitor. */
    public synchronized void submit(Transaction t) {
        waiting.add(t);
        notifyAll();                         // wake the monitor if it is sleeping in wait()
    }

    /** Consumer: sleeps (releasing the lock) until a payment is available. */
    public synchronized Transaction take() throws InterruptedException {
        while (waiting.isEmpty()) {          // "while", not "if": re-check after every wake-up
            wait();
        }
        return waiting.poll();
    }

    public synchronized int size() {
        return waiting.size();
    }
}
