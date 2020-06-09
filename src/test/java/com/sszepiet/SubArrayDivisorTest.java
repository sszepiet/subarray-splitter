package com.sszepiet;

import org.junit.Test;

import java.util.List;
import java.util.Optional;

public class SubArrayDivisorTest {

    @Test
    public void shouldDivideArrayEquallyAndFindAllSolutions() {
        final SubArraySplitter subArrayDivisor = new SubArraySplitter();
        final List<SubArraySplitter.ArrayDivisionResult> result = subArrayDivisor.divideIntoEqualSubarrays(new int[]{10, 7, 2, 10, 5, 1, 1, 4
                , 1, 11, 5}, 3, 2);
        result.forEach(System.out::println);
        System.out.println("Candidate counter: " + SubArraySplitter.ArrayDivisionResult.CANDIDATE_COUNTER.get());
        SubArraySplitter.ArrayDivisionResult.CANDIDATE_COUNTER.set(0);
    }

    @Test
    public void shouldDivideArrayEquallyAndFindFirst() {
        final SubArraySplitter subArrayDivisor = new SubArraySplitter();
        final Optional<SubArraySplitter.ArrayDivisionResult> result = subArrayDivisor.findFirstResult(new int[]{10, 7, 2, 10, 5, 1, 1, 4
                , 1, 11, 5}, 3, 2);
        System.out.println("Candidate counter: " + SubArraySplitter.ArrayDivisionResult.CANDIDATE_COUNTER.get());
        SubArraySplitter.ArrayDivisionResult.CANDIDATE_COUNTER.set(0);
    }
}
