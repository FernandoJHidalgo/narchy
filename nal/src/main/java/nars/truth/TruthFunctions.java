/*
 * TruthFunctions.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the abduction warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.truth;

import jcog.Util;
import nars.$;
import nars.Param;
import org.jetbrains.annotations.Nullable;

import java.security.InvalidParameterException;
import java.util.List;

import static jcog.Util.clamp;
import static jcog.Util.or;
import static nars.$.t;
import static nars.util.UtilityFunctions.and;

/**
 * All truth-value (and desire-value) functions used in logic rules
 */
public final class TruthFunctions {
    public static final float MAX_CONF = 1f - Param.TRUTH_EPSILON;

    /* ----- Single argument functions, called in MatchingRules ----- */

    /**
     * {<A ==> B>} |- <B ==> A>
     *
     * @param t Truth value of the premise
     * @return Truth value of the conclusion
     */
    public static Truth conversion(/*@NotNull*/ Truth t, float minConf) {
        float w = and(t.freq(), t.conf());
        float c = w2c(w);
        return c >= minConf ? t(1, c) : null;
    }

    /* ----- Single argument functions, called in StructuralRules ----- */

    /**
     * {A} |- (--A)
     *
     * @return Truth value of the conclusion
     */
    @Nullable
    public static Truth negation(@Nullable Truth v1, float minConf) {
        return ((v1 == null) || (v1.conf() < minConf)) ? null : v1.neg();

        /*

        if (t == null) return null;
        final float f = 1 - t.getFrequency();
        final float c = t.getConfidence();

        if (t.isAnalytic())
            return AnalyticTruth.get(f, c, t); //experimental: for cases where analytic is inverted, to preserve analytic state
        else
            return t(f, c, t);
            */
    }


    /**
     * {<A ==> B>} |- <(--, B) ==> (--, A)>
     *
     * @param t Truth value of the premise
     * @return Truth value of the conclusion
     */
    public static Truth contraposition(/*@NotNull*/ Truth t, float minConf) {
        float c = w2c(and(1 - t.freq(), t.conf()));
        return (c < minConf) ? null : t(0, c);
    }

    //    public static float temporalIntersection(long now, long at, long bt) {
//        //return BeliefTable.relevance(Math.abs(now-at) + Math.abs(now-bt), Math.abs(at-bt));
//        return temporalIntersection(now, at, bt, 1f);
//    }


    /**
     * {M, <M ==> P>} |- P
     *
     * @param a        Truth value of the first premise
     * @param reliance Confidence of the second (analytical) premise
     * @return AnalyticTruth value of the conclusion, because it is structural
     */
    @Nullable
    public static Truth deductionR(/*@NotNull*/ Truth a, float reliance, float minConf) {
        float f = a.freq();
        float c = and(f, a.conf(), reliance);
        return (c >= minConf) ? t(f, c) : null;
    }
        /* ----- double argument functions, called in SyllogisticRules ----- */

    /**
     * assumes belief freq=1f
     */
    @Nullable
    public static Truth deduction1(/*@NotNull*/ Truth a, float bC, float minConf) {
        return deductionB(a, 1f, bC, minConf);
    }

    @Nullable
    public static Truth deduction(/*@NotNull*/ Truth a, /*@NotNull*/ Truth b, float minConf) {
        return deductionB(a, b.freq(), b.conf(), minConf);
    }

    @Nullable
    public static Truth deductionB(/*@NotNull*/ Truth a, float bF, float bC, float minConf) {

        float f = and(a.freq(), bF);
        float c = and(f, a.conf(), bC);

        return c >= minConf ? t(f, c) : null;
    }

    /**
     * {<S ==> M>, <M <=> P>} |- <S ==> P>
     *
     * @param a Truth value of the first premise
     * @param b Truth value of the second premise
     * @return Truth value of the conclusion
     */
    @Nullable
    public static Truth analogy(/*@NotNull*/ Truth a, float bf, float bc, float minConf) {
        float c = and(a.conf(), bc, bf);
        return c < minConf ? null : t(and(a.freq(), bf), c);
    }

    @Nullable
    public static Truth analogy(/*@NotNull*/ Truth a, /*@NotNull*/ Truth b, float minConf) {
        return analogy(a, b.freq(), b.conf(), minConf);
    }

    /**
     * {<S <=> M>, <M <=> P>} |- <S <=> P>
     *
     * @param a Truth value of the first premise
     * @param b Truth value of the second premise
     * @return Truth value of the conclusion
     */
    @Nullable
    public static Truth resemblance(/*@NotNull*/ Truth a, /*@NotNull*/ Truth b, float minConf) {
        float f1 = a.freq();
        float f2 = b.freq();
        float c = and(a.conf(), b.conf(), or(f1, f2));
        return (c < minConf) ? null : t(and(f1, f2), c);
    }

