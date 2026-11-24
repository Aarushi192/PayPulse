package paypulse.util;

import paypulse.exception.ReportExportException;

/** Anything that can be written to a file. Report implements this. */
public interface Exportable {
    void export(String path) throws ReportExportException;
}
