package paypulse.util;

import paypulse.exception.InvalidRuleException;
import paypulse.model.FraudFlag;

import java.util.function.Predicate;

/**
 * Turns filter text typed in the admin menu into a Predicate.
 *   risk>=2   risk<=2   risk=3   reason:large
 */
public final class FlagFilter {

    private FlagFilter() { }

    public static Predicate<FraudFlag> parse(String text) {
        String filter = text.trim().toLowerCase();
        try {
            if (filter.startsWith("risk")) {
                if (filter.contains(">=")) {
                    int min = Integer.parseInt(filter.split(">=")[1].trim());     // "risk>=2" -> ["risk", "2"]
                    return f -> f.getRiskScore() >= min;
                } else if (filter.contains("<=")) {
                    int max = Integer.parseInt(filter.split("<=")[1].trim());
                    return f -> f.getRiskScore() <= max;
                } else if (filter.contains("=")) {
                    int exact = Integer.parseInt(filter.split("=")[1].trim());
                    return f -> f.getRiskScore() == exact;
                }
            } else if (filter.startsWith("reason:")) {
                String word = filter.substring("reason:".length()).trim();
                return f -> f.getReason().toLowerCase().contains(word);
            }
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {      // multi-catch
            throw new InvalidRuleException("Bad filter '" + text + "'. Examples: risk>=2, reason:large", e);
        }
        throw new InvalidRuleException("Unknown filter '" + text + "'. Examples: risk>=2, reason:large");
    }
}
