package nars.truth.polation;

import jcog.Paper;
import jcog.Skill;
import jcog.list.FasterList;
import nars.Op;
import nars.Task;
import nars.task.Revision;
import nars.task.Tasked;
import nars.task.util.TaskRegion;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.tuple.primitive.ObjectFloatPair;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

import static java.lang.Float.NaN;

/**
 * Truth Interpolation and Extrapolation of Temporal Beliefs/Goals
 * see:
 * https://en.wikipedia.org/wiki/Category:Intertemporal_economics
 * https://en.wikipedia.org/wiki/Discounted_utility
 */
@Paper
@Skill({"Interpolation", "Extrapolation"})
abstract public class TruthPolation extends FasterList<TruthPolation.TaskComponent> {

    public final long start;
    public final long end;
    public int dur;
    protected TruthPolation(long start, long end, int dur) {
        this.start = start;
        this.end = end;

        assert (dur > 0);
        this.dur = dur;
    }

    public TruthPolation add(Task t) {
        super.add(new TaskComponent(t));
        return this;
    }

    /**
     * remove components contributing no evidence
     */
    public final TruthPolation filter() {
        removeIf(x -> update(x) == null);
        return this;
    }

    @Nullable
    protected final TaskComponent update(int i) {
        return update(get(i));
    }

    @Nullable
    protected final TaskComponent update(TaskComponent tc) {
        return tc.update(start, end, dur);
    }

    /**
     * removes the weakest components sharing overlapping evidence with stronger ones.
     * should be called after all entries are added
     */
    public final LongHashSet filterCyclic() {
        filter();

        sortThisByFloat(tc -> -tc.eviInteg); //descending by strength
        //TODO maybe factor in originality to reduce overlap so evidence can be combined better

        //remove the weaker holder of any overlapping evidence
        LongHashSet e = new LongHashSet(size() * 2);
        removeIf(tc -> {
            long[] stamp = tc.task.stamp();

            for (long ss : stamp) {
                if (!e.add(ss))
                    return true; //overlap
            }


            return false;
        });

        return e;
    }

    /**
     * computes the final truth value
     */
    abstract public Truth truth();

    public final TruthPolation add(Tasked... tasks) {
        ensureCapacity(tasks.length);
        for (Tasked t : tasks) {
            if (t != null)
                add(t);
        }
        return this;
    }

    public final TruthPolation add(Iterable<? extends Tasked> tasks) {
        tasks.forEach(this::add);
        return this;
    }

    public final TruthPolation add(Collection<? extends Tasked> tasks) {
        ensureCapacity(tasks.size());
        return add((Iterable) tasks);
    }

    public final TruthPolation add(Tasked tt) {
        add(tt.task());
        return this;
    }

    public ObjectFloatPair<Term> filterDTDiff() {
        Term first = null, second = null;
        for (int i = 0, thisSize = this.size(); i < thisSize; i++) {
            TaskComponent t = this.get(i);
            Term ttt = t.task.term();
            if (i == 0) {
                if (!ttt.hasAny(Op.Temporal))
                    return PrimitiveTuples.pair(ttt, 1f); //we can assume all the terms are equal
                first = ttt;
            } else {
                if (!first.equals(ttt)) {
                    second = ttt;
                    //HACK stop here for now
                    //TODO do Termpolation
                    removeAbove(i-1);
                    return PrimitiveTuples.pair(first, 1f);
                }

            }
        }

//        Term content;
//        float differenceFactor = 1f;
//        if (!termSame) {
//            Task second = tt1[1].task();
//
//            Term a = first.term();
//            Term b = second.term();
//            if (a.op()!=CONJ) {
//
//
//                float diff = dtDiff(a, b);
//                if (!Float.isFinite(diff))
//                    return null; //impossible
//                if (diff > 0)
//                    differenceFactor = (float) Param.evi(1f, diff, dur); //proport
//
//                float e1 = first.evi();
//                float e2 = second.evi();
//                float firstProp = e1 / (e1 + e2);
//                content = intermpolate(first.term(), second.term(), firstProp, nar);
//            } else {
//                //CONJ merge, with any offset shift computed
//                Conj c = new Conj();
//                if (!(c.add(a, first.start()) && c.add(b, second.start()))) {
//                    return first; //failed
//                }
//                content = c.term();
//                if (start1 !=ETERNAL) {
//                    long shift = c.shift();
//                    start1 += shift;
//                    end1 = start1 + Math.min(first.range(), second.range())-1;
//                }
//            }
//
//            //TODO apply a discount factor for the relative difference of the two intermpolated terms
//
//            if (!Task.validTaskTerm(content))
//                return first;
//
//        } else {
//            content = first.term();
//        }

        return null;
    }

