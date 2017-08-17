package nars.truth.func;

import nars.$;
import nars.NAR;
import nars.Op;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.TruthFunctions;
import nars.truth.func.annotation.AllowOverlap;
import nars.truth.func.annotation.SinglePremise;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Map;

import static nars.$.t;

/**
 * http://aleph.sagemath.org/?q=qwssnn
 * <patham9> only strong rules are allowing overlap
 * <patham9> except union and revision
 * <patham9> if you look at the graph you see why
 * <patham9> its both rules which allow the conclusion to be stronger than the premises
 */
public enum BeliefFunction implements TruthOperator {

    StructuralIntersection() {
        @Override
        public Truth apply(final Truth T, final Truth B, /*@NotNull*/ NAR m, float minConf) {
            return B != null ? TruthFunctions.intersection(B, defaultTruth(m), minConf) : null;
        }
    },


    StructuralAbduction() {
        @Override
        public Truth apply(final Truth T, final Truth B, /*@NotNull*/ NAR m, float minConf) {
            return TruthFunctions.abduction(B, defaultTruth(m), minConf);
        }
    },


    Deduction() {
        @Override
        public Truth apply(Truth T, Truth B, NAR m, float minConf) {
            return TruthFunctions.deduction(T, B, minConf);
        }
    },

    @SinglePremise
    @AllowOverlap
    StructuralDeduction() {
        @Override
        public Truth apply(final Truth T, final Truth B, /*@NotNull*/ NAR m, float minConf) {
            return T != null ? TruthFunctions.deduction1(T, defaultConfidence(m), minConf) : null;
        }
    },

    /**
     * polarizes according to belief
     */
    DeductionPB() {
        @Override
        public Truth apply(Truth T, Truth B, NAR m, float minConf) {
            if (B.isNegative()) {
                Truth x = TruthFunctions.deduction(T, B.negated(), minConf);
                return x != null ? x.negated() : null;
            } else {
                return TruthFunctions.deduction(T, B, minConf);
            }
        }
    },

    @AllowOverlap
    DeductionRecursive() {
        @Override
        public Truth apply(Truth T, Truth B, NAR m, float minConf) {
            return Deduction.apply(T, B, m, minConf);
        }
    },
    @AllowOverlap
    DeductionRecursivePB() {
        @Override
        public Truth apply(Truth T, Truth B, NAR m, float minConf) {
            return DeductionPB.apply(T, B, m, minConf);
        }
    },

    Induction() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.induction(T, B, minConf);
        }
    },


//    /** task frequency negated induction */
//    InductionNeg() {
//        
//        @Override public Truth apply( final Truth T,  final Truth B, /*@NotNull*/ Memory m, float minConf) {
//            return TruthFunctions.induction(T.negated(), B, minConf);
//        }
//    },
//    /** belief frequency negated induction */
//    InductionNegB() {
//        
//        @Override public Truth apply( final Truth T,  final Truth B, /*@NotNull*/ Memory m, float minConf) {
//            return TruthFunctions.induction(T, B.negated(), minConf);
//        }
//    },

    Abduction() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.abduction(T, B, minConf);
        }
    },

    AbductionPB() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            if (B.isNegative()) {
                Truth x = TruthFunctions.abduction(T, B.negated(), minConf);
                return x != null ? x.negated() : null;
            } else {
                return TruthFunctions.abduction(T, B, minConf);
            }
        }
    },

    @AllowOverlap
    AbductionRecursivePB() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return AbductionPB.apply(T,B,m,minConf);
        }
    },

//    AbductionNeg() {
//        
//        @Override public Truth apply( final Truth T,  final Truth B, /*@NotNull*/ Memory m, float minConf) {
//            return TruthFunctions.abduction(T.negated(), B, minConf);
//        }
//    },

    Comparison() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.comparison(T, B, minConf);
        }
    },

//    ComparisonNeg() {
//        
//        @Override public Truth apply( final Truth T,  final Truth B, /*@NotNull*/ Memory m, float minConf) {
//            return TruthFunctions.comparison(T, B, true, minConf);
//        }
//    },

    Conversion() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.conversion(B, minConf);
        }
    },

//    @SinglePremise
//    Negation() {
//        
//        @Override public Truth apply( final Truth T, /* nullable */ final Truth B, /*@NotNull*/ Memory m, float minConf) {
//            return TruthFunctions.negation(T, minConf);
//        }
//    },

    @SinglePremise
    Contraposition() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.contraposition(T, minConf);
        }
    },

    //@AllowOverlap
    Resemblance() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.resemblance(T, B, minConf);
        }
    },

    Union() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.union(T, B, minConf);
        }
    },

    Intersection() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.intersection(T, B, minConf);
        }
    },

    Difference() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.difference(T, B, minConf);
        }
    },

    Analogy() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.analogy(T, B, minConf);
        }
    },
    ReduceConjunction() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.reduceConjunction(T, B, minConf);
        }
    },

