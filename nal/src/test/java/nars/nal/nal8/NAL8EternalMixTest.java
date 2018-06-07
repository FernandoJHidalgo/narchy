package nars.nal.nal8;

import nars.$;
import nars.Narsese;
import nars.nal.nal7.NAL7Test;
import nars.test.TestNAR;
import nars.time.Tense;
import nars.util.NALTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;
import static nars.time.Tense.ETERNAL;

/**
 * NAL8 tests specifically involving one or more eternal input tasks
 */
public class NAL8EternalMixTest extends NALTest {

    final int cycles = 450;

    @BeforeEach
    public void setTolerance() {
        test.confTolerance(NAL7Test.CONF_TOLERANCE_FOR_PROJECTIONS);
        test.nar.time.dur(1);
        test.nar.termVolumeMax.set(28);
    }

    @Test
    public void subsent_1_even_simpler_simplerGoalEternal() {

        test

                .input("(open(t1) && [opened]:t1)!")
                .mustGoal(cycles, "open(t1)", 1.0f, 0.81f, ETERNAL)
                .mustNotOutput(cycles, "open(t1)", GOAL, 0)
        ;

    }

    @Test
    public void subsent_1() {

        TestNAR tester = test;


        

        tester.input("[opened]:{t001}. :|:");
        tester.input("(((hold({t002}) &&+5 at({t001})) &&+5 open({t001})) &&+5 [opened]:{t001}).");

        
        tester.mustBelieve(cycles, "((hold({t002}) &&+5 at({t001})) &&+5 open({t001}))",
                1.0f, 0.81f,
                -15);









        








    }

    @Test
    public void conditional_abduction_temporal_vs_eternal() { 

        TestNAR tester = test;

        tester.input("at(SELF,{t003}). :|:");
        tester.inputAt(10, "(goto($1) ==>+5 at(SELF,$1)).");

        tester.mustBelieve(cycles, "goto({t003})", 1.0f, 0.45f, -5);

    }

    @Test
    public void ded_with_indep_var_temporal() {

        TestNAR tester = test;

        tester.input("goto({t003}). :|:");
        tester.inputAt(10, "(goto($1) ==>+5 at(SELF,$1)).");

        tester.mustBelieve(cycles, "at(SELF,{t003})", 1.0f, 0.81f, 5);

    }

    @Test
    public void ded_with_var_temporal() {

        test

                .input("goto({t003}). :|:")
                .inputAt(10, "(goto(#1) ==>+5 at(SELF,#1)).")
                .mustBelieve(cycles, "at(SELF,{t003})", 1.0f, 0.81f, 5);
    }

    @Test
    public void ded_with_var_temporal2() {

        TestNAR tester = test;

        tester.input("goto({t003}). :|: ");
        tester.inputAt(10, "(goto($1) ==>+5 at(SELF,$1)). ");

        tester.mustBelieve(cycles, "at(SELF,{t003})", 1.0f, 0.81f, 5);

    }


    @Test
    public void goal_deduction_tensed_conseq() {

        TestNAR tester = test;

        tester.input("goto(x). :|:");
        tester.inputAt(10, "(goto($1) ==>+5 at(SELF,$1)).");

        tester.mustBelieve(cycles, "at(SELF,x)", 1.0f, 0.81f, 5);
    }


    @Test
    public void condition_goal_deductionWithVariableEliminationOpposite() {

        test

                .input("goto({t003}). :|:")
                .input("(goto(#1) &&+5 at(SELF,#1))!")
                .mustGoal(2 * cycles, "at(SELF,{t003})", 1.0f, 0.81f, (t)->t >= 5)
        ;
    }

    @Test
    public void goal_deduction_impl() {

        TestNAR tester = test;
        tester.input("x:y! :|:");
        tester.input("(goto(z) ==>+5 x:y).");
        tester.mustGoal(cycles, "goto(z)", 1.0f, 0.45f, (t) -> t >= 0);
    }

