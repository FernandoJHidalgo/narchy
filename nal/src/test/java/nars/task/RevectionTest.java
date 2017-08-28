package nars.task;

import com.google.common.collect.Lists;
import nars.*;
import nars.test.analyze.BeliefAnalysis;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static nars.Op.BELIEF;
import static nars.task.RevisionTest.newNAR;
import static nars.truth.TruthFunctions.c2w;
import static nars.truth.TruthFunctions.w2c;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 5/8/16.
 */
public class RevectionTest {

    final NAR n = NARS.shell();

    @Test
    public void testRevisionEquivalence() throws Narsese.NarseseException {
        TaskBuilder a = t(1f, 0.5f, 0); //c~=0.67
        a.evidence(0);
        TaskBuilder b = t(1f, 0.5f, 0);
        b.evidence(1); //cause different hash

        //assertEquals(a.truth(), TruthPolation.truth(0, a, a)); //same item

        //System.out.println( TruthPolation.truth(0, a, b) );
        assertEquals(Revision.revise(a, b),
                TruthPolation.truth(null, (long) 0, 1, n.dur(), Lists.newArrayList(a.apply(n), b.apply(n))));

    }




    @Test
    public void testRevisionInequivalenceDueToTemporalSeparation() throws Narsese.NarseseException {
        TaskBuilder a = t(1f, 0.5f, -4).evidence(1);
        TaskBuilder b = t(0f, 0.5f, 4).evidence(2);

        int dur = 9;
        Truth pt = TruthPolation.truth(null, (long) 0, 0, dur, Lists.newArrayList(a.apply(n), b.apply(n)));
        @Nullable Truth rt = Revision.revise(a, b);

        assertEquals(pt.freq(), rt.freq(), 0.01f);
        assertTrue(pt.conf() < rt.conf()); //revection result will be less than eternal revision

    }


    @Test
    public void testRevisionEquivalence2Instant() throws Narsese.NarseseException {
        TaskBuilder a = t(1f, 0.5f, 0);
        TaskBuilder b = t(0f, 0.5f, 0);
        assertEquals( Revision.revise(a, b), TruthPolation.truth(null, (long) 0, 0,1, Lists.newArrayList(a.apply(n), b.apply(n))));
    }

    @Test
    public void testPolation1() throws Narsese.NarseseException {

        int dur = 1;

        Task a = t(1f, 0.5f, 3).evidence(1).apply(n);
        Task b = t(0f, 0.5f, 6).evidence(2).apply(n);
        for (int i = 0; i < 10; i++) {
            System.out.println(i + " " + TruthPolation.truth(null, (long) i, i, dur, Lists.newArrayList(a, b)));
        }

        System.out.println();

        Truth ab2 = TruthPolation.truth(null, (long) 3, 3, dur, Lists.newArrayList(a, b));
        assertTrue( ab2.conf() >= 0.5f );

        Truth abneg1 = TruthPolation.truth(null, (long) 3, 3, dur, Lists.newArrayList(a, b));
        assertTrue( abneg1.freq() > 0.6f );
        assertTrue( abneg1.conf() >= 0.5f );

        Truth ab5 = TruthPolation.truth(null, (long) 6, 6, dur, Lists.newArrayList(a, b));
        assertTrue( ab5.freq() < 0.35f );
        assertTrue( ab5.conf() >= 0.5f );
    }

    @Test
    public void testRevisionEquivalence4() throws Narsese.NarseseException {
        Task a = t(0f, 0.1f, 3).evidence(1).apply(n);
        Task b = t(0f, 0.1f, 4).evidence(2).apply(n);
        Task c = t(1f, 0.1f, 5).evidence(3).apply(n);
        Task d = t(0f, 0.1f, 6).evidence(4).apply(n);
        Task e = t(0f, 0.1f, 7).evidence(5).apply(n);

        for (int i = 0; i < 15; i++) {
            System.out.println(i + " " + TruthPolation.truth(null, (long) i, 1, 1, Lists.newArrayList(a, b, c, d, e)));
        }

    }

    public static TaskBuilder t(float freq, float conf, long occ) throws Narsese.NarseseException {
        return new TaskBuilder("a:b", BELIEF, $.t(freq, conf)).time(0, occ);
    }
    public static TaskBuilder t(float freq, float conf, long start, long end) throws Narsese.NarseseException {
        return new TaskBuilder("a:b", BELIEF, $.t(freq, conf)).time(0, start, end);
    }

//    public static void _main(String[] args) {
//        TruthPolation p = new TruthPolation(4,
//                0f);
//        //0.1f);
//
//        List<Task> l = Global.newArrayList();
//
//        //NAR n = new Default();
//        l.add( new TaskBuilder("a:b", BELIEF, new DefaultTruth(0f, 0.5f) ).occurr(0).setCreationTime(0) );
//        l.add( new TaskBuilder("a:b", BELIEF, new DefaultTruth(1f, 0.5f) ).occurr(5).setCreationTime(0) );
//        l.add( new TaskBuilder("a:b", BELIEF, new DefaultTruth(0f, 0.75f) ).occurr(10).setCreationTime(0) );
//        print(p, l, -5, 15);
//
//
//    }

