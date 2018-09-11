package spacegraph.space2d.container.grid;

import jcog.data.map.CellMap;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.AbstractMutableContainer;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

abstract public class MutableMapContainer<K, V> extends AbstractMutableContainer {

    /** "cells" of the container; maintain the mapping between the indexed keys and their "materialized" representations or projections  */
    protected final CellMap<K, V> cells = new CellMap<>() {
        @Override
        protected CacheCell<K, V> newCell() {
            return new SurfaceCacheCell<>();
        }

        @Override
        protected final void unmaterialize(CacheCell<K, V> entry) {
            V v = entry.value;
            MutableMapContainer.this.unmaterialize(v);
            super.unmaterialize(entry);
        }
        //        @Override
//        protected void added(CacheCell<K, V> entry) {
//            if (parent == null)
//                return;
//
//            Surface es = ((SurfaceCacheCell) entry).surface;
//            //if (es != null && es.parent == null)
//            es.start(MutableMapContainer.this);
//        }


        @Override
        protected void invalidated() {
            super.invalidated();
            invalidate();
        }
    };

    protected void unmaterialize(V v) {

    }


    @Override
    public void forEach(Consumer<Surface> each) {
        cells.forEachCell(e -> {
            if (e == null)
                throw new NullPointerException();

            Surface s = ((SurfaceCacheCell) e).surface;
            if (s != null)
                each.accept(s);
        });
    }


//    public void forEachVisible(Consumer<Surface> each) {
//        forEach(x -> {
//            if (x.visible())
//                each.accept(x);
//        });
//    }
//
//    public void forEachKeySurface(BiConsumer<? super K, Surface> each) {
//        cellMap.forEachCell((cell) -> {
//            Surface ss = ((SurfaceCacheCell) cell).surface;
//            if (ss != null)
//                each.accept(cell.key, ss);
//        });
//    }

    public void forEachValue(Consumer<? super V> each) {
        cells.forEachValue(each);
    }


    @Override
    public int childrenCount() {
        return Math.max(1, cells.size());
    }

    @Override
    protected void clear() {
        cells.clear();
    }

    protected void removeAll(Iterable<K> x) {
        cells.removeAll(x);
    }

    @Nullable
    public V getValue(K x) {
        return cells.getValue(x);
    }

    public CellMap.CacheCell<K, V> compute(K key, Function<V, V> builder) {
        return cells.compute(key, builder);
    }

    CellMap.CacheCell put(K key, V nextValue, BiFunction<K, V, Surface> renderer) {

        CellMap.CacheCell entry = cells.cache.computeIfAbsent(key, k -> cells.cellPool.get());
        return cells.update(key, entry, ((SurfaceCacheCell) entry).update(key, nextValue, renderer));

    }


    public boolean remove(K key) {
        return cells.remove(key);
    }

    public boolean removeSilently(K key) {
        return cells.removeSilently(key);
    }

    void invalidate() {
        cells.cache.invalidate();
    }

    public void getValues(Collection<V> l) {
        cells.getValues(l);
    }

    @Override
    public boolean whileEach(Predicate<Surface> o) {
        return cells.whileEach(e -> {
            Surface s = ((SurfaceCacheCell) e).surface;
            return s == null || o.test(s);
        });
    }

    @Override
    public boolean whileEachReverse(Predicate<Surface> o) {
        return cells.whileEachReverse(e -> {
            Surface s = ((SurfaceCacheCell) e).surface;
            return s == null || o.test(s);
        });
    }


    public static class SurfaceCacheCell<K, V> extends CellMap.CacheCell<K, V> {

        transient volatile Surface surface = null;

        @Override
        protected void set(V next) {

            if (next instanceof Surface)
                surface = (Surface) next;

            super.set(next);
        }

        @Override
        public void clear() {
            super.clear();

            Surface es = this.surface;
            surface = null;
//            if (es != null) {
//                es.stop();
////                es.hide();
//            }

        }

        /**
         * return true to keep or false to remove from the map
         */
        boolean update(K nextKey, V nextValue, BiFunction<K, V, Surface> renderer) {

//            if (this.key!=nextKey && this.key!=null) {
//                clear();
//            }
            if (nextValue == value)
                return true;

            this.key = nextKey;

            Surface existingSurface = surface;

            boolean create = false, delete = false;

            if (existingSurface != null) {
                if (nextValue == null) {
                    delete = true;
                } else {
                    if (value!=nextValue)  { //Objects.equals(value, nextValue)

                    } else {
                        create = true;
                    }
                }
                if (delete || create) {

                    existingSurface.stop();
                }
            } else {
                if (nextValue != null)
                    create = true;
                else
                    delete = true;
            }

            if (delete) {
                return false;
            } else if (create) {
                this.surface = renderer.apply(key, this.value = nextValue);
            }

            return true;
        }

    }
}
