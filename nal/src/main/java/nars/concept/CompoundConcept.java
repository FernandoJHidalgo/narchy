package nars.concept;

import jcog.Util;
import nars.NAR;
import nars.Task;
import nars.attention.Activation;
import nars.attention.SpreadingActivation;
import jcog.bag.Bag;
import nars.budget.BLink;
import nars.budget.BudgetMerge;
import nars.conceptualize.DefaultConceptBuilder;
import nars.conceptualize.state.ConceptState;
import nars.table.*;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termlike;
import nars.time.Time;
import nars.truth.Truth;
import nars.truth.TruthDelta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.Param.TRUTH_EPSILON;


public class CompoundConcept<T extends Compound> implements Concept, Termlike {

    @NotNull
    private final Bag<Task,BLink<Task>> taskLinks;
    @NotNull
    private final Bag<Term,BLink<Term>> termLinks;

    /**
     * how incoming budget is merged into its existing duplicate quest/question
     */


    @NotNull
    private final T term;

    @Nullable
    private QuestionTable questions;
    @Nullable
    private QuestionTable quests;
    @Nullable
    protected BeliefTable beliefs;
    @Nullable
    protected BeliefTable goals;

    private @Nullable Map meta;

    @NotNull
    private transient ConceptState state;


    /**
     * Constructor, called in Memory.getConcept only
     *
     * @param term      A term corresponding to the concept
     * @param termLinks
     * @param taskLinks
     */
    public CompoundConcept(@NotNull T term, @NotNull Bag<Term,BLink<Term>> termLinks, @NotNull Bag<Task,BLink<Task>> taskLinks, @NotNull NAR nar) {

        this.term = term;
        this.termLinks = termLinks;
        this.taskLinks = taskLinks;


        this.state = ConceptState.Deleted;
    }



    @NotNull
    @Override
    public T term() {
        return term;
    }


    @Override
    public void setMeta(@NotNull Map newMeta) {
        this.meta = newMeta;
    }

    @NotNull
    @Override
    public <C> C meta(@NotNull Object key, @NotNull BiFunction value) {
        throw new UnsupportedOperationException();
    }


    @Override
    public @Nullable Map<Object, Object> meta() {
        return meta;
    }

    @Override
    public @NotNull Bag<Task,BLink<Task>> tasklinks() {
        return taskLinks;
    }

    @NotNull
    @Override
    public Bag<Term,BLink<Term>> termlinks() {
        return termLinks;
    }


    /**
     * used for setting an explicit OperationConcept instance via java; activates it on initialization
     */
    public CompoundConcept(@NotNull T term, @NotNull NAR n) {
        this(term, (DefaultConceptBuilder) n.concepts.conceptBuilder(), n, ((DefaultConceptBuilder) n.concepts.conceptBuilder()).newBagMap(term.volume()));
    }

    /**
     * default construction by a NAR on conceptualization
     */
    CompoundConcept(@NotNull T term, @NotNull DefaultConceptBuilder b, @NotNull NAR nar, @NotNull Map sharedMap) {
        this(term, b.newBag(sharedMap), b.newBag(sharedMap), nar);
    }



    /**
     * Pending Quests to be answered by new desire values
     */
    @Nullable
    @Override
    public final QuestionTable quests() {
        return questionTableOrEmpty(quests);
    }

    @NotNull
    @Override
    public final QuestionTable questions() {
        return questionTableOrEmpty(questions);
    }


    @NotNull
    static QuestionTable questionTableOrEmpty(@Nullable QuestionTable q) {
        return q != null ? q : QuestionTable.EMPTY;
    }

    @NotNull
    static BeliefTable beliefTableOrEmpty(@Nullable BeliefTable b) {
        return b != null ? b : BeliefTable.EMPTY;
    }

    @NotNull
    final QuestionTable questionsOrNew(@NotNull NAR nar) {
        return questions == null ? (questions =
                //new ArrayQuestionTable(state.questionCap(true)))
                new HijackQuestionTable(state.questionCap(true), 3, BudgetMerge.maxBlend, nar.random))
                : questions;

    }

    @NotNull
    final QuestionTable questsOrNew(@NotNull NAR nar) {
        return quests == null ? (quests =
                //new ArrayQuestionTable(state.questionCap(false)))
                new HijackQuestionTable(state.questionCap(true), 3, BudgetMerge.maxBlend, nar.random))
                : quests;
    }

