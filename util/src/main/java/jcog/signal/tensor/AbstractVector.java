package jcog.signal.tensor;

import jcog.util.ArrayUtils;

/** 1D tensor */
public abstract class AbstractVector extends AbstractTensor {

    @Override
    public abstract int volume();

    @Override
    public int[] stride() {
        return ArrayUtils.EMPTY_INT_ARRAY;
    }

    @Override
    public int[] shape() {
        return new int[volume()];
    }

}
