package nars.term.compound.util;

import jcog.list.FasterList;
import jcog.math.random.XoRoShiRo128PlusRandom;
import nars.$;
import nars.term.Term;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.roaringbitmap.RoaringBitmap;

import java.util.Random;

import static nars.$.$$;
import static nars.Op.False;
import static nars.Op.True;
import static nars.time.Tense.ETERNAL;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;
import static org.junit.jupiter.api.Assertions.*;

public class ConjTest {

    @Test public void testSimpleEternals() {
        Conj c = new Conj();
        c.add(ETERNAL, $.the("x"));
        c.add(ETERNAL, $.the("y"));
        assertEquals("(x&&y)", c.term().toString());
        assertEquals(1, c.event.size());
        assertEquals(byte[].class, c.event.get(ETERNAL).getClass());
    }

    @Test public void testSimpleEternalsNeg() {
        Conj c = new Conj();
        c.add(ETERNAL, $.the("x"));
        c.add(ETERNAL, $.the("y").neg());
        assertEquals("((--,y)&&x)", c.term().toString());
    }

    @Test public void testSimpleEvents() {
        Conj c = new Conj();
        c.add(1, $.the("x"));
        c.add(2, $.the("y"));
        assertEquals("(x &&+1 y)", c.term().toString());
        assertEquals(1, c.shift());
        assertEquals(2, c.event.size());
    }

    @Test public void testRoaringBitmapNeededManyEventsAtSameTime() {
        Conj b = new Conj();
        for (int i = 0; i < Conj.ROARING_UPGRADE_THRESH-1; i++)
            b.add(1, $.the(String.valueOf((char)('a' + i))));
        assertEquals("(&|,a,b,c,d,e,f,g)", b.term().toString());
        assertEquals(1, b.event.size());
        assertEquals(byte[].class, b.event.get(1).getClass());

        Conj c = new Conj();
        for (int i = 0; i < Conj.ROARING_UPGRADE_THRESH+1; i++)
            c.add(1, $.the(String.valueOf((char)('a' + i))));
        assertEquals("(&|,a,b,c,d,e,f,g,h,i)", c.term().toString());
        assertEquals(1, c.event.size());
        assertEquals(RoaringBitmap.class, c.event.get(1).getClass());
    }

    @Test public void testSimpleEventsNeg() {
        Conj c = new Conj();
        c.add(1, $.the("x"));
        c.add(2, $.the("y").neg());
        assertEquals("(x &&+1 (--,y))", c.term().toString());
    }

    @Test public void testEventContradiction() {
        Conj c = new Conj();
        c.add(1, $.the("x"));
        assertFalse(c.add(1, $.the("x").neg()));
        assertEquals(False, c.term());
    }
    @Test public void testEventContradictionAmongNonContradictions() {
        Conj c = new Conj();
        c.add(1, $.the("x"));
        c.add(1, $.the("y"));
        c.add(1, $.the("z"));
        assertFalse(c.add(1, $.the("x").neg()));
        assertEquals(False, c.term());
    }
    @Test public void testEventContradictionAmongNonContradictionsRoaring() {
        Conj c = new Conj();
        c.add(ETERNAL, $$("(&&,a,b,c,d,e,f,g,h)"));
        boolean added = c.add(1, $.the("a").neg());
        assertEquals(False, c.term());
    }
    @Test public void testEventContradictionWithEternal() {
        Conj c = new Conj();
        c.add(ETERNAL, $.the("x"));
        boolean added = c.add(1, $.the("x").neg());
        assertEquals(False, c.term());
    }
    @Test public void testEventNonContradictionWithEternal() {
        Conj c = new Conj();
        c.add(ETERNAL, $.the("x"));
        boolean added = c.add(1, $.the("y"));
        assertTrue(added);
        assertEquals("(x&&y)", c.term().toString());
    }
    @Test public void testEventNonContradictionWithEternal2() {
        Conj c = new Conj();
        c.add(ETERNAL, $.the("x"));
        c.add(1, $.the("y"));
        c.add(2, $.the("z"));
        assertEquals("((y &&+1 z)&&x)", c.term().toString());
    }

    final Random rng = new XoRoShiRo128PlusRandom(1);

