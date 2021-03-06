package nars.truth;

import nars.Param;

import static nars.truth.func.TruthFunctions.c2wSafe;
import static nars.truth.func.TruthFunctions.w2cSafe;

/**
 * extends DiscreteTruth's raw hash representation with
 * a freq and evidence float pairs.
 *
 * this allows it to store, internally, more precision than the
 * discrete representation, yet is comparable with DiscreteTruth
 * (according to system TRUTH_EPSILON tolerance).
 *
 * the additional precision is useful for intermediate calculations
 * where premature rounding could snowball into significant error.
 *
 */
public final class PreciseTruth extends DiscreteTruth {

    final float f, e;


    public static PreciseTruth byConf(float freq, float conf) {
        return byFreqConfEvi(freq, conf, c2wSafe(conf));
    }

    public static PreciseTruth byEvi(float freq, float evi) {
        return byFreqConfEvi(freq, w2cSafe(evi), evi);
    }

    /** use with caution, if you are calculating a precise evi and a dithered conf, they should correspond */
    protected static PreciseTruth byFreqConfEvi(float freq, float conf, float evi) {

        if (conf >= Param.TRUTH_CONF_MAX || evi>=Param.TRUTH_EVI_MAX) {
            //upper limit on truth
            conf = Param.TRUTH_CONF_MAX;
            evi = Param.TRUTH_EVI_MAX;
        }

        return new PreciseTruth(freq, conf, evi);
    }

    private PreciseTruth(float freq, float conf, float evi) {
        super(freq, conf);
        if (!Float.isFinite(evi))
            throw new TruthException("non-finite evi", evi);
        this.e = evi;
        this.f = freq;
    }

    @Override
    public final Truth neg() {
        return byEvi(1 - f, e);
    }

    @Override
    public final float freq() {
        return f;
    }

    @Override
    public final float evi() { return e; }

    @Override
    public final float conf() {
        return w2cSafe(e);
    }

    /** create a DiscreteTruth instance, shedding the freq,evi floats stored here */
    public DiscreteTruth raw() {
        return new DiscreteTruth(this);
    }
}
