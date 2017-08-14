package nars.task;

import nars.*;
import nars.concept.Concept;
import nars.time.Tense;
import nars.truth.DiscreteTruth;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.Arrays;

import static nars.Op.BELIEF;
import static org.junit.Assert.*;

/**
 * Created by me on 11/3/15.
 */
public class TaskTest {

    @Test public void testTenseEternality() throws Narsese.NarseseException {
        NAR n = new NARS().get();

        String s = "<a --> b>.";

        assertTrue(Narsese.parse().task(s, n).start() == Tense.ETERNAL);

        assertTrue("default is eternal", Narsese.parse().task(s, n).isEternal());

        assertTrue("tense=eternal is eternal", Narsese.parse().task(s, n).start() == Tense.ETERNAL);

        //assertTrue("present is non-eternal", !Tense.isEternal(((Task)n.task(s)).present(n).start()));

    }

//    @Test public void testTenseOccurrenceOverrides() throws Narsese.NarseseException {
//
//        NAR n = new Default();
//
//        String s = "<a --> b>.";
//
//        //the final occurr() or tense() is the value applied
//        assertTrue(!Tense.isEternal(((Task)n.task(s)).eternal().occurr(100).start()));
//        assertTrue(!Tense.isEternal(((TaskBuilder)n.task(s)).eternal().present(n).start()));
//        assertTrue(Tense.isEternal(((TaskBuilder)n.task(s)).occurr(100).eternal().start()));
//    }


//    @Test public void testStampTenseOccurenceOverrides() throws Narsese.NarseseException {
//
//        NAR n = new NAR(new Default());
//
//        Task parent = n.task("<x --> y>.");
//
//
//        String t = "<a --> b>.";
//
//
//        Stamper st = new Stamper(parent, 10);
//
//        //the final occurr() or tense() is the value applied
//        assertTrue(!n.memory.task(n.term(t)).eternal().stamp(st).isEternal());
//        assertTrue(n.memory.task(n.term(t)).stamp(st).eternal().isEternal());
//        assertEquals(20, n.memory.task(n.term(t)).judgment().parent(parent).stamp(st).occurr(20).get().getOccurrenceTime());
//    }


    @Test public void testTruthHash16Plus16Bit() {
        //for TRUTH EPSILON 0.01:

        assertEquals(3276, new DiscreteTruth(0, 0.1f).hashCode());

        assertEquals(2147385014, new DiscreteTruth(1, 1.0f).hashCode());
    }

//    /** tests the ordering of tasks that differ by truth values,
//     * which is determined by directly comparing their int hashcode
//     * representation (which is perfect and lossless hash if truth epsilon
//     * is sufficiently large) */
//    @Test public void testTaskOrderByTruthViaHash() throws Narsese.NarseseException {
//        Terminal n = NARS.shell();
//        TreeSet<Task> t = new TreeSet<>();
//        int count = 0;
//        for (float f = 0; f < 1.0f; f += 0.3f)
//            for (float c = 0.01f; c < 1.0f; c += 0.3f) {
//                t.add(
//                    n.inputAndGet($.task("a:b", BELIEF,f, c).apply(n))
//                );
//                count++;
//            }
//        assertEquals(count, t.size());
//
//        List<Task> l = Lists.newArrayList(t);
//        //l.forEach(System.out::println);
//        int last = l.size() - 1;
//
//        assertTrue(l.get(0).toString(), l.get(0).toString().contains("(b-->a). %0.0;.01%"));
//        assertTrue(l.get(last).toString(), l.get(last).toString().contains("(b-->a). %.90;.91%"));
//
//        //test monotonically increasing
//        Task y = null;
//        for (int i = l.size()-1; i >=0; i--) {
//            Task x = l.get(i);
//            if (y != null) {
//                assertTrue(x.freq() <= y.freq());
//                float c = y.conf();
//                if (x.conf() < 0.90f) //wrap around only time when it will decrease
//                    assertTrue(x.conf() <= c);
//            }
//            y = x;
//        }
//    }


    @Test
    public void inputTwoUniqueTasksDef() throws Narsese.NarseseException {
        inputTwoUniqueTasks(new NARS().get());
    }
    /*@Test public void inputTwoUniqueTasksSolid() {
        inputTwoUniqueTasks(new Solid(4, 1, 1, 1, 1, 1));
    }*/
    /*@Test public void inputTwoUniqueTasksEq() {
        inputTwoUniqueTasks(new Equalized(4, 1, 1));
    }
    @Test public void inputTwoUniqueTasksNewDef() {
        inputTwoUniqueTasks(new Default());
    }*/

    public void inputTwoUniqueTasks(@NotNull NAR n) throws Narsese.NarseseException {

        //Param.DEBUG = true;

        Task x = n.inputAndGet("<a --> b>.");
        assertArrayEquals(new long[]{1}, x.stamp());
        n.cycle();

        Task y = n.inputAndGet("<b --> c>.");
        assertArrayEquals(new long[]{2}, y.stamp());
        n.cycle();

        n.reset();

        n.input("<e --> f>.  <g --> h>. "); //test when they are input on the same parse

        n.run(10);

        Task q = n.inputAndGet("<c --> d>.");
        assertArrayEquals(new long[]{5}, q.stamp());

    }


    @Test
    public void testDoublePremiseMultiEvidence() throws Narsese.NarseseException {

        //Param.DEBUG = true;
        //this.activeTasks = activeTasks;
        NAR d = new NARS().get();
        //d.log();
        d.nal(2);
        d.input("<a --> b>.", "<b --> c>.");

        long[] ev = {1, 2};
        d.eventTask.on(t -> {

            if (t instanceof DerivedTask && ((DerivedTask)t).getParentBelief()!=null && !t.cyclic())
                assertArrayEquals("all double-premise derived terms have this evidence: "
                        + t + ": " + Arrays.toString(ev) + "!=" + Arrays.toString(t.stamp()), ev, t.stamp());

            System.out.println(t.proof());

        });

        d.run(256);


    }

    @Test public void testValid() throws Narsese.NarseseException {
        NAR tt = NARS.shell();
        Task t = $.task("((&&,#1,(#1 &&+0 #3),(#2 &&+0 #3),(#2 &&+0 (toothbrush-->here))) ==>+0 lighter(I,toothbrush))", BELIEF, 1f, 0.9f).apply(tt);
        assertNotNull(t);
        Concept c = t.concept(tt,true);
        assertNotNull(c);
    }

    @Test public void testTaskNearestTimePoint() throws Narsese.NarseseException {
        NAR tt = NARS.shell();
        Task t = $.task("x", BELIEF, 1f, 0.9f).time(0, 10, 20).apply(tt);

        assertEquals(10, t.start());
        assertEquals(20, t.end());

        assertEquals(20, t.nearestTimeTo(30));
        assertEquals(18, t.nearestTimeTo(18)); //mid
        assertEquals(15, t.nearestTimeTo(15)); //mid
        assertEquals(12, t.nearestTimeTo(12)); //mid
        assertEquals(10, t.nearestTimeTo(0));

        assertEquals(15, t.nearestTimeBetween(9, 21)); //midpoint
        assertEquals(10, t.nearestTimeBetween(0, 1));
        assertEquals(10, t.nearestTimeBetween(0, 15));
        assertEquals(15, t.nearestTimeBetween(15, 15));
        assertEquals(20, t.nearestTimeBetween(30, 40));
    }
}
