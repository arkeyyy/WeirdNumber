package org.example.weird_number;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class WeirdNumbersTest {
    @Test
    void distinguishesWeirdNumbersFromOtherDivisorPatterns() {
        for (int value : new int[]{1, 2, 6, 12, 16, 36, 945, 1_800_000_000, Integer.MAX_VALUE}) {
            assertFalse(WeirdNumbers.isWeird(value), Integer.toString(value));
        }
        assertTrue(WeirdNumbers.isWeird(70));
        assertTrue(WeirdNumbers.isWeird(836));
        // 70 times the prime 30,000,001 has a proper-divisor sum of 2,220,000,218.
        assertTrue(WeirdNumbers.isWeird(2_100_000_070));
    }

    @Test
    void sampleMatchesAcrossDifferentPartitions() {
        List<Integer> expected = List.of(70, 836, 4030, 5830, 7192, 7912, 9272, 10430, 10570,
                10792, 10990, 11410, 11690, 12110, 12530, 12670, 13370, 13510, 13790,
                13930, 14770, 15610, 15890);
        for (int workers : List.of(1, 4, 11)) {
            List<Integer> matches = new ArrayList<>();
            int checked = 0;
            for (WeirdNumbers.Range range : WeirdNumbers.ranges(16000, workers)) {
                checked += range.size();
                IntStream.rangeClosed(range.first(), range.last())
                        .filter(WeirdNumbers::isWeird).forEach(matches::add);
            }
            assertEquals(expected, matches);
            assertEquals(16000, checked);
            assertEquals(15977, checked - matches.size());
        }
    }

    @Test
    void sampleRangesHaveIncreasingWeights() {
        assertEquals(List.of(new WeirdNumbers.Range(1, 1600), new WeirdNumbers.Range(1601, 4800),
                             new WeirdNumbers.Range(4801, 9600), new WeirdNumbers.Range(9601, 16000)),
                WeirdNumbers.ranges(16000, 4));
    }

    @Test
    void partitionsAreNonemptyAndCoverTheWholeInterval() {
        for (int upperLimit : List.of(1, 4, 5, 64, 65, 91, 101, Integer.MAX_VALUE)) {
            for (int workers = 1; workers <= Math.min(upperLimit, 64); workers++) {
                List<WeirdNumbers.Range> ranges = WeirdNumbers.ranges(upperLimit, workers);
                assertEquals(workers, ranges.size());
                int previous = 0;
                for (WeirdNumbers.Range range : ranges) {
                    assertEquals(previous + 1, range.first());
                    assertTrue(range.size() > 0);
                    previous = range.last();
                }
                assertEquals(upperLimit, previous);
                if (workers > 1 && workers < upperLimit) {
                    assertTrue(ranges.stream().map(WeirdNumbers.Range::size).distinct().count() > 1);
                }
            }
        }
    }

    @Test
    void invalidPartitionsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> WeirdNumbers.ranges(0, 1));
        assertThrows(IllegalArgumentException.class, () -> WeirdNumbers.ranges(100, 0));
        assertThrows(IllegalArgumentException.class, () -> WeirdNumbers.ranges(3, 4));
        assertThrows(IllegalArgumentException.class, () -> WeirdNumbers.ranges(100, 65));
    }

    @Test
    void interruptedWorkerCancelsClassification() {
        Thread.currentThread().interrupt();
        try {
            assertThrows(CancellationException.class, () -> WeirdNumbers.isWeird(836));
        } finally {
            Thread.interrupted();
        }
    }
}
