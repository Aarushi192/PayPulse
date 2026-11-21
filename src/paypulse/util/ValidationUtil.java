package paypulse.util;

import paypulse.exception.InvalidPinException;
import paypulse.exception.InvalidUPIException;

public class ValidationUtil {
    private ValidationUtil() { }

    // Valid format: handle@bank, e.g. ravi@upi. Throws if the format is wrong.
    public static void validateUpi(String upiId) throws InvalidUPIException {
        if (upiId == null || upiId.isBlank()) {
            throw new InvalidUPIException("UPI ID cannot be empty.");
        }
        if (!upiId.contains("@")) {
            throw new InvalidUPIException("UPI ID must contain '@'.");
        }
        if (upiId.indexOf('@') != upiId.lastIndexOf('@')) {
            throw new InvalidUPIException("UPI ID must contain only one '@'.");
        }
        String[] parts = upiId.split("@");
        if (parts.length != 2) {
            throw new InvalidUPIException("UPI ID must look like name@bank.");
        }
        if (!parts[0].matches("[a-z0-9._]{3,}")) {
            throw new InvalidUPIException(
                    "Part before '@' needs 3+ lowercase letters, digits, '.' or '_'.");
        }
        if (!parts[1].matches("[a-z]{2,}")) {
            throw new InvalidUPIException("Part after '@' needs 2+ lowercase letters.");
        }
    }

    // "  rAVi   kUMAR " becomes "Ravi Kumar".
    public static String cleanName(String rawName) {
        if (rawName == null || rawName.trim().isEmpty()) {
            return "";
        }
        String[] words = rawName.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(w.substring(0, 1).toUpperCase()).append(w.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    // A PIN is exactly 4 digits. Returns it as a String so leading zeros are kept.
    public static String parsePin(String rawPin) throws InvalidPinException {
        if (rawPin == null) {
            throw new InvalidPinException("PIN cannot be empty.");
        }
        String pin = rawPin.trim();
        try {
            Integer.parseInt(pin);                 // fails for letters
        } catch (NumberFormatException e) {
            throw new InvalidPinException("PIN must contain digits only.", e);
        }
        if (pin.length() != 4) {
            throw new InvalidPinException("PIN must be exactly 4 digits.");
        }
        return pin;
    }

}
