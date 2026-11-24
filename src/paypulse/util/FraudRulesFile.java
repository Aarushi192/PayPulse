package paypulse.util;

import paypulse.exception.InvalidRuleException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads fraud thresholds from rules.txt with a StreamTokenizer. File format:
 *   # a comment line
 *   LARGE_AMOUNT 10000
 *   ROUND_AMOUNT 5000
 *   RAPID_COUNT 2
 *   RAPID_SECONDS 60
 */
public final class FraudRulesFile {

    public static final List<String> KNOWN_KEYS =
            Arrays.asList("LARGE_AMOUNT", "ROUND_AMOUNT", "RAPID_COUNT", "RAPID_SECONDS");

    private FraudRulesFile() { }

    public static Map<String, Double> load(String path) throws IOException {
        Map<String, Double> thresholds = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            StreamTokenizer st = new StreamTokenizer(reader);
            st.commentChar('#');                  // everything after # is ignored
            st.wordChars('_', '_');               // allow underscores inside names

            while (st.nextToken() != StreamTokenizer.TT_EOF) {
                if (st.ttype != StreamTokenizer.TT_WORD) {
                    throw new InvalidRuleException("Line " + st.lineno() + ": expected a rule name");
                }
                String name = st.sval.toUpperCase();
                if (!KNOWN_KEYS.contains(name)) {
                    throw new InvalidRuleException("Line " + st.lineno() + ": unknown rule " + name);
                }
                if (st.nextToken() != StreamTokenizer.TT_NUMBER) {
                    throw new InvalidRuleException("Line " + st.lineno() + ": " + name + " needs a number");
                }
                if (st.nval <= 0) {
                    throw new InvalidRuleException("Line " + st.lineno() + ": " + name + " must be above 0");
                }
                thresholds.put(name, st.nval);    // nval = the number that was read (a double)
            }
        }
        return thresholds;
    }
}
