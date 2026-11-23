package org.example.weird_number;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;

final class WeirdNumbers {
    private WeirdNumbers() {
    }

    record Range(int first, int last) {
        int size() {
            return last - first + 1;
        }
    }

    static List<Range> ranges(int upperLimit, int workers) {
        if (workers < 1 || workers > Math.min(upperLimit, 64)) {
            throw new IllegalArgumentException("Invalid upper limit or thread count.");
        }
        List<Range> ranges = new ArrayList<>(workers);
        int totalWeight = workers * (workers + 1) / 2;
        int last = 0;
        for (int worker = 1; worker <= workers; worker++) {
            int cumulativeWeight = worker * (worker + 1) / 2;
            int weightedEnd = (int) ((long) upperLimit * cumulativeWeight / totalWeight);
            int first = last + 1;
            last = Math.max(first, Math.min(weightedEnd, upperLimit - (workers - worker)));
            ranges.add(new Range(first, last));
        }
        return List.copyOf(ranges);
    }

    static boolean isWeird(int value) {
        checkInterrupted();
        if (value <= 1) {
            return false;
        }

        List<Integer> divisors = new ArrayList<>();
        divisors.add(1);
        long sum = 1;
        for (int factor = 2; (long) factor * factor <= value; factor++) {
            if ((factor & 1023) == 0) {
                checkInterrupted();
            }
            if (value % factor == 0) {
                divisors.add(factor);
                sum += factor;
                int partner = value / factor;
                if (factor != partner) {
                    divisors.add(partner);
                    sum += partner;
                }
            }
        }
        if (sum <= value) {
            return false;
        }

        Collections.sort(divisors);
        long[] prefixSums = new long[divisors.size() + 1];
        long covered = 0;
        for (int index = 0; index < divisors.size(); index++) {
            int divisor = divisors.get(index);
            prefixSums[index + 1] = prefixSums[index] + divisor;
            // The intervals [0, covered] and [divisor, divisor + covered] touch or overlap.
            if (divisor <= covered + 1) {
                covered += divisor;
            }
        }
        return !hasSubset(divisors, prefixSums, covered, divisors.size(), value);
    }

    private static boolean hasSubset(List<Integer> divisors, long[] prefixSums, long covered,
                                     int count, long target) {
        while (target <= prefixSums[count]) {
            checkInterrupted();
            // A subset and its complement are equivalent searches over this prefix.
            target = Math.min(target, prefixSums[count] - target);
            if (target <= covered) {
                return true;
            }
            int divisor = divisors.get(--count);
            if (divisor <= target && hasSubset(divisors, prefixSums, covered, count, target - divisor)) {
                return true;
            }
        }
        return false;
    }

    private static void checkInterrupted() {
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException();
        }
    }
}
