package nars;

import nars.budget.Budgeted;
import nars.concept.Concept;
import nars.concept.TruthDelta;
import nars.index.TermIndex;
import nars.nal.Stamp;
import nars.time.Tense;
import nars.task.MutableTask;
import nars.task.Revision;
import nars.task.Tasked;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.ProjectedTruth;
import nars.truth.Truth;
import nars.truth.Truthed;
import nars.util.data.LongString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.TIMELESS;
import static nars.truth.TruthFunctions.eternalize;

/**
 * A task to be processed, consists of a Sentence and a BudgetValue.
 * A task references its parent and an optional causal factor (usually an Operation instance).  These are implemented as WeakReference to allow forgetting via the
 * garbage collection process.  Otherwise, Task ancestry would grow unbounded,
 * violating the assumption of insufficient resources (AIKR).
 * <p>
 * TODO decide if the Sentence fields need to be Reference<> also
 */
public interface Task extends Budgeted, Truthed, Comparable<Task>, Stamp, Termed<Compound>, Tasked {


    List<Task> EmptyTaskList = Collections.emptyList();

    static void proof(@NotNull Task task, int indent, @NotNull StringBuilder sb) {
        //TODO StringBuilder

        for (int i = 0; i < indent; i++)
            sb.append("  ");

        task.appendTo(sb, null, true);

//        List l = task.getLog();
//        if (l!=null)
//            sb.append(" log=").append(l);

//        if (task.getBestSolution() != null) {
//            if (!task.term().equals(task.getBestSolution().term())) {
//                sb.append(" solution=");
//                task.getBestSolution().appendTo(sb);
//            }
//        }

        sb.append("\n  ");

        Task pt = task.getParentTask();
        if (pt != null) {
            //sb.append("  PARENT ");
            proof(pt, indent + 1, sb);
        }

        Task pb = task.getParentBelief();
        if (pb != null) {
            //sb.append("  BELIEF ");
            proof(pb, indent + 1, sb);
        }
    }

//    static Set<Truthed> getSentences(Iterable<Task> tasks) {
//
//        int size;
//
//        size = tasks instanceof Collection ? ((Collection) tasks).size() : 2;
//
//        Set<Truthed> s = Global.newHashSet(size);
//        for (Task t : tasks)
//            s.add(t);
//        return s;
//    }


    /**
     * performs some (but not exhaustive) tests on a term to determine some cases where it is invalid as a sentence content
     * returns the compound valid for a Task if so,
     * otherwise returns null
     */
    @Nullable
    static boolean taskContentPreTest(@NotNull Term t, char punc, @NotNull NAR nar, boolean safe) {

        if (!(t instanceof Compound))
            return test(t, "Task Term is null or not a Compound", safe);

        //t = memory.index.normalize(t, !safe);

        //if (!(t instanceof Compound))
        //return test(t, "Task Term Does Not Normalize to Compound", safe);

        Compound ct = (Compound) t;

        /* A statement sentence is not allowed to have a independent variable as subj or pred"); */
        Op op = t.op();

        if (op.isStatement() && subjectOrPredicateIsIndependentVar(ct))
            return test(t, "Statement Task's subject or predicate is VAR_INDEP", safe);

//        if (hasCoNegatedAtemporalConjunction(ct)) {
//            throw new TermIndex.InvalidTaskTerm(t, "Co-negation in commutive conjunction");
//        }

        if ((punc == Symbols.GOAL || punc == Symbols.QUEST) && (op == Op.IMPL || op == Op.EQUI))
            return test(t, "Goal/Quest task term may not be Implication or Equivalence", safe);


        if (t.volume() > nar.compoundVolumeMax.intValue())
            return test(t, "Term exceeds maximum volume", safe);
        if (!t.levelValid(nar.nal()))
            return test(t, "Term exceeds maximum NAL level", safe);

        return true;
    }

