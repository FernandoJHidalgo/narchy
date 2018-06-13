/*
 * Copyright 2015 S. Webber
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.oakgp.rank;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;
import static org.oakgp.TestUtils.mockNode;

public class CandidatesTest {
    private final RankedCandidate element1 = new RankedCandidate(mockNode(), -7);
    private final RankedCandidate element2 = new RankedCandidate(mockNode(), -2.25);
    private final RankedCandidate element3 = new RankedCandidate(mockNode(), 0);
    private final RankedCandidate element4 = new RankedCandidate(mockNode(), 1);
    private final RankedCandidate element5 = new RankedCandidate(mockNode(), 785.5);
    private final RankedCandidate[] input = {element5, element3, element1, element2, element4};
    private final Candidates rankedCandidates = new Candidates(input);

    @Test
    public void testSize() {
        assertEquals(5, rankedCandidates.size());
    }

    @Test
    public void testGet() {
        assertSame(element1, rankedCandidates.get(0));
        assertSame(element2, rankedCandidates.get(1));
        assertSame(element3, rankedCandidates.get(2));
        assertSame(element4, rankedCandidates.get(3));
        assertSame(element5, rankedCandidates.get(4));
    }

    @Test
    public void testNext() {
        Iterator<RankedCandidate> itr = rankedCandidates.iterator();
        assertSame(element1, itr.next());
        assertSame(element2, itr.next());
        assertSame(element3, itr.next());
        assertSame(element4, itr.next());
        assertSame(element5, itr.next());
        try {
            itr.next();
            fail("");
        } catch (NoSuchElementException e) {
            
        }
    }

    @Test
    public void testHasNext() {
        Iterator<RankedCandidate> itr = rankedCandidates.iterator();
        assertTrue(itr.hasNext());
        assertTrue(itr.hasNext());
        assertSame(element1, itr.next());
        assertTrue(itr.hasNext());
        assertSame(element2, itr.next());
        assertTrue(itr.hasNext());
        assertSame(element3, itr.next());
        assertTrue(itr.hasNext());
        assertSame(element4, itr.next());
        assertTrue(itr.hasNext());
        assertSame(element5, itr.next());
        assertFalse(itr.hasNext());
        assertFalse(itr.hasNext());
    }

    @Test
    public void testRemove() {
        assertThrows(UnsupportedOperationException.class, () -> {
            Iterator<RankedCandidate> itr = rankedCandidates.iterator();
            itr.remove();
        });
    }

    @Test
    public void testCustomComparator() {
        Candidates defaultOrderedCandidates = new Candidates(input);
        Candidates reverseOrderedCandidates = new Candidates(Stream.of(input), Collections.reverseOrder());
        assertEquals(5, defaultOrderedCandidates.size());
        assertEquals(5, reverseOrderedCandidates.size());
        assertSame(defaultOrderedCandidates.get(0), reverseOrderedCandidates.get(4));
        assertSame(defaultOrderedCandidates.get(1), reverseOrderedCandidates.get(3));
        assertSame(defaultOrderedCandidates.get(2), reverseOrderedCandidates.get(2));
        assertSame(defaultOrderedCandidates.get(3), reverseOrderedCandidates.get(1));
        assertSame(defaultOrderedCandidates.get(4), reverseOrderedCandidates.get(0));
    }

    @Test
    public void testBest() {
        Candidates defaultOrderedCandidates = new Candidates(input);
        Candidates reverseOrderedCandidates = new Candidates(Stream.of(input), Collections.reverseOrder());
        assertSame(element1, defaultOrderedCandidates.best());
        assertSame(element5, reverseOrderedCandidates.best());
    }

    @Test
    public void testStream() {
        assertEquals("[-7.0, -2.25, 0.0, 1.0, 785.5]", rankedCandidates.stream().map(c -> c.fitness).collect(toList()).toString());
    }

    @Test
    public void testImmutable() {
        
        final RankedCandidate[] input = {element3, element1, element2};
        final Candidates rankedCandidates = new Candidates(input);
        input[0] = element4;
        input[1] = element5;
        input[2] = element1;
        assertSame(element1, rankedCandidates.get(0));
        assertSame(element2, rankedCandidates.get(1));
        assertSame(element3, rankedCandidates.get(2));
    }

    @Test
    public void testGetIndexOutOfBounds() {
        assertArrayIndexOutOfBoundsException(rankedCandidates, -1);
        assertArrayIndexOutOfBoundsException(rankedCandidates, rankedCandidates.size());
        assertArrayIndexOutOfBoundsException(rankedCandidates, rankedCandidates.size() + 1);
        assertArrayIndexOutOfBoundsException(rankedCandidates, Integer.MAX_VALUE);
    }

    private void assertArrayIndexOutOfBoundsException(Candidates candidates, int index) {
        try {
            candidates.get(index);
            fail("");
        } catch (ArrayIndexOutOfBoundsException e) {
            
        }
    }
}
