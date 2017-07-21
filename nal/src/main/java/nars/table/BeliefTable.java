package nars.table;

import jcog.pri.Prioritized;
import nars.NAR;
import nars.Task;
import nars.concept.TaskConcept;
import nars.control.Cause;
import nars.task.NALTask;
import nars.term.Compound;
import nars.truth.Stamp;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.stream.StreamSupport.stream;
import static nars.term.Terms.normalizedOrNull;
import static nars.time.Tense.ETERNAL;

/**
 * A model storing, ranking, and projecting beliefs or goals (tasks with TruthValue).
 * It should iterate in top-down order (highest ranking first)
 */
public interface BeliefTable extends TaskTable, Iterable<Task> {


    default Iterator<Task> taskIterator() {
        return iterator();
    }

    @NotNull
    BeliefTable EMPTY = new BeliefTable() {

        @Override
        public Stream<Task> stream() {
            return Stream.empty();
        }

        @Override
        public Iterator<Task> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public void forEachTask(Consumer<? super Task> x) {

        }

        @Override
        public boolean removeTask(Task x) {
            return false;
        }

        @Override
        public int capacity() {
            return 0;
        }

        @Override
        public void setCapacity(int eternals, int temporals) {
            throw new RuntimeException("tried to set capacity on null table impl");
        }

        @Override
        public Task match(long when, Task question, Compound template, boolean noOverlap, NAR nar) {
            return null;
        }

        public @Nullable Task match(long when, long now, int dur, NAR nar) {
            return null;
        }

        @Override
        public float priSum() {
            return 0;
        }


        @Override
        public Task answer(long when, long now, int dur, @NotNull Task question, Compound template, TaskConcept beliefConcept, NAR nar) {
            return null;
        }

        @Override
        public int size() {
            return 0;
        }


        @Override
        public void add(@NotNull Task input, TaskConcept concept, @NotNull NAR nar) {

        }


        public Task matchEternal() {
            return null;
        }

        @Override
        public void print(@NotNull PrintStream out) {

        }

        @Override
        public Spliterator<Task> spliterator() {
            return Spliterators.emptySpliterator();
        }



        @Override
        public Truth truth(long when, NAR nar) {
            return null;
        }

        @Override
        public void clear() {

        }

    };

    void setCapacity(int eternals, int temporals);


//    /**
//     * projects to a new task at a given time
//     * was: getTask(q, now, getBeliefs()).  Does not affect the table itself */
//    public Task project(Task t, long now);

    /*default public Task project(final Task t) {
        return project(t, Stamp.TIMELESS);
    }*/

//    static float rankEternalByConfAndOriginality(@NotNull Task b) {
//        return rankEternalByConfAndOriginality(b.conf(), b.originality());
//    }
//
//    static float rankEternalByConfAndOriginality(float conf, float originality) {
//        return and(conf, originality);
//    }
//
//    static float rankEternalByConfAndOriginality(float conf, int hypotheticalEvidenceLength /* > 0 */) {
//        return rankEternalByConfAndOriginality(conf, TruthFunctions.originality(hypotheticalEvidenceLength));
//    }


    /**
     * attempt to insert a task; returns what was input or null if nothing changed (rejected)
     */
    void add(@NotNull Task input, TaskConcept concept, @NotNull NAR nar);

    Task match(long when, Task question, @Nullable Compound template, boolean noOverlap, NAR nar);


    default void print(@NotNull PrintStream out) {
        this.forEachTask(t -> out.println(t + " " + Arrays.toString(t.stamp()))); //TODO print Stamp using same methods Task uses
    }

    default void print() {
        print(System.out);
    }

    /**
     * simple metric that guages the level of inconsistency (ex: variance) aggregated by contained belief states.
     * returns 0 if no tasks exist
     */
    default float coherence() {
        throw new UnsupportedOperationException("TODO");
    }

    default float priSum() {
        return (float) stream().mapToDouble(Prioritized::pri).sum();
    }

    /**
     * estimates the current truth value from the top task, projected to the specified 'when' time;
     * returns null if no evidence is available
     */
    Truth truth(long when, NAR nar);


    //    default float expectation(long when, int dur) {
//        Truth t = truth(when, dur);
//        return t != null ? t.expectation() : 0.5f;
//    }
//    default float motivation(long when, int dur) {
//        Truth t = truth(when, dur);
//        return t != null ? t.motivation() : 0;
//    }
//
//    default float freq(long when, int dur) {
//        Truth t = truth(when, dur);
//        return t != null ? t.freq() : Float.NaN;
//    }

    /**
     * empties the table, an unindexes them in the NAR
     */
    void clear();



