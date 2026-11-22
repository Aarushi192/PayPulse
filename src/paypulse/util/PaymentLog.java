package paypulse.util;

import java.time.LocalTime;

public final class PaymentLog {

    // StringBuffer is thread-safe: the worker threads you write later will all share this log
    private static final StringBuffer LOG = new StringBuffer();

    private PaymentLog() { }

    public static void add(String message) {
        LOG.append(LocalTime.now().withNano(0))
                .append(" | ")
                .append(message)
                .append(System.lineSeparator());
    }

    public static String dump() {
        return LOG.toString();
    }

    public static void clear() {
        LOG.setLength(0);
    }
}