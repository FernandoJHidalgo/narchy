/*
 * LocalRules.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the abduction warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.nal;

import com.gs.collections.impl.tuple.Tuples;
import nars.Memory;
import nars.NAR;
import nars.Op;
import nars.Premise;
import nars.budget.Budget;
import nars.budget.BudgetFunctions;
import nars.budget.UnitBudget;
import nars.nal.nal7.Tense;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Term;
import nars.term.compound.Compound;
import nars.truth.Truth;

import java.util.function.Consumer;


/**
 * Directly process a task by a oldBelief, with only two Terms in both. In
 * matching, the new task is compared with an existing direct Task in that
 * Concept, to carry out:
 * <p/>
 * revision: between judgments or goals on non-overlapping evidence;
 * satisfy: between a Sentence and a Question/Goal;
 * merge: between items of the same type and stamp;
 * conversion: between different inheritance relations.
 */
public enum LocalRules {
    ;

//
//    /**
//     * Check whether two sentences can be used in revision
//     *
//     * @param newBelief The first sentence
//     * @param oldBelief The second sentence
//     * @return If revision is possible between the two sentences
//     */
//    public static boolean revisible(final Sentence newBelief, final Sentence oldBelief) {
//        if (newBelief.isRevisible())
//            if (newBelief.equalTerms(oldBelief))
//                if (TemporalRules.matchingOrder(newBelief.getTemporalOrder(), oldBelief.getTemporalOrder())) {
//                    return true;
//                }
//
//        return false;
//    }


//    /**
//     * Belief revision
//     * <p>
//     * called from Concept.reviseTable and match
//     *
//     * @param newBelief       The new belief in task
//     * @param oldBelief       The previous belief with the same content
//     * @param feedbackToLinks Whether to send feedback to the links
//
//     */
//    public static Task revision(final Task newBelief, final Task oldBelief, final boolean feedbackToLinks, final NAL nal) {
//        //Stamper stamp = nal.newStampIfNotOverlapping(newBelief, oldBelief);
//        //if (stamp == null) return null;
//
//        if (Stamp.overlapping(newBelief, oldBelief))
//            return null;
//
//        Truth newBeliefTruth = newBelief.getTruth();
//        Truth oldBeliefTruth = oldBelief.getTruth();
//        Truth truth = TruthFunctions.revision(newBeliefTruth, oldBeliefTruth);
//        Budget budget = BudgetFunctions.revise(newBeliefTruth, oldBeliefTruth, truth, nal);
//
//        Task revised = nal.validDerivation(nal.newTask(newBelief.getTerm())
//                .punctuation(newBelief.getPunctuation())
//                .truth(truth)
//                .budget(budget)
//                .parent(newBelief));
//
//        if (revised != null)
//            nal.memory().logic.BELIEF_REVISION.hit();
//
//        return revised;
//    }


    /**
     * Check if a Sentence provide a better answer to a Question or Goal
     *
     * @param proposedBelief       The proposed answer
     * @param question     The question to be processed
     * @return the projected Task, or the original Task
     */
    public static void trySolution(Task question, Task solution, NAR nal, Consumer<Task> eachSolutions) {

        if ((solution == null) || (solution.getDeleted())) {
            throw new RuntimeException("proposedBelief " + solution + " deleted or null");
        }

        if (!Tense.matchingOrder(question, solution)) {
            //System.out.println("Unsolved: Temporal order not matching");
            //memory.emit(Unsolved.class, task, belief, "Non-matching temporal Order");
            return;
        }

        Task sol = solution;

        Memory memory = nal.memory;

        long now = memory.time();

        /** temporary for comparing the result before unification and after */
        //float newQ0 = TemporalRules.solutionQuality(question, belief, projectedTruth, now);

        Truth originalTruth = solution.getTruth();

        Term solTerm = sol.term();
        if (solTerm.hasVarIndep() && !solTerm.equals(question.term())) {

            Term[] u = {question.term(), solTerm};

            Premise.unify(Op.VAR_INDEP, u, nal.memory, (st) -> {

                MutableTask ss = sol.solution((Compound) st,
                        sol.getPunctuation(),
                        originalTruth,
                        sol.getOccurrenceTime(),
                        question,
                        memory
                );

                if (eachSolutions!=null)
                    eachSolutions.accept(ss);

                //TODO move this to a callee's consumer?
                processSolution(question, nal, ss, memory, now);
            });
        } else {
            if (sol == null)
                throw new RuntimeException("Unification invalid: " + solution + " unified and projected to " + sol);

            if (eachSolutions!=null)
                eachSolutions.accept(sol);

            //TODO move this to a callee's consumer?
            processSolution(question, nal, sol, memory, now);
        }

    }

