package nars.task;

import jcog.math.MultiStatistics;
import jcog.meter.event.CSVOutput;
import nars.*;
import nars.concept.BaseConcept;
import nars.table.BeliefTable;
import nars.table.RTreeBeliefTable;
import nars.task.DerivedTask;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.LongToFloatFunction;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static jcog.Texts.n4;
import static nars.Op.BELIEF;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RTreeBeliefTableTest {

    @Test
    public void testBasicOperations() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        BaseConcept X = (BaseConcept) n.conceptualize($.$("a:b"));
        RTreeBeliefTable r = new RTreeBeliefTable();
        r.setCapacity(4);

        assertEquals(0, r.size());

        Term x = X.term();
        float freq = 1f;
        float conf = 0.9f;
        int creationTime = 1;
        int start = 1, end = 1;

        Task a = add(r, x, freq, conf, start, end, n);
        assertEquals(1, r.size());

        r.add(a, X, n); assertEquals(1, r.size()); //no change for inserted duplicate

        Task b = add(r, x, 0f, 0.5f, 1, 1, n); //WEAKer
        assertEquals(2, r.size());

        Task c = add(r, x, 0.1f, 0.9f, 2, 2, n);
        assertEquals(3, r.size());

        Task d = add(r, x, 0.1f, 0.9f, 3, 4, n);
        assertEquals(4, r.size());

        System.out.println("at capacity");
        r.print(System.out);

        //try capacity limit
        Task e = add(r, x, 0.3f, 0.9f, 3, 4, n);
        assertEquals(4, r.size()); //capacity limit unaffected

        System.out.println("after capacity compress inserting " + e.toString(true));
        r.print(System.out);
    }

    @NotNull
    public Task add(RTreeBeliefTable r, Term x, float freq, float conf, int start, int end, NAR n) {
        Task a = $.belief(x, freq, conf).time(start, start, end).apply(n);
        a.pri(0.5f);
        r.add(a, (BaseConcept) n.concept(x), n);
        return a;
    }


    @Test public void testAccuracyFlat() {

        testAccuracy(1, 1,20, 8, (t) -> 0.5f); //flat
    }
    @Test public void testAccuracySineDur1() {

        testAccuracy(1, 1,20, 8, (t) -> (float)(Math.sin(t/5f)/2f+0.5f));
    }
    @Test public void testAccuracySineDur1Ext() {
        testAccuracy(1, 1,50, 8, (t) -> (float)(Math.sin(t/1f)/2f+0.5f));
    }
    @Test public void testAccuracySineDur() {
        testAccuracy(2, 2,50, 8, (t) -> (float)(Math.sin(t/5f)/2f+0.5f));
    }


    static final LongToFloatFunction stepFunction = (t) -> (Math.sin(t) / 2f + 0.5f) >= 0.5 ? 1f : 0f;

    @Test public void testAccuracySawtoothWave() {
        //this step function when sampled poorly will appear as a triangle sawtooth
        testAccuracy(1, 3, 15, 5, stepFunction);
    }
    @Test public void testAccuracySquareWave() {
        testAccuracy(1, 1, 5, 5, stepFunction);
    }

    static void testAccuracy(int dur, int period, int end, int cap, LongToFloatFunction func) {

        NAR n = NARS.shell();

        n.time.dur(dur);

        Term term = $.p("x");

        //1. populate

        //n.log();

        BaseConcept c = (BaseConcept) n.conceptualize(term);
        @NotNull BeliefTable cb = true ? c.beliefs() : c.goals();
        cb.setCapacity(0, cap);


        //int numTasks = 0;
        System.out.println("points:");
        long time;
        while ((time = n.time()) < end) {
            float f = func.valueOf(time);
            System.out.print(time + "=" + f + "\t");
            n.input($.task(term, BELIEF, f, 0.9f).time(time).setPriThen(0.5f).apply(n));
            n.run(period);
            //numTasks++;
        }
        System.out.println();




        MultiStatistics<Task> m = new MultiStatistics<Task>()
            .classify("input", (t) -> t.isInput())
            .classify("derived", (t) -> t instanceof DerivedTask)
//            .classify("revised", (t) -> t instanceof AnswerTask)
            .value("pri", (t) -> t.pri())
            .value2D("truth", (t) -> new float[] { t.freq(), t.conf() })
            .value("freqErr", (t) -> Math.abs( ((t.freq()-0.5f)*2f) - func.valueOf(t.mid())) )
            .add(c.beliefs());

        System.out.println();
        m.print();
        System.out.println();

        c.beliefs().print();

        //2. validate and calculate error
        CSVOutput csv = new CSVOutput(System.out, "time", "actual", "approx");

        double errSum = 0;
        int start = 0;
        for (long i = start; i < end; i++) {
            float actual = func.valueOf(i);

            Truth actualTruth = n.beliefTruth(term, i);
            float approx, err;
            if (actualTruth!=null) {
                approx = actualTruth.freq();
                err = Math.abs(approx - actual);
            } else {
                approx = Float.NaN;
                err = 1f;
            }

            errSum += err;

            csv.out(i, actual, approx);
            //System.out.println(n2(i) + "\t" + /*n2(err) + "\t" + */ n2(expected) + "\t" + n2(actual));
        }
        double avgErr = errSum / (end-start+1);
        System.out.println();
        System.out.println(n4(avgErr) + " avg freq err per point");
        assertTrue(avgErr < 0.1f);
    }

}
