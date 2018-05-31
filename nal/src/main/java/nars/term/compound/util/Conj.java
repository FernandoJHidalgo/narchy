package nars.term.compound.util;

import jcog.data.bit.MetalBitSet;
import jcog.list.FasterList;
import nars.NAR;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.time.Tense;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.LongToLongFunction;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.ImmutableBitmapDataProvider;
import org.roaringbitmap.PeekableIntIterator;
import org.roaringbitmap.RoaringBitmap;

import java.util.*;
import java.util.function.IntPredicate;

import static nars.Op.*;
import static nars.time.Tense.*;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 * representation of conjoined (eternal, parallel, or sequential) events specified in one or more conjunctions,
 * for use while constructing, merging, and/or analyzing
 */
public class Conj extends AnonMap {


    private static final int ROARING_UPGRADE_THRESH = 4;


    public final LongObjectHashMap event = new LongObjectHashMap<>(2);


    /**
     * state which will be set in a terminal condition, or upon term construction in non-terminal condition
     */
    Term term = null;

    public Conj() {
        super(4);
    }

    public void clear() {
        super.clear();
        event.clear();
        term = null;
    }

    public static int eventCount(Object what) {
        if (what instanceof byte[]) {
            return indexOfZeroTerminated((byte[]) what, (byte) 0);
        } else {
            return ((ImmutableBitmapDataProvider) what).getCardinality();
        }
    }

    /**
     * TODO impl levenshtein via byte-array ops
     */
    public static StringBuilder sequenceString(Term a, Conj x) {
        StringBuilder sb = new StringBuilder(4);
        int range = a.dtRange();
        final float stepResolution = 16f;
        float factor = stepResolution / range;
        a.eventsWhile((when, what) -> {
            int step = Math.round(when * factor);
            sb.append((char) step);
            sb.append(((char) x.add(what)));
            return true;
        }, 0, true, true, false, 0);

        return sb;
    }

    /**
     * returns null if wasnt contained, Null if nothing remains after removal
     */
    @Nullable
    public static Term conjDrop(Term conj, Term event, boolean earlyOrLate) {
        if (conj.op() != CONJ || conj.impossibleSubTerm(event))
            return Null;

        int cdt = conj.dt();

        if (cdt == DTERNAL || cdt == 0) {
            Term[] csDropped = conj.subterms().termsExcept(event);
            if (csDropped != null) {
                if (csDropped.length == 1)
                    return csDropped[0];
                else
                    return CONJ.compound(cdt, csDropped);
            }
        }

        Conj c = Conj.from(conj);
        long targetTime;
        if (c.event.size() == 1) {

            targetTime = c.event.keysView().longIterator().next();
        } else if (earlyOrLate) {
            Object eternalTemporarilyRemoved = c.event.remove(ETERNAL);
            targetTime = c.event.keysView().min();
            if (eternalTemporarilyRemoved != null)
                c.event.put(ETERNAL, eternalTemporarilyRemoved);
        } else {
            targetTime = c.event.keysView().max();
        }
        assert (targetTime != XTERNAL);
        boolean removed = c.remove(event, targetTime);
        if (!removed) {
            return Null;
        }

        return c.term();


    }

    public static FasterList<LongObjectPair<Term>> eventList(Term t) {
        return t.eventList(t.dt() == DTERNAL ? ETERNAL : 0, 1, true, true);
    }

    public static Conj from(Term t) {
        return from(t, t.dt() == DTERNAL ? ETERNAL : 0);
    }

    public static Conj from(Term t, long rootTime) {
        Conj x = new Conj();
        x.add(rootTime, t);
        return x;
    }

    public static Term conj(FasterList<LongObjectPair<Term>> events) {
        int eventsSize = events.size();
        switch (eventsSize) {
            case 0:
                return Null;
            case 1:
                return events.get(0).getTwo();
        }

        Conj ce = new Conj();

        for (int i = 0; i < eventsSize; i++) {
            LongObjectPair<Term> o = events.get(i);
            if (!ce.add(o.getOne(), o.getTwo())) {
                break;
            }
        }

        return ce.term();
    }

    public static Term conj(Collection<LongObjectPair<Term>> events) {
        int eventsSize = events.size();
        switch (eventsSize) {
            case 0:
                return Null;
            case 1:
                return events.iterator().next().getTwo();
        }

        Conj ce = new Conj();

        for (LongObjectPair<Term> o : events) {
            if (!ce.add(o.getOne(), o.getTwo())) {
                break;
            }
        }

        return ce.term();
    }