    public static void processSolution(Task question, NAR nal, Task sol, Memory memory, long now) {

        //use sol.getTruth() in case sol was changed since input to this method:
        float newQ = solutionQuality(question, sol, sol.getTruth(), now);
//        if (newQ == 0) {
//            memory.emotion.happy(0, questionTask, nal);
//            return null;
//        }

        Task oldBest = question.getBestSolution();

        //get the quality of the old solution if it were applied now (when conditions may differ)
        float oldQ = (oldBest != null) ? Tense.solutionQuality(question, oldBest, now) : 0;

        if (oldQ >= newQ) {
            //old solution was better
            return;
        }

        //else, new solution is btter
        memory.emotion.happy(newQ - oldQ, question);

        question.setBestSolution(sol);

        memory.logic.SOLUTION_BEST.set(newQ);


        //TODO solutionEval calculates the same solutionQuality as here, avoid this unnecessary redundancy
        Budget budget = solutionEval(question, sol, nal.time());

        if (!(question.isQuestion() || question.isQuest())) {
            System.err.println("err");
        }
        /*memory.output(task);

        //only questions and quests get here because else output is spammed
        if(task.sentence.isQuestion() || task.sentence.isQuest()) {
            memory.emit(Solved.class, task, belief);
        } else {
            memory.emit(Output.class, task, belief);
        }*/

        //nal.addSolution(nal.getCurrentTask(), budget, belief, task);

        //.reason(currentTask.getHistory())


        //if (belief != inputBelief) { //!belief.equals(inputBelief)) {
        //it was either unified and/or projected:
            /*belief = nal.addNewTask(nal.newTask(belief.getTerm(), belief.getPunctuation())
                            .truth(belief.getTruth())
                            .budget(budget)
                            .parent(belief) //.parent(questionTask, questionTask.getParentBelief())
                            .occurr(belief.getOccurrenceTime())

                            .solution(belief),
                    "Adjusted Solution",
                    true, false, false);*/


        /** decrease question's budget for transfer to solutions */
        question.getBudget().andPriority(budget.getPriority());

        //memory.eventDerived.emit(sol);
        //nal.nar().input(sol); //is this necessary? i cant find any reason for reinserting to input onw that it's part of the concept's belief/goal tables
        //}

        Task finalSolution = sol;
        //defer this event until after frame ends so reasoning in this cycle may continue
        nal.beforeNextFrame(() -> {
            nal.input(finalSolution);
            memory.eventAnswer.emit(Tuples.twin(question, finalSolution));
        });

    }