    @Test
    public void goal_deduction_impl_after() {

        TestNAR tester = test;
        tester.input("x:y! :|:");
        tester.input("(goto(z) ==>-5 x:y).");
        tester.mustGoal(cycles, "goto(z)", 1.0f, 0.45f, 5);
    }

    @Test
    public void goal_deduction_delayed_impl() {

        TestNAR tester = test;
        tester.input("x:y!");
        tester.inputAt(3, "(goto(z) ==>+5 x:y). :|:");
        tester.mustGoal(cycles, "goto(z)", 1.0f, 0.45f, (t) -> t >= 3);
    }


    @Test
    public void goal_deduction_tensed_conseq_noVar() {

        TestNAR tester = test;

        tester.inputAt(1, "goto(x). :|:");
        tester.inputAt(10, "(goto(x) ==>+5 at(SELF,x)).");

        tester.mustBelieve(cycles, "at(SELF,x)", 1.0f, 0.81f, 6);
    }

    @Test
    public void belief_deduction_by_condition() {

        TestNAR tester = test;

        tester.input("(open({t001}) ==>+5 [opened]:{t001}).");
        tester.inputAt(10, "open({t001}). :|:");

        tester.mustBelieve(cycles, "[opened]:{t001}", 1.0f, 0.81f, 15);

    }

    @Test
    public void condition_goal_deduction2() {

        test

                .input("a:b! :|:")
                .inputAt(10, "(( c:d &&+5 e:f ) ==>+0 a:b).")
                .mustGoal(cycles, "( c:d &&+5 e:f)", 1.0f, 0.45f, -5)
                .mustNotOutput(cycles, "( c:d &&+5 e:f)", GOAL, ETERNAL)
        ;
    }

    @Test
    public void condition_goal_deduction_interval() {

        test

                .input("a:b! :|:")
                .input("(( c:d &&+5 e:f ) ==>+5 a:b).")
                .mustGoal(cycles, "( c:d &&+5 e:f)", 1.0f, 0.45f, 0);
    }

    @Test
    public void condition_goal_deductionEternal() {

        test

                .input("a:b!")
                .inputAt(10, "(( c:d &&+5 e:f ) ==> a:b).")
                .mustGoal(cycles, "( c:d &&+5 e:f)", 1.0f, 0.45f, ETERNAL)
                .mustNotOutput(cycles, "( c:d &&+5 e:f)", GOAL, (t) -> t > 0)
        ;
    }

    @Test
    public void further_detachment() {

        test
                .input("reachable(SELF,{t002}). :|:")
                .inputAt(10, "(reachable(SELF,{t002}) &&+5 pick({t002}))!")
                .mustGoal(cycles, "pick({t002})", 1.0f, 0.81f, 5);

    }

    @Test
    public void condition_goal_deduction_eternal_belief() {

        test
                .input("reachable(SELF,{t002})! :|:")
                .inputAt(5, "((on($1,#2) &&+0 at(SELF,#2)) ==>+0 reachable(SELF,$1)).")
                .mustGoal(cycles, "(on({t002},#1) &&+0 at(SELF,#1))", 1.0f, 0.45f, 0)
                .mustNotOutput(cycles, "(at(SELF,#1) &&+0 on({t002},#1))", GOAL, t -> t == ETERNAL || t == 5);

    }

    @Test
    public void goal_ded_2() {

        TestNAR tester = test;

        tester.inputAt(0, "at(SELF,{t001}). :|:");
        tester.inputAt(0, "(at(SELF,{t001}) &&+5 open({t001}))!");

        tester.mustGoal(cycles, "open({t001})", 1.0f, 0.43f, 5);

    }

    @Test
    public void condition_goal_deduction_3simplerReverse() {

        test

                .inputAt(1, "at:t003! :|:")
                .input("(at:$1 ==>+5 goto:$1).")

                .mustGoal(cycles, "goto:t003", 1.0f, 0.45f, 6)
                .mustNotOutput(cycles, "goto:t003", GOAL, 0f, 1f, 0.1f, 1f, 1L);

    }