    public static Term without(Term include, Term exclude, boolean includeNeg) {
        if (include.equals(exclude))
            return True;
        if (includeNeg && include.equalsNeg(exclude))
            return True;
        if (include.op() != CONJ || include.impossibleSubTerm(exclude))
            return include;

        Conj xx = Conj.from(include);
        if (xx.removeEventsByTerm(exclude, true, includeNeg)) {
            return xx.term();
        } else {
            return include;
        }
    }

    public static Term withoutAll(Term include, Term exclude) {
        if (include.op() != CONJ)
            return include;

        if (include.equals(exclude))
            return True;

        Conj x = Conj.from(include);
        int edt = exclude.dt();
        boolean[] removed = new boolean[]{false};
        exclude.eventsWhile((when, what) -> {
            removed[0] |= x.remove(what, when);
            return true;
        }, edt == DTERNAL ? ETERNAL : 0, true, edt == DTERNAL, false, 0);

        return removed[0] ? x.term() : include;
    }

    public static Term conjMerge(Term a, Term b, int dt) {
        return (dt >= 0) ?
                conjMerge(a, 0, b, +dt + a.dtRange()) :
                conjMerge(b, 0, a, -dt + b.dtRange());
    }

    static public Term conjMerge(Term a, long aStart, Term b, long bStart) {
        Conj c = new Conj();
        if (c.add(aStart, a)) {
            c.add(bStart, b);
        }
        return c.term();
    }

    static int indexOfZeroTerminated(byte[] b, byte val) {
        for (int i = 0; i < b.length; i++) {
            byte bi = b[i];
            if (val == bi) {
                return i;
            } else if (bi == 0) {
                return -1;
            }
        }
        return -1;
    }

    public static Term conjSeq(FasterList<LongObjectPair<Term>> events) {
        return conjSeq(events, 0, events.size());
    }

    /**
     * constructs a correctly merged conjunction from a list of events, in the sublist specified by from..to (inclusive)
     * assumes that all of the event terms have distinct occurrence times
     */
    public static Term conjSeq(List<LongObjectPair<Term>> events, int start, int end) {

        LongObjectPair<Term> first = events.get(start);
        int ee = end - start;
        switch (ee) {
            case 0:
                throw new NullPointerException("should not be called with empty events list");
            case 1:
                return first.getTwo();
            case 2:
                LongObjectPair<Term> second = events.get(end - 1);
                return conjSeqFinal(
                        (int) (second.getOne() - first.getOne()),
                        /* left */ first.getTwo(), /* right */ second.getTwo());
        }

        int center = start + (end - 1 - start) / 2;


        Term left = conjSeq(events, start, center + 1);
        if (left == Null) return Null;
        if (left == False) return False;

        Term right = conjSeq(events, center + 1, end);
        if (right == Null) return Null;
        if (right == False) return False;

        int dt = (int) (events.get(center + 1).getOne() - first.getOne() - left.dtRange());

        return conjSeqFinal(dt, left, right);
    }

    private static Term conjSeqFinal(int dt, Term left, Term right) {
        assert (dt != XTERNAL);

        if (left == False) return False;
        if (left == Null) return Null;
        if (left == True) return right;

        if (right == False) return False;
        if (right == Null) return Null;
        if (right == True) return left;

        if (dt == 0 || dt == DTERNAL) {
            if (left.equals(right)) return left;
            if (left.equalsNeg(right)) return False;


        }


        if (left.compareTo(right) > 0) {

            dt = -dt;
            Term t = right;
            right = left;
            left = t;
        }

        if (left.op() == CONJ && right.op() == CONJ) {
            int ldt = left.dt(), rdt = right.dt();
            if (ldt != XTERNAL && !concurrent(ldt) && rdt != XTERNAL && !concurrent(rdt)) {
                int ls = left.subs(), rs = right.subs();
                if ((ls > 1 + rs) || (rs > ls)) {

                    return CONJ.compound(dt, new Term[]{left, right});
                }
            }
        }


        return Op.compound(CONJ, dt, left, right);
    }

