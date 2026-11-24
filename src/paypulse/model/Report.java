package paypulse.model;

import paypulse.exception.ReportExportException;
import paypulse.util.Exportable;

import java.io.FileWriter;
import java.io.IOException;

/** Abstract base class for every report. Child classes decide HOW the report text is built. */
public abstract class Report implements Exportable {

    protected final String title;

    protected Report(String title) {
        this.title = title;
    }

    /** Each report builds its own text (polymorphism). */
    public abstract String generate();

    /** CSV version. Reports that have table data override this. */
    public String toCsv() {
        return generate();
    }

    public String getTitle() { return title; }

    /** Prints the report. finally always closes the report section, even if generate() fails. */
    public void print() {
        try {
            System.out.println(generate());
        } catch (RuntimeException e) {
            System.out.println("Report failed: " + e.getMessage());
        } finally {
            System.out.println("[end of " + title + "]");
        }
    }

    /** Writes the report to a .txt or .csv file. try-with-resources closes the file for us. */
    @Override
    public void export(String path) throws ReportExportException {
        if (!path.endsWith(".txt") && !path.endsWith(".csv")) {
            throw new ReportExportException("File name must end with .txt or .csv: " + path);
        }
        String content = path.endsWith(".csv") ? toCsv() : generate();
        try (FileWriter writer = new FileWriter(path)) {
            writer.write(content);
        } catch (IOException e) {
            throw new ReportExportException("Could not export " + title + " to " + path, e);
        }
    }
}
