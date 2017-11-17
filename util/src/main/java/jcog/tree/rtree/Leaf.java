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

import jcog.Util;
import jcog.tree.rtree.util.CounterNode;
import jcog.tree.rtree.util.Stats;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Node that will contain the data entries. Implemented by different type of SplitType leaf classes.
 * <p>
 * Created by jcairns on 4/30/15.
 * <p>
 * TODO just extend FastList<>
 */
public class Leaf<T> implements Node<T, T> {

    public final T[] data;
    public short size;
    public HyperRegion region;

    protected Leaf(int mMax) {
        this((T[]) new Object[mMax]);
    }

    protected Leaf(T[] arrayInit) {
        this.region = null;
        this.data = arrayInit;
        this.size = 0;
    }

    @Override
    public Stream<T> stream() {
        return streamNodes();
    }

    @Override
    public T get(int i) {
        return data[i];
    }

    public double variance(int dim, Spatialization<T> model) {
        int s = size();
        if (s < 2)
            return 0;
        double mean = bounds().center(dim);
        double sumDiffSq = 0;
        for (int i = 0; i < s; i++) {
            T c = get(i);
            if (c == null) continue;
            double diff = model.region(c).center(dim) - mean;
            sumDiffSq += diff * diff;
        }
        return sumDiffSq / s - 1;
    }


    @Override
    public Node<T, ?> add(/*@NotNull*/ final T t, Nodelike<T> parent, /*@NotNull*/ Spatialization<T> model, boolean[] added) {

        boolean ctm = contains(t, model);
        if (parent != null && !ctm) {
            Node<T, ?> next;

            if (size < model.max) {
                final HyperRegion tRect = model.region(t);
                region = region != null ? region.mbr(tRect) : tRect;

                data[size++] = t;

                next = this;
            } else {
                next = model.split(t, this);
            }

            added[0] = true;

            return next;
        } else {

            return (parent == null && ctm) ? null : this;
        }
    }


    @Override
    public boolean AND(Predicate<T> p) {
        T[] data = this.data;
        for (int i = 0; i < size; i++)
            if (!p.test(data[i]))
                return false;
        return true;
    }

    @Override
    public boolean OR(Predicate<T> p) {
        T[] data = this.data;
        for (int i = 0; i < size; i++)
            if (p.test(data[i]))
                return true;
        return false;
    }

    @Override
    public boolean contains(T t, Spatialization<T> model) {
        if (size == 0 || (size > 1 && !region.contains(model.region(t))))
            return false;

        T[] data = this.data;
        final int s = size;
        for (int i = 0; i < s; i++) {
            T d = data[i];
            if (t == d || t.equals(d)) {
                model.merge(d, t);
                return true;
            }
        }
        return false;
    }


    @Override
    public Node<T, ?> remove(final T t, HyperRegion xBounds, Nodelike<T> parent, Spatialization<T> model, boolean[] removed) {


//        while (i < size && (data[i] != t) && (!data[i].equals(t))) {
//            i++;
//        }
//
//        int j = i;
//
//        while (j < size && ((data[j] == t) || data[j].equals(t))) {
//            j++;
//        }

        final int size = this.size;
        if (size == 0)
            return this;
        T[] data = this.data;
        int i;
        for (i = 0; i < size; i++) {
            T d = data[i];
            if (t == d || t.equals(d))
                break; //found
        }
        if (i == size)
            return this; //not found

        final int j = i + 1;
        if (j < size) {
            final int nRemaining = size - j;
            System.arraycopy(data, j, data, i, nRemaining);
            Arrays.fill(data, size - 1, size, null);
        } else {
            Arrays.fill(data, i, size, null);
        }

        this.size -= 1;
        removed[0] = true;

        region = this.size > 0 ? HyperRegion.mbr(model.region, data, this.size) : null;

        return this;

    }

//    @Override
//    public double perimeter(Spatialization<T> model) {
//        double maxVolume = 0;
//        final int s = size;
//        T[] data = this.data;
//        for (int i = 0; i < s; i++) {
//            T c = data[i];
//            double vol = model.perimeter(c);
//            if (vol > maxVolume)
//                maxVolume = vol;
//        }
//        return maxVolume;
//    }


