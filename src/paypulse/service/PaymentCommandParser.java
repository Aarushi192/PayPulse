package paypulse.service;

import paypulse.exception.InvalidAmountException;
import paypulse.exception.InvalidUPIException;
import paypulse.model.P2PTransaction;
import paypulse.model.Transaction;
import paypulse.util.PaymentLog;
import paypulse.util.ValidationUtil;

public final class PaymentCommandParser {

    private PaymentCommandParser() { }

    // Format: send <amount> to <upi> [note...]
    public static Transaction parse(String senderUpi, String command) throws InvalidUPIException {
        String[] parts = command.trim().split("\\s+", 5);   // send | amount | to | upi | note...
        double amount;
        String receiver;
        try {
            if (!parts[0].equalsIgnoreCase("send") || !parts[2].equalsIgnoreCase("to")) {
                throw new IllegalArgumentException("Use: send <amount> to <upi> [note]");
            }
            amount = Double.parseDouble(parts[1]);           // wrapper class: String to double
            receiver = parts[3];
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {   // multi-catch
            // Exception chaining: the original problem is kept as the cause
            throw new InvalidAmountException("Cannot read the payment from: \"" + command + "\"", e);
        } finally {
            PaymentLog.add("parse attempt by " + senderUpi + ": " + command);   // always runs
        }

        ValidationUtil.validateUpi(receiver);                // may throw InvalidUPIException
        String note = parts.length > 4 ? parts[4] : "";
        return new P2PTransaction(senderUpi, receiver, amount, note);
    }
}