    @Test
    public void further_detachment_2() {

        test
                .input("reachable(SELF,{t002}). :|:")
                .inputAt(3, "((reachable(SELF,{t002}) &&+5 pick({t002})) ==>+7 hold(SELF,{t002})).")
                .mustBelieve(cycles, "(pick({t002}) ==>+7 hold(SELF, {t002}))", 1.0f, 0.81f, 5)
        
        ;

    }

    @Test
    public void goal_deduction_2() {

        TestNAR tester = test;

        tester.input("goto({t001}). :|: ");
        tester.inputAt(7, "(goto($1) ==>+2 at(SELF,$1)). ");

        
        tester
                .mustBelieve(cycles, "at(SELF,{t001})", 1.0f, 0.81f, 2)
                .mustNotOutput(cycles, "at(SELF,{t001})", BELIEF, 1f, 1.0f, 0.81f, 0.81f,
                        t -> t == 0);
    }

    @Test
    public void condition_goal_deduction_2() {

        test
                .input("on({t002},{t003}). :|:")
                .input("(on({t002},#1) &&+0 at(SELF,#1))!")
                .mustGoal(cycles, "at(SELF,{t003})", 1.0f, 0.81f, 0);

        
    }

    @Test
    public void condition_belief_deduction_2() {

        test
                .input("on({t002},{t003}). :|:")
                .inputAt(2, "(on({t002},#1) &&+0 at(SELF,#1)).")
                .mustBelieve(cycles, "at(SELF,{t003})", 1.0f, 0.23f, 0)
                .mustNotOutput(cycles, "at(SELF,{t003})", BELIEF, 0, 1f, 0, 1f, ETERNAL);
    }


    @Test
    public void condition_belief_deduction_2_neg() {

        test
                .input("(--,on({t002},{t003})). :|:")
                .inputAt(2, "((--,on({t002},#1)) &&+0 at(SELF,#1)).")
                .mustBelieve(cycles, "at(SELF,{t003})", 1.0f, 0.73f, 0)
                .mustNotOutput(cycles, "at(SELF,{t003})", BELIEF, 0, 1f, 0, 1f, ETERNAL);
    }

    @Test
    public void condition_belief_deduction_2_easier() {

        test
                .input("on(t002,t003). :|:")
                .inputAt(2, "(on(t002,#1) &&+0 at(SELF,#1)).")
                .mustBelieve(cycles, "at(SELF,t003)", 1.0f, 0.43f, 0)
                .mustNotOutput(cycles, "at(SELF,t003)", BELIEF, 0, 1f, 0, 1f, ETERNAL);
    }

    @Disabled
    @Test
    public void condition_belief_deduction_2_dternal() {


        test

                .input("on:(t002,t003). :|:")
                .inputAt(10, "(on:(t002,#1) && at(SELF,#1)).") 
                .mustBelieve(cycles * 4, "at(SELF,t003)", 1.0f, 0.43f, 0)
        
        ;
    }

    @Test
    public void temporal_goal_detachment_1() {

        test

                .input("hold. :|:")
                .input("( hold &&+5 (at &&+5 open) )!")
                .mustGoal(cycles, "(at &&+5 open)", 1.0f, 0.5f, 5, 10)
        
        ;
    }

    @Test
    public void temporal_goal_detachment_2() {
        

        test
                .input("hold! :|:")
                .inputAt(2, "( hold &&+5 (eat) ).") 
                .mustGoal(cycles, "(eat)", 1f, 0.81f, 5)
        ;
    }

    @Test
    public void temporal_goal_detachment_3_valid() {

        test
                .input("(use)! :|:")
                .inputAt(2, "( hold &&+5 (use) ).") 
                .mustGoal(cycles, "hold", 1f, 0.81f, 0)
                .mustNotOutput(cycles, "(use)", GOAL, ETERNAL) 
                .mustNotOutput(cycles, "hold", GOAL, ETERNAL)
        ;
    }

    @Test
    public void temporal_goal_detachment_3_valid_negate() {

        test
                .input("--(use)! :|:")
                .inputAt(1, "( hold &&+5 --(use) ).")
                .mustGoal(cycles, "hold", 1f, 0.81f, 0)
                .mustNotOutput(cycles, "(use)", GOAL, ETERNAL) 
        ;
    }