    @Nullable
    static boolean test(@Nullable Term t, String reason, boolean safe) {
        if (safe)
            return false;
        else
            throw new NAR.InvalidTaskException(t, reason);
    }

//    static boolean hasCoNegatedAtemporalConjunction(Term term) {
//        if (term instanceof Compound) {
//
//            Compound cterm = ((Compound) term);
//
//            int dt = cterm.dt();
//            if (term.op() == CONJ && (dt ==DTERNAL || dt == 0) && cterm.subterms().hasAny(NEG)) {
//                int s = cterm.size();
//                for (int i = 0; i < s; i++) {
//                    Term x = cterm.term(i);
//                    if (x.op() == NEG) {
//                        if (cterm.containsTerm(((Compound) x).term(0))) {
//                            return true;
//                        }
//                    }
//                }
//            }
//
//            if (term.hasAll(CONJUNCTION_WITH_NEGATION)) {
//                return term.or(Task::hasCoNegatedAtemporalConjunction);
//            }
//        }
//        return false;
//
//    }

//    static float prioritySum(@NotNull Iterable<? extends Budgeted > dd) {
//        float f = 0;
//        for (Budgeted  x : dd)
//            f += x.pri();
//        return f;
//    }

    static boolean subjectOrPredicateIsIndependentVar(@NotNull Compound t) {
        return t.hasVarIndep() && ((t.isTerm(0, Op.VAR_INDEP) || t.isTerm(1, Op.VAR_INDEP)));
    }


    @Override
    @NotNull
    default Task task() {
        return this;
    }


    /**
     * Check whether different aspects of sentence are equivalent to another one
     *
     * @param that The other judgment
     * @return Whether the two are equivalent
     */
    boolean equivalentTo(@NotNull Task that, boolean punctuation, boolean term, boolean truth, boolean stamp, boolean creationTime);




//
//    /** projects a belief task to match a question */
//    @Nullable default Task projectMatched(@NotNull Task question, @NotNull Memory memory, float minConf) {
//
//        assert(!question.isEternal());
//        if (question.occurrence() == occurrence())
//            return this; //exact time already
//
//        long now = memory.time();
//
//
//        @NotNull Compound theTerm = term();
//
//
//        //TODO avoid creating new Truth instances
//        Truth solTruth = projectTruth(question.occurrence(), now, false);
//        /*if (solTruth == null)
//            return null;*/
//
//        if (solTruth.conf() < minConf)
//            return null;
//
//        //if truth instanceof ProjectedTruth, use its attached occ time (possibly eternal or temporal), otherwise assume it is this task's occurence time
//        long solutionOcc = solTruth instanceof ProjectedTruth ?
//                ((ProjectedTruth)solTruth).when : occurrence();
//
//
//
//        Budget solutionBudget = solutionBudget(question, this, solTruth, memory);
//        /*if (solutionBudget == null)
//            return null;*/
//
//        Task solution;
//        //if ((!truth().equals(solTruth)) || (!newTerm.equals(term())) || (solutionOcc!= occCurrent)) {
//        solution = new MutableTask(theTerm, punc(), solTruth)
//                .time(now, solutionOcc)
//                .parent(this)
//                .budget(solutionBudget)
//                //.state(state())
//                //.setEvidence(evidence())
//                .log("Projected Belief")
//                //.log("Projected from " + this)
//        ;
//
//        return solution;
//    }


    char punc();

//    @Nullable
//    @Override
//    long[] evidence();

    @Override
    long creation();

    @NotNull
    @Override
    Task setCreationTime(long c);

    boolean isQuestion();

    boolean isQuest();

    boolean isBelief();

    boolean isGoal();

    default boolean isCommand() {
        return (punc() == Symbols.COMMAND);
    }

    default boolean hasQueryVar() {
        return term().hasVarQuery();
    }


    @Nullable
    default StringBuilder appendTo(StringBuilder sb) {
        return appendTo(sb, null);
    }

    @Nullable
    default CharSequence toString(Memory memory, boolean showStamp) {
        return appendTo(new StringBuilder(), memory, showStamp);
    }


    @Override
    boolean delete();

    /**
     * you should use this delete, not the other
     */
    default void delete(@NotNull NAR nar) {
        nar.tasks.remove(this);
        delete();
    }

    @Nullable
    default Concept concept(@NotNull NAR n) {
        return n.concept(term(), true);
    }

    /** called if this task is entered into a concept's belief tables
     *  TODO what about for questions/quests
     * */
    void feedback(TruthDelta delta, float deltaConfidence, float deltaSatisfaction, NAR nar);

    @NotNull
    @Override
    Compound term();


    default boolean isQuestOrQuestion() {
        return isQuestion() || isQuest();
    }

