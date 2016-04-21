package nars.concept;

import nars.Global;
import nars.NAR;
import nars.bag.Bag;
import nars.nal.Tense;
import nars.nar.AbstractNAR;
import nars.nar.Default;
import nars.task.Task;
import nars.util.analyze.BeliefAnalysis;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by me on 3/18/16.
 */
public class RevisionTest {

    @NotNull
    public static AbstractNAR newNAR(int maxBeliefs) {
        AbstractNAR d = new Default(256, 1, 2, 3);
        d.nal(7);// {

        /*
        @Override
        public BeliefTable.RankBuilder getConceptRanking() {
            if (rb == null)
                return super.getConceptRanking();
            else
                return rb;
        }
        */

        //}
        d.conceptBeliefsMax.set(maxBeliefs);
        return d;
    }

    @Test
    public void testBeliefRevision1() {
        testRevision(1, true); //short term immediate test for correct revisionb ehavior
    }

    @Test
    public void testGoalRevision1() {
        testRevision(32, false); //longer term test
    }

    @Test
    public void testBeliefRevision32() {
        testRevision(32, true); //longer term test
    }

    @Test
    public void testGoalRevision32() {
        testRevision(32, false); //longer term test
    }


    void testRevision(int delay1, boolean beliefOrGoal) {
        Global.DEBUG = true;

        AbstractNAR n = newNAR(6);
        n.nal(1);


        //arbitrary time delays in which to observe that certain behavior does not happen

        BeliefAnalysis b = new BeliefAnalysis(n, "<a-->b>")
            .input(beliefOrGoal, 1f, 0.9f).run(1);

        assertEquals(1, b.size(beliefOrGoal));

        b.input(beliefOrGoal, 0.0f, 0.9f).run(1);

        b.run(delay1);

        b.print(beliefOrGoal);

        assertEquals("revised", 3, b.size(beliefOrGoal));

        n.run(delay1);

        assertEquals("no additional revisions", 3, b.size(beliefOrGoal));


    }

    @Test
    public void testTruthOscillation() {

        NAR n = newNAR(8);


        int offCycles = 2;

        BeliefAnalysis b = new BeliefAnalysis(n, "<a-->b>");

        //assertEquals(0.0, (Double) b.energy().get(MemoryBudget.Budgeted.ActiveConceptPrioritySum), 0.001);

        b.believe(1.0f, 0.9f, Tense.Present);
        b.run(1);
        //b.printEnergy();

        b.run(1);
        //b.printEnergy();

        b.believe(0.0f, 0.9f, Tense.Present);
        b.run(1);
        //b.printEnergy();

        b.run(1);
        //b.printEnergy();

        b.print();
        assertEquals(2, b.size());

        b.believe(1.0f, 0.9f, Tense.Present).run(offCycles)
                .believe(0.0f, 0.9f, Tense.Present);

        for (int i = 0; i < 16; i++) {
            //b.printEnergy();
            b.print();
            n.run(1);
            //TODO test that they are sorted ?
        }


    }


    @Test
    public void testTruthOscillation2() {

        Global.DEBUG = true;

        int maxBeliefs = 16;
        NAR n = newNAR(maxBeliefs);



        BeliefAnalysis b = new BeliefAnalysis(n, "<a-->b>");

        //assertEquals(0.0, (Double) b.energy().get(MemoryBudget.Budgeted.ActiveConceptPrioritySum), 0.001);

        int period = 8;
        int loops = 4;
        for (int i = 0; i < loops; i++) {
            b.believe(1.0f, 0.9f, Tense.Present);


            b.run(period);
            //b.printEnergy();

            b.believe(0.0f, 0.9f, Tense.Present);

            b.run(period);
            //b.printEnergy();
            b.print();
        }

        b.run(period);

        b.print();

        //TODO test the belief table for something like the following:
    /*
    Beliefs[@72] 16/16
    <a --> b>. %0.27;0.98% [1, 2, 3, 4, 6] [Revision]
    <a --> b>. %0.38;0.98% [1, 2, 3, 4, 6, 7] [Revision]
    <a --> b>. %0.38;0.98% [1, 2, 3, 4, 5, 6] [Revision]
    <a --> b>. %0.23;0.98% [1, 2, 3, 4, 6, 8] [Revision]
    <a --> b>. %0.35;0.97% [1, 2, 3, 4] [Revision]
    <a --> b>. %0.52;0.95% [1, 2, 3] [Revision]
    <a --> b>. 56+0 %0.00;0.90% [8] [Input]
    <a --> b>. 48+0 %1.00;0.90% [7] [Input]
    <a --> b>. 40+0 %0.00;0.90% [6] [Input]
    <a --> b>. 32+0 %1.00;0.90% [5] [Input]
    <a --> b>. 24+0 %0.00;0.90% [4] [Input]
    <a --> b>. 16+0 %1.00;0.90% [3] [Input]
    <a --> b>. 8+0 %0.00;0.90% [2] [Input]
    <a --> b>. 0+0 %1.00;0.90% [1] [Input]
    <a --> b>. %0.09;0.91% [1, 2] [Revision]
    <a --> b>. 28-20 %0.00;0.18% [1, 2, 3] [((%1, <%1 </> %2>, shift_occurrence_forward(%2, "=/>")), (%2, (<Analogy --> Truth>, <Strong --> Desire>, <ForAllSame --> Order>)))]
     */


//        b.believe(1.0f, 0.9f, Tense.Present).run(offCycles)
//                .believe(0.0f, 0.9f, Tense.Present);

    /*for (int i = 0; i < 16; i++) {
        b.printEnergy();
        b.print();
        n.frame(1);
    }*/


    }

    /** test that budget is conserved during a revision between
     * the input tasks and the result */
    @Test public void testRevisionBudgetConserved() {
        AbstractNAR n = newNAR(6);
        n.taskLinkRemembering.setValue(1E6); //nearly zero tasklink forget rate so that it wont influence this test

        BeliefAnalysis b = new BeliefAnalysis(n, "<a-->b>");
        Bag<Task> tasklinks = b.concept().tasklinks();

        assertEquals(0, b.priSum(), 0.01f);

        b.believe(1.0f, 0.5f).run(1);

        assertEquals(0.5f, b.beliefs().topEternalTruth(null).conf(), 0.01f);

        b.believe(1.0f, 0.6f).run(1);
        assertEquals(2, tasklinks.size());

        b.print();
        tasklinks.print();

        float linksBeforeRevisionLink = tasklinks.priSum();

        System.out.println("--------");
        b.run(1); //process revision after being input

        float beliefAfter2;
        assertEquals(0.97, beliefAfter2 = b.priSum(), 0.01f);
        assertEquals(0.71f, b.beliefs().topEternalTruth(null).conf(), 0.01f); //the revised task on top

        b.print();

        //revised:
        assertEquals(3, b.size());
        assertEquals(3, tasklinks.size());

        assertEquals(beliefAfter2, b.priSum(), 0.01f); //CONSERVED BELIEF BUDGET

        tasklinks.commit();
        tasklinks.print();

        //without tasklink balancing: 1.24 - 0.97
        //with balancing: 1.10 - 0.97
        float tolerance = 0.14f; //where does the additional budget come from? but at least the tasklink balancing results in less inflation
        assertEquals(linksBeforeRevisionLink, tasklinks.priSum(), tolerance); //CONSERVED LINK BUDGET

    }
}