    /** projects a match */
    default Task answer(long when, long now, int dur, @NotNull Task question, @Deprecated Compound template, TaskConcept beliefConcept, NAR nar) {


        Task answer = match(when, question, question.term(), false, nar);
        if (answer == null || answer.isDeleted())
            return null;

        boolean novel = true; //(answer instanceof AnswerTask); //includes: answers, revision, or dynamic
                    //&& !(answer instanceof DynamicBeliefTask);


        //project if different occurrence

        boolean relevantTime = answer.during(when);

        if (/*!answer.isEternal() && */!relevantTime) {

            if (when == ETERNAL)
                when = now;


            Truth aProj = answer.truth(when, dur, nar.confMin.floatValue());
            if (aProj != null) {

                Compound at = normalizedOrNull(answer.term(), nar.terms, nar.terms.retemporalizationZero);
                if (at==null)
                    return null;

                NALTask a = new NALTask(
                        at,
                        answer.punc(),
                        aProj, now, when, when,
                        Stamp.zip(answer.stamp(), question.stamp(), 0.5f));
                a.setPri(answer.priElseZero());
                a.cause = Cause.zip(question, answer);

                //            if (Param.DEBUG)
                //                a.log("Answer Projected");
                novel = true; //because it was projected
                answer = a;
            }
        }

        if (novel && relevantTime && question.isQuestOrQuestion() &&
                nar.conceptTerm(question.term()).equals(beliefConcept)
            ) {
            nar.input(answer);
        }

        return answer;
    }



//    /** 2-element array containing running min/max range accumulator */
//    void range(long[] t);


    //void remove(Task belief, @NotNull NAR nar);



    /* simple metric that guages the level of inconsistency between two differnt tables, used in measuring graph intercoherency */
    /*default float coherenceAgainst(BeliefTable other) {
        //TODO
        return Float.NaN;
    }*/


//    @FunctionalInterface
//    interface Ranker extends Function<Task,Float> {
//        /** returns a number producing a score or relevancy number for a given Task
//         * @param bestToBeat current best score, which the ranking can use to decide to terminate early
//         * @return a score value, or Float.MIN_VALUE to exclude that result
//         * */
//        float rank(Task t, float bestToBeat);
//
//
//        default float rank(Task t) {
//            return rank(t, Float.MIN_VALUE);
//        }
//
//        @Override default Float apply(Task t) {
//            return rank(t);
//        }
//
//    }

//    /** allowed to return null. must evaluate all items in case the final one is the
//     *  only item that does not have disqualifying rank (MIN_VALUE)
//     * */
//    @Nullable
//    default Task top(@NotNull Ranker r) {
//
//        float s = Float.MIN_VALUE;
//        Task b = null;
//
//        for (Task t : this) {
//            float x = r.rank(t, s);
//            if (x > s) {
//                s = x;
//                b = t;
//            }
//        }
//
//        return b;
//    }


//    final class SolutionQualityMatchingOrderRanker implements Ranker {
//
//        @NotNull
//        private final Task query;
//        private final long now;
//        private final boolean hasQueryVar; //cache hasQueryVar
//
//        public SolutionQualityMatchingOrderRanker(@NotNull Task query, long now) {
//            this.query = query;
//            this.now = now;
//            this.hasQueryVar = query.hasQueryVar();
//        }
//
//        @Override
//        public float rank(@NotNull Task t, float bestToBeat) {
//            Task q = query;
//
//            if (t.equals(q)) return Float.NaN; //dont compare to self
//
//            //TODO use bestToBeat to avoid extra work
//            //return or(t.getOriginality(),Tense.solutionQualityMatchingOrder(q, t, now, hasQueryVar));
//            return Tense.solutionQualityMatchingOrder(q, t, now, hasQueryVar);
//        }
//    }


//    /**
//     * Select a belief value or desire value for a given query
//     *
//     * @param query The query to be processed
//     * @param list  The list of beliefs or goals to be used
//     * @return The best candidate selected
//     */
//    public static Task getTask(final Sentence query, long now, final List<Task>... lists) {
//        float currentBest = 0;
//        float beliefQuality;
//        Task candidate = null;
//
//        for (List<Task> list : lists) {
//            if (list.isEmpty()) continue;
//
//            int lsv = list.size();
//            for (int i = 0; i < lsv; i++) {
//                Task judg = list.get(i);
//                beliefQuality = solutionQuality(query, judg.sentence, now);
//                if (beliefQuality > currentBest) {
//                    currentBest = beliefQuality;
//                    candidate = judg;
//                }
//            }
//        }
//
//        return candidate;
//    }

//
//    /** TODO experimental and untested */
//    class BeliefConfidenceAndCurrentTime implements Ranker {
//
//        private final Concept concept;
//
//        /** controls dropoff rate, measured in durations */
//        float relevanceWindow = 0.9f;
//        float temporalityFactor = 1f;
//
//
//
//        public BeliefConfidenceAndCurrentTime(Concept c) {
//            this.concept = c;
//        }
//
//        /** if returns c itself, this is a 1:1 linear mapping of confidence to starting
//         * score before penalties applied. this could also be a curve to increase
//         * or decrease the apparent relevance of certain confidence amounts.
//         * @return value >=0, <=1
//         */
//        public float confidenceScore(final float c) {
//            return c;
//        }
//
//        @Override
//        public float rank(Task t, float bestToBeat) {
//            float r = confidenceScore(t.getTruth().getConfidence());
//
//            if (!Temporal.isEternal(t.getOccurrenceTime())) {
//
//                final long now = concept.getMemory().time();
//                int dur = t.getDuration();
//                int durationsToNow = Math.abs(t.getOccurrenceTime() - now) / dur;
//
//
//                //float agePenalty = (1f - 1f / (1f + (durationsToNow / relevanceWindow))) * temporalityFactor;
//                float agePenalty = (durationsToNow / relevanceWindow) * temporalityFactor;
//                r -= agePenalty; // * temporalityFactor;
//            }
//
//            float unoriginalityPenalty = 1f - t.getOriginality();
//            r -= unoriginalityPenalty * 1;
//
//            return r;
//        }
//
//    }


//    default public Task top(boolean eternal, boolean nonEternal) {
//
//    }


//    /** temporary until goal is separated into goalEternal, goalTemporal */
//    @Deprecated default public Task getStrongestTask(final List<Task> table, final boolean eternal, final boolean temporal) {
//        for (Task t : table) {
//            boolean e = t.isEternal();
//            if (e && eternal) return t;
//            if (!e && temporal) return t;
//        }
//        return null;
//    }
//
//    public static Sentence getStrongestSentence(List<Task> table) {
//        Task t = getStrongestTask(table);
//        if (t!=null) return t.sentence;
//        return null;
//    }
//
//    public static Task getStrongestTask(List<Task> table) {
//        if (table == null) return null;
//        if (table.isEmpty()) return null;
//        return table.get(0);
//    }

//    /**
//     * Determine the rank of a judgment by its quality and originality (stamp
//     * baseLength), called from Concept
//     *
//     * @param s The judgment to be ranked
//     * @return The rank of the judgment, according to truth value only
//     */
    /*public float rank(final Task s, final long now) {
        return rankBeliefConfidenceTime(s, now);
    }*/


//    public Sentence getSentence(final Sentence query, long now, final List<Task>... lists) {
//        Task t = getTask(query, now, lists);
//        if (t == null) return null;
//        return t.sentence;
//    }
}
//    /** returns value <= 1f */
//    static float relevance(@NotNull Task t, long time, float ageFactor) {
//        return relevance(t.occurrence(), time, ageFactor);
//    }

