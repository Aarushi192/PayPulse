package paypulse.util;

import paypulse.model.FraudFlag;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

/** Appends one line per fraud flag to a text file. */
public final class FraudLog {

    private FraudLog() { }

    /** The "true" in FileWriter(path, true) means APPEND: old lines are kept. */
    public static void append(String path, FraudFlag flag, String senderUpi) throws IOException {
        try (FileWriter writer = new FileWriter(path, true)) {
            writer.write(LocalDateTime.now().withNano(0) + " | txn " + flag.getTransactionId()
                    + " | " + senderUpi + " | " + FraudFlag.riskLabel(flag.getRiskScore())
                    + " | " + flag.getReason() + System.lineSeparator());
        }
    }
}