    /**
     * Evaluate the quality of a belief as a solution to a problem, then reward
     * the belief and de-prioritize the problem
     *
     * @param task  The problem (question or goal) to be solved
     * @param solution The belief as solution
     * @param task     The task to be immediately processed, or null for continued
     *                 process
     * @return The budget for the new task which is the belief activated, if
     * necessary
     */
    public static Budget solutionEval(Task task, Task solution, long time) {
        //boolean feedbackToLinks = false;
        /*if (task == null) {
            task = nal.getCurrentTask();
            feedbackToLinks = true;
        }*/
        boolean judgmentTask = task.isJudgment();
        float quality = Tense.solutionQuality(task, solution, time);
        if (quality <= 0)
            return null;

        Budget budget = null;
        if (judgmentTask) {
            task.getBudget().orPriority(quality);
        } else {
            float taskPriority = task.getPriority();

            budget = new UnitBudget(UtilityFunctions.or(taskPriority, quality), task.getDurability(), BudgetFunctions.truthToQuality(solution.getTruth()));
            task.getBudget().setPriority(Math.min(1 - quality, taskPriority));
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

    public static float solutionQuality(Task problem, Task solution, Truth truth, long time) {
        return Tense.solutionQuality(problem.hasQueryVar(), problem.getOccurrenceTime(), solution, truth, time);
    }

    /**
     * WARNING: this assumes terms are already
     * known to be equal.
     */
    public static boolean revisible(Task newBelief, Task oldBelief) {

        //TODO maybe add DEBUG test: newBelief and oldBelief term must be equal

        if (newBelief.isRevisible() && (!newBelief.equals(oldBelief))) {
            if (Tense.matchingOrder(newBelief.getTemporalOrder(), oldBelief.getTemporalOrder()))
                return true;
        }

        return false;
    }


    /* -------------------- same terms, difference relations -------------------- */
//
//    /**
//     * The task and belief match reversely
//     *
//     * @param p Reference to the memory
//     */
//    public static Task matchReverse(final Premise p) {
//        final Task task = p.getTask();
//        final Task belief = p.getBelief();
//
//        if (TemporalRules.matchingOrder(task.getTemporalOrder(), TemporalRules.reverseOrder(belief.getTemporalOrder()))) {
//            if (task.isJudgment()) {
//                return NAL2.inferToSym(task, belief, p);
//            } else {
//                return conversion(p);
//            }
//        }
//        return null;
//    }

//    /**
//     * Inheritance/Implication matches Similarity/Equivalence
//     *
//     * @param asym   A Inheritance/Implication sentence
//     * @param sym    A Similarity/Equivalence sentence
//     * @param figure location of the shared term
//     * @param p    Reference to the memory
//     */
//    public static void matchAsymSym(final Task asym, final Task sym, int figure, final Premise p) {
//        if (p.getTask().isJudgment()) {
//            inferToAsym(asym, sym, p);
//        } else {
//            convertRelation(p);
//        }
//    }

//    /* -------------------- two-premise logic rules -------------------- */
//
//    /**
//     * {<S <-> P>, <P --> S>} |- <S --> P> Produce an Inheritance/Implication
//     * from a Similarity/Equivalence and a reversed Inheritance/Implication
//     *
//     * @param asym The asymmetric premise
//     * @param sym  The symmetric premise
//     * @param p  Reference to the memory
//     */
//    private static Task inferToAsym(Task asym, Task sym, Premise p) {
//        TaskSeed s = p.newDoublePremise(asym, sym);
//        if (s == null)
//            return null;
//
//        Statement statement = (Statement) asym.getTerm();
//        Term sub = statement.getPredicate();
//        Term pre = statement.getSubject();
//
//        Statement content = Statement.make(statement, sub, pre, statement.getTemporalOrder());
//        if (content == null) return null;
//
//        Truth truth = TruthFunctions.reduceConjunction(sym.getTruth(), asym.getTruth());
//
//
//            return p.deriveDouble(
//                    s.term(content)
//                            .punctuation(asym.getPunctuation())
//                            .truth(truth)
//                            .budget(BudgetFunctions.forward(truth, p)),
//                    false);
//
//
//
//    }
//
//    /* -------------------- one-premise logic rules -------------------- */
//
//    /**
//     * {<P --> S>} |- <S --> P> Produce an Inheritance/Implication from a
//     * reversed Inheritance/Implication
//     *
//     * @param p Reference to the memory
//     */
//    private static Task conversion(final Premise p) {
//        Truth truth = TruthFunctions.conversion(p.getBelief().getTruth());
//        Budget budget = BudgetFunctions.forward(truth, p);
//        return convertedJudgment(truth, budget, p);
//    }
//
//    /**
//     * {<S --> P>} |- <S <-> P> {<S <-> P>} |- <S --> P> Switch between
//     * Inheritance/Implication and Similarity/Equivalence
//     *
//     * @param p Reference to the memory
//     */
//    private static Task convertRelation(final Premise p) {
//        final Truth beliefTruth = p.getBelief().getTruth();
//        final AnalyticTruth truth;
//        if ((p.getTask().getTerm()).isCommutative()) {
//            truth = TruthFunctions.abduction(beliefTruth, 1.0f);
//        } else {
//            truth = TruthFunctions.deduction(beliefTruth, 1.0f);
//        }
//        if (truth != null) {
//            Budget budget = BudgetFunctions.forward(truth, p);
//            return convertedJudgment(truth, budget, p);
//        }
//        return null;
//    }
//
//    /**
//     * Convert judgment into different relation
//     * <p>
//     * called in MatchingRules
//     *
//     * @param budget The budget value of the new task
//     * @param truth  The truth value of the new task
//     * @param p    Reference to the memory
//     */
//    private static Task convertedJudgment(final Truth newTruth, final Budget newBudget, final Premise p) {
//        Statement content = (Statement) p.getTask().getTerm();
//        Statement beliefContent = (Statement) p.getBelief().getTerm();
//        int order = TemporalRules.reverseOrder(beliefContent.getTemporalOrder());
//        final Term subjT = content.getSubject();
//        final Term predT = content.getPredicate();
//        final Term subjB = beliefContent.getSubject();
//        final Term predB = beliefContent.getPredicate();
//        Term otherTerm;
//
//        if (subjT.hasVarQuery() && predT.hasVarQuery()) {
//            //System.err.println("both subj and pred have query; this case is not implemented yet (if it ever occurrs)");
//            //throw new RuntimeException("both subj and pred have query; this case is not implemented yet (if it ever occurrs)");
//        } else if (subjT.hasVarQuery()) {
//            otherTerm = (predT.equals(subjB)) ? predB : subjB;
//            content = Statement.make(content, otherTerm, predT, order);
//        } else if (predT.hasVarQuery()) {
//            otherTerm = (subjT.equals(subjB)) ? predB : subjB;
//            content = Statement.make(content, subjT, otherTerm, order);
//        }
//
//        if (content != null)
//            return p.deriveSingle(content, Symbols.JUDGMENT, newTruth, newBudget);
//
//        return null;
//    }


}
