package nars.concept.table;

import nars.NAR;
import nars.budget.Budgeted;
import nars.nal.Tense;
import nars.task.Task;
import nars.truth.Truth;
import nars.truth.TruthFunctions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static java.util.stream.StreamSupport.stream;
import static nars.nal.UtilityFunctions.and;

/**
 * A model storing, ranking, and projecting beliefs or goals (tasks with TruthValue).
 * It should iterate in top-down order (highest ranking first)
 */
public interface BeliefTable extends TaskTable {


    @Nullable
    BeliefTable EMPTY = new BeliefTable() {

        @Override
        public Iterator<Task> iterator() {
            return Collections.emptyIterator();
        }


        @Override
        public int capacity() {
            return 0;
        }

        @Override
        public void capacity(int eternals, int temporals, List<Task> displ) {

        }

        @Override
        public int size() {
            return 0;
        }


        @Override
        public boolean isEmpty() {
            return true;
        }



        @Override
        public Task add(@NotNull Task input, @NotNull QuestionTable questions, List<Task> displaced, @NotNull NAR nar) {
            return input;
        }



        @Override
        public Task eternalTop() {
            return null;
        }

        @Nullable
        @Override
        public Task topTemporal(long when, long now, Task against) {
            return null;
        }


        @Override
        public float confMax(float minFreq, float maxFreq) {
            return 0;
        }

        @Nullable
        @Override
        public Truth truth(long when, long now) {
            return null;
        }


    };

    void capacity(int eternals, int temporals, List<Task> displ);


//    /**
//     * projects to a new task at a given time
//     * was: getTask(q, now, getBeliefs()).  Does not affect the table itself */
//    public Task project(Task t, long now);

    /*default public Task project(final Task t) {
        return project(t, Stamp.TIMELESS);
    }*/

    static float rankEternalByConfAndOriginality(@NotNull Task b) {
        return rankEternalByConfAndOriginality(b.conf(), b.originality());
    }
    static float rankEternalByConfAndOriginality(float conf, float originality) {
        return and(conf, originality);
    }

