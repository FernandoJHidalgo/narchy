package jcog;

import jcog.random.XorShift128PlusRandom;
import org.apache.commons.math3.stat.Frequency;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DecideRouletteTest {

    @Test
    public void testDecideRouletteFlat() {
        XorShift128PlusRandom rng = new XorShift128PlusRandom(1);
        int uniques = 4;
        int samples = 100;

        Frequency f = new Frequency();
        for (int i = 0; i < samples; i++)
            f.addValue(Util.decideRoulette(uniques, (k) -> 0.5f, rng));

        //System.out.println(f);
        assertEquals(f.getUniqueCount(), uniques);
        float total = f.getSumFreq();
        for (int i = 0; i < uniques; i++)
            assertEquals(f.getCount(i)/total, 1f/uniques, 1f/(4*uniques));
    }
    @Test
    public void testDecideRouletteTriangular() {
        XorShift128PlusRandom rng = new XorShift128PlusRandom(1);
        int uniques = 10;
        int samples = 5000;

        Frequency f = new Frequency();
        for (int i = 0; i < samples; i++)
            f.addValue(Util.decideRoulette(uniques, (k) -> (k+1f)/(uniques), rng));

        System.out.println(f);
        assertEquals(f.getUniqueCount(), uniques);
        float total = f.getSumFreq();
        for (int i = 0; i < uniques; i++)
            assertEquals(f.getCount(i)/total, (i+1f)/(uniques*uniques/2), 1f/(4*uniques));
    }
}