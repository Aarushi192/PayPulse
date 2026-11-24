package paypulse.service;

/** Second small thread: every interval it wakes up (sleep), and prints how many flags exist now. */
public class AlertLogger implements Runnable {

    private final FraudService service;
    private final long intervalMillis;

    public AlertLogger(FraudService service, long intervalMillis) {
        this.service = service;
        this.intervalMillis = intervalMillis;
    }

    @Override
    public void run() {
        int lastSeen = 0;
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(intervalMillis);              // TIMED_WAITING state
                int now = service.getFlagCount();
                if (now != lastSeen) {
                    System.out.println("[alert-logger] open flags: " + now);
                    lastSeen = now;
                }
            }
        } catch (InterruptedException e) {
            System.out.println("[alert-logger] interrupted, stopping.");
        }
    }
}