    static float rankEternalByConfAndOriginality(float conf, int hypotheticalEvidenceLength /* > 0 */) {
        return rankEternalByConfAndOriginality(conf, TruthFunctions.originality(hypotheticalEvidenceLength));
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

    /** returns a value <= 1.0 */
    static float relevance(long delta /* positive only */, float ageFactor /* <1, divides usually */) {
        return 1f / (1f + delta*ageFactor);
    }

//    @NotNull
//    static Task stronger(@NotNull Task a, @NotNull Task b) {
//        return a.conf() > b.conf() ? a : b;
//    }

    static float rankTemporalByConfidenceAndOriginality(@NotNull Task t, long when, long now, float ageFactor, float bestSoFar) {
        return rankTemporalByConfidence(t, when, now, ageFactor, bestSoFar) * t.originality();
    }

    /**
     *
     * @param t
     * @param when target time that is being evaluated (may be 'now' or some other time projected to)
     * @param ageFactor effectively a ratio for trading off confidence against time
     * @return
     */
    static float rankTemporalByConfidence(@NotNull Task t, long when, long now, float ageFactor, float bestSoFar) {
        float c = t.truth().conf();
        //float c = t.confWeight(); //<- doesnt seem to work, produces values too high

        if (c < bestSoFar)
            return -1; //give up early since anything multiplied by relevance (<=1f) wont exceed the current best
        else {
            long dt = Math.abs(t.occurrence() - now) + Math.abs(when - now);

            float relevance = relevance(dt, ageFactor);
            relevance = relevance * relevance; //pow2 sharpening curve, defining a temporal focus shape that is stronger closer to now

            float rank = c * relevance;
            //System.out.println(now + ": " + t + " for " + when + " dt="+ dt + " rele=" + relevance + " rank=" + rank);
            return rank;
        }

    }

//    static float rankTemporalByOriginality(@NotNull Task b, long when) {
//        return BeliefTable.rankEternalByOriginality(b) *
//                BeliefTable.relevance(b, when, 1);
//    }

    /** attempt to insert a task; returns what was input or null if nothing changed (rejected) */
    Task add(@NotNull Task input, @NotNull QuestionTable questions, List<Task> displaced, @NotNull NAR nar);


//    @Nullable
//    default Task top(@NotNull NAR nar) {
//        return top(nar.time());
//    }

    @Nullable
    default Task top(long now) {
        return top(now, now);
    }

    @Nullable
    default Task top(long when, long now) {
        return top(when, now, null);
    }

    /** get the most relevant belief/goal with respect to a specific time. */
    @Nullable
    default Task top(long when, long now, @Nullable Task against) {

        final Task ete = eternalTop();
        if (when == Tense.ETERNAL) {
            if (ete != null) {
                return ete;
            } /*else {
                //eternalize the topTemporal?
            } */
        }

        Task tmp = topTemporal(when, now, against);

        if (tmp == null) {
            return ete;
        } else {
            if (ete == null) {
                return tmp;
            } else {
                return (ete.conf() > tmp.conf()) ?
                        ete : tmp;
            }
        }

    }

    /** get the top-ranking eternal belief/goal; null if no eternal beliefs known */
    @Nullable Task eternalTop();

    /** finds the most relevant temporal belief for the given time; ; null if no temporal beliefs known */
    @Nullable Task topTemporal(long when, long now, @Nullable Task against);




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

    @Nullable default Truth topEternalTruth(@Nullable Truth ifNone) {
        Task t = eternalTop();
        return t == null ? ifNone : t.truth();
    }




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

    default void print(@NotNull PrintStream out) {
        this.forEach(t -> out.println(t + " " + Arrays.toString(t.evidence()) + ' ' + t.log()));
    }
    default void print() {
        print(System.out);
    }

//    /** simple metric that guages the level of inconsistency (ex: variance) aggregated by contained belief states.
//     *  returns 0 if no tasks exist */
//    default float coherence() {
//        //TODO
//        return 0;
//    }

    default float priSum() {
        return (float) stream(spliterator(), false)
                .mapToDouble(Budgeted::pri).sum();
    }

//    /**
//     * get a random belief, weighted by their sentences confidences
//     */
//    @Nullable
//    default Task randomByConf(boolean eternal, @NotNull Random rng) {
//
//        if (isEmpty()) return null;
//
//        float totalConfidence = confSum();
//        float r = rng.nextFloat() * totalConfidence;
//
//
//        for (Task x : this) {
//            r -= x.truth().conf();
//            if (r < 0)
//                return x;
//        }
//
//        return null;
//    }


    default float confMax() {
        return confMax(0f, 1f);
    }

    float confMax(float minFreq, float maxFreq);


    /** estimates the current truth value from the top task, projected to the specified 'when' time;
     * returns null if no evidence is available */
    @Nullable Truth truth(long when, long now);



    @Nullable default Truth truth(long now) {
        return truth(now, now);
    }

    /** finds the strongest matching belief for the given term (and its possible 'dt' value) and the given occurrence time.
     *
     *  TODO consider similarity of any of term's recursive 'dt' temporality in ranking
     * */
    @Nullable
    default Task match(@NotNull Task target, long now) {

        int size = size();
        if (size == 0)
            return null;


        long occ = target.occurrence();
        return top(occ, now, target);

//        do {
//
//            Task belief = top( occ );
//
//            if (belief == null) {
//                return null;
//            } else if (belief.isDeleted()) {
//                remove(belief);
//            } else {
//                return belief;
//            }
//
//        } while (size-- > 0);
//        return null;
    }

    default float expectation(long when) {
        Truth t = truth(when);
        return t != null ? t.expectation() : 0.5f;
    }

    default float motivation(long when) {
        Truth t = truth(when);
        return t != null ? t.motivation() : 0;
    }

    /** returns 0.5 if no truth can be determined */
    default float freq(long when) {
        Truth t = truth(when);
        return t != null ? t.freq() : 0.5f;
    }


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
//                float dur = t.getDuration();
//                float durationsToNow = Math.abs(t.getOccurrenceTime() - now) / dur;
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