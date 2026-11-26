package paypulse.service;

import paypulse.exception.InvalidPinException;
import paypulse.exception.InvalidUPIException;
import paypulse.model.MerchantUser;
import paypulse.model.RegularUser;
import paypulse.model.User;
import paypulse.util.ValidationUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads users from a CSV file with BufferedReader and FileReader.
 * Format: upi_id,name,pin,balance[,type]   (type is REGULAR or MERCHANT)
 */
public class CsvUserImporter {

    /** Valid users go in a map, rejected rows (with the reason) go in a list. */
    public static class ImportResult {
        private final Map<String, User> valid = new LinkedHashMap<>();
        private final List<String> rejected = new ArrayList<>();
        private int totalRows = 0;

        public Map<String, User> getValid() { return valid; }
        public List<String> getRejected() { return rejected; }
        public int getTotalRows() { return totalRows; }

        public void printSummary() {
            System.out.println("----- Import summary -----");
            System.out.println("Rows read : " + totalRows);
            System.out.println("Accepted  : " + valid.size());
            System.out.println("Rejected  : " + rejected.size());
            for (String r : rejected) {
                System.out.println("  x " + r);
            }
        }
    }

    private CsvUserImporter() { }

    /** existingIds = UPI IDs already registered, so duplicates are rejected too. */
    public static ImportResult read(String path, Set<String> existingIds) throws IOException {
        ImportResult result = new ImportResult();

        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) {
                    continue;
                }
                if (lineNo == 1 && line.toLowerCase().startsWith("upi")) {
                    continue;                                  // header row
                }
                result.totalRows++;
                try {
                    User user = parseLine(line);
                    String upi = user.getUpiId();
                    if (existingIds.contains(upi) || result.valid.containsKey(upi)) {
                        throw new IllegalArgumentException("Duplicate UPI ID " + upi);
                    }
                    result.valid.put(upi, user);
                } catch (InvalidUPIException | InvalidPinException | IllegalArgumentException e) {
                    result.rejected.add("Line " + lineNo + " [" + line + "] -> " + e.getMessage());
                }
            }
        }
        return result;
    }

    private static User parseLine(String line)
            throws InvalidUPIException, InvalidPinException {
        String[] f = line.split(",", -1);
        if (f.length < 4 || f.length > 5) {
            throw new IllegalArgumentException("Expected 4 or 5 columns but found " + f.length);
        }
        String upi = f[0].trim();
        ValidationUtil.validateUpi(upi);

        String name = ValidationUtil.validateName(f[1]);

        String pin = ValidationUtil.parsePin(f[2]);

        double balance;
        try {
            balance = Double.parseDouble(f[3].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Balance is not a number");
        }
        if (balance < 0) {
            throw new IllegalArgumentException("Balance cannot be negative");
        }

        String type = f.length == 5 ? f[4].trim().toUpperCase() : "REGULAR";
        return switch (type) {
            case "REGULAR", "" -> new RegularUser(upi, name, pin, balance);
            case "MERCHANT" -> new MerchantUser(upi, name, pin, balance);
            default -> throw new IllegalArgumentException("Unknown user type " + type);
        };
    }
}
