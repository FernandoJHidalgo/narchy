package nars.concept.table;

import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TruthDelta;
import nars.nal.Stamp;
import nars.task.Revision;
import nars.task.TruthPolation;
import nars.truth.Truth;
import nars.util.Util;
import org.eclipse.collections.impl.list.mutable.MultiReaderFastList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.google.common.collect.Iterators.concat;
import static com.google.common.collect.Iterators.singletonIterator;
import static nars.Param.rankTemporalByConfidence;
import static nars.time.Tense.ETERNAL;
import static nars.truth.TruthFunctions.c2w;

/**
 * stores the items unsorted; revection manages their ranking and removal
 */
public class MicrosphereTemporalBeliefTable implements TemporalBeliefTable {

    static final int MAX_TRUTHPOLATION_SIZE = 32;
    static final ThreadLocal<TruthPolation> truthpolations = ThreadLocal.withInitial(() -> {
        return new TruthPolation(MAX_TRUTHPOLATION_SIZE);
    });

    private volatile int capacity;
    final MultiReaderFastList<Task> list;

    public MicrosphereTemporalBeliefTable(int initialCapacity) {
        super();
        this.list = MultiReaderFastList.newList(initialCapacity);
        this.capacity = initialCapacity;
    }

    @Override
    public Iterator<Task> iterator() {
        throw new UnsupportedOperationException();
        //return list.iterator();
    }

    @Override
    public final void forEach(Consumer<? super Task> action) {
        list.withReadLockAndDelegate(l -> {
            l.forEach(action);
        });
    }

    public void capacity(int newCapacity, long now, @NotNull List<Task> removed) {
        if (this.capacity != newCapacity) {

            this.capacity = newCapacity;

            if (size() > newCapacity) {
                list.withWriteLockAndDelegate((l) -> {

                    removeAlreadyDeleted(removed);

                    while (list.size() > newCapacity) {
                        Task weakest = weakest(list, now, null, Float.NEGATIVE_INFINITY);
                        if (weakest == null)
                            throw new NullPointerException();
                        remove(weakest, removed);
                    }

                });
            }
        }

    }

    @Override
    public final int size() {
        return list.size();
    }

    @Override
    public final boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public final int capacity() {
        return capacity;
    }


    /**
     * according to a balance of temporal proximity and confidence
     */
    public static float rank(@NotNull Task t, long when, long now) {
        //return rankTemporalByConfidenceAndOriginality(t, when, now, -1);
        return rankTemporalByConfidence(t, when, now);
    }


    @Nullable
    @Override
    public final TruthDelta add(@NotNull Task input, EternalTable eternal, @NotNull List<Task> displ, Concept concept, @NotNull NAR nar) {

        int cap = capacity();
        if (cap == 0)
            return null;

        //the result of compression is processed separately
        final TruthDelta[] delta = new TruthDelta[1];

        list.withWriteLockAndDelegate(l -> {
            final Truth before;

            long now = nar.time();

            before = truth(now, eternal);

            Task next;
            if ((next = compress(l, input, now, eternal, displ, concept)) != null) {

                list.add(input);

                if (next != input && list.size() + 1 <= cap) {
                    list.add(next);
                }

                final Truth after = truth(now, eternal);
                delta[0] = new TruthDelta(before, after);
            }
        });

        return delta[0];
    }


    @Override
    public boolean removeIf(@NotNull Predicate<? super Task> o, List<Task> displ) {
        final boolean[] modified = {false};
        list.withWriteLockAndDelegate(l -> {
            list.removeIf(((Predicate<Task>) t -> {
                if (o.test(t)) {
                    displ.add(t);
                    modified[0] = true;
                    return true;
                }
                return false;
            }));
        });
        return modified[0];
    }


    @Override
    public final boolean isFull() {
        return size() == capacity();
    }

//    @Override
//    public void minTime(long minT) {
//        this.min = minT;
//    }
//
//    @Override
//    public void maxTime(long maxT) {
//        this.max = maxT;
//    }

//
//    @Override
//    public final void range(long[] t) {
//        for (Task x : this.items) {
//            if (x != null) {
//                long o = x.occurrence();
//                if (o < t[0]) t[0] = o;
//                if (o > t[1]) t[1] = o;
//            }
//        }
//    }


    public boolean remove(Object object) {
        return list.remove(object);
    }

    private final boolean remove(@NotNull Task x, @NotNull List<Task> displ) {
        if (list.remove(x)) {
            displ.add(x);
            return true;
        }
        return false;
    }


//    @Override
//    public final boolean remove(Object object) {
//        if (super.remove(object)) {
//            invalidRangeIfLimit((Task)object);
//            return true;
//        }
//        return false;
//    }


    @Nullable
    private final Task remove(int index, @NotNull List<Task> displ) {
        @Nullable Task t = list.remove(index);
        if (t != null) {
            displ.add(t);
        }
        return t;
    }


