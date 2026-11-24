package paypulse.service;

import paypulse.model.FraudFlag;
import paypulse.model.Transaction;
import paypulse.repository.FraudFlagRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Background thread: waits for payments in the FraudInbox and checks each one. */
public class FraudMonitor implements Runnable {

    private final FraudInbox inbox;
    private final FraudService service;
    private final boolean verbose;
    private final FraudFlagRepository repository;
    private final AtomicInteger processed = new AtomicInteger();
    // Existing constructor: keeps older demos working.
    public FraudMonitor(FraudInbox inbox, FraudService service, boolean verbose) {
        this(inbox, service, verbose, null);
    }
    // New constructor: accepts a repository when database storage is enabled.
    public FraudMonitor(FraudInbox inbox, FraudService service, boolean verbose,
                        FraudFlagRepository repository) {
        this.inbox = inbox;
        this.service = service;
        this.verbose = verbose;
        this.repository = repository;
    }
    @Override
    public void run() {

        try {
            while (true) {
                Transaction t = inbox.take();
                List<FraudFlag> flags = service.check(t);

                processed.incrementAndGet();
                for (FraudFlag f : flags) {
                    // Save the detected flag in MySQL.
                    if (repository != null) {
                        try {
                            repository.save(f);
                        } catch (SQLException e) {
                            System.out.println("Could not save fraud flag: " + e.getMessage());
                        }
                    }
                    if (verbose) {
                        System.out.println("[" + Thread.currentThread().getName()
                                + "] ALERT: " + f);
                    }
                }
            }
        } catch (InterruptedException e) {
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