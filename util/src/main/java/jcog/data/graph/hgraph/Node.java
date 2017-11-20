/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */
package jcog.data.graph.hgraph;

import com.google.common.collect.Iterators;
import jcog.list.ArrayIterator;
import org.apache.commons.lang3.ArrayUtils;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 *
 * @author Thomas Wuerthinger
 */
public class Node<N, E> {


    /** buffers a lazily updated array-backed cache of the values
     * for fast iteration and streaming */
    public static class FastIteratingHashSet<X> extends LinkedHashSet<X> {

        Object[] cache = ArrayUtils.EMPTY_OBJECT_ARRAY;


        @Override
        public boolean add(X x) {
            if (super.add(x)) {
                cache = null;
                return true;
            }
            return false;
        }

        @Override
        public boolean remove(Object o) {
            if (super.remove(o)) {
                cache = null;
                return true;
            }
            return false;
        }


        protected int update() {
            int s = size();
            if (cache == null) {
                if (s == 0) {
                    cache = ArrayUtils.EMPTY_OBJECT_ARRAY;
                } else {
                    cache = new Object[s];
                    Iterator<X> xx = super.iterator();
                    int i = 0;
                    while (xx.hasNext())
                        cache[i++] = xx.next();
                }
            }
            return s;
        }

        @Override
        public void clear() {
            super.clear();
            cache = ArrayUtils.EMPTY_OBJECT_ARRAY;
        }

        @Override
        public Iterator<X> iterator() {
            switch (update()) {
                case 0:
                    return Collections.emptyIterator();
                case 1:
                    return Iterators.singletonIterator((X)cache[0]);
                default:
                    return new ArrayIterator(cache);
            }
        }

        @Override
        public Stream<X> stream() {
            switch (update()) {
                case 0:
                    return Stream.empty();
                case 1:
                    return Stream.of((X)cache[0]);
                default:
                    return Stream.of(cache).map(x -> (X) x);
            }
        }
    }

    private final static AtomicInteger serials = new AtomicInteger(1);

    public final N id;
    public final int serial, hash;
    private final Collection<Edge<N, E>> inEdges;
    private final Collection<Edge<N, E>> outEdges;

    protected Node(N id) {
        this.serial = serials.getAndIncrement();
        this.id = id;
        this.inEdges =
                new FastIteratingHashSet<>();
                //new HashSet<>();
        this.outEdges =
                //new HashSet<>();
                new FastIteratingHashSet<>();
        this.hash = id.hashCode();
    }

    public Stream<Edge<N, E>> edges(boolean in, boolean out) {
        if (out && !in) return out();
        else if (!out && in) return in();
        else return Stream.concat(out(), in());
    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    public final int ins() {
        return ins(true);
    }

    public int ins(boolean countSelfLoops) {
        if (countSelfLoops) {
            return (int) in().count();
        } else {
            return (int) in().filter(e -> e.from!=this).count();
        }
    }

    public int outs() {
        return (int) out().count();
    }


    protected boolean inAdd(Edge<N, E> e) {
        return inEdges.add(e);
    }

    protected boolean outAdd(Edge<N, E> e) {
        return outEdges.add(e);
    }

    protected boolean inRemove(Edge<N, E> e) {
        //assert inEdges.contains(e);
        return inEdges.remove(e);
    }

    protected boolean outRemove(Edge<N, E> e) {
        //assert outEdges.contains(e);
        return outEdges.remove(e);
    }

    public Stream<Edge<N, E>> in() {
        return (inEdges.stream());
    }

    public Stream<Edge<N, E>> out() {
        return (outEdges.stream());
    }

    public Stream<N> successors() {
        return out().map(e -> e.to.id);
    }
    public Stream<N> predecessors() {
        return in().map(e -> e.from.id);
    }

    @Override
    public String toString() {
        return id.toString();
    }

    public void print(PrintStream out) {
        out.println(id);
        out().forEach(e -> {
           out.println("\t" + e);
        });
    }


}