    @Override
    public Node<T, ?> update(final T told, final T tnew, Spatialization<T> model) {
        final int s = size;
        if (s <= 0)
            return this;

        T[] data = this.data;
        HyperRegion r = this.region;
        for (int i = 0; i < s; i++) {
            if (data[i].equals(told)) {
                data[i] = tnew;
            }

            r = i == 0 ? model.region(data[0]) : r.mbr(model.region(data[i]));
        }

        this.region = r;

        return this;
    }


    @Override
    public boolean intersecting(HyperRegion rect, Predicate<T> t, Spatialization<T> model) {
        short s = this.size;
        if (s > 0 && rect.intersects(region)) {
            T[] data = this.data;
            for (int i = 0; i < s; i++) {
                T d = data[i];
                if (rect.intersects(model.region(d)) && !t.test(d))
                    return false;
            }
        }
        return true;
    }

    @Override
    public boolean containing(HyperRegion rect, Predicate<T> t, Spatialization<T> model) {
        short s = this.size;
        if (s > 0 && rect.intersects(region)) { //not sure why but it seems this has to be intersects and not contains
            T[] data = this.data;
            for (int i = 0; i < s; i++) {
                T d = data[i];
                if (rect.contains(model.region(d)) && !t.test(d))
                    return false;
            }
        }
        return true;
    }

//    @Override
//    public void intersectingNodes(/*@NotNull */HyperRegion rect, Predicate<Node<T, ?>> t, Spatialization<T> model) {
//        if (size > 0 && (region.intersects(rect) || region.contains(rect)))
//            t.test(this);
//    }


    @Override
    public int size() {
        return size;
    }


    @Override
    public boolean isLeaf() {
        return true;
    }

    /*@NotNull*/
    @Override
    public HyperRegion bounds() {
        return region;
    }


    @Override
    public void forEach(Consumer<? super T> consumer) {
        for (int i = 0; i < size; i++) {
            T d = data[i];
            if (d != null)
                consumer.accept(d);
        }
    }


    @Override
    public void collectStats(Stats stats, int depth) {
        if (depth > stats.getMaxDepth()) {
            stats.setMaxDepth(depth);
        }
        stats.countLeafAtDepth(depth);
        stats.countEntriesAtDepth(size, depth);
    }

    /**
     * Figures out which newly made leaf node (see split method) to add a data entry to.
     *
     * @param l1Node left node
     * @param l2Node right node
     * @param t      data entry to be added
     * @param model
     */
    public final void transfer(final Node<T, T> l1Node, final Node<T, T> l2Node, final T t, Spatialization<T> model) {

        final HyperRegion tRect = model.region(t);
        double tCost = tRect.cost();

        final HyperRegion l1Region = l1Node.bounds();
        final HyperRegion l1Mbr = l1Region.mbr(tRect);
        double l1c = l1Mbr.cost();
        final double l1CostInc = Math.max(l1c - (l1Region.cost() + tCost), 0.0);

        final HyperRegion l2Region = l2Node.bounds();
        final HyperRegion l2Mbr = l2Region.mbr(tRect);
        double l2c = l2Mbr.cost();
        final double l2CostInc = Math.max(l2c - (l2Region.cost() + tCost), 0.0);

        Node<T, T> target;
        if (Util.equals(l1CostInc, l2CostInc, RTree.EPSILON)) {
            if (Util.equals(l1c, l2c, RTree.EPSILON)) {
                final double l1MbrMargin = l1Mbr.perimeter();
                final double l2MbrMargin = l2Mbr.perimeter();
                if (Util.equals(l1MbrMargin, l2MbrMargin, RTree.EPSILON)) {
                    // break tie by preferring the smaller smaller
                    target = ((l1Node.size() <= l2Node.size()) ? l1Node : l2Node);
                } else if (l1MbrMargin <= l2MbrMargin) {
                    target = l1Node;
                } else {
                    target = l2Node;
                }
            } else {
                if (l1c <= l2c) {
                    target = l1Node;
                } else {
                    target = l2Node;
                }
            }
        } else {
            if (l1CostInc <= l2CostInc) {
                target = l1Node;
            } else {
                target = l2Node;
            }
        }

        boolean[] added = new boolean[1];
        target.add(t, this, model, added);
        assert (added[0]);
    }

    @Override
    public Node<T, Object> instrument() {
        return new CounterNode<>(this);
    }

    @Override
    public String toString() {
        return "Leaf" + '{' + region + 'x' + size + '}';
    }
}
