/*
 * TruthValue.java
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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.truth;

import jcog.Texts;
import jcog.Util;
import nars.Op;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jcog.Util.*;
import static nars.truth.TruthFunctions.w2c;
import static nars.util.UtilityFunctions.and;


/** scalar (1D) truth value "frequency", stored as a floating point value */
public interface Truth extends Truthed {

    static float eternalize(float conf) {
        return w2c(conf);
    }

//    Term Truth_TRUE = $.the("TRUE");
//    Term Truth_FALSE = $.the("FALSE");
//    Term Truth_UNSURE = $.the("UNSURE"); //only really valid for describing expectation, not frequency by itself
//    Term Truth_MAYBE = $.the("MAYBE");
//    Term Truth_CERTAIN = $.the("CERTAIN");
//    Term Truth_UNCERTAIN = $.the("UNCERTAIN");
//
//    Comparator<Truthed> compareConfidence = (o1, o2) -> {
//        if (o1.equals(o2))
//            return 0;
//
//        float b = o2.conf();
//        float a = o1.conf();
//        if (b < a)
//            return -1;
//        else
//            return +1;
//    };


    /**
     * Get the frequency value
     *
     * @return The frequency value
     */
    @Override
    float freq();


    @Nullable
    @Override
    default Truth truth() { return this; }

    /**
     * Calculate the expectation value of the truth value
     *
     * @return The expectation value
     */
    @Override
    default float expectation() {
        return TruthFunctions.expectation(freq(), conf());
    }

    //DONT USE THIS IT IS BIASED AGAINST NEGATIVE FREQUENCY TRUTH
//    /**
//     * Calculate the absolute difference of the expectation value and that of a
//     * given truth value
//     *
//     * @param t The given value
//     * @return The absolute difference
//     */
//    default float getExpDifAbs(@NotNull Truth t) {
//        return Math.abs(expectation() - t.expectation());
//    }

    /**
     * Check if the truth value is negative
     * Note that values of 0.5 are not considered positive, being an unbiased
     * midpoint value
     *
     * @return True if the frequence is less than 1/2
     */
    default boolean isNegative() {
        return freq() < 0.5f;
    }

    /**
     * Check if the truth value is negative.
     * Note that values of 0.5 are not considered positive, being an unbiased
     * midpoint value
     *
     * @return True if the frequence is greater than 1/2
     */
    default boolean isPositive() {
        return freq() > 0.5f;
    }

    /**
     * how polarized (expectation distance from 0.5) a given truth value is:
     *      expectation=0.5        -> polarization=0
     *      expectation=0 or 1     -> polarization=1
     */
    default float polarization() {
        float exp = expectation();
        if (exp < 0.5f)
            exp = 1f - exp;
        return (exp - 0.5f) * 2f;
    }


    @NotNull
    default StringBuilder appendString(@NotNull StringBuilder sb) {
        return appendString(sb, 2);
    }


    /**
     * A simplified String representation of a TruthValue, where each factor is
     * accruate to 1%
     */
    @NotNull
    default StringBuilder appendString(@NotNull StringBuilder sb, int decimals)  {

        sb.ensureCapacity(3 + 2 * (2 + decimals) );

        return sb
            .append(Op.TRUTH_VALUE_MARK)
            .append(Texts.n(freq(), decimals))
            .append(Op.VALUE_SEPARATOR)
            .append(Texts.n(conf(), decimals))
            .append(Op.TRUTH_VALUE_MARK);
    }



    static int compare(@NotNull Truth a, @NotNull Truth b) {
        if (a == b) return 0;

        //see how Truth hash() is calculated to know why this works
        return Integer.compare(b.hashCode(), a.hashCode());

//        tc = Float.compare(truth.getFrequency(), otruth.getFrequency());
//        if (tc!=0) return tc;
//        tc = Float.compare(truth.getConfidence(), otruth.getConfidence());
//        if (tc!=0) return tc;
//
//        return 0;
    }


//    @NotNull
//    default Truth interpolate(@NotNull Truthed y) {
//        float xc = confWeight();
//        float yc = y.confWeight();
//
//        return new DefaultTruth(
//                //lerp by proportion of confidence contributed
//                lerp(freq(), y.freq(), xc / (xc+yc)),
//
//                //difference in freq means closer to the AND conf, otherwise if they are the same then closer to max
//                lerp(and(xc, yc), max(xc, yc), Math.abs(freq()-y.freq()))
//
//        );
//    }

    /** the negated (1 - freq) of this truth value */
    @NotNull default Truth neg() {
        return new PreciseTruth(1f - freq(), conf());
    }

    default boolean equals(@Nullable  Truthed x, float tolerance) {
        return x!=null && Util.equals(freq(), x.freq(), tolerance) && Util.equals(conf(), x.conf(), tolerance);
    }

    @NotNull
    default Truth negIf(boolean negate) {
        return negate ? neg() : this;
    }

//    default float eternalizedConf() {
//        return TruthFunctions.eternalize(conf());
//    }
//    default float eternalizedConfWeight() {
//        //TODO does this simplify?
//        return c2w(TruthFunctions.eternalize(conf()));
//    }

    @Nullable
    static Truth maxConf(@Nullable Truth a, @Nullable Truth b) {
        if (b == null)
            return a;
        if (a == null)
            return b;
        return a.conf() >= b.conf() ? a : b;
    }

    static float freq(float f, float epsilon) {
        return unitize(round(f, epsilon));
    }

    static float conf(float c, float epsilon) {
        return clamp(
                round(c, epsilon), //optimistic
                //floor(c, epsilon), //conservative
                epsilon, 1f - epsilon);
    }


    @Nullable default PreciseTruth ditherFreqConf(float resolution, float confMin, float confGain) {
        float c0 = confGain != 1 ? w2c(evi()) * confGain : conf();
        if (c0 < confMin)
            return null;
        float c = conf(c0, resolution); //dither confidence
        if (c < confMin)
            return null;
        return new PreciseTruth(freq(freq(), resolution), c);
    }



    @Override
    default float eviEternalized() {
//        float c = eternalizedConf();
//        return c2w(c);
        return conf(); //c2w(w2c(conf)) = conf
    }

    default float freqTimesConf() {
        return freq() * conf();
    }

    default float freqNegTimesConf() {
        return (1 - freq()) * conf();
    }


//    default Truth eternalized() {
//        return $.t(freq(), eternalizedConf());
//    }


//    static <T extends Truthed> T minConf(T a, T b) {
//        return a.conf() <= b.conf() ? a : b;
//    }


    enum TruthComponent {
        Frequency, Confidence, Expectation
    }
    
    default float component(@NotNull TruthComponent c) {
        switch (c) {
            case Frequency: return freq();
            case Confidence: return conf();
            case Expectation: return expectation();
        }
        return Float.NaN;
    }
    
    /** provides a statistics summary (mean, min, max, variance, etc..) of a particular TruthValue component across a given list of Truthables (sentences, TruthValue's, etc..).  null values in the iteration are ignored */
    @NotNull
    static DescriptiveStatistics statistics(@NotNull Iterable<? extends Truthed> t, @NotNull TruthComponent component) {
        DescriptiveStatistics d = new DescriptiveStatistics();
        for (Truthed x : t) {
            Truth v = x.truth();
            if (v!=null)
                d.addValue(v.component(component));
        }
        return d;
    }





}
