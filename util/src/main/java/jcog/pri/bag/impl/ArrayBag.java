package jcog.pri.bag.impl;

import jcog.Util;
import jcog.WTF;
import jcog.data.NumberX;
import jcog.data.atomic.AtomicFloatFieldUpdater;
import jcog.data.list.FasterList;
import jcog.data.list.table.SortedListTable;
import jcog.pri.Prioritized;
import jcog.pri.Priority;
import jcog.pri.ScalarValue;
import jcog.pri.bag.Bag;
import jcog.pri.bag.Sampler;
import jcog.pri.op.PriMerge;
import jcog.sort.SortedArray;
import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;


/**
 * A bag implemented as a combination of a Map and a SortedArrayList
 * TODO extract a version of this which will work for any Prioritized, not only BLink
 */
abstract public class ArrayBag<X, Y extends Priority> extends SortedListTable<X, Y> implements Bag<X, Y> {
    private static final AtomicFloatFieldUpdater<ArrayBag> MASS =
            new AtomicFloatFieldUpdater(ArrayBag.class, "mass");
    private static final AtomicFloatFieldUpdater<ArrayBag> PRESSURE =
            new AtomicFloatFieldUpdater(ArrayBag.class, "pressure");

    final PriMerge mergeFunction;

    private volatile int mass, pressure;

    protected ArrayBag(PriMerge mergeFunction, int capacity) {
        this(mergeFunction,
                //new HashMap<>(capacity, 0.99f)
                new UnifiedMap<>(capacity, 0.99f)
        );
        setCapacity(capacity);
    }

    protected ArrayBag(PriMerge mergeFunction, Map<X, Y> map) {
        this(0, mergeFunction, map);
    }

    protected ArrayBag(@Deprecated int cap, PriMerge mergeFunction, Map<X, Y> map) {
        super(new SortedArray(), map);
        this.mergeFunction = mergeFunction;
        setCapacity(cap);


    }

    /**
     * gets the scalar float value used in a comparison of BLink's
     * essentially the same as b.priIfFiniteElseNeg1 except it also includes a null test. otherwise they are interchangeable
     */
    static float pCmp(@Nullable Object b) {
        return b == null ? -2f : ((Prioritized) b).priElseNeg1();
    }


    @Override
    public float mass() {
        return MASS.getOpaque(this);
    }

    @Override
    public final float floatValueOf(Y y) {
        return -pCmp(y);
    }

    @Override
    public Stream<Y> stream() {
        int s = size();
        if (s == 0) return Stream.empty();
        else {
            Object[] x = items.array();
            return IntStream.range(0, Math.min(s, x.length)).mapToObj(i -> (Y) x[i]).filter(y -> y != null && !y.isDeleted());
        }
    }

    /**
     * returns whether the capacity has changed
     */
    @Override
    public final void setCapacity(int nextCapacity) {

        //TODO use atomic field setter
        if (nextCapacity != this.capacity) {
            synchronized (items) {
                if (nextCapacity != this.capacity) {

                    this.capacity = nextCapacity;

                    if (size() > nextCapacity)
                        commit(null);
                }
            }

        }
    }

    /**
     * WARNING this is a duplicate of code in hijackbag, they ought to share this through a common Pressure class extending AtomicDouble or something
     */
    @Override
    public float depressurize() {
        return Math.max((float) 0, PRESSURE.getAndZero(this));
    }

    @Override
    public void pressurize(float f) {
        PRESSURE.add(this, f);
    }

    /**
     * returns true unless failed to add during 'add' operation or becomes empty
     * call within synchronized
     *
     * @return List of trash items
     * trash must be removed from the map, outside of critical section
     * may include the item being added
     */
    private boolean tryInsertFull(Y toAdd, float toAddPri, @Nullable Consumer<Y> update, boolean commit, final FasterList<Y> trash) {

        int s = cleanIfFull() ? clean(trash, update, commit /*|| (s == capacity) && get(0) instanceof PLinkUntilDeleted*/) : size();

        int c = capacity();

        if (s + 1 <= c) {

            //space cleared for the new item

            int i = items.add(toAdd, this);
            assert (i >= 0);
            return true;

        } else {

            if (toAddPri > priMin()) {

                Y removed = items.removeLast();

                int i = items.add(toAdd, this);
                assert (i >= 0);

                trash.add(removed);

                return true;
            }
        }

        return false;
    }

