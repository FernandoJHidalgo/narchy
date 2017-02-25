package nars.test.condition;


import jcog.Texts;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.task.Tasked;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.truth.Truth;
import nars.truth.Truthed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class EternalTaskCondition implements NARCondition, Predicate<Task>, Consumer<Tasked> {

    //private static final Logger logger = LoggerFactory.getLogger(EternalTaskCondition.class);

    @NotNull
    protected final NAR nar;
    private final byte punc;

    @NotNull
    private final Term term;

    boolean succeeded;
    //long successTime = Tense.TIMELESS;

    final static Logger logger = LoggerFactory.getLogger(EternalTaskCondition.class);

    //@Expose

    //@JsonSerialize(using= JSONOutput.TermSerializer.class)
    //public  Term term;

    //@Expose
    //public  byte punc;

    public final float freqMin;
    public final float freqMax;
    public final float confMin;
    public final float confMax;
    public long creationStart, creationEnd; //-1 for not compared

    /*float tenseCost = 0.35f;
    float temporalityCost = 0.75f;*/


    //private final Observed.DefaultObserved.DefaultObservableRegistration taskRemoved;

    //@Expose
    //protected long creationTime;


    //@Expose
    //public Tense tense = Tense.Eternal;


    public final List<Task> matched = $.newArrayList(1);


    final static int maxSimilars = 2;

    @NotNull protected final TreeMap<Float, Task> similar = new TreeMap();

//    @Override
//    public final Truth getTruth() {
//        return DefaultTruth.NULL;
//    }

    public EternalTaskCondition(@NotNull NAR n, long creationStart, long creationEnd, @NotNull String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax) throws RuntimeException, nars.Narsese.NarseseException {
        //super(n.task(sentenceTerm + punc).normalize(n.memory));


            if (freqMax < freqMin) throw new RuntimeException("freqMax < freqMin");
            if (confMax < confMin) throw new RuntimeException("confMax < confMin");

            if (creationEnd - creationStart < 1)
                throw new RuntimeException("cycleEnd must be after cycleStart by at least 1 cycle");

            this.nar = n;

            this.creationStart = creationStart;
            this.creationEnd = creationEnd;
            this.freqMax = Math.min(1.0f, freqMax);
            this.freqMin = Math.max(0.0f, freqMin);
            this.confMax = Math.min(1.0f, confMax);
            this.confMin = Math.max(0.0f, confMin);
            this.punc = punc;
            this.term =
                    $.terms.parse( sentenceTerm ).term();
    }

    @NotNull
    public static String rangeStringN2(float min, float max) {
        return "(" + Texts.n2(min) + ',' + Texts.n2(max) + ')';
    }

    /**
     * a heuristic for measuring the difference between terms
     * in range of 0..100%, 0 meaning equal
     */
    public static float termDistance(@NotNull Term a, @NotNull Term b, float ifLessThan) {
        if (a.equals(b)) return 0;
        //TODO handle TermMetadata terms

        float dist = 0;
        if (a.op() != b.op()) {
            //50% for equal term
            dist += 0.2f;
            if (dist >= ifLessThan) return dist;
        } else {
            if (a instanceof Compound) { //b also a compound
                if (((Compound) a).dt() != ((Compound) b).dt()) {
                    dist += 0.2f;
                    if (dist >= ifLessThan) return dist;
                }
            }
        }


        if (a.size() != b.size()) {
            dist += 0.2f;
            if (dist >= ifLessThan) return dist;
        }


        if (a.structure() != b.structure()) {
            dist += 0.2f;
            if (dist >= ifLessThan) return dist;
        }

        //HACK use toString for now
        dist += Terms.levenshteinDistancePercent(
                a.toString(),
                b.toString()) * 0.2f;

        return dist;
    }

    @NotNull
    @Override
    public String toString() {
        return term.toString() + ((char)punc) + " %" +
                rangeStringN2(freqMin, freqMax) + ';' + rangeStringN2(confMin, confMax) + '%' + ' ' +
                " creation: (" + creationStart + ',' + creationEnd + ')';
    }

    //    public double getAcceptableDistanceThreshold() {
//        return 0.01;
//    }

//    //how many multiples of the range it is away from the acceptable time interval
//    public static double rangeError(double value, double min, double max, boolean squash) {
//        double dt;
//        if (value < min)
//            dt = min - value;
//        else if (value > max)
//            dt = value - max;
//        else
//            return 0;
//
//        double result = dt/(max-min);
//
//        if (squash)
//            return Math.tanh(result);
//        else
//            return result;
//    }

//    //time distance function
//    public double getTimeDistance(long now) {
//        return rangeError(now, creationStart, creationEnd, true);
//    }

//    //truth distance function
//    public double getTruthDistance(Truth t) {
//        //manhattan distance:
//        return rangeError(t.getFrequency(), freqMin, freqMax, true) +
//                rangeError(t.getConfidence(), confMin, confMax, true);
//
//        //we could also calculate geometric/cartesian vector distance
//    }

////    public void setRelativeOccurrenceTime(Tense t, int duration) {
////        setRelativeOccurrenceTime(Stamp.getOccurrenceTime(t, duration), duration);
////    }
//    /** task's tense is compared by its occurence time delta to the current time when processing */
//    public void setRelativeOccurrenceTime(long ocRelative, int duration) {
//        //may be more accurate if duration/2
//
//        Tense tense;
//        final float ocRel = ocRelative; //cast to float for this compare
//        if (ocRel > duration/2f) tense = Tense.Future;
//        if (ocRel < -duration/2f) tense = Tense.Past;
//        else tense = Tense.Present;
//
//        setRelativeOccurrenceTime(tense, nar.memory.duration());
//
//    }


//
//    public void setRelativeOccurrenceTime(long creationTime, int ocRelative, int duration) {
//        setCreationTime(creationTime);
//        setRelativeOccurrenceTime(ocRelative, duration);
//    }


    public boolean matches(@Nullable Task task) {
        if (task == null)
            return false;

        if (task.punc() != punc)
            return false;

        if (!truthMatches(task))
            return false;

        if (!timeMatches(task))
            return false;

        return task.term().equals(term);
    }

    private boolean truthMatches(@NotNull Truthed task) {
        if ((punc == Op.BELIEF) || (punc == Op.GOAL)) {
            Truth tt = task.truth();
            if (tt == null)
                return false;

            float fr = tt.freq();
            if ((fr > freqMax) || (fr < freqMin))
                return false;

            float co = tt.conf();
            if ((co > confMax) || (co < confMin))
                return false;

        }
        return true;
    }

    public boolean timeMatches(@NotNull Task t) {
        return creationTimeMatches() && occurrenceTimeMatches(t);
    }

//    private boolean relativeTimeMatches(Task t) {
//        if (term instanceof Compound) {
//            return ((Compound)term).t() == t.term().t();
//        }
//        return true;
//    }

    final boolean creationTimeMatches() {
        long now = nar.time();
        return !(((creationStart != -1) && (now < creationStart)) ||
                ((creationEnd != -1) && (now > creationEnd)));
    }

    protected boolean occurrenceTimeMatches(@NotNull Task t) {
        return (t.isEternal());
    }

    @Override
    public final boolean test(@NotNull Task task) {

        if (matches(task)) {
            matched.add(task);
            succeeded = true;
            return true;
        }

        recordSimilar(task);
        return false;
    }

    public void recordSimilar(@NotNull Task task) {
        final TreeMap<Float, Task> similar;
        synchronized (similar = this.similar) {

            //TODO add the levenshtein distance of other task components
            float worstDiff = similar.size() >= maxSimilars ? similar.lastKey() : Float.POSITIVE_INFINITY;

            float difference = 0;
            Compound tterm = task.term();
            difference +=
                    tterm.equals(term) ? 0 : (term.volume());
            if (difference >= worstDiff)
                return;

            float f = task.freq();
            float freqDiff = Math.min(
                    Math.abs(f - freqMin),
                    Math.abs(f - freqMax));
            difference += 2 * freqDiff;
            if (difference >= worstDiff)
                return;

            float c = task.conf();
            float confDiff = Math.min(
                    Math.abs(c - confMin),
                    Math.abs(c - confMax));
            difference += 1 * confDiff;
            if (difference >= worstDiff)
                return;

            float termDifference =
                    termDistance(tterm, term, worstDiff);
            difference += 3 * termDifference;

            if (difference >= worstDiff)
                return;


            //TODO more efficient way than this

            this.similar.put(difference, task);


            if (similar.size() > maxSimilars) {
                similar.remove(similar.lastEntry().getKey());
            }
        }
    }


//    public String getFalseReason() {
//        String x = "Unmatched; ";
//
//        if (similar!=null) {
//            x += "Similar:\n";
//            for (Map.Entry<Double,Task> et : similar.entrySet()) {
//                Task tt = et.getValue();
//                x += Texts.n4(et.getKey().floatValue()) + ' ' + tt.toString() + ' ' + tt.getLog() + '\n';
//            }
//        }
//        else {
//            x += "No similar: " + term;
//        }
//        return x;
//    }


//    public List<Task> getTrueReasons() {
//        return valid;
//        //if (!isTrue()) throw new RuntimeException(this + " is not true so has no true reasons");
//        /*return Lists.newArrayList("match at: " +
//
//                Iterables.transform(trueAt, new Function<Task, String>() {
//                    @Override
//                    public String apply(Task task) {
//                        return task.toString() + " @ " + task.sentence.getCreationTime();
//                    }
//                }));
//                */
//        //return exact;
//    }

//    @Override
//    public String toString() {
//        return succeeded  +": "  + JSONOutput.stringFromFields(this);
//    }


    @Override
    public final void accept(@NotNull Tasked tasked) {
        accept(tasked.task());
    }

    public final void accept(@NotNull Task task) {

        if (succeeded) return; //no need to test any further


        if (test(task)) {
            succeeded = true;
            //successTime = nar.time();
        } else {
            //logger.info("non-matched: {}", task);
            //logger.info("\t{}", task.getLogLast());
        }

    }


    @Override
    public long getFinalCycle() {
        return creationEnd;
    }


    @Override
    public final boolean isTrue() {
        return succeeded;
    }


    @Override
    public void log(@NotNull Logger logger) {
        String msg = succeeded ? " OK" : "ERR" + '\t' + toString();
        if (succeeded) {
            logger.info(msg);

            if (matched != null && logger.isTraceEnabled()) {
                matched.forEach(s -> {
                    logger.trace("\t{}", s);
                    //logger.debug("\t\t{}", s.getLog());
                    //logger.debug(s.getExplanation().replace("\n", "\n\t\t"));
                });
            }
        } else {
            assert (matched.isEmpty());

            logger.error(msg);

            synchronized (similar) {
                if (!similar.isEmpty()) {
                    similar.values().forEach(s -> {
                        String pattern = "SIM\n{}";
                        logger.info(pattern, s.proof());
                        //logger.debug(s.getExplanation().replace("\n", "\n\t\t"));
                    });
                }
            }
        }


    }



    /* calculates the "cost" of an execution according to certain evaluated condtions
     //     *  this is the soonest time at which all output conditions were successful.
     //     *  if any conditions were not successful, the cost is infinity
     //     * */
//    public static double cost(@NotNull Iterable<EternalTaskCondition> conditions) {
//        long lastSuccess = Tense.TIMELESS;
//        for (EternalTaskCondition e : conditions) {
//            long est = e.successTime;
//            if (est != Tense.TIMELESS) {
//                if (lastSuccess < est) {
//                    lastSuccess = est;
//                }
//            }
//        }
//        if (lastSuccess != Tense.TIMELESS) {
//            //score = 1.0 + 1.0 / (1+lastSuccess);
//            return lastSuccess;
//        }
//
//        return Double.POSITIVE_INFINITY;
//    }
//
//    /** returns a function of the cost characterizing the optimality of the conditions
//     *  monotonically increasing from -1..+1 (-1 if there were errors,
//     *  0..1.0 if all successful.  limit 0 = takes forever, limit 1.0 = instantaneous
//     */
//    public static double score(@NotNull List<EternalTaskCondition> requirements) {
//        double cost = cost(requirements);
//        return Double.isFinite(cost) ? 1.0 / (1.0 + cost) : -1;
//
//    }
}
