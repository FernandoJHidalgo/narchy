package jcog.bag.util;

import jcog.bag.Bag;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Random;
import java.util.function.Consumer;

/**
 * proxies to a delegate bag

 * TODO find any inherited methods which would return the proxied
 * bag instead of this instance
 */
public class ProxyBag<X,Y> implements Bag<X,Y> {

    Bag<X,Y> bag;

    public ProxyBag(Bag<X, Y> delegate) {
        set(delegate);
    }

    public final void set(Bag<X,Y> delegate) {
        bag = delegate;
    }

    @Override
    public float pri(Y key) {
        return bag.pri(key);
    }

    @Override
    public X key(Y value) {
        return bag.key(value);
    }


    @Override
    public @Nullable Y get(Object key) {
        return bag.get(key);
    }

    @Override
    public int capacity() {
        return bag.capacity();
    }

    @Override
    public void forEach(Consumer<? super Y> action) {
        bag.forEach(action);
    }

    @Override
    public void forEachKey(Consumer<? super X> each) {
        bag.forEachKey(each);
    }


    @Override
    public void clear() {
        bag.clear();
    }

    @Nullable
    @Override
    public Y remove(X x) {
        return bag.remove(x);
    }

    @Override
    public Y put(Y b, @Nullable MutableFloat overflowing) {
        return bag.put(b, overflowing);
    }


    @NotNull
    @Override
    public Iterable<Y> sample(Random rng, Bag.BagCursor<? super Y> each) {
        bag.sample(rng, each);
        return this;
    }

    @Override
    public int size() {
        return bag.size();
    }

    @NotNull
    @Override
    public Iterator<Y> iterator() {
        return bag.iterator();
    }


    @Override
    public void setCapacity(int c) {
        bag.setCapacity(c);
    }

    @Override
    public float priMax() {
        return bag.priMax();
    }

    @Override
    public float priMin() {
        return bag.priMin();
    }


    @NotNull
    @Override
    public Bag<X,Y> commit(Consumer<Y> update) {
        bag.commit(update);
        return this;
    }



    @Override
    public void onAdd(Y v) {
        bag.onAdd(v);
    }

    @Override
    public void onRemove(Y v) {
        bag.onRemove(v);
    }

    @Override
    public void onReject(Y v) {
        bag.onReject(v);
    }

    @Override
    public Iterable<Y> commit() {
        bag.commit();
        return this;
    }

    @Override
    public float mass() {
        return bag.mass();
    }
}
