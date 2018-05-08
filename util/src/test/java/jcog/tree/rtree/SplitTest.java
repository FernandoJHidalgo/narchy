package jcog.tree.rtree;

import jcog.tree.rtree.rect.RectDouble2D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SplitTest {

        /**
     * Adds many random entries to trees of different types and confirms that
     * no entries are lost during insert/split.
     */
    @Test
    public void randomEntryTest() {

        int entryCount = 32*1024;

        final RectDouble2D[] rects = RTree2DTest.generateRandomRects(entryCount);

        for (Spatialization.DefaultSplits s : Spatialization.DefaultSplits.values()) {
            for (int min : new int[]{2, 3, 4}) {
                for (int max : new int[]{min, min+1, 8}) {

                    int TOTAL = Math.round(max/8f * rects.length);
                    if (TOTAL%2==1) TOTAL++;

                    assert(TOTAL<=entryCount); //even # only
                    assert(TOTAL%2==0); //even # only
                    int HALF = TOTAL/2;


                    final RTree<RectDouble2D> t = RTree2DTest.createRect2DTree(s, min, max);
                    int i = 0;
                    for (int i1 = 0; i1 < HALF; i1++) {
                        RectDouble2D r = rects[i1];
                        boolean added = t.add(r);
                        if (!added) {
                            t.add(r); //for debugging: try again and see what happened
                            fail("");
                        }
                        assertTrue(added);
                        assertEquals(++i, t.size());
                        //assertEquals(i, rTree.stats().getEntryCount());

                        boolean tryAddingAgainToTestForNonMutation = t.add(r);
                        if (tryAddingAgainToTestForNonMutation) {
                            t.add(r); //for debugging: try again and see what happened
                            fail("");
                        }
                        assertFalse(tryAddingAgainToTestForNonMutation, i + "==?" + t.size()); //reinsertion of existing element will not affect size and will return false here
                        assertEquals(i, t.size()); //reinsertion should cause no change in size
                        //assertEquals(i, rTree.stats().getEntryCount());
                    }


                    assertEquals(HALF, t.size());

                    assertEquals(HALF, t.stats().print(System.out).size());


                    for (int k = 0; k < HALF; k++) {
                        assertFalse(t.add(rects[k])); //should detect the duplicate
                    }

                    for (int k = 0; k < HALF; k++) {
                        RectDouble2D a = rects[k];
                        RectDouble2D b = rects[k + HALF];
                        assertNotEquals(a,b);

                        assertFalse(t.contains(b));
                        assertTrue(t.contains(a));
                        t.replace(a, b);

//                        if (t.contains(a))
//                            t.replace(a, b); //try again for debugging

                        assertFalse(t.contains(a));
                        assertTrue(t.contains(b));

                        assertEquals(HALF, t.size()); //size unchanged

                    }

                    //size should remain equal
                    assertEquals(HALF, t.size());

                    assertEquals(HALF, t.stats().size());

                    for (int k = 0; k < HALF; k++) {
                        assertTrue(t.add(rects[k])); //should be unique, so it should be dded
                    }


                    assertEquals(TOTAL, t.size());
                    assertEquals(TOTAL, t.stats().size());

                    final int[] andCount = {0};
                    assertTrue(t.root().AND(x -> {
                        andCount[0]++;
                        return true;
                    }));
                    assertEquals(TOTAL, andCount[0]);

                    final int[] orCount = {0};
                    assertFalse(t.OR(x -> {
                        orCount[0]++;
                        return false;
                    }));
                    assertEquals(TOTAL, orCount[0]);

                    final int[] eachCount= {0};
                    t.forEach(x -> {
                        eachCount[0]++;
                    });
                    assertEquals(TOTAL, eachCount[0]);
                }
            }
        }
    }

}
