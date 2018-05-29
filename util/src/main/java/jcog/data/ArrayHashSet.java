/*
 * Copyright (c) 2013, SRI International
 * All rights reserved.
 * Licensed under the The BSD 3-Clause License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 * http://opensource.org/licenses/BSD-3-Clause
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * Redistributions of source code must retain the above copyright
 * notice, this arrayList of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright
 * notice, this arrayList of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * Neither the name of the aic-expresso nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jcog.data;

import jcog.list.FasterList;
import org.eclipse.collections.impl.set.immutable.AbstractImmutableSet;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.*;
import java.util.function.Consumer;

/**
 * Analogous to {@link java.util.LinkedHashSet}, but with an {@link java.util.ArrayList} instead of a {@link java.util.LinkedList},
 * offering the same advantages (random access) and disadvantages (slower addition and removal of elements),
 * but with the extra advantage of offering an iterator that is actually a {@link java.util.ListIterator}.
 *
 * @param <X> the type of the elements
 *            <p>
 *            from: https://github.com/aic-sri-international/aic-util/blob/master/src/main/java/com/sri/ai/util/collect/ArrayHashSet.java
 * @author braz
 */
public class ArrayHashSet<X> extends AbstractSet<X> implements ArraySet<X> {

    public static ArrayHashSet EMPTY = new ArrayHashSet(0) {
        @Override
        public boolean add(Object element) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object first() {
            return null;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public ListIterator listIterator() {
            return Collections.emptyListIterator();
        }

        @Override
        public ListIterator listIterator(int index) {
            assert (index == 0);
            return Collections.emptyListIterator();
        }

        @Override
        public Iterator iterator() {
            return Collections.emptyIterator();
        }
    };
    public final FasterList<X> list;
    private Set<X> set = Set.of();

//	final static List EMPTY_LIST = Collections.emptyList();

    public ArrayHashSet() {
        this(4);
    }

    public ArrayHashSet(int capacity) {
//		this.set  =
//				new HashSet<>(capacity, 0.9f);
//				//new UnifiedSet<>(capacity);
        this.list =
                //EMPTY_LIST; //new ArrayList<E>(capacity);
                new FasterList<>(capacity);
    }

    public ArrayHashSet(Collection<X> collection) {
        this();
        collection.forEach(this::add);
    }

    // ArraySet methods

    public static <X> ArrayHashSet<X> of(X... x) {
        ArrayHashSet a = new ArrayHashSet(x.length);
        Collections.addAll(a, x);
        return a;
    }

    @Override
    public void forEach(Consumer<? super X> action) {
        list.forEach(action);
    }

    @Override
    public ListIterator<X> listIterator() {
        return new ArrayHashSetIterator();
    }

    @Override
    public ListIterator<X> listIterator(int index) {
        return new ArrayHashSetIterator(index);
    }

    public boolean OR(org.eclipse.collections.api.block.predicate.Predicate<? super X> test) {
        return list.anySatisfy(test);
    }

    public boolean AND(org.eclipse.collections.api.block.predicate.Predicate<? super X> test) {
        return list.allSatisfy(test);
    }

//	@Override
//	public void set(int index, X element) {
//		ArrayHashSet<X>.ArrayHashSetIterator listIterator = listIterator(index);
//		listIterator.next();
//		listIterator.set(element);
//	}

    // end of ArraySet methods

    // required implementations

    @Override
    public X get(int index) {
        return list.get(index);
    }

    @Override
    public boolean add(X element) {
        switch (list.size()) {
            case 0: //upgrade from immutable empty to small set
                set = new UnifiedSet(1);
                set.add(element);
                list.add(element);
                return true;
        }

        //default:
        if (set.add(element)) {
            list.add(element);
            return true;
        }
        return false;
    }

    @Override
    public Iterator<X> iterator() {
        return new ArrayHashSetIterator();
    }

    // end of required implementations

    // methods not required to be implemented, but more efficient

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean contains(Object o) {
        return set.contains(o);
    }

    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return list.toArray(a);
    }

    @Override
    public boolean remove(Object o) {
        boolean removed = set.remove(o);
        if (removed) {
            list.remove(o);

            switch (size()) {
                case 0:
                    set = Set.of();
                    break; //downgrade to immutable empty set
            }
        }

        return removed;
    }

//	@Override
//	public boolean isEmpty() {
//		return list== EMPTY_LIST;
//	}

    @Override
    public void clear() {
//		if (list != EMPTY_LIST) {
//			list = EMPTY_LIST;
        if (list.clearIfChanged()) {
            //set.clear();
            set = Set.of();
        }
//		}
    }



    @Override
    public X remove(Random random) {
        int s = size();
        if (s == 0) return null;
        int index = s == 1 ? 0 : random.nextInt(s);
        X removed;
        remove(removed = list.remove(index));
        return removed;
    }

    @Override
    public void shuffle(Random random) {
        Collections.shuffle(list, random);
    }


    // end of methods not required to be implemented, but more efficient

    private class ArrayHashSetIterator implements ListIterator<X> {

        private final ListIterator<X> arrayListIterator;
        private X lastElementProvided;

        public ArrayHashSetIterator() {
            this(-1);
        }

        public ArrayHashSetIterator(int index) {
            this.arrayListIterator = index == -1 ? list.listIterator() : list.listIterator(index);
        }

        @Override
        public boolean hasNext() {
            return arrayListIterator.hasNext();
        }

        @Override
        public X next() {
            return lastElementProvided = arrayListIterator.next();
        }

        @Override
        public void add(X element) {
            if (set.add(element)) {
                arrayListIterator.add(element);
            }
        }

        @Override
        public boolean hasPrevious() {
            return arrayListIterator.hasPrevious();
        }

        @Override
        public int nextIndex() {
            return arrayListIterator.nextIndex();
        }

        @Override
        public X previous() {
            return lastElementProvided = arrayListIterator.previous();
        }

        @Override
        public int previousIndex() {
            return arrayListIterator.previousIndex();
        }

        @Override
        public void remove() {
            if (set instanceof AbstractImmutableSet)
                set = new UnifiedSet(set);
            boolean removed = set.remove(lastElementProvided);
            assert (removed);

            arrayListIterator.remove();

//			if (list.isEmpty())
//				list = EMPTY_LIST;
        }

        @Override
        public void set(X element) {
            if (element.equals(lastElementProvided)) {
                // no need to do anything
            } else {
                if (set.contains(element)) {
                    // cannot add because element would appear more than once
                    throw new IllegalArgumentException("Cannot set already-present element in a different position in ArrayHashSet.");
                } else {
                    arrayListIterator.set(element);
                    set.remove(lastElementProvided);
                    set.add(element);
                }
            }
        }
    }
}