    /**
     * allows an implementation to remove items which may have been deleted (by anything) since commit checked for them
     */
    protected boolean cleanIfFull() {
        return false;
    }

    protected void sort(int from /* inclusive */, int to /* inclusive */) {
        items.sort(ArrayBag::pCmp, from, to);
    }

    @Override
    public final float priUpdate(Y key) {
        return key.priCommit();
    }

    private int clean(List<Y> trash, @Nullable Consumer<Y> update, boolean commit) {

//        float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY, mass = 0;


        SortedArray items2 = this.items;
        final Object[] l = items2.array();

        float above = Float.POSITIVE_INFINITY;
        int mustSort = -1;
        int s = size();
        for (int i = 0; i < s; ) {
            Y y = (Y) l[i];
            assert y != null;
            float p = commit ? priUpdate(y) : pri(y);
            if (update != null && p == p) {
                update.accept(y);
                p = pri(y);
            }
            if (p == p) {
//                min = Math.min(min, p);
//                max = Math.max(max, p);
                mass += p;
                if (p - above >= ScalarValue.EPSILON)
                    mustSort = i;

                above = p;
                i++;
            } else {
                trash.add(y);
                items2.removeFast(i);
                s--;
                //dont increment i
            }
        }

        ArrayBag.MASS.set(this, mass);


        int c = capacity;

        while (s > c) {
            trash.add(this.items.removeLast());
            s--;
        }


        if (mustSort != -1)
            sort(0, Math.min(s, mustSort));

        return s;
    }

    /**
     * chooses a starting index randomly then iterates descending the list
     * of items. if the sampling is not finished it restarts
     * at the top of the list. so for large amounts of samples
     * it will be helpful to call this in batches << the size of the bag.
     */

    @Override
    public void sample(Random rng, Function<? super Y, SampleReaction> each) {

        newItemsArray:
        while (true) {
            Object[] ii;
            int s;
            int i = -1;
            while ((s = Math.min((ii = items.array()).length, size())) > 0) {

                if (i < 0) {
                    i = sampleStart(rng, s);
                } else {
                    i = sampleNext(rng, s, i);
                }

                Object x = ii[i];

                if (x != null) {
                    Y y = (Y) x;
                    float yp = priUpdate(y);
                    if (yp != yp) {
                        remove(y, i); //deleted, remove
                    } else {

                        SampleReaction next = each.apply(y);

                        if (next.remove)
                            remove(y, i);

                        if (next.stop)
                            return;
                    }
                }

            }

            return;
        }

    }

    /**
     * size > 0
     */
    protected static int sampleStart(@Nullable Random rng, int size) {
        assert (size > 0);
        if (size == 1 || rng == null)
            return 0;
        else {
//            float min = this.priMin();
//            float max = this.priMax();
//            float diff = max - min;
//            if (diff > Prioritized.EPSILON * size) {
            float i = rng.nextFloat();

            //i = Util.lerp(diff, i /* flat */, (i * i) /* curved */);
            i = (i * i);

            return Util.clamp(0, Math.round(i * (size - 1)), size - 1);
//            } else {
//                return random.nextInt(size);
//            }
        }
    }

    protected static int sampleNext(@Nullable Random rng, int size, int i) {
        if (rng == null) {
            if (++i >= size)
                i = 0;

            return i;
        } else {
            float runLength = 3;
            float restartProb = (1f / (1 + runLength));
            if (rng.nextFloat() < restartProb) {
                return sampleStart(rng, size);
            }

            if (--i >= 0)
                return i; //decrease toward high end
            else
                return sampleStart(rng, size);
        }

    }

    @Nullable
    @Override
    public Y remove(X x) {
        Y removed;
        synchronized (items) {
            removed = super.remove(x);
        }
        if (removed != null) {
            removed(removed);
        }
        return removed;
    }