    /**
     * similar to conjMerge but interpolates events so the resulting
     * intermpolation is not considerably more complex than either of the inputs
     */
    public static Term conjIntermpolate(Term a, Term b, long bOffset, NAR nar) {
        return new Conjterpolate(a, b, bOffset, nar).term();
    }

//    static int conflictOrSame(Object e, int id) {
//        if (e == null) {
//
//        } else if (e instanceof RoaringBitmap) {
//            RoaringBitmap r = (RoaringBitmap) e;
//            if (r.contains(-id))
//                return -1;
//            else if (r.contains(id)) {
//                return +1;
//            }
//        } else if (e instanceof byte[]) {
//            byte[] r = (byte[]) e;
//            if (indexOfZeroTerminated(r, (byte) -id) != -1)
//                return -1;
//            else if (indexOfZeroTerminated(r, (byte) id) != -1)
//                return +1;
//        }
//        return 0;
//    }

    /**
     * returns false if contradiction occurred, in which case this
     * ConjEvents instance is
     * now corrupt and its result via .term() should be considered final
     */
    public boolean add(long at, Term what) {

        if (term != null)
            throw new RuntimeException("already term-inated to: " + term);

        if (what == True)
            return true;
        else if (what == False) {
            this.term = False;
            return false;
        } else if (what == Null) {
            this.term = Null;
            return false;
        }


        Op x = what.op();
        boolean polarity;
        if (x == NEG) {
            what = what.unneg();
            polarity = false;
        } else {
            polarity = true;
        }

        if (x == CONJ) {
            int dt = what.dt();

            boolean atEternal = at == ETERNAL;

            if ((dt != XTERNAL)


                    && (!atEternal || (dt == DTERNAL))

            ) {

                return what.eventsWhile(this::add,
                        at,
                        true,
                        true,
                        false, 0);


            }
        }


        int id = add(what);
        if (!polarity)
            id = -id;

        if (!addIfValid(at, id)) {
            term = False;
            return false;
        } else {
            return true;
        }
    }

    public boolean add(Term t, long start, long end, int maxSamples, int minSegmentLength) {
        if ((start == end) || start == ETERNAL) {
            return add(start, t);
        } else {
            if (maxSamples == 1) {

                return add((start + end) / 2L, t);
            } else {

                long dt = Math.max(minSegmentLength, (end - start) / maxSamples);
                long x = start;
                while (x < end) {
                    if (!add(x, t))
                        return false;
                    x += dt;
                }
                return true;
            }
        }
    }

    protected boolean addIfValid(long at, int id) {


        Object what = event.get(at);
        if (what == null) {
            byte[] bwhat = new byte[ROARING_UPGRADE_THRESH];
            bwhat[0] = (byte) id;
            event.put(at, bwhat);
            return true;
        }
        if (what instanceof RoaringBitmap) {
            RoaringBitmap r = (RoaringBitmap) what;
            if (!r.contains(-id)) {
                r.add(id);
                return true;
            }
        } else {
            byte[] ii = (byte[]) what;
            if (indexOfZeroTerminated(ii, ((byte) id)) != -1) {

                return true;
            }
            if (indexOfZeroTerminated(ii, ((byte) -id)) == -1) {
                int nextSlot = indexOfZeroTerminated(ii, (byte) 0);
                if (nextSlot != -1) {
                    ii[nextSlot] = (byte) id;
                } else {

                    RoaringBitmap rb = new RoaringBitmap();
                    for (byte b : ii)
                        rb.add(b);
                    rb.add(id);
                    event.put(at, rb);
                }
                return true;
            }
        }
        return false;
    }

    public int add(Term t) {
        assert (t != null && !(t instanceof Bool));
        return termToId.getIfAbsentPutWithKey(t.unneg(), tt -> {
            //int s = termToId.size();
            int s = idToTerm.addAndGetSize(tt);
            assert (s < Byte.MAX_VALUE);
            return (byte) s;
        });
    }

    /**
     * returns index of an item if it is present, or -1 if not
     */
    public byte index(Term t) {
        return termToId.getIfAbsent(t.unneg(), (byte) -1);
    }

    public byte get(Term x) {
        boolean neg;
        if (neg = (x.op() == NEG))
            x = x.unneg();
        byte index = index(x);
        if (index == -1)
            return Byte.MIN_VALUE;

        if (neg)
            index = (byte) (-index);

        return index;
    }

