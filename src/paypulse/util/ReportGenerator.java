package paypulse.util;

import paypulse.model.Report;

import java.util.ArrayList;
import java.util.List;

/** Generic helper. "T extends Report" is a BOUNDED type: only Report or its child classes are allowed. */
public class ReportGenerator<T extends Report> {

    private final List<T> reports = new ArrayList<>();

    public void add(T report) {
        reports.add(report);
    }

    public int size() {
        return reports.size();
    }

    public void printAll() {
        for (T r : reports) {
            r.print();
        }
    }

    /** Wildcard method: accepts a List of ANY Report type (List<TopSendersReport>, List<Report>, ...). */
    public static void printAll(List<? extends Report> list) {
        for (Report r : list) {
            r.print();
        }
    }
}