    /**
     * {<S ==> M>, <P ==> M>} |- <S ==> P>
     *
     * @param a Truth value of the first premise
     * @param b Truth value of the second premise
     * @return Truth value of the conclusion, or null if either truth is analytic already
     */
    public static Truth induction(/*@NotNull*/ Truth a, /*@NotNull*/ Truth b, float minConf) {
        float c = w2c(and(b.freq(), a.conf(), b.conf()));

//        float aF = a.freq();
//        float bF = b.freq();
//        float c = w2c(and(
//                freqSimilarity(aF, bF),
//                a.conf(), b.conf()));
        return (c < minConf) ? null : t(a.freq(), c);
    }



    /**
     * {<M ==> S>, <M ==> P>} |- <S ==> P>
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    public static Truth abduction(/*@NotNull*/ Truth v1, /*@NotNull*/ Truth v2, float minConf) {
        return induction(v2, v1, minConf);
    }

    /**
     * {<M ==> S>, <P ==> M>} |- <S ==> P>
     *
     * @param a Truth value of the first premise
     * @param b Truth value of the second premise
     * @return Truth value of the conclusion
     */
    public static Truth exemplification(/*@NotNull*/ Truth a, /*@NotNull*/ Truth b, float minConf) {
        float c = w2c(and(a.freq(), b.freq(), a.conf(), b.conf()));
        return c < minConf ? null : t(1, c);
    }


    @Nullable
    public static Truth comparison(/*@NotNull*/ Truth a, /*@NotNull*/ Truth b, float minConf) {
        return comparison(a, b, false, minConf);
    }

    /**
     * {<M ==> S>, <M ==> P>} |- <S <=> P>
     *
     * @param a Truth value of the first premise
     * @param b Truth value of the second premise
     * @return Truth value of the conclusion
     */
    @Nullable
    public static Truth comparison(/*@NotNull*/ Truth a, /*@NotNull*/ Truth b, boolean invertA, float minConf) {
        float f1 = a.freq();
        if (invertA) f1 = 1 - f1;

        float f2 = b.freq();


        float f0 = or(f1, f2);
        float c = w2c(and(f0, a.conf(), b.conf()));
        if (c < minConf)
            return null;

        float f = (Util.equals(f0, 0, Param.TRUTH_EPSILON)) ? 0 : (and(f1, f2) / f0);
        return t(f, c);
    }

    /**
     * measures the similarity or coherence of two freqency values
     */
    public static float freqSimilarity(float aFreq, float bFreq) {
        if (aFreq == bFreq) return 1f;

        //linear
        return 1f - Math.abs(aFreq - bFreq);

        //TODO check this:
        //return Math.max((aFreq * bFreq), (1f - aFreq) * (1f - bFreq));
    }

    /**
     * A function specially designed for desire value [To be refined]
     */
    @Nullable
    public static Truth desireStrongNew(/*@NotNull*/ Truth goal, /*@NotNull*/ Truth cond, float minConf) {

        float goalFreq = goal.freq();
        float goalPol = (goalFreq - 0.5f);
        float condPol = (cond.freq() - 0.5f);

        float c = and(/*Math.abs(condPol) * 2f,*/ cond.conf(), goal.conf());
        if (c < minConf)
            return null;
        else {
            float a = Math.signum(condPol) * Math.abs(goalPol * condPol);
            return t(a * 2f + 0.5f, c);
        }

//        float c = and(a.conf(), b.conf(), freqSimilarity(aFreq, bFreq));
//        return c < minConf ? null : desire(aFreq, bFreq, c);
    }

    /**
     * A function specially designed for desire value [To be refined]
     */
    @Nullable
    public static Truth desireStrongOriginal(/*@NotNull*/ Truth a, /*@NotNull*/ Truth b, float minConf) {
        float bFreq = b.freq();
        float c = and(a.conf(), b.conf(), bFreq);
        return c < minConf ? null : desire(a.freq(), bFreq, c);
    }
