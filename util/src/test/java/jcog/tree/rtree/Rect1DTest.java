package jcog.tree.rtree;

import jcog.tree.rtree.rect.RectDouble1D;
import jcog.tree.rtree.split.LinearSplitLeaf;
import jcog.tree.rtree.util.Stats;
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by me on 12/2/16.
 */
public class Rect1DTest {
    @Test
    public void centroidTest() {

        RectDouble1D rect = new RectDouble1D.DefaultRect1D(0, 4);

        HyperPoint centroid = rect.center();
        double x = centroid.coord(0);
        assertTrue(x == 2.0d, "Bad X-coord of centroid - expected " + 2.0 + " but was " + x);

    }

    /**
     * Use an small bounding box to ensure that only expected rectangles are returned.
     * Verifies the count returned from search AND the number of rectangles results.
     */
    @Test
    public void rect2DSearchTest() {

        final int entryCount = 20;

        
            RTree<Double> t = new RTree<>((x) -> new RectDouble1D.DefaultRect1D(x, x), 2, 3,
                    new LinearSplitLeaf<>());
            for (int i = 0; i < entryCount; i++) {
                t.add((double)(i*i));
            }

            

            Stats s = t.stats();
            System.out.println(s);

            DoubleArrayList d = new DoubleArrayList();
            t.whileEachIntersecting(new RectDouble1D.DefaultRect1D(1, 101), d::add);

            assertEquals(10, d.size());

            System.out.println(d);





















    }
}
