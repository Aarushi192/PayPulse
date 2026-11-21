package paypulse.util;

import paypulse.exception.InvalidPinException;
import paypulse.exception.InvalidUPIException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/** Validation helpers: Strings, wrappers and Predicate rules. */
public class ValidationUtil {

    private ValidationUtil() { }

    // ---------- Reusable rules as Predicate<String> (lambdas) ----------
    public static final Predicate<String> NOT_BLANK = s -> s != null && !s.isBlank();
    public static final Predicate<String> HAS_ONE_AT =
            s -> s.contains("@") && s.indexOf('@') == s.lastIndexOf('@');
    public static final Predicate<String> HANDLE_OK =
            s -> s.split("@", -1)[0].matches("[a-z0-9._]{3,}");
    public static final Predicate<String> BANK_OK =
            s -> s.split("@", -1)[1].matches("[a-z]{2,}");

    /** Ordered rules: the first failing rule decides the error message. */
    private static final Map<String, Predicate<String>> UPI_RULES = new LinkedHashMap<>();

    static {
        UPI_RULES.put("UPI ID cannot be empty.", NOT_BLANK);
        UPI_RULES.put("UPI ID must contain exactly one '@'.", HAS_ONE_AT);
        UPI_RULES.put("Part before '@' needs 3+ lowercase letters, digits, '.' or '_'.", HANDLE_OK);
        UPI_RULES.put("Part after '@' needs 2+ lowercase letters.", BANK_OK);
    }

    /** Valid format: handle@bank, e.g. ravi@upi. Throws if the format is wrong. */
    public static void validateUpi(String upiId) throws InvalidUPIException {
        for (Map.Entry<String, Predicate<String>> rule : UPI_RULES.entrySet()) {
            if (!rule.getValue().test(upiId)) {
                throw new InvalidUPIException(rule.getKey());
            }
        }
    }

    /** True or false version, handy for streams and filters. */
    public static final Predicate<String> IS_VALID_UPI = s -> {
        try {
            validateUpi(s);
            return true;
        } catch (InvalidUPIException e) {
            return false;
        }
    };

    /** "  rAVi   kUMAR " becomes "Ravi Kumar". */
    public static String cleanName(String rawName) {
        if (rawName == null || rawName.trim().isEmpty()) {
            return "";
        }
        String[] words = rawName.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(w.substring(0, 1).toUpperCase()).append(w.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    /** A PIN is exactly 4 digits. Returns it as a String so leading zeros are kept. */
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

    /** Parses a money amount. Must be a number greater than zero. */
    public static double parseAmount(String raw) {
        try {
            double amount = Double.parseDouble(raw.trim());
            if (Double.isNaN(amount) || Double.isInfinite(amount) || Double.compare(amount, 0.0) <= 0) {
                throw new IllegalArgumentException("Amount must be greater than zero.");
            }
            return amount;
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException("Amount must be a number.", e);
        }
    }
}