    default boolean isBeliefOrGoal() {
        return isBelief() || isGoal();
    }

//    /** allows for budget feedback that occurrs on revision */
//    default boolean onRevision(Task conclusion) {
//        return true;
//    }

    /**
     * for question tasks: when an answer appears.
     * return false if this question is finished and should be removed from a table containing it
     */
    default boolean onAnswered(Task answer) {
        return true;
    }


//    @NotNull
//    default Task projectTask(long when, long now) {
//        Truth adjustedTruth = projectTruth(when, now, false);
//        long occ = occurrence();
//        long projOcc = (adjustedTruth instanceof ProjectedTruth) ? ((ProjectedTruth)adjustedTruth).when : occ;
//        return /*occ == projOcc &&*/ adjustedTruth.equals(truth()) ? this :
//                MutableTask.project(this, adjustedTruth, now, projOcc);
//
//    }


//    /** get the absolute time of an event subterm, if present, TIMELESS otherwise */
//    default long subtermTimeAbs(Term x) {
//        long t = subtermTime(x);
//        if (t == TIMELESS) return TIMELESS;
//        return t + occurrence();
//    }

//    /** relevant time of an event subterm (or self), if present, TIMELESS otherwise */
//    default long subtermTime(Term x) {
//        return term().subtermTime(x, x.t());
//    }


//    default float projectionConfidence(long when, long now) {
//        //TODO avoid creating Truth Values by calculating the confidence directly. then use this in projection's original usage as well
//
//        float factor = TruthFunctions.temporalProjection(getOccurrenceTime(), when, now);
//
//        return factor * getConfidence();
//
//        //return projection(when, now).getConfidence();
//    }


//    final class Solution extends AtomicReference<Task> {
//        Solution(Task referent) {
//            super(referent);
//        }
//
//        @NotNull
//        @Override
//        public String toString() {
//            return "Solved: " + get();
//        }
//    }

    @NotNull
    default StringBuilder toString(/**@Nullable*/Memory memory) {
        return appendTo(null, memory);
    }

    @NotNull
    default StringBuilder appendTo(@Nullable StringBuilder sb, /**@Nullable*/Memory memory) {
        if (sb == null) sb = new StringBuilder();
        return appendTo(sb, memory, false);
    }

    @NotNull
    @Deprecated
    default String toStringWithoutBudget() {
        return toStringWithoutBudget(null);
    }

    @NotNull
    @Deprecated
    default String toStringWithoutBudget(Memory memory) {
        StringBuilder b = new StringBuilder();
        appendTo(b, memory, true, false,
                false, //budget
                false//log
        );
        return b.toString();
    }

    @Nullable
    @Deprecated
    default StringBuilder appendTo(StringBuilder buffer, /**@Nullable*/Memory memory, boolean showStamp) {
        boolean notCommand = punc() != Symbols.COMMAND;
        return appendTo(buffer, memory, true, showStamp && notCommand,
                notCommand, //budget
                showStamp //log
        );
    }

    @Nullable
    default StringBuilder appendTo(@Nullable StringBuilder buffer, /**@Nullable*/@Nullable Memory memory, boolean term, boolean showStamp, boolean showBudget, boolean showLog) {


        Compound t = term();
        String contentName = t.toString();

        CharSequence tenseString;
        if (memory != null) {
            tenseString = getTense(memory.time(), 1);
        } else {
            //TODO dont bother craeting new StringBuilder and calculating the entire length etc.. just append it to a reusable StringReader?
            appendOccurrenceTime(
                    (StringBuilder) (tenseString = new StringBuilder()));
        }


        CharSequence stampString = showStamp ? stampAsStringBuilder() : null;

        int stringLength = contentName.length() + tenseString.length() + 1 + 1;

        if (truth() != null)
            stringLength += 11;

        if (showStamp)
            stringLength += stampString.length() + 1;

        /*if (showBudget)*/
        //"$0.8069;0.0117;0.6643$ "
        stringLength += 1 + 6 + 1 + 6 + 1 + 6 + 1 + 1;

        String finalLog;
        if (showLog) {
            Object ll = lastLogged();

            finalLog = (ll != null ? ll.toString() : null);
            if (finalLog != null)
                stringLength += finalLog.length() + 1;
            else
                showLog = false;
        } else
            finalLog = null;


        if (buffer == null)
            buffer = new StringBuilder(stringLength);
        else
            buffer.ensureCapacity(stringLength);


        if (showBudget) {
            budget().toBudgetStringExternal(buffer).append(' ');
        }

        buffer.append(contentName).append(punc());

        if (tenseString.length() > 0)
            buffer.append(' ').append(tenseString);

        if (truth() != null) {
            buffer.append(' ');
            truth().appendString(buffer, 2);
        }

        if (showStamp) {
            buffer.append(' ').append(stampString);
        }

        if (showLog) {
            buffer.append(' ').append(finalLog);
        }

        return buffer;
    }

