package paypulse.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Person B, generics. One class that works for any type that can compare itself:
 * Integer, Double, String, ... T extends Comparable<T> is a "bounded type parameter".
 */
public class SortUtil<T extends Comparable<T>> {

    /** Insertion sort written by hand. Returns a new sorted list, the input is not changed. */
    public List<T> sorted(List<T> items) {
        List<T> result = new ArrayList<>(items);
        for (int i = 1; i < result.size(); i++) {
            T key = result.get(i);
            int j = i - 1;
            while (j >= 0 && result.get(j).compareTo(key) > 0) {
                result.set(j + 1, result.get(j));
                j--;
            }
            result.set(j + 1, key);
        }
        return result;
    }

    public boolean isSorted(List<T> items) {
        for (int i = 1; i < items.size(); i++) {
            if (items.get(i - 1).compareTo(items.get(i)) > 0) {
                return false;
            }
        }
        return true;
    }

    /** Biggest item, or null for an empty list. */
    public T max(List<T> items) {
        T best = null;
        for (T item : items) {
            if (best == null || item.compareTo(best) > 0) {
                best = item;
            }
        }
        return best;
    }

    // ---------- generic METHODS (their own <T>, no class needed) ----------
    public static <E> List<E> filter(List<E> items, Predicate<E> keep) {
        List<E> result = new ArrayList<>();
        for (E item : items) {
            if (keep.test(item)) {
                result.add(item);
            }
        }
        return result;
    }

    public static <E, R> List<R> map(List<E> items, Function<E, R> change) {
        List<R> result = new ArrayList<>();
        for (E item : items) {
            result.add(change.apply(item));
        }
        return result;
    }

    /** Wildcard: accepts List<Integer>, List<Double>, ... anything that is a Number. */
    public static double sum(List<? extends Number> numbers) {
        double total = 0;
        for (Number n : numbers) {
            total += n.doubleValue();
        }
        return total;
    }
}