//    default Task top(Task query, long now) {
//
//        switch (size()) {
//            case 0: return null;
//            case 1: return top();
//            default:
//                //TODO re-use the Ranker
//                return top(new SolutionQualityMatchingOrderRanker(query, now));
//        }
//
//    }

//    /** temporal relevance; returns a value <= 1.0f; */
//    static float relevance(long from, long to, float ageFactor) {
//        //assert(from!=Tense.ETERNAL);
//        /*if (from == Tense.ETERNAL)
//            return Float.NaN;*/
//
//        return relevance(Math.abs(from - to), ageFactor);
//    }

//    @NotNull
//    static Task stronger(@NotNull Task a, @NotNull Task b) {
//        return a.conf() > b.conf() ? a : b;
//    }

//    static float rankTemporalByConfidenceAndOriginality(@NotNull Task t, long when, long now, float bestSoFar) {
//        return rankTemporalByConfidence(t, when, now, bestSoFar) * t.originality();
//    }

//    static float rankTemporalByOriginality(@NotNull Task b, long when) {
//        return BeliefTable.rankEternalByOriginality(b) *
//                BeliefTable.relevance(b, when, 1);
//    }



    /* when does projecting to now not play a role? I guess there is no case,
    //wo we use just one ranker anymore, the normal solution ranker which takes
    //occurence time, originality and confidence into account,
    //and in case of question var, the truth expectation and complexity instead of confidence
    Ranker BeliefConfidenceOrOriginality = (belief, bestToBeat) -> {
        final float confidence = belief.getTruth().getConfidence();
        final float originality = belief.getOriginality();
        return or(confidence, originality);
    };*/


//    /** computes the truth/desire as an aggregate of projections of all
//     * beliefs to current time
//     */
//    default float getMeanProjectedExpectation(long time, int dur) {
//        int size = size();
//        if (size == 0) return 0;
//
//        float[] d = {0};
//        forEach(t -> d[0] +=
//                relevance(t, time, dur) //projectionQuality(t.freq(), t.conf(), t, time, time, false)
//                * t.expectation());
//
//        float dd = d[0];
//
//        if (dd == 0) return 0;
//
//        return dd / size;
//
//    }
//    static float projectionQuality(float freq, float conf, @NotNull Task t, long targetTime, long currentTime, boolean problemHasQueryVar) {
////        float freq = getFrequency();
////        float conf = getConfidence();
//
//        long taskOcc = t.occurrence();
//
//        if (!Tense.isEternal(taskOcc) && (targetTime != taskOcc)) {
//            conf = TruthFunctions.eternalize(conf);
//            if (targetTime != Tense.ETERNAL) {
//                float factor = TruthFunctions.temporalProjection(taskOcc, targetTime, currentTime);
//                float projectedConfidence = factor * t.conf();
//                if (projectedConfidence > conf) {
//                    conf = projectedConfidence;
//                }
//            }
//        }
//
//        return problemHasQueryVar ? Truth.expectation(freq, conf) / t.term().complexity() : conf;
//
//    }

