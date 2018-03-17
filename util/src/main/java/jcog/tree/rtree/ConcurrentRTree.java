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

import jcog.tree.rtree.util.Stats;
import jcog.util.LambdaStampedLock;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Created by jcovert on 12/30/15.
 */
public class ConcurrentRTree<T> extends LambdaStampedLock implements Space<T> {

    private final Space<T> tree;

//  TODO move this to a subclass
//    final QueueLock<T> toAdd, toRemove;
//        if (async) {
//            toAdd = new QueueLock<>(new DisruptorBlockingQueue<T>(8), this::add);
//            toRemove = new QueueLock<>(new DisruptorBlockingQueue<T>(8), this::remove);
//        } else {
//            toAdd = toRemove = null;
//        }


    public ConcurrentRTree(Space<T> tree) {
        super();
        this.tree = tree;
    }

    @Override
    public Spatialization<T> model() {
        return tree.model();
    }

    /**
     * Blocking locked search
     *
     * @param rect - HyperRect to search
     * @param t    - array to hold results
     * @return number of entries found
     */
    @Override
    public int containedToArray(HyperRegion rect, T[] t) {
        return read(() -> tree.containedToArray(rect, t));
    }

    @Override
    public Node<T, ?> root() {
        return tree.root();
    }

    /**
     * Blocking locked add
     *
     * @param t - entry to add
     */
    @Override
    public boolean add(T t) {
        return write(() -> tree.add(t));
    }

//    @Override
//    public void intersectingNodes(HyperRegion start, Predicate<Node<T, ?>> eachWhile) {
//        readLock().lock();
//        try {
//            tree.intersectingNodes(start, eachWhile);
//        } finally {
//            readLock().unlock();
//        }
//    }

    /**
     * prefer this instead of add() in multithread environments, because it elides what might ordinarily involve a lock wait
     */
    @Override
    public void addAsync(T t) {
//        if (toAdd!=null)
//            toAdd.accept(t);
//        else
        add(t);
    }

    @Override
    public void removeAsync(T t) {
//        if (toRemove!=null)
//            toRemove.accept(t);
//        else
        remove(t);
    }


    @Override
    public boolean remove(T x) {
        return write(() -> tree.remove(x));
    }

    public void removeAll(Iterable<? extends T> t) {
        write(() -> t.forEach(this::remove));
    }


    public void read(Consumer<Space<T>> x) {
        read(() -> x.accept(tree));
    }

    /**
     * doesnt lock, use at your own risk
     */
    public void readDirect(Consumer<Space<T>> x) {
        x.accept(tree);
    }

    public void write(Consumer<Space<T>> x) {
        write(() -> x.accept(tree));
    }

    public void readOptimistic(Consumer<Space<T>> x) {
        readOptimistic(() -> {
            x.accept(tree);
        });
    }

    @Override
    public HyperRegion bounds(T task) {
        return tree.bounds(task);
    }


    /**
     * Blocking locked update
     *
     * @param told - entry to update
     * @param tnew - entry with new value
     */
    @Override
    public void replace(T told, T tnew) {
        write(() -> tree.replace(told, tnew));
    }

    @Override
    public int size() {
        return tree.size();
        //return readOptimistic(tree::size);
    }

    @Override
    public void clear() {
        write(tree::clear);
    }

    @Override
    public void forEach(Consumer<? super T> consumer) {

        read(() -> tree.forEach(consumer));
    }

    @Override
    public void whileEachContaining(HyperRegion rect, Predicate<T> t) {
        read(() -> tree.whileEachContaining(rect, t));
    }

    @Override
    public void whileEachIntersecting(HyperRegion rect, Predicate<T> t) {
        read(() -> tree.whileEachIntersecting(rect, t));
    }

    /**
     * warning: not locked
     */
    @Override
    public Stream<T> stream() {
        return root().stream().filter(Objects::nonNull);
    }

    /**
     * warning: not locked
     */
    @Override
    public Iterator<T> iterator() {
        return stream().iterator();
    }

    @Override
    public Stats stats() {
        return read(tree::stats);
    }

    @Override
    public String toString() {
        return tree.toString();
    }

    @Override
    public boolean contains(T t, HyperRegion b, Spatialization<T> model) {
        return read(() -> tree.contains(t, b, model));
    }

    @Override
    public boolean contains(T t) {
        return read(() -> tree.contains(t));
    }


}
