package nars.budget.policy;

import nars.$;
import nars.NAR;
import nars.Symbols;
import nars.Task;
import nars.budget.Budget;
import nars.budget.BudgetFunctions;
import nars.budget.RawBudget;
import nars.nal.Premise;
import nars.nal.meta.PremiseEval;
import nars.term.Termed;
import nars.time.Tense;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.nal.UtilityFunctions.and;

/**
 * Created by me on 5/23/16.
 */
public class TaskBudgeting {

    public static @Nullable Budget derivation(float derivationQuality, @NotNull Termed derived, @NotNull PremiseEval p) {

        Premise baseBudget = p.premise;

        //Penalize by complexity: RELATIVE SIZE INCREASE METHOD
        /** occam factor */
        float occam = occamGrowth(derived, baseBudget);


        final float durability =
                baseBudget.dur() * occam * derivationQuality;
        if (durability < p.durMin)
            return null;

        float priority =
                //nal.taskLink.priIfFiniteElseZero() * volRatioScale;
                //or(nal.taskLink.priIfFiniteElseZero(), nal.termLink.priIfFiniteElseZero())
                //or(nal.taskLink.priIfFiniteElseZero(), nal.termLink.priIfFiniteElseZero())
                baseBudget.pri() * occam * derivationQuality
                //;
                ;

        final float quality =
                baseBudget.qua() * derivationQuality;
                //and(baseQuality, factor);
                //baseBudget.qua();



        //* occam //priority should be reduced as well as durability, because in the time between here and the next forgetting it should not have similar priority as parent in cases like Belief:Identity truth function derivations
        //* qual
        //if (priority * durability < Param.BUDGET_EPSILON)
        //return null;

        return $.b(priority, durability, quality);


        /* ORIGINAL: https://code.google.com/p/open-nars/source/browse/trunk/nars_core_java/nars/inference/BudgetFunctions.java
            Item t = memory.currentTaskLink;
            if (t == null) {
                t = memory.currentTask;
            }
            float priority = t.getPriority();
            float durability = t.getDurability() / complexity;
            float quality = qual / complexity;
            TermLink termLink = memory.currentBeliefLink;
            if (termLink != null) {
                priority = or(priority, termLink.getPriority());
                durability = and(durability, termLink.getDurability());
                float targetActivation = memory.getConceptActivation(termLink.getTarget());
                termLink.incPriority(or(quality, targetActivation));
                termLink.incDurability(quality);
            }
            return new BudgetValue(priority, durability, quality);
         */
    }

    /** occam's razor: penalize relative complexity growth
     * @return a value between 0 and 1 that priority will be scaled by */
    public static float occamGrowth(@NotNull Termed derived, @NotNull Premise pp) {
        Task parentBelief = pp.belief;
        int parentComplexity;
        int taskCompl = pp.task.complexity();
        if (parentBelief!=null) // && parentBelief.complexity() > parentComplexity)
            parentComplexity =
                //Math.max(taskCompl, parentBelief.complexity());
                Math.min(taskCompl, parentBelief.complexity());
        else
            parentComplexity = taskCompl;

        int derivedComplexity = derived.complexity();
        return parentComplexity / (1f + Math.max(parentComplexity, derivedComplexity));
    }

    /**
     * Backward logic with CompoundTerm conclusion, stronger case
     */
    @Nullable
    public static Budget derivationBackward(@NotNull Termed content, @NotNull PremiseEval premise) {
        return derivation(premise.nar.qualityDefault(Symbols.QUESTION), content, premise);
    }

    /**
     * Forward logic with CompoundTerm conclusion
     */
    @Nullable
    public static Budget derivationForward(@NotNull Truth truth, @NotNull Termed content, @NotNull PremiseEval premise) {
        return derivation(
                BudgetFunctions.truthToQuality(truth),
                content,
                premise);
    }

    /**
     * Evaluate the quality of a belief as a solution to a problem, then reward
     * the belief and de-prioritize the problem
     *
     * @param question  The problem (question or goal) to be solved
     * @param solution The belief as solution
     * @param question     The task to be immediately processed, or null for continued
     *                 process
     * @return The budget for the new task which is the belief activated, if
     * necessary
     */
    public static Budget solutionBudget(@NotNull Task question, @NotNull Task solution, @NotNull Truth projectedTruth, @NotNull NAR m) {
        //boolean feedbackToLinks = false;
        /*if (task == null) {
            task = nal.getCurrentTask();
            feedbackToLinks = true;
        }*/


        boolean judgmentTask = question.isBelief();
        //float om = orderMatch(problem.term(), solution.term(), duration);
        //if (om == 0) return 0f;
        float quality = Tense.solutionQuality(question, solution, projectedTruth, m.time());
        if (quality <= 0)
            return null;

        Budget budget = null;
        if (judgmentTask) {
            question.budget().orPriority(quality);
        } else {
            float taskPriority = question.pri();

            budget = new RawBudget(
                    and(taskPriority, quality),
                    //UtilityFunctions.or(taskPriority, quality),
                    question.dur(), BudgetFunctions.truthToQuality(solution.truth()));
            question.budget().setPriority(Math.min(1 - quality, taskPriority));
        }
        /*
        if (feedbackToLinks) {
            TaskLink tLink = nal.getCurrentTaskLink();
            tLink.setPriority(Math.min(1 - quality, tLink.getPriority()));
            TermLink bLink = nal.getCurrentBeliefLink();
            bLink.incPriority(quality);
        }*/
        return budget;
    }


//    public static float solutionQuality(Task problem, Task solution, Truth truth, long time) {
//        return Tense.solutionQuality(problem.hasQueryVar(), problem.getOccurrenceTime(), solution, truth, time);
//    }

}