//    /**
//     * A function specially designed for desire value [To be refined]
//     */
//    @Nullable public static Truth desireWeakNew(/*@NotNull*/ Truth a, /*@NotNull*/ Truth b, float minConf) {
//        float aFreq = a.freq();
//        float bFreq = b.freq();
//        float c = and(a.conf(), b.conf(), freqSimilarity(aFreq,bFreq), w2c(1.0f));
//        return c < minConf ? null : desire(aFreq, bFreq, c);
//    }

    /**
     * A function specially designed for desire value [To be refined]
     */
    public static Truth desireWeakOriginal(/*@NotNull*/ Truth a, /*@NotNull*/ Truth b, float minConf) {
        float bFreq = b.freq();
        float c = and(a.conf(), b.conf(), bFreq, w2c(1.0f));
        return c < minConf ? null : desire(a.freq(), bFreq, c);
    }

    /*@NotNull*/
    static Truth desire(float f1, float f2, float c) {
        return t(and(f1, f2), c);
    }



//    /**
//     * original name: desireDed
//     * A function specially designed for desire value [To be refined]
//     *
//     * @param a Truth value of the first premise
//     * @param b Truth value of the second premise
//     * @return Truth value of the conclusion
//     */
//    @Nullable
//    public static Truth desireDed(/*@NotNull*/ Truth a, float bf, float bc, float minConf) {
//
//        float abConf = and(a.conf(), bc);
//        if (abConf < minConf) return null;
//        else {
//            //float f = and(a.freq(), b.freq());
//            float f = 0.5f + 2 * ((a.freq() - 0.5f) * (bf - 0.5f));
//            return t(f, abConf);
//        }
//    }



    /* ----- double argument functions, called in CompositionalRules ----- */

    /**
     * {<M --> S>, <M <-> P>} |- <M --> (S|P)>
     *
     * @param a Truth value of the first premise
     * @param b Truth value of the second premise
     * @return Truth value of the conclusion
     */
    @Nullable
    public static Truth union(/*@NotNull*/ Truth a, /*@NotNull*/ Truth b, float minConf) {
        float abConf = and(a.conf(), b.conf());
        return (abConf < minConf) ?
                null :
                t(or(a.freq(), b.freq()), abConf);
    }

    /**
     * {<M --> S>, <M <-> P>} |- <M --> (S&P)>
     *
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    @Nullable
    public static Truth intersection(@Nullable Truth v1, /*@NotNull*/ Truth v2, float minConf) {
        if (v1 == null) return null;
        float c = and(v1.conf(), v2.conf());
        return (c < minConf) ?
                null :
                t(and(v1.freq(), v2.freq()), c);
    }

    public static Truth intersection(@Nullable List<Truth> truths, float minConf) {
        float f = 1f;
        float c = 1f;
        for (Truth t : truths) {
            f *= t.freq();
            c *= t.conf();
            if (c < minConf)
                return null;
        }
        return $.t(f, c);
    }

//    private static float freqInterp(float f1, float f2, float c1, float c2) {
//        float w1 = c2w(c1);
//        float w2 = c2w(c2);
//        return lerp(f2, f1, w1/(w1+w2));
//    }

//    /**
//     * {(||, A, B), (--, B)} |- A
//     * @param a Truth value of the first premise
//     * @param b Truth value of the second premise
//     * @return Truth value of the conclusion
//     */
//    @Nullable
//    public static Truth reduceDisjunction(/*@NotNull*/ Truth a, /*@NotNull*/ Truth b, float minConf) {
//        Truth nn = negation(b, minConf);
//        if (nn == null) return null;
//
//        Truth ii = intersection(a, nn, minConf);
//        if (ii == null) return null;
//        return deductionR(ii, 1.0f, minConf);
//    }

    /**
     * {(--, (&&, A, B)), B} |- (--, A)
     *
     * @return Truth value of the conclusion
     */
    @Nullable
    public static Truth reduceConjunction(/*@NotNull*/ Truth v1, /*@NotNull*/ Truth v2, float minConf) {

        Truth n1 = negation(v1, minConf);
        if (n1 == null) return null;

        Truth i12 = intersection(n1, v2, minConf);
        if (i12 == null) return null;

        Truth v11 = deductionR(i12, 1.0f, minConf);
        if (v11 == null) return null;

        return v11.neg(); //negation(v11, minConf);


//        AnalyticTruth x = deduction(
//                intersection(negation(a), b),
//                1f
//        );
//        if (x!=null)
//            return x.negate();
//        else
//            return null;
    }

