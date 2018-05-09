package nars.term;

import com.google.common.collect.Iterators;
import jcog.math.random.XoRoShiRo128PlusRandom;
import nars.$;
import nars.IO;
import nars.Narsese;
import nars.Op;
import nars.subterm.ArrayTermVector;
import nars.subterm.Subterms;
import nars.subterm.TermVector2;
import nars.subterm.UnitSubterm;
import nars.term.anon.Anom;
import nars.term.anon.Anon;
import nars.term.anon.AnonVector;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static nars.$.$;
import static nars.Op.CONJ;
import static nars.Op.PROD;
import static org.junit.jupiter.api.Assertions.*;

public class AnonTest {

    @Test
    public void testAtoms() throws Narsese.NarseseException {
        assertAnon("_1", "a");
        assertAnon("#1", $.varDep(1)); //unchanged
        assertAnon("_1", $.the(2)); //int remaps to internal int

        assertNotEquals(Anom.the(0), $.the(0));
        assertNotEquals(Anom.the(2), $.the(2));
    }

    @Test
    public void testCompounds() throws Narsese.NarseseException {
        assertAnon("(_1-->_2)", "(a-->b)");

        assertAnon("(_1-->#1)", "(a-->#1)");

        assertAnon("{_1}", "{a}");
        assertAnon("{_1,_2}", "{a,b}");
        assertAnon("{_1,_2,_3}", "{a,b,c}");
        assertAnon("(_1 ==>+- _2)", "(x ==>+- y)");
        {
            Anon a = assertAnon("((_1&&_2) ==>+- _3)" , "((x&&y) ==>+- z)");
            assertEquals("(x&&y)", a.get(CONJ.the(Anom.the(1), Anom.the(2))).toString());
        }


        assertAnon("(((_1-->(_2,_3,#1))==>(_4,_5)),?2)",
                "(((a-->(b,c,#2))==>(e,f)),?1)");

    }

    @Test
    public void testCompoundsWithNegations() throws Narsese.NarseseException {
        assertAnon("((--,_1),_1,_2)", "((--,a), a, c)");
        assertAnon("(--,((--,_1),_1,_2))", "--((--,a), a, c)");
    }

    @Test
    @Disabled
    public void testIntRange() throws Narsese.NarseseException {
        assertEquals("(4..6-->x)", $("((|,4,5,6)-->x)").toString());
        assertAnon("(_0-->_1)", "((|,4,5,6)-->x)");
    }



    @Test
    public void testAnomVector() {

        Term[] x = {Anom.the(3), Anom.the(1), Anom.the(2)};

        assertEqual(new UnitSubterm(x[0]), new AnonVector(x[0]));
        assertEqual(new TermVector2(x[0], x[1]), new AnonVector(x[0], x[1]));
        assertEqual(new ArrayTermVector(x), new AnonVector(x));
    }

    @Test
    public void testAnomVectorNegations() {

        Term[] x = {Anom.the(3), Anom.the(1), Anom.the(2).neg()};

        AnonVector av = new AnonVector(x);
        ArrayTermVector bv = new ArrayTermVector(x);
        assertEqual(bv, av);

        Term twoNeg = x[2];
        assertTrue(av.contains(twoNeg));
        assertTrue(av.containsRecursively(twoNeg));
        assertTrue(av.containsRecursively(twoNeg.neg()), () -> av + " containsRecursively " + twoNeg.neg());


        //assertTrue(The.subterms(x) instanceof AnonVector );
    }

    @Test
    public void testMixedAnonVector() {

        Term[] x = {$.varDep(1), $.varIndep(2), $.varQuery(3), Anom.the(4)};
        Random rng = new XoRoShiRo128PlusRandom(1);
        for (int i = 0; i < 4; i++) {

            ArrayUtils.shuffle(x, rng);

            assertEqual(new UnitSubterm(x[0]), new AnonVector(x[0]));
            assertEqual(new TermVector2(x[0], x[1]), new AnonVector(x[0], x[1]));
            assertEqual(new ArrayTermVector(x), new AnonVector(x));
        }
    }

    static Anon assertAnon(String expect, String test) throws Narsese.NarseseException {
        return assertAnon(expect, $(test));
    }

    /**
     * roundtrip test
     */
    static Anon assertAnon(String expect, Term x) {
        Anon a = new Anon();
        Term y = a.put(x);
        Term z = a.get(y);
        assertEquals(expect, y.toString());
        assertEquals(x, z);
        return a;
    }

    static void assertEqual(Subterms v, AnonVector a) {
        assertEquals(v, a);
        assertEquals(v.toString(), a.toString());
        assertEquals(v.hashCode(), a.hashCode());
        assertEquals(v.hashCodeSubterms(), a.hashCodeSubterms());
        assertTrue(Iterators.elementsEqual(v.iterator(), a.iterator()));
        assertEquals(Op.terms.theCompound(PROD, v), Op.terms.theCompound(PROD, a));

        byte[] bytesExpected = IO.termToBytes($.pFast(v));
        byte[] bytesActual = IO.termToBytes($.pFast(a));
        assertArrayEquals(bytesExpected, bytesActual);
    }

}