    public boolean remove(Term t, long at) {

        Object o = event.get(at);
        if (o == null)
            return false;


        int i = get(t);
        if (i == Byte.MIN_VALUE)
            return false;

        if (removeFromEvent(at, o, true, i) != 0) {
            term = null;
            return true;
        }
        return false;
    }


    /**
     * returns:
     * +2 removed, and now this event time is empty
     * +1 removed
     * +0 not removed
     */
    private int removeFromEvent(long at, Object o, boolean autoRemoveIfEmpty, int... i) {
        if (o instanceof RoaringBitmap) {
            boolean b = false;
            RoaringBitmap oo = (RoaringBitmap) o;
            for (int ii : i)
                b |= oo.checkedRemove(ii);
            if (!b) return 0;
            if (oo.isEmpty()) {
                if (autoRemoveIfEmpty)
                    event.remove(at);
                return 2;
            } else {
                return 1;
            }
        } else {
            byte[] b = (byte[]) o;

            int num = ArrayUtils.indexOf(b, (byte) 0);
            if (num == -1) num = b.length;

            int removals = 0;
            for (int ii : i) {
                int bi = ArrayUtils.indexOf(b, (byte) ii);
                if (bi != -1) {
                    if (b[bi] != 0) {
                        b[bi] = 0;
                        removals++;
                    }
                }
            }

            if (removals == 0)
                return 0;
            else if (removals == num) {
                if (autoRemoveIfEmpty)
                    event.remove(at);
                return 2;
            } else {


                MetalBitSet toRemove = MetalBitSet.bits(b.length);

                for (int zeroIndex = 0; zeroIndex < b.length; zeroIndex++) {
                    if (b[zeroIndex] == 0)
                        toRemove.set(zeroIndex);
                }

                b = ArrayUtils.removeAll(b, toRemove);
                event.put(at, b);
                return 1;
            }
        }
    }

    boolean removeEventsByTerm(Term t, boolean pos, boolean neg) {

        boolean negateInput;
        if (t.op() == NEG) {
            negateInput = true;
            t = t.unneg();
        } else {
            negateInput = false;
        }

        int i = get(t);
        int[] ii;
        if (pos && neg) {
            ii = new int[]{i, -i};
        } else if (pos) {
            ii = new int[]{negateInput ? -i : i};
        } else if (neg) {
            ii = new int[]{negateInput ? i : -i};
        } else {
            throw new UnsupportedOperationException();
        }


        final boolean[] removed = {false};
        LongArrayList eventsToRemove = new LongArrayList(4);
        event.forEachKeyValue((when, o) -> {
            int result = removeFromEvent(when, o, false, ii);
            if (result == 2) {
                eventsToRemove.add(when);
            }
            removed[0] |= result > 0;
        });

        if (!eventsToRemove.isEmpty()) {
            eventsToRemove.forEach(event::remove);
        }

        if (removed[0]) {
            term = null;
            return true;
        }
        return false;
    }

