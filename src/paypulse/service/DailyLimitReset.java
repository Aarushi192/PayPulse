package paypulse.service;

/** Background thread that sets every user's daily spending back to 0. */
public class DailyLimitReset implements Runnable {

    private final UserService userService;
    private final long intervalMillis;
    private volatile int cycles = 0;

    public DailyLimitReset(UserService userService, long intervalMillis) {
        this.userService = userService;
        this.intervalMillis = intervalMillis;
    }

    @Override
    public void run() {
        System.out.println("[" + Thread.currentThread().getName() + "] reset thread started");
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(intervalMillis);              // TIMED_WAITING here
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();        // keep the flag, then leave the loop
                break;
            }
            try {
                userService.resetAllDailyLimits();
                cycles++;
                System.out.println("[" + Thread.currentThread().getName()
                        + "] daily limits reset (cycle " + cycles + ")");
            } catch (RuntimeException e) {
                System.out.println("[" + Thread.currentThread().getName()
                        + "] reset failed: " + e.getMessage());
            }
        }
        System.out.println("[" + Thread.currentThread().getName() + "] reset thread stopping");
    }

    public int getCycles() {
        return cycles;
    }
}
