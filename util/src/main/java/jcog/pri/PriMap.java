/*
 * Copyright (c) 2017 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jcog.pri;

import jcog.Util;
import jcog.util.FloatFloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.ShortToShortFunction;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectFloatProcedure;
import org.eclipse.collections.api.map.primitive.ObjectShortMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.*;

import static jcog.Util.unitize;

/**
 * This file was automatically generated from template file objectPrimitiveHashMap.stg.
 *
 * @since 3.0.
 */
public class PriMap<K> {
    public static final short EMPTY_VALUE = (short) -1;

    private static final long serialVersionUID = 1L;

    private static final float OCCUPIED_DATA_RATIO = 2f;
    //private static final float LOAD_FACTOR = 0.5f;

    private static final int DEFAULT_INITIAL_CAPACITY = 8;

    private static final Object NULL_KEY = new Object() {
        
        public boolean equals(Object obj) {
            throw new RuntimeException("Possible corruption through unsynchronized concurrent modification.");
        }

        
        public int hashCode() {
            throw new RuntimeException("Possible corruption through unsynchronized concurrent modification.");
        }

        
        public String toString() {
            return "ObjectShortHashMap.NULL_KEY";
        }
    };

    protected static final Object REMOVED_KEY = new Object() {

        public boolean equals(Object obj) {
            throw new RuntimeException("Possible corruption through unsynchronized concurrent modification.");
        }


        public int hashCode() {
            throw new RuntimeException("Possible corruption through unsynchronized concurrent modification.");
        }


        public String toString() {
            return "ObjectShortHashMap.REMOVED_KEY";
        }
    };

    protected Object[] keys;
    protected short[] values;

    protected int size;
    private int occupiedWithSentinels;


    public PriMap() {
        this.resize(DEFAULT_INITIAL_CAPACITY << 1);
    }

    public PriMap(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("initial capacity cannot be less than 0");
        }
        int capacity = this.smallestPowerOfTwoGreaterThan(this.fastCeil(initialCapacity * OCCUPIED_DATA_RATIO));
        this.resize(capacity);
    }

//    public PriMap(ObjectShortMap<? extends K> map) {
//        this(Math.max(map.size(), DEFAULT_INITIAL_CAPACITY));
//        this.putAll(map);
//    }


    private int smallestPowerOfTwoGreaterThan(int n) {
        return n > 1 ? Integer.highestOneBit(n - 1) << 1 : 1;
    }

    private int fastCeil(float v) {
        int possibleResult = (int) v;
        if (v - possibleResult > 0.0F) {
            possibleResult++;
        }
        return possibleResult;
    }


//    public boolean equals(Object obj) {
//        if (this == obj) {
//            return true;
//        }
//
//        if (!(obj instanceof ObjectShortMap)) {
//            return false;
//        }
//
//        ObjectShortMap<K> other = (ObjectShortMap<K>) obj;
//
//        if (this.size() != other.size()) {
//            return false;
//        }
//
//        for (int i = 0; i < this.keys.length; i++) {
//            if (isNonSentinel(this.keys[i]) && (!other.containsKey((K) this.keys[i]) || this.values[i] != other.getOrThrow((K) this.keys[i]))) {
//                return false;
//            }
//        }
//        return true;
//    }


//    public int hashCode() {
//        int result = 0;
//
//        for (int i = 0; i < this.keys.length; i++) {
//            if (isNonSentinel(this.keys[i])) {
//                result += ((K) this.keys[i] == null ? 0 : this.keys[i].hashCode()) ^ (int) this.values[i];
//            }
//        }
//        return result;
//    }


    public String toString() {
        StringBuilder appendable = new StringBuilder();

        appendable.append("{");

        boolean first = true;

        for (int i = 0; i < this.keys.length; i++) {
            Object key = this.keys[i];
            if (isNonSentinel(key)) {
                if (!first) {
                    appendable.append(", ");
                }
                appendable.append((K) key).append("=").append(this.values[i]);
                first = false;
            }
        }
        appendable.append("}");

        return appendable.toString();
    }


    public int size() {
        return this.size;
    }


    public boolean isEmpty() {
        return this.size() == 0;
    }


