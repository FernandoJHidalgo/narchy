package jcog.version;

import org.jetbrains.annotations.NotNull;

/**
 * versioning context that holds versioned instances
 * a maximum stack size is provided at construction and will not be exceeded
 */
public class Versioning<X>
        //FastList<Versioned<X>> {
        /*FasterList<Versioned<X>>*/ {

    private final Versioned[] items;
    protected int size = 0;

    public int ttl;

    public Versioning(int stackMax, int initialTTL) {
        //super(0, new Versioned[stackMax]);
        this.items = new Versioned[stackMax];
        assert(stackMax > 0);
        setTTL(initialTTL);
    }

    @NotNull
    @Override
    public String toString() {
        return size + ":" + super.toString();
    }


    public final boolean revertLive(int to) {
        if (live()) {
            revert(to);
            return true;
        }
        return false;
    }

    /**
     * reverts/undo to previous state
     * returns whether any revert was actually applied
     */
    public final boolean revert(int when) {

        int s = size;
        if (s<=when || s == 0)
            return true;

        int c = s - when;
        final Versioned<X>[] i = this.items;

        while (c-- > 0) {
            Versioned<X> x = i[--s];
            if (x!=null) {
                x.pop();
                i[s] = null;
            }
        }

        this.size = s;
        assert(s == when);
        return true;
    }


    public void clear() {
        revert(0);
    }

    public final boolean add(/*@NotNull*/ Versioned<X> newItem) {
        Versioned<X>[] ii = this.items;
        if (ii.length > this.size) {
            ii[this.size++] = newItem; //cap
            return true;
        }
        return false;
    }

    /** returns remaining TTL */
    public final void stop() {
        setTTL(0);
    }

//    public final int getAndStop() {
//        int t = ttl;
//        stop();
//        return t;
//    }



//    /**
//     * empty for subclass impl
//     */
//    public void onDeath() {
//
//    }

    /**
     * whether the unifier should continue: if TTL is non-zero.
     */
    public final boolean live() {
        return ttl > 0;
    }

    public final void setTTL(int ttl) {
        this.ttl = ttl;
    }
}