    @NotNull
    final BeliefTable beliefsOrNew(@NotNull NAR nar) {
        return beliefs == null ? (beliefs = newBeliefTable(nar, true)) : beliefs;
    }


    @NotNull
    final BeliefTable goalsOrNew(@NotNull NAR nar) {
        return goals == null ? (goals = newBeliefTable(nar, false)) : goals;
    }

    @NotNull
    protected BeliefTable newBeliefTable(NAR nar, boolean beliefOrGoal) {
        int eCap = state.beliefCap(this, beliefOrGoal, true);
        int tCap = state.beliefCap(this, beliefOrGoal, false);
        return newBeliefTable(nar, beliefOrGoal, eCap, tCap);
    }


    protected BeliefTable newBeliefTable(NAR nar, boolean beliefOrGoal, int eCap, int tCap) {

        return new DefaultBeliefTable( );
    }

    public ListTemporalBeliefTable newTemporalTable(final int tCap) {
        return new ListTemporalBeliefTable(tCap);
    }

    public EternalTable newEternalTable(int eCap) {
        return eCap > 0 ? new EternalTable(eCap) : EternalTable.EMPTY;
    }


    /**
     * Judgments directly made about the term Use ArrayList because of access
     * and insertion in the middle
     */
    @NotNull
    @Override
    public final BeliefTable beliefs() {
        return beliefTableOrEmpty(beliefs);
    }

    /**
     * Desire values on the term, similar to the above one
     */
    @NotNull
    @Override
    public final BeliefTable goals() {
        return beliefTableOrEmpty(goals);
    }


    public
    @Nullable
    boolean processQuest(@NotNull Task task, @NotNull NAR nar) {
        return processQuestion(task, nar);
    }


    @Override
    public void delete(@NotNull NAR nar) {

        Concept.delete(this, nar);

        beliefs = goals = null;
        questions = quests = null;
        meta = null;
    }


    /**
     * To accept a new judgment as belief, and check for revisions and solutions
     * Returns null if the task was not accepted, else the goal which was accepted and somehow modified the state of this concept
     */
    public @Nullable TruthDelta processBelief(@NotNull Task belief, @NotNull NAR nar) {
        return processBeliefOrGoal(belief, beliefsOrNew(nar), questions(), nar);
    }

    /**
     * To accept a new goal, and check for revisions and realization, then
     * decide whether to actively pursue it
     * Returns null if the task was not accepted, else the goal which was accepted and somehow modified the state of this concept
     */
    public @Nullable TruthDelta processGoal(@NotNull Task goal, @NotNull NAR nar) {
        return processBeliefOrGoal(goal, goalsOrNew(nar), quests(), nar);
    }

    /**
     * @return null if the task was not accepted, else the goal which was accepted and somehow modified the state of this concept
     * TODO remove synchronized by lock-free technique
     */
    private final TruthDelta processBeliefOrGoal(@NotNull Task belief, @NotNull BeliefTable target, @NotNull QuestionTable questions, @NotNull NAR nar) {

        return target.add(belief, questions, this, nar);

    }


    @Override
    public final ConceptState state() {
        return state;
    }

    @Override
    public final ConceptState state(@NotNull ConceptState p, NAR nar) {
        ConceptState current = this.state;
        if (current != p) {
            this.state = p;
            linkCapacity( p.linkCap(this, true), p.linkCap(this, false));

            beliefCapacity(p, nar);
            questionCapacity(p, nar);
        }
        return current;
    }



    protected void beliefCapacity(@NotNull ConceptState p, NAR nar) {

        int be = p.beliefCap(this, true, true);
        int bt = p.beliefCap(this, true, false);

        int ge = p.beliefCap(this, false, true);
        int gt = p.beliefCap(this, false, false);

        beliefCapacity(be, bt, ge, gt, nar);
    }

    protected final void beliefCapacity(int be, int bt, int ge, int gt, NAR nar) {

        beliefs().capacity(be, bt, nar);
        goals().capacity(ge, gt, nar);

    }

    protected void questionCapacity(@NotNull ConceptState p, NAR nar) {
        questions().capacity((byte) p.questionCap(true), nar);
        quests().capacity((byte) p.questionCap(false), nar);
    }

