package nars.task;

import jcog.random.XorShift128PlusRandom;
import nars.$;
import nars.Param;
import nars.truth.DiscreteTruth;
import nars.truth.Truth;
import nars.truth.TruthFunctions;
import org.junit.Test;

import static nars.Param.TRUTH_EPSILON;
import static org.junit.Assert.*;


public class TruthTest {

    @Test
    public void testFreqEquality() {
        Truth a = new DiscreteTruth(1.0f, 0.9f);
        Truth aCopy = new DiscreteTruth(1.0f, 0.9f);
        assertEquals(a, aCopy);

        Truth aEqualWithinThresh = new DiscreteTruth(
                1.0f - Param.TRUTH_EPSILON / 2.1f /* slightly less than half */, 0.9f);
        assertEquals(a, aEqualWithinThresh);
        assertEquals(a.hashCode(), aEqualWithinThresh.hashCode());

        Truth aNotWithinThresh = new DiscreteTruth( 1.0f - Param.TRUTH_EPSILON * 1.0f, 0.9f);
        assertNotEquals(a, aNotWithinThresh);
        assertNotEquals(a.hashCode(), aNotWithinThresh.hashCode());

    }

    @Test
    public void testConfEquality() {
        Truth a = new DiscreteTruth(1.0f, 0.5f);

        Truth aEqualWithinThresh = new DiscreteTruth(1.0f, 0.5f - Param.TRUTH_EPSILON / 2.1f /* slightly less than half the epsilon */);
        assertEquals(a, aEqualWithinThresh);
        assertEquals(a.hashCode(), aEqualWithinThresh.hashCode());

        Truth aNotWithinThresh = new DiscreteTruth(1.0f, 0.5f - Param.TRUTH_EPSILON * 1.0f);
        assertNotEquals(a, aNotWithinThresh);
        assertNotEquals(a.hashCode(), aNotWithinThresh.hashCode());
    }


//    @Test public void testEpsilon() {
//        float e = 0.1f;
//
//        Truth a = BasicTruth.genew DefaultTruth(1.0f, 0.9f, e);
//        assertEquals(a.getEpsilon(), e, 0.0001);
//
//        Truth aCopy = BasicTruth.genew DefaultTruth(1.0f, 0.9f, e);
//        assertEquals(a, aCopy);
//
//        Truth aEqualWithinThresh = BasicTruth.genew DefaultTruth(1.0f - a.getEpsilon() / 2, 0.9f, e);
//        assertEquals(a, aEqualWithinThresh);
//
//        Truth aNotWithinThresh = BasicTruth.genew DefaultTruth(1.0f - a.getEpsilon(), 0.9f, e);
//        assertNotEquals(a, aNotWithinThresh);
//    }

    @Test public void testTruthHash() {
        assertEquals( new DiscreteTruth(0.5f, 0.5f).hashCode(), new DiscreteTruth(0.5f, 0.5f).hashCode() );
        assertNotEquals( new DiscreteTruth(1.0f, 0.5f).hashCode(), new DiscreteTruth(0.5f, 0.5f).hashCode() );
        assertNotEquals( new DiscreteTruth(0.51f, 0.5f).hashCode(), new DiscreteTruth(0.5f, 0.5f).hashCode() );
        assertNotEquals( new DiscreteTruth(0.506f, 0.5f).hashCode(), new DiscreteTruth(0.5f, 0.5f).hashCode() );

        assertEquals( new DiscreteTruth(0, 0.01f).hashCode(), new DiscreteTruth(0, 0.01f).hashCode() );

        //0.01 granularity
        assertEquals( new DiscreteTruth(0.504f, 0.5f, 0.01f).hashCode(), new DiscreteTruth(0.5f, 0.5f, 0.01f).hashCode() );
        assertEquals( new DiscreteTruth(0.004f, 0.01f, 0.01f).hashCode(), new DiscreteTruth(0, 0.01f, 0.01f).hashCode() );
        assertNotEquals( new DiscreteTruth(0.006f, 0.01f, 0.01f).hashCode(), new DiscreteTruth(0, 0.01f, 0.01f).hashCode() );


    }

    @Test public void testTruthHashUnhash() {
        XorShift128PlusRandom rng = new XorShift128PlusRandom(2);
        for (int i = 0; i < 1000; i++)
            hashUnhash(rng.nextFloat(), Param.TRUTH_EPSILON*2 + rng.nextFloat() * (1f-Param.TRUTH_EPSILON*2));
    }

