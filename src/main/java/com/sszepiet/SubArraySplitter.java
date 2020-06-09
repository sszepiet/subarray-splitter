package com.sszepiet;

import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SubArraySplitter {

    public List<ArrayDivisionResult> divideIntoEqualSubarrays(final int[] n, int numberOfSubarrays, int numberOfElementsToDrop) {
        return doDivision(n, numberOfSubarrays, numberOfElementsToDrop)
                .filter(ArrayDivisionResult::isSuccessful)
                .collect(Collectors.toList());
    }

    public Optional<ArrayDivisionResult> findFirstResult(final int[] n, int numberOfSubarrays, int numberOfElementsToDrop) {
        return doDivision(n, numberOfSubarrays, numberOfElementsToDrop)
                .filter(ArrayDivisionResult::isSuccessful)
                .findFirst();
    }

    private Stream<ArrayDivisionResult> doDivision(final int[] n, int numberOfSubarrays, int numberOfElementsToDrop) {
        long[] sumOfN = new long[n.length];
        PriorityQueue<Integer> minElements = new PriorityQueue<>(Collections.reverseOrder());
        PriorityQueue<Integer> maxElements = new PriorityQueue<>();
        for (int i = 0; i < numberOfElementsToDrop; i++) {
            minElements.add(n[i]);
            maxElements.add(n[i]);
            if (i - 1 >= 0) {
                sumOfN[i] = n[i] + sumOfN[i - 1];
            } else {
                sumOfN[i] = n[i];
            }
        }
        for (int i = numberOfElementsToDrop; i < n.length; i++) {
            sumOfN[i] = n[i] + sumOfN[i - 1];
            minElements.offer(Math.min(minElements.poll(), n[i]));
            maxElements.offer(Math.max(maxElements.poll(), n[i]));
        }
        final long lowerBound = new BigDecimal(sumOfN[n.length - 1])
                .subtract(new BigDecimal(maxElements.stream().reduce(0, Integer::sum)))
                .divide(new BigDecimal(numberOfSubarrays), RoundingMode.CEILING)
                .longValue();
        final long upperBound = new BigDecimal(sumOfN[n.length - 1])
                .subtract(new BigDecimal(minElements.stream().reduce(0, Integer::sum)))
                .divide(new BigDecimal(numberOfSubarrays), RoundingMode.FLOOR)
                .longValue();

        final ArrayDivisionResult result = new ArrayDivisionResult(sumOfN, lowerBound, upperBound, numberOfSubarrays, numberOfElementsToDrop);

        return IntStream.rangeClosed(0, numberOfElementsToDrop)
                .mapToObj(result::withLeadingDrops)
                .flatMap(this::goDeeper);
    }

    private Stream<ArrayDivisionResult> goDeeper(ArrayDivisionResult result) {
        if (result.isSuccessful()) {
            System.out.println("Current result is successful!");
            return Stream.of(result);
        }
        final int lowerBoundIndex = result.binarySearchLowerBoundIndex();
        final int upperBoundIndex = result.binarySearchUpperBoundIndex();
        if (!result.subarrays.isEmpty() && result.sumUpTo[lowerBoundIndex] - result.sumUpTo[result.getStartIndex() - 1] != result.lowerBound) {
            System.out.println("Found subarray does not match requirements, rolling back");
            return Stream.empty();
        }
        return IntStream.rangeClosed(lowerBoundIndex, upperBoundIndex)
                .mapToObj(i -> createResultsWithAllDroppedNumberVariations(result, i))
                .flatMap(Function.identity())
                .flatMap(this::goDeeper)
                .filter(ArrayDivisionResult::isSuccessful)
                .findFirst().stream();
    }

    private Stream<ArrayDivisionResult> createResultsWithAllDroppedNumberVariations(ArrayDivisionResult result, int targetIndex) {
        return IntStream.rangeClosed(0, result.remainingDrops())
                .mapToObj(i -> result.withNextSubarray(new Subarray(result.getStartIndex(), targetIndex)).withFollowingDrops(i));
    }

    @ToString
    public static class ArrayDivisionResult {
        public static final AtomicInteger CANDIDATE_COUNTER = new AtomicInteger();

        private final long[] sumUpTo;

        private final long lowerBound;
        private final long upperBound;

        private final int numberOfSubarrays;
        private final int numberOfDrops;

        private final List<Subarray> subarrays;
        private final List<Integer> droppedIndexes;

        public ArrayDivisionResult(long[] sumUpTo, long lowerBound, long upperBound, int numberOfSubarrays, int dropExactly) {
            this.sumUpTo = sumUpTo;
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
            this.numberOfSubarrays = numberOfSubarrays;
            this.numberOfDrops = dropExactly;
            this.subarrays = new ArrayList<>(numberOfSubarrays);
            this.droppedIndexes = new ArrayList<>(dropExactly);
        }

        public ArrayDivisionResult withLeadingDrops(int numberOfLeadingDrops) {
            if (numberOfLeadingDrops > 0) {
                final ArrayDivisionResult result = new ArrayDivisionResult(this.sumUpTo, this.lowerBound, this.upperBound,
                        this.numberOfSubarrays, this.numberOfDrops);
                IntStream.range(0, numberOfLeadingDrops)
                        .forEach(result.droppedIndexes::add);
                return result;
            } else {
                return this;
            }
        }

        public ArrayDivisionResult withFollowingDrops(int numberOfFollowingDrops) {
            if (numberOfFollowingDrops > 0) {
                final ArrayDivisionResult result = new ArrayDivisionResult(this.sumUpTo, this.lowerBound, this.upperBound,
                        this.numberOfSubarrays, this.numberOfDrops);
                result.subarrays.addAll(this.subarrays);
                result.droppedIndexes.addAll(this.droppedIndexes);
                IntStream.range(this.getStartIndex(), this.getStartIndex() + numberOfFollowingDrops)
                        .forEach(result.droppedIndexes::add);
                System.out.println("New candidate in town: " + result);
                CANDIDATE_COUNTER.incrementAndGet();
                return result;
            } else {
                System.out.println("New candidate in town: " + this);
                CANDIDATE_COUNTER.incrementAndGet();
                return this;
            }
        }

        public ArrayDivisionResult withNextSubarray(Subarray subarray) {
            long arraySum;
            if (this.subarrays.isEmpty()) {
                arraySum = sumUpTo[subarray.endIndex];
            } else {
                arraySum = this.lowerBound;
            }
            final ArrayDivisionResult result = new ArrayDivisionResult(sumUpTo, arraySum, arraySum, this.numberOfSubarrays, this.numberOfDrops);
            result.droppedIndexes.addAll(this.droppedIndexes);
            result.subarrays.addAll(this.subarrays);
            result.subarrays.add(subarray);
            return result;
        }

        public int getStartIndex() {
            return Math.max(!subarrays.isEmpty() ? subarrays.get(subarrays.size() - 1).endIndex + 1: 0,
                    !droppedIndexes.isEmpty() ? droppedIndexes.get(droppedIndexes.size() - 1) + 1 : 0);
        }

        public int remainingDrops() {
            return numberOfDrops - droppedIndexes.size();
        }

        public int binarySearchLowerBoundIndex() {
            return binarySearchLowerBound(getStartIndex(), sumUpTo.length - 1);
        }

        public int binarySearchUpperBoundIndex() {
            return binarySearchUpperBound(getStartIndex(), sumUpTo.length - 1);
        }

        public boolean isSuccessful() {
            return subarrays.size() == this.numberOfSubarrays && this.droppedIndexes.size() == this.numberOfDrops;
        }

        private int binarySearchLowerBound(int low, int high) {
            long offset = 0;
            if (getStartIndex() > 0) {
                offset = sumUpTo[getStartIndex() - 1];
            }
            while (low < high) {
                int mid = ((low + high) / 2);
                if (sumUpTo[mid] - offset < lowerBound) {
                    low = mid + 1;
                } else if (sumUpTo[mid] - offset > lowerBound) {
                    high = mid - 1;
                } else if (sumUpTo[mid] - offset == lowerBound) {
                    return mid;
                }
            }
            return high;
        }

        private int binarySearchUpperBound(int low, int high) {
            long offset = 0;
            if (getStartIndex() > 0) {
                offset = sumUpTo[getStartIndex() - 1];
            }
            while (low < high) {
                int mid = ((low + high) / 2);
                if (sumUpTo[mid] - offset < upperBound) {
                    low = mid + 1;
                } else if (sumUpTo[mid] - offset > upperBound) {
                    high = mid - 1;
                } else if (sumUpTo[mid] - offset == upperBound) {
                    return mid;
                }
            }
            return low;
        }
    }

    @ToString
    public static class Subarray {
        int startIndex;
        int endIndex;

        public Subarray(int startIndex, int endIndex) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }
    }
}