    /**
     * To answer a quest or q by existing beliefs
     *
     * @param q         The task to be processed
     * @param nar
     * @param displaced
     * @return the relevant task
     */
    public boolean processQuestion(@NotNull Task q, @NotNull NAR nar) {

        final QuestionTable questionTable;
        final BeliefTable answerTable;
        if (q.isQuestion()) {
            //if (questions == null) questions = new ArrayQuestionTable(nar.conceptQuestionsMax.intValue());
            questionTable = questionsOrNew(nar);
            answerTable = beliefs();
        } else { // else if (q.isQuest())
            //if (quests == null) quests = new ArrayQuestionTable(nar.conceptQuestionsMax.intValue());
            questionTable = questsOrNew(nar);
            answerTable = goals();
        }


        return questionTable.add(q, answerTable,  nar) != null;
    }




    public static final BudgetMerge DuplicateMerge = BudgetMerge.maxHard; //this should probably always be max otherwise incoming duplicates may decrease the existing priority

    /**
     * Directly process a new task. Called exactly once on each task. Using
     * local information and finishing in a constant time. Provide feedback in
     * the taskBudget value of the task.
     * <p>
     * called in Memory.immediateProcess only
     *
     * @return null if not processed, or an Activation instance to continue with link activation and feedback
     */
    @Nullable
    @Override
    public final Activation process(@NotNull Task input, @NotNull NAR nar) {


        boolean accepted = false;

        TruthDelta delta = null;

        switch (input.punc()) {
            case BELIEF:
                delta = processBelief(input, nar);
                break;

            case GOAL:
                delta = processGoal(input, nar);
                break;

            case QUESTION:
                accepted = processQuestion(input, nar);
                break;

            case QUEST:
                accepted = processQuest(input, nar);
                break;

            default:
                throw new RuntimeException("Invalid sentence type: " + input);
        }

        if (delta != null)
            accepted = true;

        Activation a;
        if (accepted) {
            a = activateTask(input, nar);

            if (delta != null) {
                //beliefs/goals
                feedback(input, delta, (CompoundConcept) a.origin, nar);
            } else {
                //questions/quests
                input.feedback(delta, 0, 0, nar);
            }

            //check again if during feedback, the task decided deleted itself
            if (input.isDeleted()) {
                a = null;
            }
        } else {
            input.feedback(null, Float.NaN, Float.NaN, nar);
            a = null;
        }

        return a;
    }

    public Activation activateTask(@NotNull Task input, @NotNull NAR nar) {
        return activateTask(input, nar, 1f);
    }

    public Activation activateTask(@NotNull Task input, @NotNull NAR nar, float scale) {
        //return new DepthFirstActivation(input, this, nar, nar.priorityFactor.floatValue());
        return new SpreadingActivation(input, this, nar, nar.priorityFactor.floatValue() * scale);
    }

