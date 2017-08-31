package nars.task;

import jcog.Util;
import jcog.decide.DecideSoftmax;
import nars.Task;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import static jcog.Util.sqr;
import static nars.truth.TruthFunctions.w2c;

/**
 * Truth Interpolation and Extrapolation of Temporal Beliefs/Goals
 * see:
 * https://en.wikipedia.org/wiki/Category:Intertemporal_economics
 * https://en.wikipedia.org/wiki/Discounted_utility
 */
public interface TruthPolation extends Consumer<Tasked> {

    PreciseTruth truth();


    /**
     * computes truth at a given time from iterative task samples
     * includes variance calculation for reduction of evidence in proportion to confusion/conflict
     * uses "waldorf method" to calculate a running variance
     * additionally, the variance is weighted by the contributor's confidences
     */
    public static class TruthPolationBasic implements TruthPolation {
        float eviSum, wFreqSum;
        final long start, end;
        final int dur;

        public TruthPolationBasic(long start, long end, int dur) {
            this.start = start;
            this.end = end;
            this.dur = dur;
        }

        @Override
        public void accept(Tasked t) {
            Task task = t.task();
            float tw = task.evi(start, end, dur);
            if (tw > 0) {
                eviSum += tw;
                wFreqSum += tw * task.freq();
            }

        }


        public PreciseTruth truth() {
            if (eviSum > 0) {
                float f = wFreqSum / eviSum;
                return new PreciseTruth(f, eviSum, false);

            } else {
                return null;
            }

        }
    }

    /** TODO this does not fairly handle equal values; the first will be chosen */
    public static class TruthPolationGreedy implements TruthPolation {

        final long start, end;
        final int dur;
        float bestE = Float.NEGATIVE_INFINITY, bestF = Float.NaN;

        public TruthPolationGreedy(long start, long end, int dur) {
            this.start = start;
            this.end = end;
            this.dur = dur;
        }

        @Override
        public void accept(Tasked t) {
            Task task = t.task();
            float e = task.evi(start, end, dur);
            if (e > bestE) {
                bestE = e;
                bestF = task.freq();
            }
        }


        public PreciseTruth truth() {
            float f = this.bestF;
            if (f != f)
                return null;

            return new PreciseTruth(f, bestE, false);
        }
    }

    public static class TruthPolationSoftMax implements TruthPolation {

        final long when;
        final int dur;
        final FloatArrayList freq = new FloatArrayList();
        final FloatArrayList conf = new FloatArrayList();

        public TruthPolationSoftMax(long when, int dur) {
            this.when = when;
            this.dur = dur;
        }

        @Override
        public void accept(Tasked t) {
            Task task = t.task();
            conf.add(task.conf(when, dur)); //TODO start,end
            freq.add(task.freq());
        }


        public PreciseTruth truth() {
            if (!conf.isEmpty()) {
                int which = new DecideSoftmax(0f, ThreadLocalRandom.current()).decide(conf.toArray(), -1);
                float f = freq.get(which);
                float c = conf.get(which);
                return new PreciseTruth(f, c);

            } else {
                return null;
            }

        }
    }
    public static class TruthPolationRoulette implements TruthPolation {

        final long start, end;
        final int dur;
        final FloatArrayList freq = new FloatArrayList();
        final FloatArrayList evi = new FloatArrayList();
        private final Random rng;

        public TruthPolationRoulette(long start, long end, int dur, final Random rng) {
            this.start = start;
            this.end = end;
            this.dur = dur;
            this.rng = rng;
        }

        @Override
        public void accept(Tasked t) {
            Task task = t.task();
            evi.add(task.evi(start, end, dur));
            freq.add(task.freq());
        }


        public PreciseTruth truth() {
            if (!evi.isEmpty()) {
                int which = Util.decideRoulette(freq.size(), evi::get, rng);
                float f = freq.get(which);
                float e = evi.get(which);
                return new PreciseTruth(f, e, false);

            } else {
                return null;
            }

        }
    }

    /**
     * computes truth at a given time from iterative task samples
     * includes variance calculation for reduction of evidence in proportion to confusion/conflict
     * uses "waldorf method" to calculate a running variance
     * additionally, the variance is weighted by the contributor's confidences
     */
    public static class TruthPolationWithVariance implements TruthPolation {
        float eviSum, wFreqSum;
        float meanSum = 0.5f, deltaSum;
        int count;

        final long when;
        final int dur;

        public TruthPolationWithVariance(long when, int dur) {
            this.when = when;
            this.dur = dur;
        }

        @Override
        public void accept(Tasked tt) {
            Task task = tt.task();
            float tw = task.evi(when, dur);

            if (tw > 0) {

                if (!task.isEternal())
                    tw = tw/(1f + ((float)task.range())/dur); //dilute the long task in proportion to how many durations it consumes beyond point-like (=0)

                eviSum += tw;

                float f = task.freq();
                wFreqSum += tw * f;

                //        double delta = value - tmpMean;
                //        mean += delta / ++count;
                //        sSum += delta * (value - mean);
                float tmpMean = meanSum;
                float delta = f - tmpMean;
                meanSum += delta / ++count;
                deltaSum += delta * (f - meanSum) * w2c(tw); //scale the delta sum by the conf so that not all tasks contribute to the variation equally
            }

        }


        public PreciseTruth truth() {
            if (eviSum > 0) {
                float f = wFreqSum / eviSum;

                float var =
                        deltaSum / count;

                return new PreciseTruth(f, eviSum * (1f / (1f + var)), false);

            } else {
                return null;
            }

        }
    }


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
