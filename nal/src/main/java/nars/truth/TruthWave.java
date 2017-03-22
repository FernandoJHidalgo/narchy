package nars.truth;

import nars.NAR;
import nars.table.BeliefTable;
import nars.time.Tense;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/** compact chart-like representation of a belief state at each time cycle in a range of time.
 *  useful as a memoized state snapshot of a belief table
 *  stored in an array of float quadruplets for each task:
 *      1) start
 *      2) end
 *      3) freq
 *      4) conf
 *      5) quality
 * */
public class TruthWave {

    private static final int ENTRY_SIZE = 5;

    /** start and stop interval (in cycles) */
    long start;
    long end;

    /** sequence of triples (freq, conf, occurr) for each task; NaN for eternal */
    float[] truth;
    int size;
    @Nullable
    public Truth current;

    public TruthWave(int initialCapacity) {
        resize(initialCapacity);
        clear();
    }

    private void clear() {
        size = 0;
        start = end = Tense.ETERNAL;
    }

    private void resize(int cap) {
        truth = new float[ENTRY_SIZE*cap];
    }

    public TruthWave(@NotNull BeliefTable b, @NotNull NAR n) {
        this(b.size());
        set(b, n.time(), n.time.dur());
    }

    /** clears and fills this wave with the data from a table */
    public void set(@NotNull BeliefTable b, long now, int dur) {
        int s = b.size();
        if (s == 0) {
            this.current = null;
            clear();
            return;
        }
        ensureSize(s);

        float[] t = this.truth;

        final int[] size = {0};
        b.forEachTask(x -> {
            int ss = size[0];
            if (ss < s) { //HACK in case the table size changed since allocating above
                int j = (size[0]++) * ENTRY_SIZE;
                load(t, j, x.start(), x.end(), x, x.qua());
            }
        });
        this.size = size[0];

        //compute time range
        float start = Float.POSITIVE_INFINITY;
        float end = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < size[0]; i++) {
            float starts = t[i*ENTRY_SIZE + 0];
            if (start == start) {
                float ends = t[i*ENTRY_SIZE + 1];

                float oo = (starts + ends)/2; //midpoint

                if (oo > end) end = oo;
                if (oo < start) start = oo;
            }
        }
        this.start = (long) start;
        this.end = (long) end;
        this.current = b.truth(now, dur);
    }

    public static void load(float[] array, int index, long start, long end, @Nullable Truthed truth, float qua) {
        if (truth == null)
            return;
        array[index++] = start == Tense.ETERNAL ? Float.NaN : start;
        array[index++] = end == Tense.ETERNAL ? Float.NaN : end;
        array[index++] = truth.freq();
        array[index++] = truth.conf();
        array[index++] = qua;
    }

    public void ensureSize(int s) {

        int c = capacity();

        if (c < s)
            resize(s);
        else {
            if (s < c) Arrays.fill(truth, 0); //TODO memfill only the necessary part of the array that won't be used
        }

    }


    /** fills the wave with evenly sampled points in a time range */
    public void project(@NotNull BeliefTable table, float minT, float maxT, int dur, int points) {
        clear();

        if (minT == maxT) {
            return;
        }
        ensureSize(points);

        float dt = (maxT-minT)/(points);
        float t = minT;
        float[] data = this.truth;
        int j = 0;
        for (int i = 0; i < points; i++) {
            int lt = Math.round(t);
            Truth x = table.truth(lt, dur);
            if (x!=null) {
                load(data, (j++) * ENTRY_SIZE, lt, lt, x, 0.5f);
            }
            t+= dt;
        }
        this.current = null;
        this.size = j;
        this.start = (long)Math.floor(minT);
        this.end = (long)Math.ceil(maxT);
    }

    public boolean isEmpty() { return size == 0; }

    public long start() {
        return start;
    }

    public long end() {
        return end;
    }


    @FunctionalInterface public interface TruthWaveVisitor {
        void onTruth(float f, float c, float start, float end, float qua);
    }

    public final void forEach(@NotNull TruthWaveVisitor v) {
        int n = this.size;
        float[] t = this.truth;
        int j = 0;
        for (int i = 0; i < n; i++) {
            float s = t[j++];
            float e = t[j++];
            float f = t[j++];
            float c = t[j++];
            float q = t[j++];
            v.onTruth(f, c, s, e, q);
        }
    }

    public final int capacity() { return truth.length / ENTRY_SIZE; }

//        //get min and max occurence time
//        for (Task t : beliefs) {
//            long o = t.occurrence();
//            if (o == Tense.ETERNAL) {
//                expectEternal1 += t.truth().expectationPositive();
//                expectEternal0 += t.truth().expectationNegative();
//                numEternal++;
//            }
//            else {
//                numTemporal++;
//                if (o > max) max = o;
//                if (o < min) min = o;
//            }
//        }
//
//        if (numEternal > 0) {
//            expectEternal1 /= numEternal;
//            expectEternal0 /= numEternal;
//        }
//
//        start = min;
//        end = max;
//
//        int range = length();
//        expect = new float[2][];
//        expect[0] = new float[range+1];
//        expect[1] = new float[range+1];
//
//        if (numTemporal > 0) {
//            for (Task t : beliefs) {
//                long o = t.occurrence();
//                if (o != Tense.ETERNAL) {
//                    int i = (int)(o - min);
//                    expect[1][i] += t.truth().expectationPositive();
//                    expect[0][i] += t.truth().expectationNegative();
//                }
//            }
//
//            //normalize
//            for (int i = 0; i < (max-min); i++) {
//                expect[0][i] /= numTemporal;
//                expect[1][i] /= numTemporal;
//            }
//        }
//
//    }
//
//    //TODO getFrequencyAnalysis
//    //TODO getDistribution
//
//    public int length() { return (int)(end-start); }
//
//    public void print() {
//        System.out.print("eternal=" + numEternal + ", temporal=" + numTemporal);
//
//
//        if (length() == 0) {
//            System.out.println();
//            return;
//        }
//        System.out.println(" @ " + start + ".." + end);
//
//        for (int c = 0; c < 2; c++) {
//            for (int i = 0; i < length(); i++) {
//
//                float v = expect[c][i];
//
//                System.out.print(Util.n2u(v) + ' ');
//
//            }
//            System.out.println();
//        }
//    }


    @NotNull
    @Override
    public String toString() {
        return start() + ".." + end() + ": " + Arrays.toString(truth);
    }
}
