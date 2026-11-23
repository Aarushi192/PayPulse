package paypulse.service;

import paypulse.model.Transaction;
import paypulse.model.TransactionStatus;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/** Person B, collections: keeps every payment in an ArrayList, a HashMap, a LinkedHashSet and a Queue. */
public class HistoryService {

    private final List<Transaction> all = new ArrayList<>();                   // in the order payments were made
    private final Map<Integer, Transaction> byId = new HashMap<>();            // fast lookup by payment ID
    private final Set<String> recentReceivers = new LinkedHashSet<>();         // no duplicates, keeps order
    private final Queue<Transaction> pending = new ArrayDeque<>();             // first queued = first processed

    // ---------- Comparators ----------
    /** A named Comparator class: biggest amount first. */
    public static class AmountComparator implements Comparator<Transaction> {
        @Override
        public int compare(Transaction a, Transaction b) {
            return Double.compare(b.getAmount(), a.getAmount());
        }
    }

    /** An anonymous Comparator: newest first. */
    public static final Comparator<Transaction> NEWEST_FIRST = new Comparator<Transaction>() {
        @Override
        public int compare(Transaction a, Transaction b) {
            int byTime = b.getTimestamp().compareTo(a.getTimestamp());
            return byTime != 0 ? byTime : Integer.compare(b.getId(), a.getId());   // same instant: higher ID is newer
        }
    };

    // ---------- Recording ----------
    /** Records a finished (success or failed) payment in all the collections. */
    public void record(Transaction t) {
        all.add(t);
        byId.put(t.getId(), t);
        recentReceivers.remove(t.getReceiverUpi());       // remove + add moves it to the end (most recent)
        recentReceivers.add(t.getReceiverUpi());
    }

    // ---------- Looking things up ----------
    public Transaction findById(int id) {
        return byId.get(id);                              // null when there is no such payment
    }

    public int size() {
        return all.size();
    }

    /** A copy, so the menu cannot change the real list by accident. */
    public List<Transaction> getAll() {
        return new ArrayList<>(all);
    }

    public List<Transaction> sortedByAmount() {
        List<Transaction> copy = getAll();
        Collections.sort(copy, new AmountComparator());
        return copy;
    }

    public List<Transaction> newestFirst() {
        List<Transaction> copy = getAll();
        copy.sort(NEWEST_FIRST);
        return copy;
    }

    public List<Transaction> oldestFirst() {
        List<Transaction> copy = getAll();                // already oldest first
        return copy;
    }

    public List<Transaction> reversed() {
        List<Transaction> copy = getAll();
        Collections.reverse(copy);
        return copy;
    }

    /** Last few people paid, most recent first. */
    public List<String> recentReceivers(int limit) {
        List<String> list = new ArrayList<>(recentReceivers);
        Collections.reverse(list);
        return list.size() > limit ? new ArrayList<>(list.subList(0, limit)) : list;
    }

    // ---------- ListIterator: removing failed payments ----------
    /** Removes FAILED payments from the history; returns how many were removed. */
    public int removeFailed() {
        int removed = 0;
        ListIterator<Transaction> it = all.listIterator();
        while (it.hasNext()) {
            Transaction t = it.next();
            if (t.getStatus() == TransactionStatus.FAILED) {
                it.remove();                               // safe removal while looping
                byId.remove(t.getId());
                removed++;
            }
        }
        return removed;
    }

    // ---------- Queue: payments waiting to be sent ----------
    public void queuePending(Transaction t) {
        pending.offer(t);
    }

    /** Takes the oldest queued payment, or null if the queue is empty. */
    public Transaction nextPending() {
        return pending.poll();
    }

    public int pendingCount() {
        return pending.size();
    }

    public List<Transaction> pendingList() {
        return new ArrayList<>(pending);
    }
}