//    public boolean notEmpty() {
//        return this.size() != 0;
//    }


//    public String makeString() {
//        return this.makeString(", ");
//    }


//    public String makeString(String separator) {
//        return this.makeString("", separator, "");
//    }


//    public String makeString(String start, String separator, String end) {
//        Appendable stringBuilder = new StringBuilder();
//        this.appendString(stringBuilder, start, separator, end);
//        return stringBuilder.toString();
//    }


//    public void appendString(Appendable appendable) {
//        this.appendString(appendable, ", ");
//    }
//
//
//    public void appendString(Appendable appendable, String separator) {
//        this.appendString(appendable, "", separator, "");
//    }
//
//
//    public void appendString(Appendable appendable, String start, String separator, String end) {
//        try {
//            appendable.append(start);
//
//            boolean first = true;
//
//            for (int i = 0; i < this.keys.length; i++) {
//                Object key = this.keys[i];
//                if (isNonSentinel(key)) {
//                    if (!first) {
//                        appendable.append(separator);
//                    }
//                    appendable.append(String.valueOf(String.valueOf(this.values[i])));
//                    first = false;
//                }
//            }
//            appendable.append(end);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    public short[] toArray() {
//        short[] result = new short[this.size()];
//        int index = 0;
//
//        for (int i = 0; i < this.keys.length; i++) {
//            if (isNonSentinel(this.keys[i])) {
//                result[index] = this.values[i];
//                index++;
//            }
//        }
//        return result;
//    }


    public void clear() {
        this.size = 0;
        this.occupiedWithSentinels = 0;
        Arrays.fill(this.keys, null);
        Arrays.fill(this.values, EMPTY_VALUE);
    }


    static final float resolution = Short.MAX_VALUE - 1;

    public static short shortPri(float p) {
//        assert (p == p);
//        return (short) Math.round(resolution * unitize(p));
//    }
//
//    public static short shortPriOrNeg1(float p) {
        if (p != p)
            return -1;
        else
            return clamp(Math.round(resolution * unitize(p)));
    }

    protected static short clamp(int x) {
        return (short) Util.clamp(x, -1, Short.MAX_VALUE);
    }

    public static float priShort(short p) {
        if (p < 0)
            return Float.NaN;
        else
            return ((float) p) / resolution;
    }

    public void set(K key, float pri) {
        set(key, shortPri(pri));
    }

    public void add(K key, float pri) {
        assert (pri == pri);
        addToValue(key, shortPri(pri));
    }

    protected void set(K key, short value) {
        int index = this.probe(key);

        Object ki = this.keys[index];
        if (isNonSentinel(ki) && nullSafeEquals((K) ki, key)) {
            // key already present in map
            this.values[index] = value;
            return;
        }

        this.addKeyValueAtIndex(key, value, index);
    }


    public void putAll(ObjectShortMap<? extends K> map) {
        map.forEachKeyValue(this::set);
    }


    protected boolean removeKey(K key) {
        int index = this.probe(key);
        return this.removeKeyAtIndex(key, index);
    }

    protected boolean removeKeyAtIndex(K key, int index) {
        Object ki = this.keys[index];
        if (isNonSentinel(ki) && nullSafeEquals((K) ki, key)) {
            this.occupiedWithSentinels++;
            this.keys[index] = REMOVED_KEY;
            this.values[index] = EMPTY_VALUE;
            this.size--;
//            if (this.occupiedWithSentinels / ((float)size) > OCCUPIED_DATA_RATIO) {
//                //rehash(keys.length); //clean sentinels
//            }
            return true;
        }
        return false;
    }


    public void remove(Object key) {
        this.removeKey((K) key);
    }