    @Nullable
    default Object lastLogged() {
        List<String> log = log();
        return log == null || log.isEmpty() ? null : log.get(log.size() - 1);
    }


    @NotNull
    default String proof() {
        StringBuilder sb = new StringBuilder();
        return proof(sb).toString();
    }

    @NotNull
    default StringBuilder proof(@NotNull StringBuilder temporary) {
        temporary.setLength(0);
        proof(this, 0, temporary);
        return temporary;
    }

    @Nullable
    default Truth getDesire() {
        return truth();
    }


    @Nullable Object log(int index);

    /**
     * append a log entry; returns this task
     */
    @NotNull
    Task log(Object entry);

    /**
     * append log entries; returns this task
     */
    @NotNull
    Task log(List entries);

    /**
     * get the recorded log entries
     */
    @Nullable
    List log();


    /**
     * Check if a Task is a direct input,
     * or if its origin has been forgotten or never known
     */
    default boolean isInput() {
        return evidence().length == 1;
        //return evidence().length <= 1;
        //return (getParentTask() == null);
        //return (evidence().length <= 1) && ;
    }


//    /**
//     * a task is considered amnesiac (origin not rememebered) if its parent task has been forgotten (garbage collected via a soft/weakref)
//     */
//    default boolean isAmnesiac() {
//        return !isInput() && getParentTask() == null;
//    }


    /**
     * if unnormalized, returns a normalized version of the task,
     * null if not normalizable
     */
    void normalize(@NotNull NAR memory) throws NAR.InvalidTaskException, TermIndex.InvalidConceptException;


//    default void ensureValidParentTaskRef() {
//        if ((getParentTaskRef() != null && getParentTask() == null))
//            throw new RuntimeException("parentTask must be null itself, or reference a non-null Task");
//    }


    void setTruth(@NotNull Truth t);


//
//    /** normalize a collection of tasks to each other
//     * so that the aggregate budget sums to a provided
//     * normalization amount.
//     * @param derived
//     * @param premisePriority the total value that the derivation group should reach, effectively a final scalar factor determined by premise parent and possibly existing belief tasks
//     * @return the input collection, unmodified (elements may be adjusted individually)
//     */
//    static void normalizeCombined(@NotNull Iterable<Task> derived, float premisePriority) {
//
//
//        float totalDerivedPriority = prioritySum(derived);
//        float factor = Math.min(
//                    premisePriority/totalDerivedPriority,
//                    1.0f //limit to only diminish
//                );
//
//        if (!Float.isFinite(factor))
//            throw new RuntimeException("NaN");
//
//        derived.forEach(t -> t.budget().priMult(factor));
//    }
//
//    static void normalize(@NotNull Iterable<Task> derived, float premisePriority) {
//        derived.forEach(t -> t.budget().priMult(premisePriority));
//    }
//    static void inputNormalized(@NotNull Iterable<Task> derived, float premisePriority, @NotNull Consumer<Task> target) {
//        derived.forEach(t -> {
//            t.budget().priMult(premisePriority);
//            target.accept(t);
//        });
//    }

    @NotNull
    static Task command(@NotNull Compound op) {
        //TODO use lightweight CommandTask impl without all the logic metadata
        return new MutableTask(op, Symbols.COMMAND, null);
    }

    default boolean isEternal() {
        return occurrence() == ETERNAL;
    }


