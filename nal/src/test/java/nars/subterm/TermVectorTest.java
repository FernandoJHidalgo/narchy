package nars.subterm;

import nars.$;
import nars.Narsese;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Atomic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 11/12/15.
 */
public class TermVectorTest {

    @Test
    public void testSubtermsEquality() throws Narsese.NarseseException {

        Term a = $.$("(a-->b)");
        

        













        
        

        













        
        Compound b = $.impl(Atomic.the("a"), Atomic.the("b"));

        assertEquals(a.subterms(), b.subterms());
        assertEquals(a.subterms().hashCode(), b.subterms().hashCode());

        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());

        assertEquals(0, Subterms.compare(a.subterms(), b.subterms()));
        assertEquals(0, Subterms.compare(b.subterms(), a.subterms()));

        assertNotEquals(0, a.compareTo(b));
        assertNotEquals(0, b.compareTo(a));

        /*assertTrue("after equality test, subterms vector determined shareable",
                a.subterms() == b.subterms());*/


    }

    @Test public void testSortedTermContainer() throws Narsese.NarseseException {
        Term aa = $.$("a");
        Term bb = $.$("b");
        Subterms a = Op.terms.newSubterms(aa, bb);
        assertTrue(a.isSorted());
        Subterms b = Op.terms.newSubterms(bb, aa);
        assertFalse(b.isSorted());
        Subterms s = Op.terms.newSubterms(Terms.sorted(b.arrayShared()));
        assertTrue(s.isSorted());
        assertEquals(a, s);
        assertNotEquals(b, s);
    }



}