    @Nullable
    private void remove(Y y, int suspectedPosition) {
        boolean removed;
        synchronized (items) {
            if (items.get(suspectedPosition) == y) {
                items.remove(suspectedPosition);
                mapRemove(y);
                removed = true;
            } else {
                removed = false;
            }
        }
        if (removed) {
            removed(y); //outside of synch, call removed
        } else {
            remove(key(y)); //wasnt found with provided index, use standard method by key
        }
    }

    @Override
    public Y put(final Y incoming, final NumberX overflow) {

        final int capacity = this.capacity;

        if (capacity == 0) {
            incoming.delete();
            return null;
        }

        float p = incoming.priElseZero();
        pressurize(p);


        //HACK special case for saving a lot of unnecessary work when merge=Max
        //TODO may also work with average and replace merges
        //TODO this can only work if the bag is sorted
        if (this.mergeFunction == PriMerge.max && fastMergeMaxReject() && isFull()) {
            if (p < priMin()) {
                return null; //fast drop the novel task due to insufficient priority
                //TODO feedback the min priority necessary when capacity is reached, and reset to no minimum when capacity returns
            }
        }

        X key = key(incoming);


        boolean inserted;

        @Nullable FasterList<Y> trash;

        synchronized (items) {

            Y existing = getExisting(key);

            if (existing != null) {
                if (existing != incoming) {
                    return merge(existing, incoming, overflow);
                } else {
                    if (overflow != null)
                        overflow.add(p);
                    return incoming; //exact same instance
                }
            } /* else { ...*/

            int s = size();

            if (s >= capacity) {

                trash = new FasterList<>(4);

                inserted = tryInsertFull(incoming, p, null, false, trash);

            } else {
                int i = items.add(incoming, -p, this);
                assert i >= 0;

                inserted = true;
                trash = null;
            }

            if (inserted) {
                MASS.add(this, p);
                map.put(key, incoming);
            }

            /*
            inserted = map.compute(key, (kk, existing) -> {
                Y v;
                if (existing != null) {
                    if (existing != incoming) {
                        v = merge(existing, incoming, overflow);
                    } else {
                        if (overflow != null)
                            overflow.add(p);
                        v = existing;
                    }
                } else {
                    if (insert(incoming, trash)) {
                        v = incoming;
                    } else {
                        v = null;
                    }
                }
                return v;
            });
            */


//            trash.removeIf(x -> {
//                if (x != incoming) {
//                    mapRemove(x);
//                    return false; //keep
//                }
//                return true; //exclude from trash
//            });

            if (trash != null && !trash.isEmpty())
                trash.forEach(this::mapRemove);
            else
                trash = null;

            //assert(map.size()==items.size());
        }


        if (trash != null) //outside synch
            trash.forEach(this::removed);

        if (!inserted) {

            onReject(incoming);
            if (overflow != null)
                overflow.add(p);
            incoming.delete();

            return null;

        } else {

            onAdd(incoming);

            return incoming;
        }

    }

    protected boolean fastMergeMaxReject() {
        return false;
    }

    protected Y getExisting(X key) {
        return map.get(key);
    }

    /**
     * will not need to be sorted after calling this; the index is automatically updated
     */
    private Y merge(Y existing, Y incoming, @Nullable NumberX overflow) {


        int posBefore = items.indexOf(existing, this);
        if (posBefore == -1) {
//            //try harder: compare by keys, even if the value refuse to respond true to equals()
//            X ki = key(incoming);
//            int s = size();
//            for (int i = 0; i < s; i++) {
//                if (ki.equals(key(items.get(i)))) {
//                    posBefore = i;
//                    break;
//                }
//            }
//            if (posBefore == -1)
            throw new RuntimeException("Bag Map and List became unsynchronized: " + existing + " not found");
        }

        float priBefore = existing.priCommit();
        Y result;
        float delta;

        float oo = merge(existing, incoming);
        float priAfter = existing.pri();
        if (priAfter != priAfter) {
            throw new WTF("incoming deleted existing"); //just for detection but maybe helpful in some merge fucntions
        }

        delta = existing.priElseZero() - priBefore;
        if (overflow != null)
            overflow.add(oo);

        result = existing;


        if (Math.abs(delta) >= ScalarValue.EPSILON) {
            items.adjust(posBefore, this);

            MASS.add(this, delta);
        }

        incoming.delete();

        return result;
    }

