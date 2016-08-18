package nars.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;


/**
 * adapter to a Map for coordinating changes in a Map with another Collection
 */
public abstract class CollectorMap<K, V> {

    @NotNull
    public final Map<K, V> map;

    protected CollectorMap(@NotNull Map<K, V> map) {
        this.map = map;
    }

    @Nullable
    abstract public K key(@NotNull V v);

    @Override
    public String toString() {
        return map.toString();
    }

    /**
     * returns an object that stores the items so that it can be synchronized upon
     */
    abstract protected Object _items();

    /**
     * implementation for adding the value to another collecton (called internally)
     * returns null if successful, non-null if an item was displaced it will be that item
     */
    @Nullable
    protected abstract V addItem(@NotNull V e);

    /**
     * implementation for removing the value to another collecton (called internally)
     */
    @Nullable
    protected abstract V removeItem(@NotNull V e);

    public final void forEach(@NotNull BiConsumer<K, V> each) {
        map.forEach(each);
    }


//    @Nullable
//    public V putIfAbsent(K key, V value) {
//        V existing = putKeyIfAbsent(key, value);
//        if (existing != null) {
//            return existing;
//        }
//        addItem(value);
//        return null;
//    }


//    @Nullable
//    public V put(@NotNull K key, @NotNull V value) {
//
//
//        V removed = map.put(key, value);
//
//        V displaced;
//        synchronized (_items()) {
//            displaced = mergeList(key, value, removed);
//        }
//
//        if (displaced!=null) {
//            removeKeyForValue(displaced);
//        }
//        return displaced;
//
//    }

//    /** the key of the displaced item needs to be removed from the table sometime after calling this */
//    public @Nullable V mergeList(@NotNull K key, @NotNull V value, @Nullable V removed) {
//
//        if (removed != null) {
//
//            if (removed == value) {
//                //rejected input
//                return value;
//            } else {
//                //displaced other
//                V remd;
//                synchronized (_items()) {
//                    remd = removeItem(removed);
//                }
//                if (remd == null)
//                    throw new RuntimeException("unable to remove item corresponding to key " + key);
//
//            }
//        }
//
//        V displaced;
//        synchronized (_items()) {
//            displaced = addItem(value);
//        }
//
//        if (displaced != null) { //&& (!key(removed2).equals(key))) {
//            if (removed != null && removed != displaced) {
//                throw new RuntimeException("Only one item should have been removed on this insert; both removed: " + removed + ", " + displaced);
//            }
//            removed = displaced;
//        }
//
//        return removed;
//    }

    @Nullable
    public V remove(@NotNull K x) {

        synchronized (_items()) {
            V e = map.remove(x);

            if (e != null) {
                V removed;
                removed = removeItem(e);
                //            if (removed == null) {
                //                /*if (Global.DEBUG)
                //                    throw new RuntimeException(key + " removed from index but not from items list");*/
                //                //return null;
                //            }

                if (removed!=null && removed != e) {
                    throw new RuntimeException(x + " removed " + e + " but item removed was " + removed);
                }
                return removed;
            }
        }

        return null;
    }


//    /** does a more exhaustive removal in case the BLink no longer has the key (ex: weakref) */
//    protected final void removeKey(BLink<V> item) {
//        boolean removed = map.values().remove(item); //removeIf((v)->(v==item));
//        if (!removed)
//            throw new RuntimeException("Bag fault while trying to remove key by item value");
//    }

    protected final @Nullable V removeKeyForValue(@NotNull V value) {
        @Nullable K key = key(value);
        return key != null ? map.remove(key) : null;
    }


//    public final boolean containsValue(V it) {
//        return map.containsValue(it);
//    }

    public void clear() {
        map.clear();
    }

    @Nullable
    public final V get(@NotNull Object key) {
        return map.get(key);
    }

    public final V merge(@NotNull K key, @NotNull V value, @NotNull BiFunction<? super V, ? super V, ? extends V> c) {
        return map.merge(key, value, c);
    }

    public final V compute(@NotNull K key, @NotNull BiFunction<? super K, ? super V, ? extends V> c) {
        return map.compute(key, c);
    }


    public boolean containsKey(@NotNull K name) {
        return map.containsKey(name);
    }

    @NotNull
    public Set<K> keySet() {
        return map.keySet();
    }

    @NotNull
    public Collection<V> values() {
        return map.values();
    }


//    /**
//     * put key in index, do not add value
//     */
//    protected final V putKey(K key, V value) {
//        return map.put(key, value);
//    }
//    public final V putKeyIfAbsent(K key, V value) {
//        return map.putIfAbsent(key, value);
//    }


}