    public Term term() {
        if (term != null)
            return term;


        int numTimes = event.size();
        if (numTimes == 0)
            return True;


        IntPredicate validator = null;
        Object eternalWhat = event.get(ETERNAL);
        Term eternal = term(ETERNAL, eternalWhat);
        if (eternal != null) {

            if (eternal instanceof Bool)
                return this.term = eternal;

            if (numTimes > 1) {


                if (eternal.op() == CONJ) {

                    if (eternalWhat instanceof byte[]) {
                        byte[] b = (byte[]) eternalWhat;
                        validator = (i) -> indexOfZeroTerminated(b, (byte) -i) == -1;
                    } else {
                        RoaringBitmap b = (RoaringBitmap) eternalWhat;
                        validator = (i) -> !b.contains(-i);
                    }
                } else {
                    Term finalEternal = eternal;
                    validator = (t) -> !finalEternal.equalsNeg(idToTerm.get(Math.abs(t - 1)).negIf(t < 0));
                }
            }
        }

        Term ci;
        if (eternal != null && numTimes == 1) {
            ci = eternal;
        } else {
            FasterList<LongObjectPair<Term>> temporals = new FasterList<>(numTimes - (eternal != null ? 1 : 0));
            Iterator<LongObjectPair<Term>> ii = event.keyValuesView().iterator();
            while (ii.hasNext()) {
                LongObjectPair<Term> next = ii.next();
                long when = next.getOne();
                if (when == ETERNAL)
                    continue;

                Term wt = term(when, next.getTwo(), validator);

                if (wt == True) {
                    continue;
                } else if (wt == False) {
                    return this.term = False;
                } else if (wt == Null) {
                    return this.term = Null;
                }

                temporals.add(pair(when, wt));
            }

            Term temporal;
            int ee = temporals.size();
            switch (ee) {
                case 0:
                    temporal = null;
                    break;
                case 1:
                    temporal = temporals.get(0).getTwo();
                    break;
                default:
                    temporals.sortThisBy(LongObjectPair::getOne);
                    temporal = conjSeq(temporals);
                    break;
            }

            if (eternal != null && temporal != null) {
                ci = CONJ.the(temporal, eternal);
            } else if (eternal == null) {
                ci = temporal;
            } else /*if (temporal == null)*/ {
                ci = eternal;
            }
        }


        if (ci.op() == CONJ && ci.hasAny(NEG)) {
            Subterms cci;
            if ((cci = ci.subterms()).hasAny(CONJ)) {
                int ciDT = ci.dt();
                if (ciDT == 0 || ciDT == DTERNAL) {
                    int s = cci.subs();
                    RoaringBitmap ni = null, nc = null;
                    for (int i = 0; i < s; i++) {
                        Term cii = cci.sub(i);
                        if (cii.op() == NEG) {
                            Term cInner = cii.unneg();
                            if (cInner.op() == CONJ && cInner.dt() == ciDT /* same DT */) {
                                if (nc == null) nc = new RoaringBitmap();
                                nc.add(i);
                            } else {
                                if (ni == null) ni = new RoaringBitmap();
                                ni.add(i);
                            }
                        }
                    }
                    if (nc != null && ni != null) {
                        int[] bb = ni.toArray();
                        MetalBitSet toRemove = MetalBitSet.bits(bb.length);
                        PeekableIntIterator ncc = nc.getIntIterator();
                        while (ncc.hasNext()) {
                            int nccc = ncc.next();
                            for (int j = 0; j < bb.length; j++) {
                                Term NC = cci.sub(nccc).unneg();
                                Term NX = cci.sub(bb[j]).unneg();
                                if (NC.contains(NX)) {
                                    toRemove.set(nccc);
                                }
                            }
                        }
                        if (toRemove.cardinality() > 0) {
                            return CONJ.compound(ciDT, cci.termsExcept(toRemove));
                        }
                    }


                }
            }
        }
        return ci;
    }

    public long shift() {
        long min = Long.MAX_VALUE;
        LongIterator ii = event.keysView().longIterator();
        while (ii.hasNext()) {
            long t = ii.next();
            if (t != DTERNAL) {
                if (t < min)
                    min = t;
            }
        }
        return min;
    }

    public Term term(long when) {
        return term(when, event.get(when), null);
    }

    private Term term(long when, Object what) {
        return term(when, what, null);
    }

    private Term term(long when, Object what, IntPredicate validator) {

        if (what == null) return null;

        final RoaringBitmap rb;
        final byte[] b;
        int n;
        if (what instanceof byte[]) {
            b = (byte[]) what;
            rb = null;
            n = indexOfZeroTerminated(b, (byte) 0);
            if (n == 1) {

                return sub(b[0], null, validator);
            }
        } else {
            rb = (RoaringBitmap) what;
            b = null;
            n = rb.getCardinality();
        }


        final boolean[] negatives = {false};
        MutableSet<Term> t = new UnifiedSet(4);
        if (b != null) {
            for (byte x : b) {
                if (x == 0)
                    break;
                t.add(sub(x, negatives, validator));
            }
        } else {
            rb.forEach((int termIndex) ->
                t.add(sub(termIndex, negatives, validator))
            );
        }

        if (negatives[0] && n > 1) {


            Iterator<Term> oo = t.iterator();
            List<Term> csa = null;
            while (oo.hasNext()) {
                Term x = oo.next();
                if (x.hasAll(NEG.bit | CONJ.bit)) {
                    if (x.op() == NEG) {
                        Term x0 = x.sub(0);
                        if (x0.op() == CONJ && CONJ.commute(x0.dt(), x0.subs())) {
                            Term disj = x.unneg();
                            SortedSet<Term> disjSubs = disj.subterms().toSetSortedExcept(t::contains);

                            if (!disjSubs.isEmpty()) {

                                oo.remove();

                                if (!disjSubs.isEmpty()) {
                                    if (csa == null)
                                        csa = new FasterList(1);
                                    csa.add(
                                            CONJ.the(disj.dt(), disjSubs).neg()
                                    );
                                }
                            }
                        }
                    }
                }
            }
            if (csa != null)
                t.addAll(csa);
        }

        int ts = t.size();
        switch (ts) {
            case 0:
                return True;
            case 1:
                return t.getOnly();
            default: {
                int dt;
                if (when == ETERNAL) {
                    dt = DTERNAL;
                } else {
                    dt = 0;
                }
                return Op.compound(CONJ, dt, t.toArray(Op.EmptyTermArray));

            }
        }
    }