    @Test
    public void detaching_condition0() {

        TestNAR tester = test;

        int when = 4;
        tester.input("( ( hold:t2 &&+1 (att1 &&+1 open:t1)) ==>+1 opened:t1).");
        tester.inputAt(when, "hold:t2. :|:");

        String result = "((att1 &&+1 open:t1) ==>+1 opened:t1)";
        tester.mustBelieve(cycles, result, 1.0f, 0.81f, when + 1);

    }

    @Test
    public void detaching_condition() {

        test
                .input("( ( hold(SELF,{t002}) &&+5 (at(SELF,{t001}) &&+5 open({t001}))) ==>+5 [opened]:{t001}).")
                .inputAt(10, "hold(SELF,{t002}). :|:")
                .mustBelieve(cycles, "((at(SELF,{t001}) &&+5 open({t001})) ==>+5 [opened]:{t001})", 1.0f, 0.81f, 15);

    }

    @Test
    public void subgoal_1_abd() {

        TestNAR tester = test;

        tester.input("opened:{t001}. :|:");
        tester.input("((hold(SELF,{t002}) &&+5 ( at(SELF,{t001}) &&+5 open({t001}))) ==>+5 opened:{t001}).");

        tester.mustBelieve(cycles, "( hold(SELF,{t002}) &&+5 ( at(SELF,{t001}) &&+5 open({t001})))",
                1.0f, 0.45f,
                -15);

    }

    @Test
    public void temporal_deduction_2() {

        TestNAR tester = test;

        tester.input("((hold(SELF,{t002}) &&+5 (at(SELF,{t001}) &&+5 open({t001}))) ==>+5 [opened]:{t001}).");
        tester.inputAt(2, "hold(SELF,{t002}). :|: ");

        tester.mustBelieve(cycles, "((at(SELF,{t001}) &&+5 open({t001})) ==>+5 [opened]:{t001})", 1.0f, 0.81f, 2 + 5);

    }


    @Test
    public void implSubstitutionViaSimilarity() {

        test
                .input("(a:b<->c:d).") 
                .input("(c:d ==>+1 e:f). :|:") 
                .mustBelieve(cycles, "(a:b ==>+1 e:f)", 1.0f, 0.81f, 0)
                .mustNotOutput(cycles, "(a:b ==>+1 e:f)", BELIEF, ETERNAL);
    }

    @Test
    public void implSubstitutionViaSimilarityReverse() {

        test

                .input("(a:b<->c:d).") 
                .input("(e:f ==>+1 c:d). :|:") 
                .mustBelieve(cycles, "(e:f ==>+1 a:b)", 1.0f, 0.4f, 0)
                .mustNotOutput(cycles, "(e:f ==>+1 a:b)", BELIEF, ETERNAL);
    }


    @Test
    public void testDeiredConjDelayed() {

        test
                .believe("(x)", Tense.Present, 1f, 0.9f)
                .goal("((x) &&+3 (y))")
                .mustGoal(cycles, "(y)", 1f, 0.81f, (t) -> t >= 3)
                .mustNotOutput(cycles, "(y)", GOAL, ETERNAL);
    }

    @Test
    public void testDesiredConjDelayedNeg() {

        test
                .believe("(x)", Tense.Present, 0f, 0.9f)
                .goal("(--(x) &&+3 (y))")
                .mustGoal(cycles, "(y)", 1f, 0.81f, (x)->x>=3)
                .mustNotOutput(cycles, "(y)", GOAL, ETERNAL);
    }

    @Test
    public void testBelievedImplOfDesireDelayed() {

        test
                .goal("(x)", Tense.Present, 1f, 0.9f)
                .believe("((x)==>+3(y))")
                .mustGoal(cycles, "(y)", 1f, 0.81f, 3)
        
        ;
    }

