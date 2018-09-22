package nars.nal.nal3;

import nars.NARS;
import nars.nal.nal8.GoalDecompositionTest;
import nars.test.TestNAR;
import org.junit.jupiter.api.Test;

class DiffGoalTest {



    private final static String X = ("X");
    private final static String Xe = ("(X-->A)");
    private final static String Xi = ("(A-->X)");
    private final static String Y = ("Y");
    private final static String Ye = ("(Y-->A)");
    private final static String Yi = ("(A-->Y)");

    @Test
    void testNoDifference() {
        for (boolean beliefPos : new boolean[]{true, false}) {
            float f = beliefPos ? 1f : 0f;
            for (boolean fwd : new boolean[]{true, false}) {
                testGoalDiff(false, beliefPos, true, fwd, f, 0.81f);
            }
        }
    }
    @Test
    void testDifference() {
        testGoalDiff(true, true, true, true, 0, 0.81f);
        testGoalDiff(true, false, true, false, 1, 0.81f);
        //TODO more cases
    }

    private void testGoalDiff(boolean goalPolarity, boolean beliefPolarity, boolean diffIsGoal, boolean diffIsFwd, float f, float c) {

        testGoalDiff(goalPolarity, beliefPolarity, diffIsGoal, diffIsFwd, true, f, c);

    }

    private void testGoalDiffRaw() {
        new TestNAR(NARS.tmp(3))
                .input("X")
                .input("(X ~ Y)")
                .mustGoal(GoalDecompositionTest.cycles, "Y", 0, 0.81f)
                .run(16);
        new TestNAR(NARS.tmp(3))
                .input("X")
                .input("--(X ~ Y)")
                .mustGoal(GoalDecompositionTest.cycles, "Y", 1, 0.81f)
                .run(16);
        new TestNAR(NARS.tmp(3))
                .input("Y")
                .input("--(X ~ Y)")
                .mustGoal(GoalDecompositionTest.cycles, "X", 1, 0.81f)
                .run(16);
        new TestNAR(NARS.tmp(3))
                .input("--Y")
                .input("(X ~ Y)")
                .mustGoal(GoalDecompositionTest.cycles, "X", 1, 0.81f)
                .run(16);
    }

    private TestNAR testGoalDiff(boolean goalPolarity, boolean beliefPolarity, boolean diffIsGoal, boolean diffIsFwd, boolean diffIsEx, float f, float c) {
        String goalTerm, beliefTerm;
        String first, second;
        if (diffIsFwd) {
            first = X;
            second = Y;
        } else {
            first = Y;
            second = X;
        }
        String diff = (diffIsEx) ?
                "((" + first + "~" + second + ")-->A)" :
                "(A-->(" + first + "-" + second + "))";
        String XX = diffIsEx ? Xe : Xi;
        String YY = diffIsEx ? Ye : Yi;
        if (diffIsGoal) {
            goalTerm = diff;
            beliefTerm = XX;
        } else {
            beliefTerm = diff;
            goalTerm = XX;
        }
        if (!goalPolarity)
            goalTerm = "(--," + goalTerm + ")";
        if (!beliefPolarity)
            beliefTerm = "(--," + beliefTerm + ")";

        String goalTask = goalTerm + "!";
        String beliefTask = beliefTerm + ".";

        String expectedTask = YY + "!";
        if (f < 0.5f) expectedTask = "(--," + expectedTask + ")";

        System.out.println(goalTask + "\t" + beliefTask + "\t=>\t" + expectedTask);


        return new TestNAR(NARS.tmp(3))
                .input(goalTask)
                .input(beliefTask)
                .mustGoal(GoalDecompositionTest.cycles, YY, f, c)
                .run(16);
    }
}