    public Task weakest(List<Task> l, long now, @Nullable Task toMergeWith, float minRank) {
        Task weakest = null;
        float weakestRank = minRank;
        int n = size();


        long[] mergeEvidence = toMergeWith != null ? toMergeWith.evidence() : null;
        long then = toMergeWith != null ? toMergeWith.occurrence() : now;

        for (int i = 0; i < n; i++) {

            Task ii = l.get(i);
            if (mergeEvidence != null &&
                    ((!Param.REVECTION_ALLOW_MERGING_OVERLAPPING_EVIDENCE &&
                            (/*Stamp.isCyclic(iiev) || */Stamp.overlapping(mergeEvidence, ii.evidence()))
                    )))
                continue;

            //consider ii for being the weakest ranked task to remove
            float r = rank(ii, then, now);

//                r *=
//                        (1 + Math.abs(ii.freq() - toMergeWith.freq())); //similar frequency makes them more likely to be paired

            //if (toMergeWith != null) {
            //* (1 + Math.abs(ii.occurrence() - toMergeWith.occurrence()));
            //}

            //(toMergeWith!=null ? (1f / (1f + Math.abs(ii.freq()-toMergeWith.freq()))) : 1f); //prefer close freq match
            if (weakest == null || r < weakestRank) {
                weakestRank = r;
                weakest = ii;
            }

        }

        return weakest;
    }


//    @Nullable
//    protected Task compress(@NotNull List<Task> displ, long now) {
//        return compress(null, now, null, displ, null);
//    }

    /**
     * frees one slot by removing 2 and projecting a new belief to their midpoint. returns the merged task
     */
    @Nullable
    protected Task compress(List<Task> l, @Nullable Task input, long now, @Nullable EternalTable eternal, @NotNull List<Task> displ, @Nullable Concept concept) {

        int cap = capacity();
        if (size() < cap || removeAlreadyDeleted(displ) < cap) {
            return input; //no need for compression
        }


        float inputRank = input != null ? rank(input, now, now) : Float.POSITIVE_INFINITY;

        Task a = weakest(l, now, null, inputRank);
        if (a == null || !remove(a, displ)) {
            //dont continue if the input was too weak, or there was a problem removing a (like it got removed already by a different thread or something)
            return null;
        }

        Task b = weakest(l, now, a, Float.POSITIVE_INFINITY);

        if (b != null && remove(b, displ)) {
            return merge(a, b, now, concept, eternal);
        } else {
            return input;
        }

    }

    /**
     * t is the target time of the new merged task
     */
    @Nullable
    private Task merge(@NotNull Task a, @NotNull Task b, long now, Concept concept, @Nullable EternalTable eternal) {
        double ac = c2w(a.conf());
        double bc = c2w(b.conf());
        long mid = (long) Math.round(Util.lerp((double) a.occurrence(), (double) b.occurrence(), ac / (ac + bc)));

        //more evidence overlap indicates redundant information, so reduce the confWeight (measure of evidence) by this amount
        //TODO weight the contributed overlap amount by the relative confidence provided by each task
        float overlap = Stamp.overlapFraction(a.evidence(), b.evidence());

        Truth t = Revision.revision(a, b, 1f - (overlap / 2f), Param.TRUTH_EPSILON /*nar.confMin*/);
        if (t != null)
            return Revision.mergeInterpolate(a, b, mid, now, t, concept);

        return null;
    }


    @Nullable
    @Override
    public final Task match(long when, long now, @Nullable Task against) {

        if (list.isEmpty())
            return null;

        final Task[] best = new Task[1];

        list.withReadLockAndDelegate(l -> {

            float bestRank = Float.NEGATIVE_INFINITY;

            int s = l.size();
            for (int i = 0; i < s; i++) {
                Task x = l.get(i);
                if (x != null && !x.isDeleted()) {

                    float r = rank(x, when, now);// / (1f + Revision.dtDifference(against, x));

                    if (r > bestRank) {
                        best[0] = x;
                        bestRank = r;
                    }

                }
            }


        });

        return best[0];
        //}

        //return null;

    }

    @Nullable
    public final Truth truth(long when, @Deprecated @Nullable EternalTable eternal) {
        return truth(when, when, eternal);
    }