//    public short removeKeyIfAbsent(K key, short value) {
//        int index = this.probe(key);
//        if (isNonSentinel(this.keys[index]) && nullSafeEquals((K) this.keys[index], key)) {
//            this.keys[index] = REMOVED_KEY;
//            short oldValue = this.values[index];
//            this.values[index] = EMPTY_VALUE;
//            this.size--;
//            this.occupiedWithSentinels++;
//
//            return oldValue;
//        }
//        return value;
//    }


    public short getIfAbsentPut(K key, short value) {
        int index = this.probe(key);
        Object ki = this.keys[index];
        if (isNonSentinel(ki) && nullSafeEquals((K) ki, key)) {
            return this.values[index];
        }
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }


    public void updateValue(K key, float initialValueIfAbsent, FloatToFloatFunction function) {
        updateValue(key, shortPri(initialValueIfAbsent), (short v) -> shortPri(function.valueOf(priShort(v))));
    }

    public short updateValue(K key, short initialValueIfAbsent, ShortToShortFunction function) {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && nullSafeEquals((K) this.keys[index], key)) {
            this.values[index] = function.valueOf(this.values[index]);
            return this.values[index];
        }
        short value = function.valueOf(initialValueIfAbsent);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    private void addKeyValueAtIndex(@NotNull K key, short value, int index) {
        if (this.keys[index] == REMOVED_KEY) {
            --this.occupiedWithSentinels;
        }
        this.keys[index] = (key);
        this.values[index] = value;
        ++this.size;
        if (this.size + this.occupiedWithSentinels > this.maxOccupiedWithData()) {
        //if (this.size / ((float)keys.length) > LOAD_FACTOR) {
            this.rehashAndGrow();
        }
    }


    protected short addToValue(K key, short toBeAdded) {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && nullSafeEquals((K) this.keys[index], key)) {
            short v = this.values[index];
            if (v == -1)
                v = toBeAdded; //NaN -> value
            else
                v += toBeAdded;
            return (this.values[index] = v);
        }
        this.addKeyValueAtIndex(key, toBeAdded, index);
        return this.values[index];
    }

    protected interface ShortShortToShortFunction {
        short apply(short a, short b);
    }


    /**
     * previous value in high 16 bits, next value in low 16 bits
     */
    protected int update(K key, float incoming, FloatFloatToFloatFunction merge, Runnable beforeAdd) {
        return update(key, shortPri(incoming), (short v, short i) -> shortPri(merge.apply(priShort(v), priShort(i))), beforeAdd);
    }

    /**
     * previous value in high 16 bits, next value in low 16 bits
     */
    protected int update(K key, short incoming, ShortShortToShortFunction merge, @Nullable Runnable beforeAdd) {
        int index = this.probe(key);
        short v0, v;
        Object ki = this.keys[index];
        if (isNonSentinel(ki) && nullSafeEquals((K) ki, key)) {
            v0 = this.values[index];
            assert (v0 >= 0);
//            if (v0 < 0)
//                v = incoming; //NaN -> value
//            else {
            v = merge.apply(v0, incoming);
//            }
            if (v < 0) {
                removeKeyAtIndex(key, index);
                v = -1;
            } else {
                this.values[index] = v;
            }
        } else {
            if (beforeAdd != null)
                beforeAdd.run();
            this.addKeyValueAtIndex(key, incoming, index);
            v = incoming;
            v0 = -1;
        }
        return Util.intFromShorts(v0, v);
    }


    public float get(Object key) {
        return priShort(getShort(key));
    }

    protected short getShort(Object key) {
        return this.getIfAbsent(key, EMPTY_VALUE);
    }

    public short getIfAbsent(Object key, short ifAbsent) {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && nullSafeEquals((K) this.keys[index], key)) {
            return this.values[index];
        }
        return ifAbsent;
    }


    public boolean containsKey(Object key) {
        int index = this.probe(key);
        return isNonSentinel(this.keys[index]) && nullSafeEquals((K) this.keys[index], key);
    }

//
//    public boolean containsValue(short value) {
//        for (int i = 0; i < this.values.length; i++) {
//            if (isNonSentinel(this.keys[i]) && this.values[i] == value) {
//                return true;
//            }
//        }
//        return false;
//    }


