package jcog.bag.impl;

import jcog.pri.Prioritized;
import jcog.pri.Priority;
import jcog.pri.op.PriMerge;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class PriArrayBag<X extends Priority> extends ArrayBag<X,X> {

    public PriArrayBag(PriMerge mergeFunction, @NotNull Map<X, X> map) {
        super(mergeFunction, map);
    }

    public PriArrayBag(PriMerge mergeFunction, @NotNull Map<X, X> map, int cap) {
        this(mergeFunction, map);
        setCapacity(cap);
    }

    @Nullable
    @Override public X key(@NotNull X k) {
        return k;
    }
}