    /**
     * apply derivation feedback and update NAR emotion state
     */
    protected static void feedback(@NotNull Task input, @NotNull TruthDelta delta, @NotNull CompoundConcept concept, @NotNull NAR nar) {


        //update emotion happy/sad
        Truth before = delta.before;
        Truth after = delta.after;

        float deltaSatisfaction, deltaConf;

        if (before != null && after != null) {

            float deltaFreq = after.freq() - before.conf();
            deltaConf = after.conf() - before.conf();

            Truth other;
            float polarity = 0;

            Time time = nar.time;
            float dur = time.dur();
            long now = time.time();
            if (input.isBelief()) {
                //compare against the current goal state
                other = concept.goals().truth(now, dur);
                if (other != null)
                    polarity = +1f;
            } else if (input.isGoal()) {
                //compare against the current belief state
                other = concept.beliefs().truth(now, dur);
                if (other != null)
                    polarity = -1f;
            } else {
                other = null;
            }


            if (other != null) {

                float f = other.freq();

                if (Util.equals(f, 0.5f, TRUTH_EPSILON)) {

                    //ambivalence: no change
                    deltaSatisfaction = 0;

                } else if (f > 0.5f) {
                    //measure how much the freq increased since goal is positive
                    deltaSatisfaction = +polarity * deltaFreq / (2f * (other.freq() - 0.5f));
                } else {
                    //measure how much the freq decreased since goal is negative
                    deltaSatisfaction = -polarity * deltaFreq / (2f * (0.5f - other.freq()));
                }

                nar.emotion.happy(deltaSatisfaction, input.term());

            } else {
                deltaSatisfaction = 0;
            }

        } else {
            if (before == null && after != null) {
                deltaConf = after.conf();
            } else {
                deltaConf = 0;
            }
            deltaSatisfaction = 0;
        }

        if (!Util.equals(deltaConf, 0f, TRUTH_EPSILON))
            nar.emotion.confident(deltaConf, input.term());

        input.feedback(delta, deltaConf, deltaSatisfaction, nar);

    }

//    private void checkConsistency() {
//        synchronized (tasks) {
//            int mapSize = tasks.size();
//            int tableSize = beliefs().size() + goals().size() + questions().size() + quests().size();
//
//            int THRESHOLD = 50; //to catch when the table explodes and not just an off-by-one inconsistency that will correct itself in the next cycle
//            if (Math.abs(mapSize - tableSize) > THRESHOLD) {
//                //List<Task> mapTasks = new ArrayList(tasks.keySet());
//                Set<Task> mapTasks = tasks.keySet();
//                ArrayList<Task> tableTasks = Lists.newArrayList(
//                        Iterables.concat(beliefs(), goals(), questions(), quests())
//                );
//                //Collections.sort(mapTasks);
//                //Collections.sort(tableTasks);
//
//                System.err.println(mapSize + " vs " + tableSize + "\t\t" + mapTasks.size() + " vs " + tableTasks.size());
//                System.err.println(Joiner.on('\n').join(mapTasks));
//                System.err.println("----");
//                System.err.println(Joiner.on('\n').join(tableTasks));
//                System.err.println("----");
//            }
//        }
//    }

//    public long minTime() {
//        ageFactor();
//        return min;
//    }
//
//    public long maxTime() {
//        ageFactor();
//        return max;
//    }
//
//    public float ageFactor() {
//
//        if (min == ETERNAL) {
//            //invalidated, recalc:
//            long t[] = new long[] { Long.MAX_VALUE, Long.MIN_VALUE };
//
//            beliefs.range(t);
//            goals.range(t);
//
//            if (t[0] == Long.MAX_VALUE) {
//                min = max= 0;
//            } else {
//                min = t[0];
//                max = t[1];
//            }
//
//        }
//
//        //return 1f;
//        long range = max - min;
//        /* history factor:
//           higher means it is easier to hold beliefs further away from current time at the expense of accuracy
//           lower means more accuracy at the expense of shorter memory span
//     */
//        float historyFactor = Param.TEMPORAL_DURATION;
//        return (range == 0) ? 1 :
//                ((1f) / (range * historyFactor));
//    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj || term.equals(obj);
    }

    @Override
    public final int hashCode() {
        return term.hashCode();
    }

    @Override
    public final String toString() {
        return term.toString();
    }

    @NotNull
    public Term term(int i) {
        return term.term(i);
    }

    @Override
    public int size() {
        return term.size();
    }

    /** first-level only */
    @Deprecated @Override public boolean containsTerm(@NotNull Termlike t) {
        return term.containsTerm(t);
    }

    @Deprecated
    @Override
    public boolean hasTemporal() {
        return term.hasTemporal();
    }

    @Nullable
    @Deprecated
    @Override
    public Term termOr(int i, @Nullable Term ifOutOfBounds) {
        return term.termOr(i, ifOutOfBounds);
    }

    @Deprecated
    @Override
    public boolean and(@NotNull Predicate<Term> v) {
        return term.and(v);
    }

    @Deprecated
    @Override
    public boolean or(@NotNull Predicate<Term> v) {
        return term.or(v);
    }

    @Deprecated
    @Override
    public int vars() {
        return term.vars();
    }

    @Deprecated
    @Override
    public int varIndep() {
        return term.varIndep();
    }

    @Deprecated
    @Override
    public int varDep() {
        return term.varDep();
    }

    @Deprecated
    @Override
    public int varQuery() {
        return term.varQuery();
    }

    @Deprecated
    @Override
    public int varPattern() {
        return term.varPattern();
    }

    @Deprecated
    @Override
    public int complexity() {
        return term.complexity();
    }

    @Deprecated
    @Override
    public int structure() {
        return term.structure();
    }

    @Override
    public int volume() {
        return term.volume();
    }

    public int taskCount() {
        int s = 0;
        if (beliefs!=null)
            s += beliefs.size();
        if (goals!=null)
            s += goals.size();
        if (questions!=null)
            s += questions.size();
        if (quests!=null)
            s += quests.size();

        return s;
    }

//    static final class MyMicrosphereTemporalBeliefTable extends MicrosphereTemporalBeliefTable {
//
//        private final Time time;
//
//        public MyMicrosphereTemporalBeliefTable(int tCap, Time time) {
//            super(tCap);
//            this.time = time;
//        }
//
//        @Override public float focus(float dt, float evidence) {
//            return TruthPolation.evidenceDecay(evidence, time.duration(), dt);
//        }
//    }
}