//    public void forEach(ShortProcedure procedure) {
//        this.each(procedure);
//    }
//
//    /**
//     * @since 7.0.
//     */
//
//    public void each(ShortProcedure procedure) {
//        this.forEachValue(procedure);
//    }
//
//
//    public void forEachValue(ShortProcedure procedure) {
//        for (int i = 0; i < this.keys.length; i++) {
//            if (isNonSentinel(this.keys[i])) {
//                procedure.value(this.values[i]);
//            }
//        }
//    }
//

    public void forEachKey(Procedure<? super K> procedure) {
        for (int i = 0; i < this.keys.length; i++) {
            if (isNonSentinel(this.keys[i])) {
                procedure.value((K) this.keys[i]);
            }
        }
    }


    public void forEachKeyValue(ObjectFloatProcedure<? super K> procedure) {
        for (int i = 0; i < this.keys.length; i++) {
            if (isNonSentinel(this.keys[i])) {
                procedure.value((K) this.keys[i], priShort(this.values[i]));
            }
        }
    }


    /**
     * Rehashes every element in the set into a new backing table of the smallest possible size and eliminating removed sentinels.
     */
    public void compact() {
        this.rehash(this.smallestPowerOfTwoGreaterThan(this.size()));
    }

    private void rehashAndGrow() {
        this.rehash(this.keys.length << 1);
    }

    protected void rehash(int newCapacity) {
        int oldLength = this.keys.length;
        Object[] old = this.keys;
        short[] oldValues = this.values;
        this.resize(newCapacity);
        this.size = 0;
        this.occupiedWithSentinels = 0;

        for (int i = 0; i < oldLength; i++) {
            Object oi = old[i];
            if (isNonSentinel(oi)) {
                this.set((K) oi, oldValues[i]);
            }
        }
    }

    // exposed for testing
    int probe(Object element) {
        assert (element != REMOVED_KEY);
        final int index = this.spread(element);

        int removedIndex = -1;
        final Object ki = this.keys[index];
        if (isRemovedKey(ki)) {
            removedIndex = index;
        } else if (ki == null || nullSafeEquals(toNonSentinel(this.keys[index]), element)) {
            //} else if (ki == null || nullSafeEquals((K) ki, element)) {
            return index;
        }

        int nextIndex = index;
        int probe = 17;

        // loop until an empty slot is reached
        while (true) {
            // Probe algorithm: 17*n*(n+1)/2 where n = no. of collisions
            nextIndex += probe;
            probe += 17;
            nextIndex &= this.keys.length - 1;

            Object kni = this.keys[nextIndex];
            if (isRemovedKey(kni)) {
                if (removedIndex == -1) {
                    removedIndex = nextIndex;
                }
            } else if (nullSafeEquals(this.toNonSentinel(kni), element)) {
            //} else if (nullSafeEquals((K) kni, element)) {
                return nextIndex;
            } else if (kni == null) {
                return removedIndex == -1 ? nextIndex : removedIndex;
            }
        }
    }

    private K toNonSentinel(Object key) {
        return key == NULL_KEY ? null : (K) key;
    }

    // exposed for testing
    int spread(Object element) {
        // This function ensures that hashCodes that differ only by
        // constant multiples at each bit position have a bounded
        // number of collisions (approximately 8 at default load factor).
        int h = element == null ? 0 : element.hashCode();
        h ^= h >>> 20 ^ h >>> 12;
        h ^= h >>> 7 ^ h >>> 4;
        return h & (this.keys.length - 1);
    }

    private static boolean nullSafeEquals(Object value, Object other) {
        if (value == null) {
            if (other == null) {
                return true;
            }
        } else if (other == value || value.equals(other)) {
            return true;
        }
        return false;
    }

    private void resize(int sizeToAllocate) {
        this.keys = new Object[sizeToAllocate];
        this.values = new short[sizeToAllocate];
        Arrays.fill(values, EMPTY_VALUE);
    }

    private static boolean isRemovedKey(Object key) {
        return key == REMOVED_KEY;
    }

    protected static <K> boolean isNonSentinel(K key) {
        //return /*key != null && */!isRemovedKey(key);
        return key != null && key != REMOVED_KEY;
    }

    private int maxOccupiedWithData() {
        int capacity = this.keys.length;
        // need at least one free slot for open addressing
        return Math.round(Math.min(capacity - 1, capacity / OCCUPIED_DATA_RATIO));
    }

