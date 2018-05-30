package nars.truth;

import org.jetbrains.annotations.Nullable;

import static nars.truth.TruthFunctions.w2cSafe;

/** indicates an implementation has, or is associated with a specific TruthValue */
public interface Truthed  {

    @Nullable
    Truth truth();


    default float expectation() { return truth().expectation(); }

    /** value between 0 and 1 indicating how distant the frequency is from 0.5 (neutral) */
    default float polarity() {
        return Math.abs(freq() - 0.5f)*2f;
    }
    /**
     * Check if the truth value is negative
     * Note that values of 0.5 are not considered positive, being an unbiased
     * midpoint value
     *
     * @return True if the frequence is less than (but not equal to) 1/2
     */
    default boolean isNegative() {
        return freq() < 0.5f;
    }

    /**
     * Check if the truth value is negative.
     * Note that values of 0.5 are not considered positive, being an unbiased
     * midpoint value
     *
     * @return True if the frequence is greater than or equal to 1/2
     */
    default boolean isPositive() {
        return freq() >= 0.5f;
    }























    default float freq() {
        Truth t = truth();
        
        return t.freq(); 
    }
    default float conf() {
        
        return w2cSafe(evi()); 
    }
    /** weight of evidence ( confidence converted to weight, 'c2w()' )  */
    default float evi() {
        return truth().evi(); 
    }


    default float eviEternalized() {
        return TruthFunctions.eternalize(evi());
    }

    default float confEternalized() {
        return w2cSafe(eviEternalized());
    }

    default float eviEternalized(float horizon) {
        return w2cSafe(conf(), horizon);
    }


    











}
