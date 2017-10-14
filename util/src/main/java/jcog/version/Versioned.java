package jcog.version;

import jcog.list.FasterList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Maintains a versioned snapshot history (stack) of a changing value.
 * Managed by a Versioning context
 */
public class Versioned<X> extends
        //FastList<X>
        FasterList<X>
        //LimitedFasterList<X> //not supported yet
{


    @NotNull
    protected final Versioning context;

//    public Versioned(@NotNull Versioned<X> copy) {
//        super(copy);
//        this.context = copy.context;
//    }

    public Versioned(@NotNull Versioning sharedContext, int cap) {
        super(cap);
        this.context = sharedContext;
    }

    @Override
    public final boolean equals(Object otherVersioned) {
        return this == otherVersioned;
    }

//    boolean revertNext(int count) {
//        int p = size - count;
//        if (p >= 0) {
//            popTo(p);
//            return true;
//        }
//        return false;
//    }



    /**
     * gets the latest value
     */
    @Nullable
    public final X get() {
        int s = this.size;
        return s > 0 ? this.items[s - 1] : null;
    }


    /**
     * sets thens commits
     * returns null if the capacity was hit, or some other error
     */
    @Nullable
    public Versioned<X> set(@NotNull X nextValue) {
//        if (context.add(this) && add(nextValue)) {
//            System.out.println("set: " + nextValue);
//            return this;
//        } else return null;
        return context.add(this) && add(nextValue) ? this : null;
    }



    @Override
    public final String toString() {
        X v = get();
        if (v != null)
            return v.toString();
        return "null";
    }

    public final String toStackString() {
        StringBuilder sb = new StringBuilder("(");
        int s = size();
        for (int i = 0; i < s; i++) {
            //sb.append('(');
            sb.append(get(i));
            //sb.append(')');
            if (i < s - 1)
                sb.append(", ");
        }
        return sb.append(')').toString();

    }

    public final X getAndPop() {
        X x = items[--size];
        items[size+1] = null;
        return x;
    }

    public void pop() {

            //if (size > 0) { //HACK wtf would it be empty
//                Object removed =
//                    items[--size];

        items[--size] = null;

//                if (removed == null) {
//                    throw new NullPointerException();
//                }
            //}

    }

    /** transparently replace the leading value */
    public void replaceTop(X newValue) {
        items[size-1] = newValue;
    }

}