    public static void print(@NotNull List<Task> l, int start, int end) {
        //interpolation (revision) and extrapolation (projection)
        System.out.println("INPUT");
        for (Task t : l) {
            System.out.println(t);
        }

        System.out.println();

        System.out.println("TRUTHPOLATION");
        for (long d = start; d < end; d++) {
            Truth a1 = TruthPolation.truth(null, d, d,1, l);
            System.out.println(d + ": " + a1);
        }
    }


    @Test
    public void testTemporalProjectionInterpolation() throws Narsese.NarseseException {

        Param.DEBUG = true;

        int maxBeliefs = 12;
        NAR n = newNAR(maxBeliefs);


        BeliefAnalysis b = new BeliefAnalysis(n, "<a-->b>");
        b.believe(0.5f, 1.0f, 0.85f, 5);
        b.believe(0.5f, 0.0f, 0.85f, 10);
        b.believe(0.5f, 1.0f, 0.85f, 15);
        b.run(1);

        assertTrue(3 <= b.size(true));

        int period = 1;
        int loops = 20;

        Set<Task> tops = new HashSet();
        for (int i = 0; i < loops; i++) {


            b.run(period);

            long now = b.nar.time();

            Task tt = n.belief(b.concept().term(), now);
            tops.add(tt);

            System.out.println(now + " " +  tt);

        }

        assertTrue("all beliefs covered", 3 <= tops.size());

        b.print();

    }

    @Test
    public void testTemporalProjectionConfidenceAccumulation2_1() {
        testConfidenceAccumulation(2, 1f, 0.1f);
    }

    @Test
    public void testTemporalProjectionConfidenceAccumulation2_5() {
        testConfidenceAccumulation(2, 1f, 0.5f);
    }
    @Test
    public void testTemporalProjectionConfidenceAccumulation2_9() {

        testConfidenceAccumulation(2, 1f, 0.9f);
        testConfidenceAccumulation(2, 0.5f, 0.9f);
        testConfidenceAccumulation(2, 0f, 0.9f);
    }

    @Test
    public void testTemporalProjectionConfidenceAccumulation3_1_pos() {
        testConfidenceAccumulation(3, 1f, 0.1f);
    }
    @Test
    public void testTemporalProjectionConfidenceAccumulation3_1_neg() {
        testConfidenceAccumulation(3, 0f, 0.1f);
    }
    @Test
    public void testTemporalProjectionConfidenceAccumulation3_1_mid() {
        testConfidenceAccumulation(3, 0.5f, 0.1f);
    }

    @Test
    public void testTemporalProjectionConfidenceAccumulation3_5() {
        testConfidenceAccumulation(3, 1f, 0.5f);
    }

    @Test
    public void testTemporalProjectionConfidenceAccumulation3_9() {
        testConfidenceAccumulation(3, 1f, 0.9f);
    }


    public void testConfidenceAccumulation(int repeats, float freq, float inConf) {
        int maxBeliefs = repeats*4;

        NAR n = newNAR(maxBeliefs);

        n.log();

        long at = 5;

        float outConf = w2c( c2w(inConf)*repeats);

        BeliefAnalysis b = null;
        try {
            b = new BeliefAnalysis(n, "<a-->b>");
        } catch (Narsese.NarseseException e) {
            assertTrue(false);
        }
        for (int i = 0; i < repeats; i++) {
            b.believe(0.5f, freq, inConf, at);
        }

        b.run(1);

        b.print();
        assertTrue( repeats <= b.size(true));

        @Nullable Truth result = n.beliefTruth(b, at);
        assertEquals(freq, result.freq(), 0.25f);
        assertEquals(outConf, result.conf(), 0.25f);
    }


    @Test
    public void testTemporalRevection() throws Narsese.NarseseException {

        Param.DEBUG = true;

        int maxBeliefs = 4; //includes 3 eternal beliefs we arent using:
        NAR n = newNAR(maxBeliefs);


        BeliefAnalysis b = new BeliefAnalysis(n, "<a-->b>");

        //assertEquals(0.0, (Double) b.energy().get(MemoryBudget.Budgeted.ActiveConceptPrioritySum), 0.001);


        b.believe(0.5f, 0.0f, 0.85f, 5);
        n.cycle();
        b.believe(0.5f, 0.95f, 0.85f, 10);
        n.cycle();
        b.believe(0.5f, 1.0f, 0.85f, 11); //this and the previous one should get combined when inserting the 4th
        n.cycle();

        b.print();
        assertEquals(3, b.size(true));
        assertEquals(5, b.wave().start());
        assertEquals(11, b.wave().end());

        b.believe(0.5f, 1.0f, 0.99f, 12); //this should cause the cycle=10 and cycle=11 beliefs to get revected into one and allow this belief to be inserted
        //the cycle=5 belief should remain since it is more unique

        n.run(3);
        b.print();
        //assertEquals(4, b.capacity(true));
        assertEquals(5, b.size(true));

        assertEquals(5, b.wave().start());
        assertEquals(12, b.wave().end());

    }

}