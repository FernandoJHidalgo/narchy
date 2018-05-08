package jcog.tree.rtree;

        /*
         * #%L
         * Conversant RTree
         * ~~
         * Conversantmedia.com © 2016, Conversant, Inc. Conversant® is a trademark of Conversant, Inc.
         * ~~
         * Licensed under the Apache License, Version 2.0 (the "License");
         * you may not use this file except in compliance with the License.
         * You may obtain a copy of the License at
         *
         *      http://www.apache.org/licenses/LICENSE-2.0
         *
         * Unless required by applicable law or agreed to in writing, software
         * distributed under the License is distributed on an "AS IS" BASIS,
         * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
         * See the License for the specific language governing permissions and
         * limitations under the License.
         * #L%
         */


import jcog.tree.rtree.util.CounterNode;
import jcog.tree.rtree.util.Stats;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * <p>Data structure to make range searching more efficient. Indexes multi-dimensional information
 * such as geographical coordinates or rectangles. Groups information and represents them with a
 * minimum bounding rectangle (mbr). When searching through the tree, any query that does not
 * intersect an mbr can ignore any data entries in that mbr.</p>
 * <p>More information can be @see <a href="https://en.wikipedia.org/wiki/R-tree">https://en.wikipedia.org/wiki/R-tree</a></p>
 * <p>
 * Created by jcairns on 4/30/15.</p>
 */
public class RTree<T> implements Space<T> {

    private static final AtomicIntegerFieldUpdater size = AtomicIntegerFieldUpdater.newUpdater(RTree.class, "_size");

    private volatile Node<T, ?> root;

    /** size counter
     *  volatile for concurrent implementations
     */
    private volatile int _size;

    public final Spatialization<T> model;

    public RTree(@Nullable Function<T, HyperRegion> spatialize, final int mMin, final int mMax, final Split<T> splitType) {
        this(new Spatialization<>(spatialize, splitType, mMin, mMax));
    }

    public RTree(Spatialization<T> model) {
        this.model = model;
        clear();
    }

    @Override
    public Stream<T> stream() {
        return root.stream();
    }

    @Override
    public Iterator<T> iterator() {
        return root.iterator();
    }

    @Override
    public final void clear() {
        size.updateAndGet(this, (sizeBeforeClear)->{
            if (sizeBeforeClear > 0 || root == null)
                this.root = model.newLeaf();
            return 0;
        });
    }

    @Override
    public boolean OR(Predicate<T> o) {
        return root().OR(o);
    }

    @Override
    public boolean AND(Predicate<T> o) {
        return root().AND(o);
    }

    /**
     * TODO handle duplicate items (ie: dont increase entryCount if exists)
     */
    @Override
    public boolean add(/*@NotNull*/ final T t) {
//        int before = size;

        boolean[] added = new boolean[1];
        Node<T, ?> nextRoot = root.add(t, this, model, added);
        if (nextRoot != null) {
            this.root = nextRoot;
            if (added[0]) {
                size.incrementAndGet(this);
                return true;
            }
        }
        return false; //duplicate or merged
    }


    @Override
    public Spatialization<T> model() {
        return model;
    }

    /**
     * @param xBounds - the bounds of t which may not necessarily need to be the same as the bounds as model might report it now; for removing a changing value
     */
    @Override
    public boolean remove(final T x) {
        int before = _size;
        if (before == 0)
            return false;
        boolean[] removed = new boolean[1];
        root = root.remove(x, model.bounds(x), model, removed);
        if (removed[0]) {
            size.decrementAndGet(this);
            return true;
        }
        return false;
    }

    @Override
    public boolean replace(final T told, final T tnew) {
        if (told == tnew) {
            return true;
            //throw new UnsupportedOperationException("equivalent instance");
        }

        if (model.bounds(told).equals(model.bounds(tnew))) {
            //same bounds, so replace inline
            root = root.replace(told, tnew, model);
            return true;
        } else {
            //different bounds, so remove first then add
            boolean removed = remove(told);
            if (!removed) {
                return false;
            } else {
                boolean added = add(tnew);
                if (!added)
                    throw new UnsupportedOperationException("error adding " + tnew);
                return true;
            }
        }
    }

//    /**
//     * returns whether or not the HyperRect will enclose all of the data entries in t
//     *
//     * @param rect HyperRect to contain entries
//     * @param t    Data entries to be evaluated
//     * @return Whether or not all entries lie inside rect
//     */
//    public boolean contains(final HyperRect rect, final T... t) {
//        for (int i = 0; i < t.length; i++) {
//            if (!rect.contains(spatialize.apply(t[i]))) {
//                return false;
//            }
//        }
//        return true;
//    }

    /**
     * @return number of data entries stored in the RTree
     */
    @Override
    public int size() {
        return size.get(this);
    }

//    static boolean isEqual(final double a, final double b, final double eps) {
//        return Math.abs(a - b) <= ((Math.abs(Math.abs(a) < Math.abs(b) ? b : a)) * eps);
//    }

    @Override
    public void forEach(Consumer<? super T> consumer) {
        root.forEach(consumer);
    }

    @Override
    public void whileEachIntersecting(HyperRegion rect, Predicate<T> t) {
        root.intersecting(rect, t, model);
    }

    @Override
    public void whileEachContaining(HyperRegion rect, final Predicate<T> t) {
        root.containing(rect, t, model);
    }

    /**
     * returns how many items were filled
     */
    @Override
    public int containedToArray(HyperRegion rect, final T[] t) {
        final int[] i = {0};
        root.containing(rect, (x) -> {
            t[i[0]++] = x;
            return i[0] < t.length;
        }, model);
        return i[0];
    }

    public Set<T> containedToSet(HyperRegion rect) {
        int s = size();
        Set<T> t = new HashSet(s);
        root.containing(rect, x -> {
            t.add(x);
            return true;
        }, model);
        return t;
    }


    void instrumentTree() {
        root = root.instrument();
        CounterNode.searchCount = 0;
        CounterNode.bboxEvalCount = 0;
    }

    @Override
    public Stats stats() {
        Stats stats = new Stats();
        stats.setType(model);
        stats.setMaxFill(model.max);
        stats.setMinFill(model.min);
        root.collectStats(stats, 0);
        return stats;
    }


//    @Override
//    public void intersectingNodes(HyperRegion start, Predicate<Node<T, ?>> eachWhile) {
//        if (size == 0)
//            return;
//        root.intersectingNodes(start, eachWhile, model);
//    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[size=" + size() + ']';
    }

    @Override
    public Node<T, ?> root() {
        return this.root;
    }

    @Override
    public boolean contains(T t, HyperRegion b, Spatialization<T> model) {
        return root.contains(t, b, model);
    }

    public boolean contains(T t) {
        return contains(t, model.bounds(t), model);
    }

    @Override
    public HyperRegion bounds(T x) {
        return model.bounds(x);
    }

}