//    /**
//     * {(--, (&&, A, (--, B))), (--, B)} |- (--, A)
//     * @param a Truth value of the first premise
//     * @param b Truth value of the second premise
//     * @return Truth value of the conclusion
//     */
//    @Nullable
//    public static Truth reduceConjunctionNeg(/*@NotNull*/ Truth a, /*@NotNull*/ Truth b,float minConf) {
//        return reduceConjunction(a, negation(b, minConf), minConf);
//    }

    /**
     * {(&&, <#x() ==> M>, <#x() ==> P>), S ==> M} |- <S ==> P>
     *
     * @param a Truth value of the first premise
     * @param b Truth value of the second premise
     * @return Truth value of the conclusion
     */
    public static Truth anonymousAnalogy(/*@NotNull*/ Truth a, /*@NotNull*/ Truth b, float minConf) {
        float v0c = w2c(a.conf());
        //since in analogy it will be and() with it, if it's already below then stop
        return v0c < minConf ? null : analogy(b, a.freq(), v0c, minConf);
    }

    /**
     * decompose positive / negative
     */
    @Nullable
    public static Truth decompose(@Nullable Truth a, @Nullable Truth b, boolean x, boolean y, boolean z, float minConf) {
        if (a == null || b == null) return null;

        float c12 = and(a.conf(), b.conf());
        if (c12 < minConf) return null;
        float f1 = a.freq(), f2 = b.freq();
        float f = and(x ? f1 : 1 - f1, y ? f2 : 1 - f2);
        float c = and(f, c12);
        return c < minConf ? null : t(z ? f : 1 - f, c);
    }

    @Nullable
    public static Truth difference(/*@NotNull*/ Truth a, /*@NotNull*/ Truth b, float minConf) {
        return intersection(a, b.neg(), minConf);

//        float f1 = a.freq();
//        float f2 = b.freq();
//
//        float c1 = a.conf();
//        float c2 = b.conf();
//
//        //      or(and(not(f 1 ), c 1 ), and(f 2 , c 2 ))
//        //    + and(f 1 , c 1 , not(f 2 ), c 2 )
//        ///float cA =  or(and( 1 - f1, c1 ), and( f2 , c2 ));
//        float cA =  and(and( 1 - f1, c1 ), and( f2 , c2 ));
//        float cB =  and( f1 , c1, (1-f2), c2 );
//        float c = Math.max(cA, cB);
//
//        return (c < minConf) ? null : t(and(f1, (1 - f2)), c);

    }


//    public static float eternalize(float conf) {
//        return w2c(conf);
//    }


    public static float c2w(float c) {
        return c2w(c, Param.HORIZON);
    }

    /**
     * A function to convert confidence to weight
     *
     * @param c confidence, in [0, 1)
     * @return The corresponding weight of evidence, a non-negative real number
     */
    private static float c2w(float c, float horizon) {
        if (c != c || (c > MAX_CONF) || (c < 0))
            throw new InvalidParameterException();
        return c2wSafe(c, horizon);
    }

    public static float c2wSafe(float c, float horizon) {
        return horizon * c / (1f - c);
    }

    /**
     * A function to convert weight to confidence
     *
     * @param w Weight of evidence, a non-negative real number
     * @return The corresponding confidence, in [0, 1)
     */
    public static float w2c(float w) {
        return w2c(w, Param.HORIZON);
    }

    private static float w2c(float w, float horizon) {
        if ((w != w) || (w < 0))
            throw new IllegalArgumentException();
        return clamp(w / (w + horizon), 0, MAX_CONF);
    }

    public static float confAnd(Truthed... tt) {
        float c = 1f;
        for (Truthed x : tt)
            c *= x.conf();
        return c;
    }

    public static float originality(int evidenceLength) {
        if (evidenceLength == 1) {
            return 1f;
        } else {
            assert (evidenceLength > 0);
            return 1.0f / (1f + (evidenceLength - 1f) / (Param.STAMP_CAPACITY - 1));
        }
    }

    public static float expectation(float frequency, float confidence) {
        return (confidence * (frequency - 0.5f) + 0.5f);
    }
}

//    public static float projection(long sourceTime, long targetTime, long currentTime) {
//        if (sourceTime == targetTime) {
//            return 1f;
//        } else {
//            long denom = (abs(sourceTime - currentTime) + abs(targetTime - currentTime));
//            return denom == 0 ? 1f : (abs(sourceTime - targetTime)) / (float) denom;
//        }
//    }

//    /*@NotNull*/
//    public static ProjectedTruth eternalize(/*@NotNull*/ Truth t) {
//        return new ProjectedTruth(
//                t.freq(),
//                eternalize(t.conf()),
//                Tense.ETERNAL
//        );
//    }
//    public static float temporalProjection(long sourceTime, long targetTime, long currentTime) {
//        long den = (abs(sourceTime - currentTime) + abs(targetTime - currentTime));
//        if (den == 0) return 1f;
//        return abs(sourceTime - targetTime) / (float)den;
//    }