    @Test
    public void testConjEventConsistency3ary() {
        for (int i = 0; i < 100; i++) {
            assertConsistentConj(3, 0, 7);
        }
    }
    @Test
    public void testConjEventConsistency4ary() {
        for (int i = 0; i < 100; i++) {
            assertConsistentConj(4, 0, 11);
        }
    }
    @Test
    public void testConjEventConsistency5ary() {
        for (int i = 0; i < 300; i++) {
            assertConsistentConj(5, 0, 17);
        }
    }

    private void assertConsistentConj(int variety, int start, int end) {
        FasterList<LongObjectPair<Term>> x = newRandomEvents(variety, start, end);

        Term y = Conj.conj(x.clone());
        FasterList<LongObjectPair<Term>> z = y.eventList();


        System.out.println(x + "\t" + y + "\t" + z);

        
        if (!x.equals(z)) {
            Term y2 = Conj.conj(x.clone());
        }

        assertEquals(x, z);
    }

    private FasterList<LongObjectPair<Term>> newRandomEvents(int variety, int start, int end) {
        FasterList<LongObjectPair<Term>> e = new FasterList<>();
        long earliest = Long.MAX_VALUE;
        for (int i = 0; i < variety; i++) {
            long at = (long) rng.nextInt(end - start) + start;
            earliest = Math.min(at, earliest);
            e.add(pair(at, $.the(String.valueOf((char)('a' + i)))));
        }
        
        long finalEarliest = earliest;
        e.replaceAll((x)-> pair(x.getOne()- finalEarliest, x.getTwo()));
        e.sortThisByLong(LongObjectPair::getOne);
        return e;
    }

    @Test public void testConjComplexAddRemove() {
        Term x = $$("(( ( ((_1-->_2),_3) &| (--,_4)) &| (_5 &| _6)) &&+8 ( (((_1-->_2),_3) &| (--,_4)) &| (_5 &|_6))))");
        Conj c = Conj.from(x);
        assertEquals(x, c.term());
        boolean removedLast = c.remove($$("((_1-->_2),_3)"), c.event.keysView().max());
        assertTrue(removedLast);
        assertEquals(
                "((&|,((_1-->_2),_3),(--,_4),_5,_6) &&+8 (&|,(--,_4),_5,_6))",
                c.term().toString());
        boolean removedFirst = c.remove($$("((_1-->_2),_3)"), c.event.keysView().min());
        assertTrue(removedFirst);
        assertEquals(
                "((&|,(--,_4),_5,_6) &&+8 (&|,(--,_4),_5,_6))",
                c.term().toString());

    }

    @Test
    public void testWrappingCommutiveConjunction() {

        {

            
            Term xEternal = $$("((((--,angX) &&+4 x) &&+10244 angX) && y)");
            assertEquals("((((--,angX) &&+4 x) &&+10244 angX)&&y)",
                    xEternal.toString());
        }
    }
    @Test @Disabled
    public void testWrappingCommutiveConjunctionX() {
        {
            
            Term xFactored = $$("((x&&y) &&+1 (y&&z))");
            assertEquals("((x &&+1 z)&&y)", xFactored.toString());

            
            Term xAndContradict = $$("((x &&+1 x)&&--x)");
            assertEquals(False,
                    xAndContradict);

            
            Term xAndRedundant = $$("((x &&+1 x)&&x)");
            assertEquals("(x &&+1 x)",
                    xAndRedundant.toString());

            
            Term xAndRedundantParallel = $$("(((x &| y) &| z)&&x)");
            assertEquals("(&|,x,y,z)",
                    xAndRedundantParallel.toString());

            
            Term xAndContradictParallel = $$("(((x &| y) &| z)&&--x)");
            assertEquals(False,
                    xAndContradictParallel);

            
            Term xAndContradictParallelMultiple = $$("(&&,x,y,((x &| y) &| z))");
            assertEquals("(&|,x,y,z)",
                    xAndContradictParallelMultiple.toString());

            
            Term xAndContradict2 = $$("((((--,angX) &&+4 x) &&+10244 angX) && --x)");
            assertEquals(False, xAndContradict2);

            
            Term xAndContradict3 = $$("((((--,angX) &&+4 x) &&+10244 angX) && angX)");
            assertEquals(False, xAndContradict3);

            
            Term xParallel = $$("((((--,angX) &&+4 x) &&+10244 angX) &&+0 y)");
            assertEquals(False, xParallel);

        }

        {
            

            Term xParallelContradiction4 = $$("((((--,angX) &&+4 x) &&+10244 angX) &&+0 angX)");
            assertEquals(False, xParallelContradiction4);
        }


        {
            Term x = $$("((((--,angX) &&+4 x) &&+10244 angX) &| angX)");
            Term y = $$("(angX &| (((--,angX) &&+4 x) &&+10244 angX))");
            assertEquals(x, y);
            
        }
    }

