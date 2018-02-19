package jcog.math.tensor;

import com.google.common.base.Joiner;
import com.google.common.collect.AbstractIterator;
import jcog.Texts;
import jcog.util.FloatFloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.procedure.primitive.IntFloatProcedure;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;
import java.util.function.Supplier;

public interface Tensor extends Supplier<float[]> {

    static Tensor vectorFromTo(int start, int end) {

        return vectorFromToBy(start, end, 1);
    }

    static Tensor vectorFromToBy(int start, int end, int steps) {

        int elements = (end - start + steps) / steps;
        float[] values = new float[elements];

        for (int i = 0; i < elements; i++)
            values[i] = start + i * steps;

        return new ArrayTensor((float[])values);
    }

    static Tensor empty(int dimension) {
        return vectorOf(0, dimension);
    }

    static Tensor vectorOf(float value, int dimension) {
        float[] values = new float[dimension];
        Arrays.fill(values, value);
        return new ArrayTensor(values);
    }

    static Tensor forEach(Tensor vector, FloatToFloatFunction operator) {
        return new TensorFunc(vector, operator);
    }

    static Tensor logEach(Tensor vector) {
        return forEach(vector, d -> (float)Math.log(d));
    }

    static Tensor sqrtEach(Tensor vector) {
        return forEach(vector, d -> (float)Math.sqrt(d));
    }

    static Tensor powEach(Tensor vector, double power) {
        return forEach(vector, d -> (float)Math.pow(d, power));
    }

    /*static float[] copyVectorValues(Tensor vector) {
        return vector.snapshot();
    }*/

    /** element-wise addition */
    default TensorFunc add(float v) {
        return apply((x) -> x + v);
    }

    /** element-wise multiplication */
    default TensorFunc scale(float v) {
        return apply((x) -> x * v);
    }

    default TensorTensorFunc func(Tensor x, FloatFloatToFloatFunction f)  {
        return new TensorTensorFunc(this, x, f);
    }

    /** element-wise addition */
    default Tensor add(Tensor vector) {
        return func(vector, (a,b)->a+b);
    }

    /** element-wise multiplication */
    default Tensor scale(Tensor vector) {
        return func(vector, (a,b)->a*b);
    }



    static Tensor normalize(Tensor vector) {
        return vector.scale(1f / sum(vector));
    }

    static float sum(Tensor vector) {
        return vector.sum();
    }

    static Tensor randomVector(int dimension, float min, float max) {
        final Random random = new Random();
        return forEach(new ArrayTensor(new float[dimension]),
                        d -> (float)random.nextDouble() * (max - min) + min);
    }

    static Tensor randomVectorGauss(int dimension, float mean, float standardDeviation, Random random) {
        return forEach(new ArrayTensor(dimension),
                        d -> (float)random.nextGaussian() * standardDeviation + mean);
    }

    @Override
    default float[] get() {
        return snapshot();
    }

    float get(int... cell);

    float get(int linearCell);

    int index(int... cell);

    default void set(float newValue, int linearCell) {
        throw new UnsupportedOperationException("read-only");
    }

    default void set(float newValue, int... cell) {
        set(newValue, index(cell));
    }


    float[] snapshot();

    //void copyTo(float[] target, int targetOffset, int... subset);

    int[] shape();

//    //TODO
//    default Tensor noised(float noiseFactor, Random rng) {
//        return new
//    }
//    ..etc

    /**
     * hypervolume, ie total # cells
     */
    default int volume() {
        int[] s = shape();
        int v = s[0];
        for (int i = 1; i < s.length; i++)
            v *= s[i];
        return v;
    }

    /**
     * receives the pair: linearIndex,value (in increasing order)
     * should not be subclassed
     */
    default void forEach(IntFloatProcedure sequential) {
        forEach(sequential, 0, volume());
    }

    /**
     * receives the pair: linearIndex,value (in increasing order within provided subrange, end <= volume())
     */
    void forEach(IntFloatProcedure sequential, int start, int end);

    /** should not need subclassed */
    default void writeTo(float[] target) {
        writeTo(target, 0);
    }

    default void writeTo(float[] target, int offset) {
        forEach((i, v) -> {
            target[i + offset] = v;
        });
    }

    /** should not need subclassed */
    default void writeTo(FloatToFloatFunction perElement, float[] target) {
        writeTo(perElement, target, 0);
    }

    default void writeTo(FloatToFloatFunction perElement, float[] target, int offset) {
        forEach((i, v) -> {
            target[i + offset] = perElement.valueOf(v);
        });
    }

    /** should not need subclassed */
    default void writeTo(FloatFloatToFloatFunction perElement, float[] target) {
        writeTo(perElement, target, 0);
    }

    default void writeTo(FloatFloatToFloatFunction perElement, float[] target, int offset) {
        forEach((i, v) -> {
            target[i + offset] = perElement.apply(target[i + offset], v);
        });
    }

    default TensorFunc apply(FloatToFloatFunction f) {
        return new TensorFunc(this, f);
    }


    default float max() {
        final float[] max = {Float.MIN_VALUE};
        forEach((i, v) -> {
            if (max[0] < v)
                max[0] = v;
        });
        return max[0];
    }

    default float min() {
        final float[] min = {Float.MAX_VALUE};
        forEach((i, v) -> {
            if (min[0] > v)
                min[0] = v;
        });
        return min[0];
    }

    default float sum() {
        final float[] sum = {0};
        forEach((i,x) -> {
            sum[0] += x;
        });
        return sum[0];
    }


    /** produces a string which is separated by tab characters (for .TSV) and each
     * value is rounded to 4 digits of decimal precision
     */
    default String tsv4() {
        return Joiner.on('\t').join(iterator(Texts::n4));
    }

    /** produces a string which is separated by tab characters (for .TSV) and each
     * value is rounded to 2 digits of decimal precision
     */
    default String tsv2() {
        return Joiner.on('\t').join(iterator(Texts::n2));
    }

    default <X> Iterator<X> iterator(FloatToObjectFunction<X> map) {
        return new AbstractIterator<X>() {
            int j;
            final int limit = volume();
            @Override
            protected X computeNext() {
                if (j == limit) return endOfData();
                return map.valueOf(get(j++));
            }
        };
    }

    default boolean equalShape(Tensor b) {
        return this == b || Arrays.equals(shape(), b.shape());
    }

    default BufferedTensor buffered() {
        return new BufferedTensor(this);
    }

    /**
     * Returns the value of the buffer at the given fraction along its length (0 = start, 1 = end). Uses linear interpolation.
     *
     * @param fraction the point along the buffer to inspect.
     * @return the value at that point.
     */
    default float getFractInterp(float fraction) {
        int v = volume()-1;
        float posInBuf = fraction * v;
        int lowerIndex = Math.max(0, Math.round(posInBuf - 0.5f));
        int upperIndex = Math.min(v, Math.round(posInBuf + 0.5f));
        float offset = posInBuf - lowerIndex;
        float l = get(lowerIndex);
//        if (upperIndex == lowerIndex)
//            return l;

        float u = get(upperIndex);
        return (1 - offset) * l + offset * u;
    }

    /**
     * Returns the value of the buffer at the given fraction along its length (0 = start, 1 = end). No interpolation.
     *
     * @param fraction the point along the buffer to inspect.
     * @return the value at that point.
     */

    default float getFractRaw(float fraction) {
        return get((int) (fraction * volume()));
    }


//    int[] coord(int index, int[] coord);

//    default void forEachWithCoordinate(FloatObjectProcedure<int[]> coord ) {
//
//    }


}