//    private int maxOccupiedWithSentinels() {
//        return this.keys.length / OCCUPIED_SENTINEL_RATIO;
//    }


    private class KeySet implements Set<K> {

        public boolean equals(Object obj) {
            if (obj instanceof Set) {
                Set<?> other = (Set<?>) obj;
                if (other.size() == this.size()) {
                    return this.containsAll(other);
                }
            }
            return false;
        }


        public int hashCode() {
            int hashCode = 0;
            Object[] table = PriMap.this.keys;
            for (int i = 0; i < table.length; i++) {
                Object key = table[i];
                if (PriMap.isNonSentinel(key)) {
                    K nonSentinelKey = (K) key;
                    hashCode += nonSentinelKey.hashCode();
                }
            }
            return hashCode;
        }


        public int size() {
            return PriMap.this.size();
        }


        public boolean isEmpty() {
            return PriMap.this.isEmpty();
        }


        public boolean contains(Object o) {
            return PriMap.this.containsKey(o);
        }


        public Object[] toArray() {
            int size = PriMap.this.size();
            Object[] result = new Object[size];
            this.copyKeys(result);
            return result;
        }


        public <T> T[] toArray(T[] result) {
            int size = PriMap.this.size();
            if (result.length < size) {
                result = (T[]) Array.newInstance(result.getClass().getComponentType(), size);
            }
            this.copyKeys(result);
            if (size < result.length) {
                result[size] = null;
            }
            return result;
        }


        public boolean add(K key) {
            throw new UnsupportedOperationException("Cannot call add() on " + this.getClass().getSimpleName());
        }


        public boolean remove(Object key) {
            int oldSize = PriMap.this.size();
            PriMap.this.removeKey((K) key);
            return oldSize != PriMap.this.size();
        }


        public boolean containsAll(Collection<?> collection) {
            for (Object aCollection : collection) {
                if (!PriMap.this.containsKey(aCollection)) {
                    return false;
                }
            }
            return true;
        }


        public boolean addAll(Collection<? extends K> collection) {
            throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
        }


        public boolean retainAll(Collection<?> collection) {
            int oldSize = PriMap.this.size();
            Iterator<K> iterator = this.iterator();
            while (iterator.hasNext()) {
                K next = iterator.next();
                if (!collection.contains(next)) {
                    iterator.remove();
                }
            }
            return oldSize != PriMap.this.size();
        }


        public boolean removeAll(Collection<?> collection) {
            int oldSize = PriMap.this.size();
            for (Object object : collection) {
                PriMap.this.removeKey((K) object);
            }
            return oldSize != PriMap.this.size();
        }


        public void clear() {
            PriMap.this.clear();
        }


        public Iterator<K> iterator() {
            return new KeySetIterator();
        }

        private void copyKeys(Object[] result) {
            int count = 0;
            for (int i = 0; i < PriMap.this.keys.length; i++) {
                Object key = PriMap.this.keys[i];
                if (PriMap.isNonSentinel(key)) {
                    result[count++] = PriMap.this.keys[i];
                }
            }
        }
    }

    private class KeySetIterator implements Iterator<K> {
        private int count;
        private int position;
        private K currentKey;
        private boolean isCurrentKeySet;


        public boolean hasNext() {
            return this.count < PriMap.this.size();
        }


        public K next() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            }
            this.count++;
            Object[] keys = PriMap.this.keys;
            while (!isNonSentinel(keys[this.position])) {
                this.position++;
            }
            this.currentKey = (K) PriMap.this.keys[this.position];
            this.isCurrentKeySet = true;
            this.position++;
            return (K) this.currentKey;
        }


        public void remove() {
            throw new UnsupportedOperationException();
//            if (!this.isCurrentKeySet) {
//                throw new IllegalStateException();
//            }
//
//            this.isCurrentKeySet = false;
//            this.count--;
//
//            if (isNonSentinel(this.currentKey)) {
//                int index = this.position - 1;
//                PriMap.this.removeKeyAtIndex((K) this.currentKey, index);
//            } else {
//                PriMap.this.removeKey(this.currentKey);
//            }
        }
    }
}