    @Disabled
    @Test
    public void testFactorFromEventSequence() {
        Term yParallel1 = $$("((((--,angX) &&+4 x) &&+10244 angX) &&+0 y)");
        String yParallel2Str = "((((--,angX)&|y) &&+4 (x&|y)) &&+10244 (angX&|y))";
        Term yParallel2 = $$(yParallel2Str);
        assertEquals(yParallel1, yParallel2);
        assertEquals(yParallel2Str, yParallel1.toString());
    }
    @Disabled
    @Test
    public void testFactorFromEventParallel() {
        Term yParallelOK = $$("(((a&&x) &| (b&&x)) &| (c&&x))");
        assertEquals("", yParallelOK.toString());
        

        Term yParallelContradict = $$("((a&&x) &| (b&&--x))");
        assertEquals(False, yParallelContradict);
    }

    @Test public void testConjWithoutAll() {
        assertEquals("(a&&b)", Conj.withoutAll(
                $$("(&&,a,b,c)"),
                $$("(&&,c,d,e)")).toString());

        assertEquals("(a&|b)", Conj.withoutAll(
                $$("(&|,a,b,c)"),
                $$("(&|,c,d,e)")).toString());


        assertEquals(
                //"(&&,a,b,c)",
                "(a&&b)",
                Conj.withoutAll(
                $$("(&&,a,b,c)"),
                $$("(&|,c,d,e)")).toString());

        assertEquals("(a&&b)", Conj.withoutAll(
                $$("(&&,a,b,--c)"),
                $$("(&&,--c,d,e)")).toString());
    }

    @Test public void testConjWithoutAllMixEternalAndParallel() {

        Term x = $$("((b&&c)&|(x&&y))");
        assertEquals("((b&&c)&|(x&&y))", x.toString());

        Term y = $$("(&|,(b&&c),x)");
        assertEquals("((b&&c)&|x)", y.toString());

        assertEquals("(x&&y)", Conj.withoutAll(x, y).toString());

    }

    @Test public void testEmptyConjResultTerm() {
        Conj c = new Conj();
        assertEquals(True, c.term());
    }
    @Test public void testEmptyConjTrueEternal() {
        Conj c = new Conj();
        c.add(ETERNAL, True);
        assertEquals(True, c.term());
    }
    @Test public void testEmptyConjTrueTemporal() {
        Conj c = new Conj();
        c.add(0, True);
        assertEquals(True, c.term());
    }
    @Test public void testEmptyConjFalseEternal() {
        Conj c = new Conj();
        c.add(ETERNAL, False);
        assertEquals(False, c.term());
    }
    @Test public void testEmptyConjFalseTemporal() {
        Conj c = new Conj();
        c.add(0, False);
        assertEquals(False, c.term());
    }
    @Test public void testEmptyConjFalseEternalShortCircuit() {
        Conj c = new Conj();
        c.add(ETERNAL, $$("x"));
        boolean addedFalse = c.add(ETERNAL, False);
        assertFalse(addedFalse);
        //boolean addedAfterFalse = c.add($$("y"), ETERNAL);
        assertEquals(False, c.term());
    }

    @Test public void testEmptyConjFalseTemporalShortCircuit() {
        Conj c = new Conj();
        c.add(0, $$("x"));
        boolean addedFalse = c.add(0, False);
        assertFalse(addedFalse);
        //boolean addedAfterFalse = c.add($$("y"), 0);
        assertEquals(False, c.term());
    }

}