    public Term sub(int termIndex) {
        return sub(termIndex, null, null);
    }

    public Term sub(int termIndex, @Nullable boolean[] negatives, @Nullable IntPredicate validator) {
        assert (termIndex != 0);

        boolean neg = false;
        if (termIndex < 0) {
            termIndex = -termIndex;
            neg = true;
        }

        if (validator != null && !validator.test(termIndex))
            return False;

        Term c = idToTerm.get(termIndex - 1);
        if (neg) {
            c = c.neg();
            if (negatives != null)
                negatives[0] = true;
        }
        return c;
    }

    /**
     * for each of b's events, find the nearest matching event in a while constructing a new Conj consisting of their mergers
     */
    static class Conjterpolate extends Conj {

        private final Conj aa;
        private final Term b;
        private final NAR nar;
        private final Random rng;
        private final boolean mergeOrChoose;

        public Conjterpolate(Term a, Term b, long bOffset, NAR nar) {

            this.b = b;
            this.nar = nar;
            this.mergeOrChoose = nar.dtMergeOrChoose();
            this.rng = nar.random();
            Conj aa = new Conj();
            aa.add(0, a);
            this.aa = aa;
            add(bOffset, b);
        }

        @Override
        public boolean add(long bt, final Term what) {
            assert (bt != XTERNAL);


            if (what == b)
                return super.add(bt, what);
            else {
                boolean neg = what.op() == NEG;


                byte tInA = (byte) ((aa.termToId.get(neg ? what.unneg() : what) + 1) * (neg ? -1 : +1));


                LongArrayList whens = new LongArrayList(2);

                aa.event.forEachKeyValue((long when, Object wat) -> {
                    if (wat instanceof RoaringBitmap) {
                        RoaringBitmap r = (RoaringBitmap) wat;
                        if (r.contains(tInA) && !r.contains(-tInA)) {
                            whens.add(when);
                        }
                    } else {
                        byte[] ii = (byte[]) wat;
                        if (ArrayUtils.indexOf(ii, tInA) != -1 && ArrayUtils.indexOf(ii, (byte) -tInA) == -1) {
                            whens.add(when);
                        }
                    }
                });

                int ws = whens.size();
                if (ws == 0) {
                    return super.add(bt, what);
                }

                long theWhen;
                if (ws > 1) {
                    LongToLongFunction temporalDistance;
                    if (bt == ETERNAL) {
                        temporalDistance = (at) -> at == ETERNAL ? 0 : 1;
                    } else {
                        long finalBt = bt;
                        temporalDistance = (at) -> at != ETERNAL ? Math.abs(finalBt - at) : Long.MAX_VALUE;
                    }
                    long[] whensArray = whens.toArray();
                    ArrayUtils.sort(whensArray, temporalDistance);

                    theWhen = whensArray[whensArray.length - 1];
                } else {
                    theWhen = whens.get(0);
                }

                return super.add(Tense.dither(merge(theWhen, bt), nar), what);
            }

        }

        long merge(long x, long y) {
            if (x == y) return x;
            if (x == ETERNAL | y == ETERNAL)
                return ETERNAL;
            if (x == XTERNAL | y == XTERNAL)
                throw new RuntimeException("xternal in conjtermpolate");


            if (mergeOrChoose || Math.abs(x - y) < 2 /* 1 apart, then choose */) {
                if (Math.abs(x - y) > 1) {
                    return (x + y) / 2L;
                }
            }


            return rng.nextBoolean() ? x : y;
        }

    }


}