    public byte punc() {
        if (isEmpty()) throw new RuntimeException();
        return get(0).task.punc(); //HACK assumes any others are of the same type
    }

    public TaskRegion[] tasks() {
        int size = this.size();
        TaskRegion[] t = new TaskRegion[size];
        for (int i = 0; i < size; i++) {
            t[i] = get(i).task;
        }
        return t;
    }

    public static class TaskComponent {
        public final Task task;

        /**
         * NaN if not yet computed
         */
        public float eviInteg = NaN;
        public float freq = NaN;

        TaskComponent(Task task) {
            this.task = task;
        }

        @Override
        public String toString() {
            return eviInteg + "," + freq + "=" + task;
        }

        public boolean isComputed() {
            float f = freq;
            return f == f;
        }

        /**
         * TODO move to TruthPolation
         */
        @Nullable
        public final TaskComponent update(long start, long end, int dur) {
            if (!isComputed()) {
                float eTotal =
                        Revision.eviInteg(task, start, end, dur);

                if (eTotal < Float.MIN_NORMAL) {
                    this.eviInteg = -1;
                    return null; //no evidence; remove
                } else {
                    this.freq = task.freq();
                    this.eviInteg = eTotal;
                    return this;
                }
            } else {
                return this.eviInteg > 0 ? this : null;
            }


        }
    }


//    /**
//     * computes truth at a given time from iterative task samples
//     * includes variance calculation for reduction of evidence in proportion to confusion/conflict
//     * uses "waldorf method" to calculate a running variance
//     * additionally, the variance is weighted by the contributor's confidences
//     */
//    class TruthPolationDefault implements TruthPolation {
//        public float eviSum, wFreqSum;
//        final long start, end;
//
//
//        /**
//         * computes an adjusted durability no larger than the minimum
//         * distance to the target range provided by the input tasks.
//         * uses the temporals that will be accepted later to determine the duration on a setup pass
//         *
//         //decrease the eternal evidence in proportion to the specificity of the temporal evidence
//         //in other words, eternal evidence is proportional to the range of time specified by the temporals
//         //for an approximation of the specified 'temporal density'
//         //we can use the total task duration divided by the total range they cover
//         //thus if the tasks are few and far between then eternal contributes more of its influence
//         //and if the tasks are dense and overlapping across a short range then eternal will be ignored more.
//         */
//        public TruthPolationDefault(long start, long end, int dur, Iterable<? extends Tasked> temporals, boolean computeDensity) {
//            if (start != ETERNAL) {
//                long minDur = dur;
//                for (Tasked t : temporals) {
//                    Task tt = t.task();
//                    assert(!tt.isEternal());
//
//                    long dd = tt.minDistanceTo(start, end);
//
//                    if (dd == 0) {
//                        minDur = 0; //minimum possible
//                        break;
//                    } else {
//                        minDur = Math.min(minDur, dd);
//                    }
//                    if (computeDensity) {
//                        long ts = Util.clamp(tt.start(), start, end);
//                        long te = Util.clamp(tt.end(), start, end);
//                        spanStart = Math.min(ts, spanStart);
//                        spanEnd = Math.max(te, spanEnd);
//                        rangeSum += Math.max(1, te - ts);
//                    }
//                }
//                assert (minDur < Integer.MAX_VALUE);
//                dur = (int) Math.max(1, minDur);
//            }
//            this.start = start;
//            this.end = end;
//            this.dur = dur;
//        }
//
//        @Override
//        public void accept(Tasked t) {
//            Task task = t.task();
//            Truth tt = task.truth(start, end, dur, 0);
//
//            if (tt != null) {
//                accept(tt.freq(), tt.evi());
//            }
//
//        }
//
//        public void accept(float f, float e) {
//            eviSum += e;
//            wFreqSum += e * f;
//        }
//
//        @Override
//        public PreciseTruth truth() {
//
//            float c = w2cSafe(eviSum);
//            if (c < Param.TRUTH_EPSILON)
//                return null;
//            else {
//                float f = (wFreqSum / eviSum);
//                return new PreciseTruth(f, c);
//            }
//
//
//        }
//    }
//

//    class TruthPolationConf implements TruthPolation {
//        float confSum, wFreqSum;
//        final long start, end;
//        final int dur;
//
//        public TruthPolationConf(long start, long end, int dur) {
//            this.start = start;
//            this.end = end;
//            this.dur = dur;
//        }
//
//        @Override
//        public void accept(Tasked t) {
//            Task task = t.task();
//            float c = task.conf(start, end, dur);
//            if (c > 0) {
//                confSum += c;
//                wFreqSum += c * task.freq();
//            }
//
//        }
//
//
//        @Override
//        public PreciseTruth truth() {
//            if (confSum > 0) {
//                float f = wFreqSum / confSum;
//                float c = confSum;
//                if (c < Param.TRUTH_EPSILON)
//                    return null; //high-pass conf filter
//
//                return new PreciseTruth(f, Util.min(1f - Param.TRUTH_EPSILON, c));
//
//            } else {
//                return null;
//            }
//
//        }
//    }
//
//
//    class TruthPolationGreedy implements TruthPolation {
//
//        final long start, end;
//        final int dur;
//        private final Random rng;
//        float bestE = Float.NEGATIVE_INFINITY;
//        final FloatArrayList bestF = new FloatArrayList(4);
//
//        public TruthPolationGreedy(long start, long end, int dur) {
//            this(start, end, dur, null);
//        }
//
//        public TruthPolationGreedy(long start, long end, int dur, Random rng) {
//            this.start = start;
//            this.end = end;
//            this.dur = dur;
//            this.rng = rng;
//        }
//
//        @Override
//        public void accept(Tasked t) {
//            Task task = t.task();
//            float e = task.evi(start, end, dur);
//            if (e > bestE) {
//                bestF.clear();
//            }
//            if (e >= bestE) {
//                bestF.add(task.freq());
//                bestE = e;
//            }
//        }
//
//
//        @Override
//        public PreciseTruth truth() {
//            FloatArrayList f = this.bestF;
//            int s = f.size();
//
//            float g;
//            switch (s) {
//                case 0:
//                    return null;
//                case 1:
//                    g = f.get(0);
//                    break;
//                default: {
//                    Random r;
//                    if (rng == null)
//                        r = ThreadLocalRandom.current();
//                    else
//                        r = rng;
//                    g = f.get(r.nextInt(s));
//                    break;
//                }
//            }
//
//            return new PreciseTruth(g, bestE, false);
//        }
//    }
//
//    class TruthPolationSoftMax implements TruthPolation {
//
//        final long when;
//        final int dur;
//        final FloatArrayList freq = new FloatArrayList();
//        final FloatArrayList conf = new FloatArrayList();
//
//        public TruthPolationSoftMax(long when, int dur) {
//            this.when = when;
//            this.dur = dur;
//        }
//
//        @Override
//        public void accept(Tasked t) {
//            Task task = t.task();
//            conf.add(task.conf(when, dur)); //TODO start,end
//            freq.add(task.freq());
//        }
//
//
//        @Override
//        public PreciseTruth truth() {
//            if (!conf.isEmpty()) {
//                int which = new DecideSoftmax(0f, ThreadLocalRandom.current()).decide(conf.toArray(), -1);
//                float f = freq.get(which);
//                float c = conf.get(which);
//                return new PreciseTruth(f, c);
//
//            } else {
//                return null;
//            }
//
//        }
//    }
//
//    class TruthPolationRoulette implements TruthPolation {
//
//        final long start, end;
//        final int dur;
//        final FloatArrayList freq = new FloatArrayList();
//        final FloatArrayList evi = new FloatArrayList();
//        private final Random rng;
//
//        public TruthPolationRoulette(long start, long end, int dur, final Random rng) {
//            this.start = start;
//            this.end = end;
//            this.dur = dur;
//            this.rng = rng;
//        }
//
//        @Override
//        public void accept(Tasked t) {
//            Task task = t.task();
//            evi.add(task.evi(start, end, dur));
//            freq.add(task.freq());
//        }
//
//
//        @Override
//        public PreciseTruth truth() {
//            if (!evi.isEmpty()) {
//                int which = Roulette.decideRoulette(freq.size(), evi::get, rng);
//                float f = freq.get(which);
//                float e = evi.get(which);
//                return new PreciseTruth(f, e, false);
//
//            } else {
//                return null;
//            }
//
//        }
//    }
//
//    /**
//     * computes truth at a given time from iterative task samples
//     * includes variance calculation for reduction of evidence in proportion to confusion/conflict
//     * uses "waldorf method" to calculate a running variance
//     * additionally, the variance is weighted by the contributor's confidences
//     */
//    class TruthPolationWithVariance implements TruthPolation {
//        float eviSum, wFreqSum;
//        float meanSum = 0.5f, deltaSum;
//        int count;
//
//        final long when;
//        final int dur;
//
//        public TruthPolationWithVariance(long when, int dur) {
//            this.when = when;
//            this.dur = dur;
//        }
//
//        @Override
//        public void accept(Tasked tt) {
//            Task task = tt.task();
//            float tw = task.evi(when, dur);
//
//            if (tw > 0) {
//
//                if (!task.isEternal())
//                    tw = tw / (1f + ((float) task.range()) / dur); //dilute the long task in proportion to how many durations it consumes beyond point-like (=0)
//
//                eviSum += tw;
//
//                float f = task.freq();
//                wFreqSum += tw * f;
//
//                //        double delta = value - tmpMean;
//                //        mean += delta / ++count;
//                //        sSum += delta * (value - mean);
//                float tmpMean = meanSum;
//                float delta = f - tmpMean;
//                meanSum += delta / ++count;
//                deltaSum += delta * (f - meanSum) * w2c(tw); //scale the delta sum by the conf so that not all tasks contribute to the variation equally
//            }
//
//        }
//
//
//        @Override
//        public PreciseTruth truth() {
//            if (eviSum > 0) {
//                float f = wFreqSum / eviSum;
//
//                float var =
//                        deltaSum / count;
//
//                return new PreciseTruth(f, eviSum * (1f / (1f + var)), false);
//
//            } else {
//                return null;
//            }
//
//        }
//    }


//    /**
//     * returns (freq, evid) pair
//     */
//    @Nullable
//    public static PreciseTruth truthRaw(@Nullable Task topEternal, long when, int dur, @NotNull Iterable<Task> tasks) {
//
//        float[] fe = new float[2];
//
//
//        // Contribution of each task's truth
//        // use forEach instance of the iterator(), since HijackBag forEach should be cheaper
//        tasks.forEach(t -> {
//
//            float tw = t.evi(when, dur);
//
//            if (tw > 0) {
//                freqSum += tw;
//                wFreqSum += tw * t.freq();
//            }
//
//        });
//        float evidence = freqSum;
//        float freqEvi = wFreqSum;
//
//        if (topEternal != null) {
//            float ew = topEternal.evi();
//            evidence += ew;
//            freqEvi += ew * topEternal.freq();
//        }
//
//        if (evidence > 0) {
//            float f = freqEvi / evidence;
//            return new PreciseTruth(f, evidence, false);
//        } else {
//            return null;
//        }
//    }

}