    private float merge(Y existing, Y incoming) {
        return mergeFunction.merge(existing, incoming);
    }

    private Y mapRemove(Y x) {
        Y removed = map.remove(key(x));
        if (removed == null)
            throw new WTF();
        return removed;
    }

    @Override
    public Bag<X, Y> commit(Consumer<Y> update) {



        /*if ((update != null && s > 0) || (update == null && s > capacity))*/
        {
            @Nullable FasterList<Y> trash = new FasterList(4);
            synchronized (items) {

                clean(trash, update, true);

                trash.forEach(this::mapRemove);
            }


            trash.forEach(this::removed);
        }

        return this;
    }

    private void removed(Y y) {
        MASS.add(this, -priElse(y, 0));
        onRemove(y);
        y.delete();
    }

    @Override
    public final void clear() {
        clear(this::removed);
        MASS.zero(this);
        depressurize();
    }

    public final void clear(Consumer<? super Y> each) {
        clear(-1, each);
    }

    /**
     * removes the top n items
     *
     * @param n # to remove, if -1 then all are removed
     */
    public final void clear(int n, Consumer<? super Y> each) {

        assert (n != 0);

        Collection<Y> popped = new FasterList<>(n > 0 ? Math.min(n, size()) : size());

        popBatch(n, popped);

        if (popped != null)
            popped.forEach(each::accept);

    }

    @Override
    public Sampler<Y> pop(Random rng, int max, Consumer<? super Y> each) {
        if (rng == null) {
            //high-efficiency non-random pop
            clear(max, each);
            return this;
        } else {
            return Bag.super.pop(rng, max, each);
        }
    }


    public Sampler<Y> popBatch(int n, Collection<Y> popped) {

        popped.clear();
        synchronized (items) {

            int s = size();
            if (s > 0) {

                int toRemove = n == -1 ? s : Math.min(s, n);

                items.removeRange(0, toRemove, (e) -> popped.add(mapRemove(e)));

            }
        }

        return this;
    }

    @Override
    public float pri(Y key) {
        return key.pri();
    }

    @Override
    public void forEachKey(Consumer<? super X> each) {
        forEach(x -> each.accept(key(x)));
    }

    @Override
    public void forEach(Consumer<? super Y> action) {


        int s = size();
        if (s <= 0)
            return;

        //synchronized (items) {
        s = size();
        Object[] yy = items.array();
        List<ObjectIntPair<Y>> removals = null;
        for (int i = 0; i < s; i++) {
            Object y0 = yy[i];
            if (y0 == null)
                continue; //throw new WTF();

            Y y = (Y) y0;
            float p = pri(y);
            if (p == p) {
                action.accept(y);
            } else {
                if (removals==null)
                    removals = new LinkedList();
                removals.add(pair(y, i));
            }
        }
        if (removals!=null) {
            for (ObjectIntPair<Y> r : removals) {
                remove(r.getOne(), r.getTwo());
            }
        }


    }


    @Override
    public String toString() {
        return super.toString() + '{' + items.getClass().getSimpleName() + '}';
    }

    @Override
    public float priMax() {
        Y x = items.first();
        return x != null ? priElse(x, -1) : 0;
    }

    @Override
    public float priMin() {
        Y x = items.last();
        return x != null ? priElse(x, -1) : 0;
    }

//    private static final class SortedPLinks extends SortedArray {
////        @Override
////        protected Object[] newArray(int s) {
////            return new Object[s == 0 ? 2 : s + Math.max(1, s / 2)];
////        }
//
////        @Override
////        protected int grow(int oldSize) {
////            return super.grow(oldSize);
////        }
//
//        @Override
//        protected boolean grows() {
//            return false;
//        }
//    }


}


































