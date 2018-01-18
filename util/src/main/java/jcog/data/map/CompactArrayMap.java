package jcog.data.map;

import jcog.TODO;

import java.util.Arrays;
import java.util.function.Function;


public class CompactArrayMap<K, V>  {

    Object[] items = null;

    public CompactArrayMap() {
    }

    public boolean containsValue(Object aValue) {
        throw new TODO();
    }

    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    public V get(Object key) {
        Object[] a = items;
        if (a!=null) {
            int s = a.length;
            for (int i = 0; i < s; ) {
                Object k = a[i];
                if (k!=null) {
                    if (keyEquals(k, key))
                        return (V) a[i + 1];
                }
                i += 2;
            }
        }
        return null;
    }

    public int size() {
        Object[] i = this.items;
        return i.length/2;
    }

    public void put(K key, V value) {
        synchronized (this) {
            Object[] a = items;
            if (a == null) {
                this.items = new Object[] { key, value };
            } else {
                int s = a.length;
                for (int i = 0; i < s; ) {
                    if (keyEquals(a[i], key)) {
                        a[i + 1] = value; //directly modify
                        return;
                    }
                    i += 2;
                }
                a = Arrays.copyOf(a, s+2);
                a[s++] = key;
                a[s] = value;
                this.items = a;
            }
        }
    }

    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        V e = get(key);
        if (e!=null)
            return e;

        V v = mappingFunction.apply(key);
        synchronized (this) {
            put(key, v);
            return v;
        }
    }



    public boolean remove(Object object) {
        throw new UnsupportedOperationException("use removeKey");
    }

    public V removeKey(Object key) {
        throw new TODO();
//        int i = indexOf(key);
//        if (i != -1) {
//
//        }
//        return null;
    }

    /** override for alternate equality test */
    boolean keyEquals(Object a, Object b) {
        return a.equals(b);
    }


    public void clear() {
        //synchronized(this) {
            items = null;
        //}
    }

    public void clearExcept(K key) {
        //synchronized(this) {
            V exist = get(key);
            clear();
            if (exist!=null)
                put(key, exist);
        //}
    }

    public void clearPut(K key, V value) {
        //synchronized(this) {
            clear();
            if (value!=null)
                put(key, value);
        //}
    }
}
