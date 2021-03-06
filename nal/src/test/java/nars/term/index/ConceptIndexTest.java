package nars.term.index;

import nars.*;
import nars.index.concept.*;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;


class ConceptIndexTest {

    public static void testIndex(@NotNull ConceptIndex i) throws Narsese.NarseseException {

        NAR t = new NARS().index(i).withNAL(1,1).get();

        //testTermSharing(i);
        testTaskConceptSharing(t);


        long c1 = t.concepts().count();
        assertTrue(5 < c1);

        t.stop();
        t.concepts.clear();

        long c2 = t.concepts().count();
        assertTrue(c1 > c2);

    }

    private static void testTermSharing(@NotNull ConceptIndex tt) throws Narsese.NarseseException {

        tt.start(NARS.shell());
        testShared(tt, "<<x-->w> --> <y-->z>>");
        testShared(tt, "<a --> b>");
        testShared(tt, "(c, d)");
        testShared(tt, "<e <=> f>");


    }

    private static void testShared(@NotNull ConceptIndex i, @NotNull String s) throws Narsese.NarseseException {

        int t0 = i.size();


        Term a = i.get(Narsese.term(s, true), true).term();

        int t1 = i.size();


        if (a instanceof Compound) {
            assertTrue(t0 < t1);
        }

        Term a2 = i.get(Narsese.term(s, true), true).term();
        testShared(a, a2);

        assertEquals(i.size(), t1 /* unchanged */);


        Compound b = (Compound) i.get(Narsese.term('(' + s + ')', true), true).term();
        testShared(a.term(), b.sub(0));

        assertEquals(i.size(), t1 + 1 /* one more for the product container */);


    }

    private static void testShared(@NotNull Termed t1, @NotNull Termed t2) {


        assertEquals(t1.term(), t2.term());
        if (t1 != t2)
            System.err.println("share failed: " + t1 + ' ' + t1.getClass() + ' ' + t2 + ' ' + t2.getClass());

        assertEquals(t1, t2);
        assertSame(t1, t2);

        if (t1 instanceof Compound) {

            for (int i = 0; i < t1.term().subs(); i++)
                testShared(((Subterms)t1).sub(i), ((Subterms)t2).sub(i));
        }
    }

    private static void testCommonPrefix(boolean direction) {
        MaplikeConceptIndex i = (MaplikeConceptIndex) (NARS.shell().concepts);
        Atomic sui = Atomic.the("substituteIfUnifies");
        Atomic su = Atomic.the("substitute");

        if (direction) {
            i.get(sui, true);
            i.get(su, true);
        } else {
            i.get(su, true);
            i.get(sui, true);
        }


        System.out.println(i);
        i.print(System.out);


        assertEquals(sui, i.concept(sui, false).term());
        assertEquals(su, i.concept(su, false).term());
        assertNotEquals(sui, i.concept(su, false).term());

    }


    static void testTaskConceptSharing(NAR n) throws Narsese.NarseseException {



        String x = "(a --> b).";
        Task t1 = n.inputTask(x);
        Task t2 = n.inputTask(x);
        n.run();

        testShared(n.concept(t1), n.concept(t2));

        String y = "(c --> b).";
        Task t3 = n.inputTask(y);
        n.run(9);

        testShared(n.concept("b"), n.concept(t3.term().sub(1)));
    }


    @Test
    void testMapConceptIndex() throws Narsese.NarseseException {
        testIndex(
                new MapConceptIndex(new HashMap(1024))
        );
    }
    @Test
    void testHijackConceptIndex() throws Narsese.NarseseException {
        testIndex(
                new HijackConceptIndex(1024, 4)
        );
    }
    @Test
    void testCaffeineConceptIndex() throws Narsese.NarseseException {
        testIndex(
                new CaffeineIndex(1024)
        );
    }
    @Test
    void testTreeConceptIndex() throws Narsese.NarseseException {
        testIndex(
                new TreeConceptIndex(1024)
        );
    }

//    void testNotShared(@NotNull TimeAware n, @NotNull String s) throws Narsese.NarseseException {
//        Termed t1 = $.$(s);
//        Termed t2 = $.$(s);
//        assertEquals(t1, t2);
//        assertNotSame(t1, t2);
//    }

    @Disabled
    @Test
    void testRuleTermsAddedToMemoryTermIndex() {

        NAR d = NARS.shell();
        Set<Term> t = new TreeSet();
        d.concepts.forEach(x -> t.add(x.term()));

        assertTrue(t.size() > 100);


    }

    @Test
    void testCommonPrefix1() {
        testCommonPrefix(true);
    }

    @Test
    void testCommonPrefix2() {
        testCommonPrefix(false);
    }

    @Test
    void testConceptualizable() throws Narsese.NarseseException {
        Compound c = $.$("(((#1,#2,a02)-->#3)&&((#1,#2,a32)-->#3))");
        assertTrue(c.isNormalized());
        assertTrue(Task.validTaskTerm(c, (byte) 0, true));
    }
}