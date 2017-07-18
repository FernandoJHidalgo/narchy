package nars.index.term.tree;

import jcog.tree.radix.MyConcurrentRadixTree;
import nars.Narsese;
import org.junit.Test;

import java.util.List;

import static nars.$.$;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 10/21/16.
 */
public class TreeTermIndexTest {

    @Test
    public void testVolumeSubTrees() throws Narsese.NarseseException {
        TreeTermIndex t = new TreeTermIndex( 128);
        t.set($("a"));
        t.set($("(a)"));
        t.set($("(a-->b)"));
        t.set($("(a-->(b,c,d))"));
        t.set($("(a-->(b,c,d,e,f,g))"));
        t.set($("(a-->(b,c,d,e,f,g,h,i,j,k))"));
        t.concepts.prettyPrint(System.out);
        t.print(System.out);
        assertEquals(6, t.size());
        System.out.println(t.concepts.root);

        //each item is in a different leaf because of the volume byte prefix
        List<MyConcurrentRadixTree.Node> oe = t.concepts.root.getOutgoingEdges();
        assertEquals(6, oe.size());

        assertTrue(oe.get(0).toString().length() < oe.get(1).toString().length());
        assertTrue(oe.get(0).toString().length() < oe.get(oe.size()-1).toString().length());
    }
}