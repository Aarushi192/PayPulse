package paypulse.service;

import paypulse.model.Transaction;

import java.util.LinkedList;
import java.util.Queue;


public class TransactionQueue {

    private final Queue<Transaction> line = new LinkedList<>();
    private final int capacity;
    private boolean closed = false;

    public TransactionQueue(int capacity) {
        this.capacity = capacity;
    }

    /** Adds a payment. Waits (releasing the lock) while the line is full. */
    public synchronized void put(Transaction t) throws InterruptedException {
        while (line.size() == capacity) {        // while, not if: re-check after every wake-up
            wait();
        }
        if (closed) {
            throw new IllegalStateException("Queue is closed");
        }
        line.add(t);
        notifyAll();                             // wake workers that were waiting for work
    }

    /** Takes the oldest payment. Waits while empty. Returns null when closed and nothing is left. */
    public synchronized Transaction take() throws InterruptedException {
        while (line.isEmpty()) {
            if (closed) {
                return null;                     // tells the worker to stop
            }
            wait();
        }
        Transaction t = line.remove();
        notifyAll();                             // wake producers that were waiting for space
        return t;
    }

    /** No more payments will be added. Wakes everyone so idle workers can finish. */
    public synchronized void close() {
        closed = true;
        notifyAll();
    }

    public synchronized int size() {
        return line.size();
    }
}