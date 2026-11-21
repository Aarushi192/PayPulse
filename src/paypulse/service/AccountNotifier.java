package paypulse.service;

import java.util.LinkedList;
import java.util.Queue;

/** Bounded notification queue using wait() and notifyAll() (producer-consumer). */
public class AccountNotifier {

    private final Queue<String> events = new LinkedList<>();
    private final int capacity;

    public AccountNotifier(int capacity) {
        this.capacity = capacity;
    }

    /** Producer: waits while the queue is full. */
    public synchronized void post(String event) throws InterruptedException {
        while (events.size() == capacity) {
            wait();                                // queue full, release the lock and sleep
        }
        events.add(event);
        notifyAll();                               // wake any waiting consumer
    }

    /** Consumer: waits while the queue is empty. */
    public synchronized String take() throws InterruptedException {
        while (events.isEmpty()) {
            wait();                                // nothing to read, release the lock and sleep
        }
        String event = events.poll();
        notifyAll();                               // wake any waiting producer
        return event;
    }

    public synchronized int size() {
        return events.size();
    }
}
