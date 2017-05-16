package nars.task;

import nars.NAR;
import nars.Param;
import nars.Task;
import nars.premise.Derivation;
import nars.premise.Premise;
import nars.term.Compound;
import nars.truth.Truth;
import nars.truth.TruthDelta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * TODO extend an ImmutableTask class
 */
abstract public class DerivedTask extends ImmutableTask {

    @Nullable public transient Premise premise;

    //TODO should this also affect the Belief task?

    public DerivedTask(@NotNull Compound tc, byte punct, @Nullable Truth truth, @NotNull Derivation p, long[] evidence, long now, long start, long end) {
        super(tc, punct, truth, now, start, end, evidence);

        this.premise = Param.DEBUG ? p.premise : null;

//        if (!isBeliefOrGoal() || tc.term().dt()!=DTERNAL) {
//            //if this is a question or temporal relation use default method
//            this.startEnd = null;
//        } else {
//            long defaultStart = start();
//            long defaultEnd = end();
//            long start, end;
//            if (p.belief!=null && !p.task.isEternal() && !p.belief.isEternal()) {
//                Interval i = Interval.union(
//                        p.task.start(), p.task.end(), p.belief.start(), p.belief.end());
//                start = i.a;
//                end = i.b;
//
//            } else {
//                start = p.task.start();
//                end = p.task.end();
//            }
//
//            if ((defaultStart == start) && (defaultEnd == end))
//                this.startEnd = null; //use default
//            else
//                this.startEnd = new long[] { start, end };
//        }


    }

//    @Override
//    public long start() {
//        return startEnd == null ? super.start() : startEnd[0];
//    }
//
//    @Override
//    public long end() {
//        return startEnd == null ? super.end() : startEnd[1];
//    }



    @Override
    public final boolean isInput() {
        return false;
    }

    @Override
    @Nullable
    public final Task getParentTask() {
        Premise p = this.premise;
        return p != null ? p.task : null;
    }

    @Override
    @Nullable
    public final Task getParentBelief() {
        Premise p = this.premise;
        return p != null ? p.belief : null;
    }


    //    /** next = the child which resulted from this and another task being revised */
//    @Override public boolean onRevision(@NotNull Task next) {
//
//
//        return true;
//    }

//    public void multiplyPremise(float factor, boolean alsoDurability) {
//        multiply(factor, taskLink, alsoDurability);
//        multiply(factor, termLink, alsoDurability);
//    }


    @Override
    public boolean delete() {
        if (super.delete()) {

            if (!Param.DEBUG) { //keep premise information in DEBUG mode for analysis
                this.premise = null;
            }
            return true;
        }
        return false;
    }


    public static class DefaultDerivedTask extends DerivedTask {

        public DefaultDerivedTask(@NotNull Compound tc, @Nullable Truth truth, byte punct, long[] evidence, @NotNull Derivation premise, long now, long start, long end) {
            super(tc, punct, truth, premise, evidence, now, start, end);
        }


//        private void feedbackToPremiseConcepts(@NotNull NAR nar) {
//            Concept thisConcept = concept(nar);
//            if (thisConcept != null) {
//                Premise p;
//                if ((p = premise) != null)
//                    feedbackToPremiseConcepts(thisConcept, nar, p.concept);
//
//                feedbackToPremiseConcepts(thisConcept, nar, getParentTask());
//                feedbackToPremiseConcepts(thisConcept, nar, getParentBelief());
//            }
//        }
//
//        private void feedbackToPremiseConcepts(@NotNull Concept thisConcept, @NotNull NAR nar, @Nullable Termed p) {
//            if (p == null)
//                return;
//
//            boolean tasklinked = Param.DERIVATION_TASKLINKED;
//            boolean termlinked = Param.DERIVATION_TERMLINKED;
//            if (tasklinked || termlinked) {
//                Concept parentConcept = nar.concept(p);
//                if (parentConcept != null) {
//
//                    //TODO use CrossLink or other Activation's here?
//
//                    if (tasklinked) {
//                        parentConcept.tasklinks().put(new DependentBLink(this));
//                    }
//
//                    if (termlinked && !thisConcept.equals(parentConcept)) {
//                        Budget b = budget();
//                        parentConcept.termlinks().put(new RawBLink(thisConcept.term(), b));
//                        thisConcept.termlinks().put(new RawBLink(parentConcept.term(), b));
//                    }
//
//                }
//            }
//        }

//        public void feedbackToPremiseLinks(float deltaConfidence, float deltaSatisfaction, @NotNull NAR nar) {
//
//
//                /* HEURISTIC */
//
//            float rate = nar.linkFeedbackRate.floatValue();
//            if (rate > 0) {
//                float boost = 0f;
//                boost += rate * Math.abs(deltaConfidence);
//                boost += rate * Math.abs(deltaSatisfaction);
//
//                //            float boost =
//                //                    //1f + or(Math.abs(deltaConfidence), Math.abs(deltaSatisfaction));
//                //                    //1f + deltaConfidence * Math.abs(deltaSatisfaction);
//                //                    //1f + Math.max(deltaConfidence, deltaSatisfaction);
//                //                    1f + confBoost / 2f + satisBoost / 2f;
//
//
//                if (boost > Param.BUDGET_EPSILON) {
//
//                    @Nullable Premise premise1 = this.premise;
//                    if (premise1 != null) {
//
//                        Concept c = nar.concept(premise1.concept);
//
//                        if (c != null) {
//                            nar.activate(c, boost);
//
//                            BLink<Term> b = new RawBLink(premise1.term, boost, 0.5f);
//                            c.termlinks().put(b);
//                            //c.tasklinks().boost(premise.task, score);
//                            //nar.concept(c.term(), b);
//                        }
//
//
//                    }
//
//                    //budget().priMult(score);
//
//                }
//
//
//            }
//        }

    }

//    public static class CompetingDerivedTask extends DerivedTask {
//
//
//        public CompetingDerivedTask(@NotNull Termed<Compound> tc, byte punct, @Nullable Truth truth, @NotNull PremiseEval premise) {
//            super(tc, punct, truth, premise);
//        }
//
//        @Override
//        public boolean onConcept(@NotNull Concept c, float score) {
//            if (super.onConcept(c, score)) {
//                Premise p = this.premise;
//                if (p != null) {
//                    Concept pc = p.conceptLink;
//                    Concept.linkPeer(pc.termlinks(), p.termLink, budget(), qua());
//                    Concept.linkPeer(pc.tasklinks(), p.taskLink, budget(), qua());
//                }
//                return true;
//            }
//            return false;
//        }
//
//        @Override
//        public boolean delete() {
//            if (super.delete()) {
//                Premise p = this.premise;
//                if (p != null) {
//                    Concept pc = p.concept();
//                    Concept.linkPeer(pc.termlinks(), p.termLink, UnitBudget.Zero, qua());
//                    Concept.linkPeer(pc.tasklinks(), p.taskLink, UnitBudget.Zero, qua());
//                }
//
//                this.premise = null;
//
//                return true;
//            }
//            return false;
//        }
//    }

}
//scratch
//float deathFactor = 1f - 1f / (1f +(conf()/evidence().length));
//float deathFactor = (1f/(1 + c * c * c * c));
