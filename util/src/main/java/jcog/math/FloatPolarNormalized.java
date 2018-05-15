package jcog.math;

import jcog.pri.Prioritized;

/** balances at zero, balanced normalization of positive and negative ranges (radius)
 *  output is normalized to range 0..1.0
 * */
@Deprecated public class FloatPolarNormalized extends FloatNormalized {

    public FloatPolarNormalized(FloatSupplier in) {
        this(in, Prioritized.EPSILON);
    }

    public FloatPolarNormalized(FloatSupplier in, float radius) {
        super(in, radius);
    }
}
