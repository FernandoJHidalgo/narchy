package nars.derive;

import nars.control.Derivation;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.func.TruthOperator;

import static nars.Op.*;

/**
 * Evaluates the truth of a premise
 */
abstract public class Solve extends AbstractPred<Derivation> {

    private final TruthOperator belief;
    private final TruthOperator goal;
    private final boolean beliefProjected;

    Solve(Term id, TruthOperator belief, TruthOperator goal, boolean beliefProjected) {
        super(id);
        this.belief = belief;
        this.goal = goal;
        this.beliefProjected = beliefProjected;
    }

    @Override
    public float cost() {
        return 2f;
    }

    @Override
    public final boolean test(Derivation d) {

        boolean single;
        Truth t;

        byte punc = punc(d);
        switch (punc) {
            case BELIEF:
            case GOAL:
                TruthOperator f = (punc == BELIEF) ? belief : goal;
                if (f == null)
                    return false; //there isnt a truth function for this punctuation

                single = f.single();

                if (!single) {
                    if ((beliefProjected ? d.beliefTruth : d.beliefTruthRaw) == null)
                        return false; //double premise requiring a belief, but belief is null
                }



                float confMin = d.confMin;
                if ((t = f.apply(
                        d.taskTruth, //task truth is not involved in the outcome of this; set task truth to be null to prevent any negations below:
                        single ? null : beliefProjected ? d.beliefTruth : d.beliefTruthRaw,
                        d.nar, confMin
                )) == null)
                    return false;


                float overlap;
                if (f.allowOverlap()) {
                    overlap = 0;
                } else {
                    overlap = (single ? d.overlapSingle : d.overlapDouble);
                }

                if (overlap > 0) {
                    return false;

//                    float e = t.evi() * (1f-overlap);
//                    if (e < Pri.EPSILON) //yes Pri epsilon
//                        return false;
//
//                    t = t.withEvi(e);
//                    if (t.conf() < confMin)
//                        return false;

//                    if (d.random.nextFloat() <= overlap)
//                        return false;
                }

                break;

            case QUEST:
            case QUESTION:
                float o = d.overlapSingle;
                if (o > 0 && d.random.nextFloat() <= o)
                    return false;

//                byte tp = d.taskPunct;
//                if ((tp == QUEST) || (tp == GOAL))
//                    punc = QUEST; //use QUEST in relation to GOAL or QUEST task

                single = true;
                t = null;
                break;

            default:
                throw new InvalidPunctuationException(punc);
        }

        d.concTruth = t;
        d.concPunc = punc;
        d.single = single;
        return true;
    }


    public abstract byte punc(Derivation d);

    /**
     * Created by me on 5/26/16.
     */
    public static final class SolvePuncOverride extends Solve {
        private final byte puncOverride;


        public SolvePuncOverride(Compound i, byte puncOverride, TruthOperator belief, TruthOperator desire, boolean beliefProjected) {
            super(i, belief, desire, beliefProjected);
            this.puncOverride = puncOverride;
        }


        @Override
        public byte punc(Derivation d) {
            return puncOverride;
        }

    }

    /**
     * Created by me on 5/26/16.
     */
    public static final class SolvePuncFromTask extends Solve {

        public SolvePuncFromTask(Compound i, TruthOperator belief, TruthOperator desire, boolean beliefProjected) {
            super(i, belief, desire, beliefProjected);
        }

        @Override
        public byte punc(Derivation d) {
            return d.taskPunct;
        }

    }


//    static final AbstractPred<Derivation> NotCyclic = new AbstractPred<Derivation>($.the("notCyclic")) {
//
//        @Override
//        public boolean test(Derivation d) {
//            return !d.cyclic;
//        }
//
//        @Override
//        public float cost() {
//            return 0.1f;
//        }
//    };

//    static final AbstractPred<Derivation> NotCyclicIfTaskIsQuestionOrQuest = new AbstractPred<Derivation>($.the("notCyclicIfTaskQue")) {
//
//        @Override
//        public float cost() {
//            return 0.15f;
//        }
//
//        @Override
//        public boolean test(Derivation d) {
//            if (d.cyclic) {
//                byte p = d.taskPunct;
//                if (p == QUESTION || p == QUEST)
//                    return false;
//            }
//            return true;
//        }
//
//    };
}

