package paypulse.exception;

/** CHECKED exception: a report could not be written to a file. Keeps the original error as its cause. */
public class ReportExportException extends Exception {
    public ReportExportException(String message) {
        super(message);
    }

    // Exception chaining: "cause" is the original problem (for example an IOException).
    public ReportExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