    @Nullable
    @Override
    public final Truth truth(long when, long now, @Nullable EternalTable eternal) {


        Task topEternal = eternal.strongest();
        Truth topEternalTruth = topEternal != null ? topEternal.truth() : null;

        int s = size();
        if (s == 0)
            return topEternalTruth;

//        Task[] copy;
//        synchronized (this) {
//            //clone a copy so that truthpolation can freely operate asynchronously
//            s = size();
//            if (s == 0) return null;
//            if (topEternal != null) s++;
//            copy = toArrayExact(new Task[s]);
//        }

//        if (topEternal != null)
//            copy[s - 1] = topEternal;

        Truth res;
        if (s == 1) {
            Task the = list.get(0);
            res = the.truth();
            long o = the.occurrence();
            if ((now == ETERNAL || when == now) && o == when) //optimization: if at the current time and when
                return res;
            return res != null ? Revision.project(res, when, now, o, false) : topEternalTruth;

        } else {
            final Truth[] tt = new Truth[1];
            list.withReadLockAndDelegate(l ->
                    tt[0] = truthpolations.get().truth(when, now,
                            topEternal != null ?
                                    concat(l.iterator(), singletonIterator(topEternal)) :
                                    l.iterator()
                    )
            );
            return tt[0];
        }

    }


    private int removeAlreadyDeleted(@NotNull List<Task> displ) {
        List<Task> list = this.list;
        int s = size();
        for (int i = 0; i < s; ) {
            Task x = list.get(i);
            if (x == null || x.isDeleted()) {
                if (remove(i, displ) != null)
                    s--;
                else
                    break;
            } else {
                i++;
            }
        }
        return s;
    }


    //    public final boolean removeIf(@NotNull Predicate<? super Task> o) {
//
//        IntArrayList toRemove = new IntArrayList();
//        for (int i = 0, thisSize = this.size(); i < thisSize; i++) {
//            Task x = this.get(i);
//            if ((x == null) || (o.test(x)))
//                toRemove.add(i);
//        }
//        if (toRemove.isEmpty())
//            return false;
//        toRemove.forEach(this::remove);
//        return true;
//    }

    //    public Task weakest(Task input, NAR nar) {
//
//        //if (polation == null) {
//            //force update for current time
//
//        polation.credit.clear();
//        Truth current = truth(nar.time());
//        //}
//
////        if (polation.credit.isEmpty())
////            throw new RuntimeException("empty credit table");
//
//        List<Task> list = list();
//        float min = Float.POSITIVE_INFINITY;
//        Task minT = null;
//        for (int i = 0, listSize = list.size(); i < listSize; i++) {
//            Task t = list.get(i);
//            float x = polation.value(t, -1);
//            if (x >= 0 && x < min) {
//                min = x;
//                minT = t;
//            }
//        }
//
//        System.out.println("removing " + min + "\n\t" + polation.credit);
//
//        return minT;
//    }


    //    public @Nullable Truth topTemporalCurrent(long when, long now, @Nullable Task topEternal) {
//        //find the temporal with the best rank
//        Task t = topTemporal(when, now);
//        if (t == null) {
//            return (topEternal != null) ? topEternal.truth() : Truth.Null;
//        } else {
//            Truth tt = t.truth();
//            return (topEternal() != null) ? tt.interpolate(topEternal.truth()) : tt;
//
//            //return t.truth();
//        }
//    }


//    //NEEDS DEBUGGED
//    @Nullable public Truth topTemporalWeighted(long when, long now, @Nullable Task topEternal) {
//
//        float sumFreq = 0, sumConf = 0;
//        float nF = 0, nC = 0;
//
//        if (topEternal!=null) {
//            //include with strength of 1
//
//            float ec = topEternal.conf();
//
//            sumFreq += topEternal.freq() * ec;
//            sumConf += ec;
//            nF+= ec;
//            nC+= ec;
//        }
//
//        List<Task> temp = list();
//        int numTemporal = temp.size();
//
//        if (numTemporal == 1) //optimization: just return the only temporal truth value if it's the only one
//            return temp.get(0).truth();
//
//
////        long maxtime = Long.MIN_VALUE;
////        long mintime = Long.MAX_VALUE;
////        for (int i = 0, listSize = numTemporal; i < listSize; i++) {
////            long t = temp.get(i).occurrence();
////            if (t > maxtime)
////                maxtime = t;
////            if (t < mintime)
////                mintime = t;
////        }
////        float dur = 1f/(1f + (maxtime - mintime));
//
//
//        long mdt = Long.MAX_VALUE;
//        for (int i = 0; i < numTemporal; i++) {
//            long t = temp.get(i).occurrence();
//            mdt = Math.min(mdt, Math.abs(now - t));
//        }
//        float window = 1f / (1f + mdt/2);
//
//
//        for (int i = 0, listSize = numTemporal; i < listSize; i++) {
//            Task x = temp.get(i);
//
//            float tc = x.conf();
//
//            float w = TruthFunctions.temporalIntersection(
//                    when, x.occurrence(), now, window);
//
//            //strength decreases with distance in time
//            float strength =  w * tc;
//
//            sumConf += tc * w;
//            nC+=tc;
//
//            sumFreq += x.freq() * strength;
//            nF+=strength;
//        }
//
//        return nC == 0 ? Truth.Null :
//                new DefaultTruth(sumFreq / nF, (sumConf/nC));
//    }

}