//    ReduceDisjunction() {
//        
//        @Override public Truth apply( final Truth T,  final Truth B, /*@NotNull*/ Memory m, float minConf) {
//            if (B == null || T == null) return null;
//            return TruthFunctions.reduceDisjunction(T, B,minConf);
//        }
//    },

//    ReduceConjunctionNeg() {
//        
//        @Override public Truth apply( final Truth T,  final Truth B, /*@NotNull*/ Memory m, float minConf) {
//            if (B == null || T == null) return null;
//            return TruthFunctions.reduceConjunctionNeg(T, B,minConf);
//        }
//    },

    AnonymousAnalogy() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.anonymousAnalogy(T, B, minConf);
        }
    },
    Exemplification() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            if (B == null || T == null) return null;
            return TruthFunctions.exemplification(T, B, minConf);
        }
    },


    DecomposePositiveNegativeNegative() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.decompose(T, B, true, false, false, minConf);
        }
    },

    DecomposeNegativeNegativeNegative() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.decompose(T, B, false, false, false, minConf);
        }
    },

    DecomposePositiveNegativePositive() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.decompose(T, B, true, false, true, minConf);
        }
    },

    DecomposeNegativePositivePositive() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.decompose(T, B, false, true, true, minConf);
        }
    },

    DecomposePositivePositivePositive() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.decompose(T, B, true, true, true, minConf);
        }
    },

    @SinglePremise
    Identity() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthOperator.identity(T, minConf);
        }
    },

    /**
     * same as identity but allows overlap
     */
    @SinglePremise
    @AllowOverlap
    IdentityTransform() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthOperator.identity(T, minConf);
        }
    },

    BeliefIdentity() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthOperator.identity(B, minConf);
        }


    },

    //@AllowOverlap
    BeliefStructuralDeduction() {
        @Override
        public Truth apply(final Truth T, final Truth B, /*@NotNull*/ NAR m, float minConf) {
            if (B == null) return null;
            return TruthFunctions.deduction1(B, defaultConfidence(m), minConf);
        }
    },

    //@AllowOverlap
    BeliefStructuralAbduction() {
        @Override
        public Truth apply(final Truth T, final Truth B, /*@NotNull*/ NAR m, float minConf) {
            if (B == null) return null;
            return TruthFunctions.abduction(B, $.t(1f, defaultConfidence(m)), minConf);
        }
    },

    //@AllowOverlap
    BeliefStructuralAnalogy() {
        @Override
        public Truth apply(final Truth T, final Truth B, /*@NotNull*/ NAR m, float minConf) {
            if (B == null) return null;
            return TruthFunctions.analogy(B, $.t(1f, defaultConfidence(m)), minConf);
        }
    },

    //@AllowOverlap
    BeliefStructuralDifference() {
        @Override
        public Truth apply(final Truth T, final Truth B, /*@NotNull*/ NAR m, float minConf) {
            if (B == null) return null;
            Truth res = TruthFunctions.deduction1(B, defaultConfidence(m), minConf);
            return (res != null) ? t(1.0f - res.freq(), res.conf()) : null;
        }
    },

//    BeliefNegation() {
//        
//        @Override public Truth apply(final Truth T,  final Truth B, /*@NotNull*/ Memory m, float minConf) {
//            return TruthFunctions.negation(B, minConf);
//        }
//    }

    ;

    /*@NotNull*/
    private static Truth defaultTruth(/*@NotNull*/ NAR m) {
        return m.truthDefault(Op.BELIEF);
    }

    private static float defaultConfidence(/*@NotNull*/ NAR m) {
        return m.confDefault(Op.BELIEF);
    }


//    /**
//     * @param T taskTruth
//     * @param B beliefTruth (possibly null)
//     * @return
//     */
//    @Override
//    abstract public Truth apply(Truth T, Truth B, /*@NotNull*/ Memory m);


    //TODO use an enum map with terms bound to the enum values directly
    static final Map<Term, TruthOperator> atomToTruthModifier = $.newHashMap(BeliefFunction.values().length);


    static {
        TruthOperator.permuteTruth(BeliefFunction.values(), atomToTruthModifier);
    }

    @Nullable
    public static TruthOperator get(Term a) {
        return atomToTruthModifier.get(a);
    }

    public final boolean single;
    public final boolean overlap;

    BeliefFunction() {

        try {
            Field enumField = getClass().getField(name());
            this.single = enumField.isAnnotationPresent(SinglePremise.class);
            this.overlap = enumField.isAnnotationPresent(AllowOverlap.class);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }


    }

    @Override
    public final boolean single() {
        return single;
    }

    @Override
    public final boolean allowOverlap() {
        return overlap;
    }

}
