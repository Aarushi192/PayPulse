package paypulse.service;

import paypulse.model.FraudFlag;
import paypulse.model.Transaction;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Background thread: waits for payments in the FraudInbox and checks each one. */
public class FraudMonitor implements Runnable {

    private final FraudInbox inbox;
    private final FraudService service;
    private final boolean verbose;                         // true = print alerts on the console
    private final AtomicInteger processed = new AtomicInteger();

    public FraudMonitor(FraudInbox inbox, FraudService service, boolean verbose) {
        this.inbox = inbox;
        this.service = service;
        this.verbose = verbose;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Transaction t = inbox.take();              // waits here when there is nothing to check
                List<FraudFlag> flags = service.check(t);
                processed.incrementAndGet();
                if (verbose) {
                    for (FraudFlag f : flags) {
                        System.out.println("[" + Thread.currentThread().getName() + "] ALERT: " + f);
                    }
                }
            }
        } catch (InterruptedException e) {
            // interrupt() was called: leave the loop and finish the thread
            if (verbose) {
                System.out.println("[" + Thread.currentThread().getName() + "] stopped.");
            }
            Thread.currentThread().interrupt();
        }
    }

    public int getProcessed() {
        return processed.get();
    }
}