    @NotNull
    default StringBuilder appendOccurrenceTime(@NotNull StringBuilder sb) {
        long oc = occurrence();
        long ct = creation();

        /*if (oc == Stamp.TIMELESS)
            throw new RuntimeException("invalid occurrence time");*/
        if (ct == ETERNAL)
            throw new RuntimeException("invalid creation time");

        //however, timeless creation time means it has not been perceived yet

        if (oc == ETERNAL) {
            if (ct == TIMELESS) {
                sb.append(":-:");
            } else {
                sb.append(':').append(ct).append(':');
            }

        } else if (oc == TIMELESS) {
            sb.append("N/A");

        } else {
            int estTimeLength = 8; /* # digits */
            sb.ensureCapacity(estTimeLength);
            sb.append(ct);

            long OCrelativeToCT = (oc - ct);
            if (OCrelativeToCT >= 0)
                sb.append('+'); //+ sign if positive or zero, negative sign will be added automatically in converting the int to string:
            sb.append(OCrelativeToCT);

        }

        return sb;
    }

    default String getTense(long currentTime, int duration) {

        long ot = occurrence();

        if (Tense.isEternal(ot)) {
            return "";
        }

        switch (Tense.order(currentTime, ot, duration)) {
            case 1:
                return Symbols.TENSE_FUTURE;
            case -1:
                return Symbols.TENSE_PAST;
            default:
                return Symbols.TENSE_PRESENT;
        }
    }

    @NotNull
    default CharSequence stampAsStringBuilder() {

        long[] ev = evidence();
        int len = ev.length;
        int estimatedInitialSize = 8 + (len * 3);

        StringBuilder buffer = new StringBuilder(estimatedInitialSize);
        buffer.append(Symbols.STAMP_OPENER);

        if (creation() == TIMELESS) {
            buffer.append('?');
        } else if (!Tense.isEternal(occurrence())) {
            appendOccurrenceTime(buffer);
        } else {
            buffer.append(creation());
        }
        buffer.append(Symbols.STAMP_STARTER).append(' ');

        int base = LongString.maxBase();
        for (int i = 0; i < len; i++) {

            if (ev[i] == Long.MAX_VALUE && i == len - 1) {
                buffer.append(';'); //trailing cyclic value
            } else {
                buffer.append(
                        LongString.toString(ev[i], base)
                );
            }
            if (i < (len - 1)) {
                buffer.append(Symbols.STAMP_SEPARATOR);
            }
        }

        buffer.append(Symbols.STAMP_CLOSER); //.append(' ');

        //this is for estimating an initial size of the stringbuffer
        //System.out.println(baseLength + " " + derivationChain.size() + " " + buffer.baseLength());

        return buffer;


    }


    default long occurrence() {
        return ETERNAL;
    }


//    default Truth projection(long targetTime, long now) {
//        return projection(targetTime, now, true);
//    }

    //projects the truth to a certain time, covering all 4 cases as discussed in
    //https://groups.google.com/forum/#!searchin/open-nars/task$20eteneral/open-nars/8KnAbKzjp4E/rBc-6V5pem8J
    @Nullable
    default ProjectedTruth projectTruth(long targetTime, long now, boolean eternalizeIfWeaklyTemporal) {


        Truth currentTruth = truth();
        long occ = occurrence();

        if (targetTime == ETERNAL) {

            return isEternal() ? new ProjectedTruth(currentTruth, ETERNAL) : eternalize(currentTruth);

        } else {

            return Revision.project(currentTruth, targetTime, now, occ, eternalizeIfWeaklyTemporal);
        }

    }


//    final class ExpectationComparator implements Comparator<Task>, Serializable {
//        static final Comparator the = new ExpectationComparator();
//        @Override public int compare(@NotNull Task b, @NotNull Task a) {
//            return Float.compare(a.expectation(), b.expectation());
//        }
//    }
//
//    final class ConfidenceComparator implements Comparator<Task>, Serializable {
//        static final Comparator the = new ExpectationComparator();
//        @Override public int compare(@NotNull Task b, @NotNull Task a) {
//            return Float.compare(a.conf(), b.conf());
//        }
//    }


    @Nullable
    default Term term(int i) {
        return term().term(i);
    }

    @Nullable
    default Task getParentTask() {
        return null;
    }

    @Nullable
    default Task getParentBelief() {
        return null;
    }

    default boolean cyclic() {
        return Stamp.isCyclic(evidence());
    }

    @Override
    default boolean delete(@Nullable Object removalReason) {
        if (removalReason != null)
            log(removalReason);
        return delete();
    }

    default boolean temporal() {
        return occurrence() != ETERNAL;
    }


    default int dt() {
        return term().dt();
    }

}