    static void hashUnhash(float f, float c) {
        Truth t = new DiscreteTruth(f, c);
        if (t == null)
            return;
        Truth u = DiscreteTruth.intToTruth(t.hashCode());
        assertNotNull(t +  " unhased to null via hashCode " + t.hashCode(), u);
        assertEquals(t, u);
    }

//    @Test public void testInterpolate() {
//        {
//            Truth a = new DefaultTruth(0.75f, 0.5f);
//            Truth b = new DefaultTruth(0.5f, 0.25f);
//            assertEquals(new DefaultTruth(0.67f, 0.41f), a.interpolate(b));
//        }
//
//        {
//            Truth a = new DefaultTruth(0.75f, 0.25f);
//            Truth b = new DefaultTruth(0.5f, 0.5f);
//            assertEquals(new DefaultTruth(0.58f, 0.41f), a.interpolate(b));
//        }
//
//        {
//            Truth a = new DefaultTruth(0.55f, 0.25f);
//            Truth b = new DefaultTruth(0.5f, 0.5f);
//            assertEquals(new DefaultTruth(0.52f, 0.48f), a.interpolate(b));
//        }
//        Truth a = new DefaultTruth(0.95f, 0.5f);
//        Truth b = new DefaultTruth(0.5f, 0.01f);
//        assertEquals(new DefaultTruth(0.94f, 0.28f), a.interpolate(b));
//    }

    @Test
    public void testExpectation() {
        assertEquals(0.75f, new DiscreteTruth(1f, 0.5f).expectation(), 0.01f);
        assertEquals(0.95f, new DiscreteTruth(1f, 0.9f).expectation(), 0.01f);
        assertEquals(0.05f, new DiscreteTruth(0f, 0.9f).expectation(), 0.01f);
    }

    @Test public void testTruthRevision() {
        Truth d = Revision.revise(new DiscreteTruth(1f, 0.1f), new DiscreteTruth(1f, 0.1f));
        assertEquals(1f, d.freq(), 0.01f);
        assertEquals(0.18f, d.conf(), 0.01f);

        Truth a = Revision.revise(new DiscreteTruth(1f, 0.3f), new DiscreteTruth(1f, 0.3f));
        assertEquals(1f, a.freq(), 0.01f);
        assertEquals(0.46f, a.conf(), 0.01f);

        Truth b = Revision.revise(new DiscreteTruth(0f, 0.3f), new DiscreteTruth(1f, 0.3f));
        assertEquals(0.5f, b.freq(), 0.01f);
        assertEquals(0.46f, b.conf(), 0.01f);

        Truth c = Revision.revise(new DiscreteTruth(1f, 0.9f), new DiscreteTruth(1f, 0.9f));
        assertEquals(1f, c.freq(), 0.01f);
        assertEquals(0.95f, c.conf(), 0.01f);
    }


//    @Test
//    public void testXNOR() {
//
//        assertEquals(1f, xnor(1f,1f), 0.01f );
//        assertEquals(0.5f, xnor(0.5f,1f), 0.01f );
//        assertEquals(0f, xnor(0f,1f), 0.01f );
//
//        assertEquals(0.5f, xnor(0.5f,0.5f), 0.01f );
//        assertEquals(0.48f, xnor(0.4f,0.6f), 0.01f );
//
//        assertEquals(1f, xnor(0f,0f), 0.01f );
//        assertEquals(0.625f, xnor(0.25f,0.25f), 0.01f );
//    }

//    @Test
//    public void testANDB() {
//        assertEquals(1f, andb(1f,1f), 0.01f );
//        assertEquals(0f, andb(0f,0f), 0.01f );
//        assertEquals(0.5f, andb(0.5f,0.5f), 0.01f );
//        assertEquals(0.5f, andb(0.5f,0.75f), 0.01f );
//
//        assertEquals(0.5f, andb(0f,1f), 0.01f );
//
//    }




    public static void printTruthChart() {
        float c = 0.9f;
        for (float f1 = 0f; f1 <= 1.001f; f1+=0.1f) {
            for (float f2 = 0f; f2 <= 1.001f; f2+=0.1f) {
                Truth t1 = new DiscreteTruth(f1, c);
                Truth t2 = new DiscreteTruth(f2, c);
                System.out.println(t1 + " " + t2 + ":\t" +
                        TruthFunctions.comparison(t1, t2, TRUTH_EPSILON));
            }
        }
    }

    @Test public void testTruthPolarity() {
        assertEquals(0f, $.t(0.5f, 0.9f).polarization(), 0.01f);
        assertEquals(0.9f, $.t(0f, 0.9f).polarization(), 0.01f);
        assertEquals(0.9f, $.t(1f, 0.9f).polarization(), 0.01f);
        assertEquals(0.5f, $.t(1f, 0.5f).polarization(), 0.01f);
    }
}