    @Test
    public void testGoalConjunctionDecomposeSuffix() {

        test
                .goal("((x) &&+3 (y))", Tense.Eternal, 1f, 0.9f)
                .inputAt(4, "(x). :|:")
                .mustGoal(cycles, "(y)", 1f, 0.81f, (4 + 3))
                .mustNotOutput(cycles, "(y)", GOAL, 3)
        
        ;
    }

    @Test
    public void testNegatedImplicationS() {

        test
                .goal("(R)")
                .input("((--,a:b) ==>+0 (R)). :|:")
                .mustGoal(cycles, "a:b", 0.0f, 0.43f, 0);
    }

    @Test
    public void testNegatedImplicationP() {

        test

                .input("(R)! :|:")
                .input("((S) ==>+0 --(R)).") 
                .mustGoal(cycles, "(S)", 0.0f, 0.81f, 0);
    }

    @Test
    public void testNegatedImplicationTerm2() {

        test
                .input("(R)! :|:")
                .input("((--,a:b) ==>+0 (R)).")
                .mustGoal(cycles, "a:b", 0.0f, 0.81f, 0);

    }

    @Test
    public void testNegatedImplicationTerm3() {

        test
                .input("(R). :|:")
                .input("((--,a:b) &&+0 (R))!")
                .mustGoal(cycles, "a:b", 0.0f, 0.81f, 0);
    }


    @Disabled
    @Test
    public void disjunctionBackwardsQuestionTemporal() {

        test
                .inputAt(0, "(||, (x), (y))?")
                .believe("(x)", Tense.Present, 1f, 0.9f)
                .mustBelieve(cycles, "(&&, (--,(x)), (--,(y)))", 0f, 0.81f, 0);
    }

    @Test
    public void testGoalImplComponentTemporal() {

        test
                .input("(happy)! :|:")
                .input("((--,(in)) ==>+1 ((happy) &&-1 (--,(out)))).")
                .mustGoal(cycles, "(in)", 0f, 0.42f, 0);
    }

    @Test
    public void testGoalImplComponentWithVar() {

        test.nar.runAt(cycles * 4, () -> {
            try {
                test.nar.concept($.$("c($1)")).print();
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            }
        });

        test
                .input("((a($x) &&+4 b($x)) ==>-3 c($x)).")
                .inputAt(0, "c(x)! :|:")
                
                .mustGoal(cycles * 5, "b(x)", 1f, 0.73f,
                        (t) -> t >= 3 /* early since c(x) is alrady active when this gets derived */);
    }

    @Test
    public void testPredictiveImplicationTemporalEternal() {

        test
                .inputAt(0, "((out) ==>-3 (happy)).")
                .inputAt(13, "(happy)! :|:")
                .mustGoal(cycles, "(out)", 1f, 0.81f, 16)
                .mustNotOutput(cycles, "(out)", GOAL, 3);
    }

    @Test
    public void testPredictiveImplicationEternalTemporal() {

        test
                .inputAt(0, "((out) ==>-3 (happy)). :|:")
                .inputAt(13, "(happy)!")
                .mustGoal(cycles, "(out)", 1f, 0.81f, 3)
                .mustNotOutput(cycles, "(out)", GOAL, 13);
    }


    @Test
    public void deriveNegInhGoalTemporal() {

        test
                .input("b:a! :|:") 
                .input("c:b.")
                .input("--y:x!  :|:") 
                .input("z:y.")
                .mustGoal(cycles * 2, "c:a", 1f, 0.81f, 0)
                .mustGoal(cycles * 2, "z:x", 0f, 0.81f, 0);
    }

    @Test
    public void testStrongUnificationDeductionPN() {
        
        test
                .input("((--,Y) ==>+1 (X)).")
                .input("(--,Y). :|:")
                .mustBelieve(cycles, "(X)", 1f, 0.81f, 1)
                .mustNotOutput(cycles, "(X)", BELIEF, ETERNAL)
        ;
    }

    @Test
    public void testStrongUnificationAbductionPN() {
        
        test
                .input("((--,X) ==>+1 (Y)).")
                .input("(Y). :|:")
                .mustBelieve(cycles, "X", 0f, 0.45f, -1);
    }

}
