package nars.time;

import nars.$;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.nal.meta.Conclude;
import nars.nal.meta.OccurrenceSolver;
import nars.nal.meta.PremiseEval;
import nars.nal.rule.PremiseRule;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.util.InvalidTermException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.$.unneg;
import static nars.Op.CONJ;
import static nars.nal.TermBuilder.productNormalize;
import static nars.task.Revision.chooseByConf;
import static nars.time.Tense.*;

/**
 * Strategies for solving temporal components of a derivation
 */
@FunctionalInterface
public interface TimeFunctions {


    /**
     * @param derived   raw resulting untemporalized derived term that may or may not need temporalized and/or occurrence shifted as part of a derived task
     * @param p         current match context
     * @param d         derivation rule being evaluated
     * @param occReturn holds the occurrence time as a return value for the callee to use in building the task
     * @param confScale
     * @return
     */
    @Nullable Compound compute(@NotNull Compound derived, @NotNull PremiseEval p, @NotNull Conclude d, @NotNull long[] occReturn, @NotNull float[] confScale);


    static long earlyOrLate(long t, long b, boolean early) {
        boolean tEternal = t == ETERNAL;
        boolean bEternal = b == ETERNAL;
        if (!tEternal && !bEternal) {
            if (early)
                return t <= b ? t : b;
            else
                return t >= b ? t : b;
        } else if (!bEternal && tEternal) {
            return b;
        } else if (!tEternal && bEternal) {
            return t;
        }
        return ETERNAL;
    }

    //nars.Premise.OccurrenceSolver latestOccurrence = (t, b) -> earlyOrLate(t, b, false);
    OccurrenceSolver earliestOccurrence = (t, b) -> earlyOrLate(t, b, true);


    /**
     * early-aligned, difference in dt
     */
    TimeFunctions dtTminB = (@NotNull Compound derived, @NotNull PremiseEval p, @NotNull Conclude d, @NotNull long[] occReturn, float[] confScale) -> dtDiff(derived, p, occReturn, +1);
    /**
     * early-aligned, difference in dt
     */
    TimeFunctions dtBminT = (@NotNull Compound derived, @NotNull PremiseEval p, @NotNull Conclude d, @NotNull long[] occReturn, float[] confScale) -> dtDiff(derived, p, occReturn, -1);
    /**
     * early-aligned, difference in dt
     */
    TimeFunctions dtIntersect = (@NotNull Compound derived, @NotNull PremiseEval p, @NotNull Conclude d, @NotNull long[] occReturn, float[] confScale) -> dtDiff(derived, p, occReturn, 0);

    /**
     * early-aligned, difference in dt
     */
    TimeFunctions dtUnion = (@NotNull Compound derived, @NotNull PremiseEval p, @NotNull Conclude d, @NotNull long[] occReturn, float[] confScale) -> {
        //TODO
        return dtDiff(derived, p, occReturn, 2);
    };
    TimeFunctions dtUnionReverse = (@NotNull Compound derived, @NotNull PremiseEval p, @NotNull Conclude d, @NotNull long[] occReturn, float[] confScale) -> {
        //TODO
        return dtDiff(derived, p, occReturn, -2);
    };

    /**
     * does nothing but set DTternal; the input tasks will be considered to be eternal
     */
    @Nullable TimeFunctions dternal = (derived, p, d, occReturn, confScale) -> dt(derived, DTERNAL, p, occReturn);

    @NotNull
    static Compound dtDiff(@NotNull Compound derived, @NotNull PremiseEval p, @NotNull long[] occReturn, int polarity) {

        Compound taskTerm = (Compound) $.unneg(p.taskTerm);
        Termed<Compound> beliefTerm = p.beliefTerm;

        int dt;
        int ttd = taskTerm.dt();
        int btd = beliefTerm.term().dt();
        if (ttd != DTERNAL && btd != DTERNAL) {
            switch (polarity) {
                case 0:
                    dt = (ttd + btd) / 2; //intersect: 0
                    break;
                case 2:  //union
                    dt = -(ttd + btd);
                    break;
                case -2:  //unionReverse
                    dt = (ttd + btd);
                    break;
                case -1:
                case +1: //difference: -1 or +1
                    dt = (ttd - btd);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        } else if (ttd != DTERNAL) {
            dt = ttd;
        } else if (btd != DTERNAL) {
            dt = btd;
        } else {
            dt = DTERNAL;
        }

        if ((polarity == 0) || (polarity == 2) || (polarity == -2)) {
            occReturn[0] = p.occurrenceTarget(earliestOccurrence); //TODO CALCULATE

            //restore forward polarity for function call at the end
            polarity = 1;
        } else {
            //diff
            occReturn[0] = p.occurrenceTarget(earliestOccurrence);
        }

        return deriveDT(derived, polarity, p, dt, occReturn);
    }

    /**
     * simple delta-time between task and belief resulting in the dt of the temporal compound.
     * this assumes the conclusion term is size=2 compound.
     * no occurence shift
     * should be used in combination with a "premise Event" precondition
     */
    TimeFunctions occForward = (derived, p, d, occReturn, confScale) -> occBeliefMinTask(derived, p, occReturn, +1);
    TimeFunctions occReverse = (derived, p, d, occReturn, confScale) -> occBeliefMinTask(derived, p, occReturn, -1);


//    /**
//     * if the premise is an event (and it is allowed to not be) then the dt is the difference
//     * in task and belief occurrence times, and the occurrence time is the belief's.
//     */
//    Temporalize dtIfEvent = (derived, p, d, occReturn) -> {
//        if (!p.premise.isEvent()) {
//            return derived;
//        } else {
//            return occBeliefMinTask(derived, p, occReturn, +1);
//        }
//    };

//    /** shifts to task's predicate by the task's dt (if present) */
//    Temporalize taskPredicate = (derived, p, d, occReturn) -> {
//        long oc = p.premise.occurrenceTarget(latestOccurrence);
//        int tdt = p.premise.task().term().dt();
//        if (tdt!=ITERNAL)
//            oc += tdt;
//        occReturn[0] = oc;
//        return derived;
//    };

    @NotNull
    static Compound occBeliefMinTask(@NotNull Compound derived, @NotNull PremiseEval p, @NotNull long[] occReturn, int polarity) {

        int eventDelta;

        long beliefO = p.belief.occurrence();
        long taskO = p.task.occurrence();
        if (beliefO != ETERNAL && taskO != ETERNAL) {
            long earliest = p.occurrenceTarget(earliestOccurrence);

            //TODO check valid int/long conversion
            eventDelta = (int) (
                    beliefO -
                            taskO);


            occReturn[0] = earliest;
        } else if (beliefO != ETERNAL) {
            occReturn[0] = beliefO;
            eventDelta = DTERNAL;
        } else if (taskO != ETERNAL) {
            occReturn[0] = taskO;
            eventDelta = DTERNAL;
        } else {
            eventDelta = DTERNAL;
        }


        //HACK to handle commutive switching so that the dt is relative to the effective subject
        if (eventDelta != 0 && eventDelta != DTERNAL && derived.op().commutative) {

            Term bt = p.beliefTerm;
            Term d0 = derived.term(0);

            if (derivationMatch(bt, d0))
                eventDelta *= -1;
        }


        return deriveDT(derived, polarity, p, eventDelta, occReturn);
    }

    @Deprecated
    static boolean derivationMatch(Term a, Term b) {
        return productNormalize(unneg(a)).equalsIgnoringVariables(productNormalize(unneg(b)));
    }

    static boolean derivationMatch(Term a, Term b, PremiseEval p) {
        return resolve(p, a).equalsIgnoringVariables(resolve(p, b));
    }


    /**
     * copiesthe 'dt' and the occurence of the task term directly
     */
    TimeFunctions dtTaskExact = (@NotNull Compound derived, @NotNull PremiseEval p, @NotNull Conclude d, long[] occReturn, float[] confScale) -> dtExact(derived, occReturn, p, true);
    TimeFunctions dtBeliefExact = (@NotNull Compound derived, @NotNull PremiseEval p, @NotNull Conclude d, long[] occReturn, float[] confScale) -> dtExact(derived, occReturn, p, false);


    /**
     * special handling for dealing with detaching, esp. conjunctions which involve a potential mix of eternal and non-eternal premise components
     */
    @Nullable TimeFunctions decomposeTask = (@NotNull Compound derived, @NotNull PremiseEval p, @NotNull Conclude d, @NotNull long[] occReturn, float[] confScale) ->
            decompose(derived, p, occReturn, true);
    @Nullable TimeFunctions decomposeBelief = (@NotNull Compound derived, @NotNull PremiseEval p, @NotNull Conclude d, @NotNull long[] occReturn, float[] confScale) ->
            decompose(derived, p, occReturn, false);


    /*** special case for decomposing conjunctions in the task slot */
    @Nullable TimeFunctions decomposeTaskSubset = (@NotNull Compound derived, @NotNull PremiseEval p, @NotNull Conclude d, @NotNull long[] occReturn, float[] confScale) -> {
        Task task = p.task;
        Compound taskTerm = task.term();

        occReturn[0] = task.occurrence();
        int taskSize = taskTerm.size();

        if (derived.op() == CONJ && (task.volume() == derived.volume() && taskSize == derived.size())) {
            //something wrong happened with the ellipsis selection.
            //being a decomposition it should produce a smaller result
            throw new InvalidTermException(derived.op(), derived.terms(), "ellipsis commutive match fault: same as parent");
        }
        int dt = task.dt();

        if (taskSize <= 2) { //conjunction of 1 can not occur actually, but for completeness use the <=
            //a decomposition of this will result in a complete subterm (half) of the input.
            if (dt!=DTERNAL || dt!=XTERNAL || dt!=0) {
                //call "decomposeTask"
                return decompose(derived, p, occReturn, true);
            }
        } else {
            //3 or more
            if (derived.op() == CONJ) {
                //in this case, set the resulting conjunction to either the && or &| state, copying from the task's term
                return deriveDT(derived, +0, p, dt, occReturn);
            }

        }

        return derived;
    };

    @Nullable
    static Compound decompose(@NotNull Compound derived, @NotNull PremiseEval p, @NotNull long[] occReturn, boolean decomposeTask) {


        Task task = p.task;
        Task belief = p.belief;
        //if (premBelief == null)
        //premBelief = p.task; //it is the task itself being decomposed

        Task decomposingTask = (decomposeTask) ? task : belief;
        Task otherTask = (decomposeTask) ? belief : task;

        Compound decomposedTerm = (Compound) (decomposeTask ? p.taskTerm : p.beliefTerm);
        //int dtDecomposed = decomposedTerm.dt();

        long occDecomposed = decomposeTask ? task.occurrence() : (belief != null ? belief.occurrence() : ETERNAL);

        //the non-decomposed counterpart of the premise


        long occOther = (otherTask != null) ? otherTask.occurrence() : ETERNAL;


        if ((occDecomposed == ETERNAL) && (occOther == ETERNAL)) {
            //no temporal basis that can apply. only derive an eternal result
            // if there is no actual temporal relation in the decomposition

            int ddt = decomposedTerm.dt();
            if ((ddt == 0 || ddt == DTERNAL || ddt == XTERNAL)) {
                return derived; //allow eternal decompose since no temporal information is inside the decomposed term
            } else {
                return noTemporalBasis(derived);
            }

        } else {

            long occ;

            //int edtDecomposed = dtDecomposed != DTERNAL ? dtDecomposed : 0; //effective dt decomposed

            if (decomposedTerm.size() != 2) {
                //probably a (&&+0, ...)
                occ = occDecomposed != ETERNAL ? occDecomposed : occOther;
            } else {

                occ = ETERNAL;

                Term rDerived = resolve(p, derived);
                Term rDecomposed = resolve(p, decomposedTerm);

                long relOccDecomposed = ETERNAL, relOccOther = ETERNAL;

                if (occDecomposed != ETERNAL) {

                    int dt = rDecomposed.subtermTime(rDerived);
                    if (dt != DTERNAL)
                        relOccDecomposed = occDecomposed + dt;

                }

                if (occOther != ETERNAL) {

                    @Nullable Term rOtherTerm = resolve(p, decomposeTask ? p.beliefTerm.term() : p.taskTerm);

                    if (derivationMatch(rOtherTerm, derived, p)) {
                        relOccOther = occOther;
                    } else {
                        int otherInDecomposed = rDecomposed.subtermTime(rOtherTerm);
                        if (decomposedTerm.dt() == 0 && otherInDecomposed == 0) {
                            //special case for &&+0 having undergone some unrecognizable change
                            relOccOther = occOther - otherInDecomposed; //+0 should ensure it has the same time as this siblign event

                        } else if (otherInDecomposed != DTERNAL) {
                            int derivedInDecomposed = rDecomposed.subtermTime(rDerived);
                            if (derivedInDecomposed != DTERNAL) {
                                //now compute the dt between derived and otherTerm, as a shift added to occOther
                                relOccOther = occOther + (derivedInDecomposed - otherInDecomposed);
                            }
                        }

                    }

                }

                if (relOccDecomposed != ETERNAL && relOccOther != ETERNAL) {
                    //if both provide a possible timing for the result,
                    // choose by random wighted confidence which one
                    Task t = chooseByConf(task, belief, p);
                    if (t == decomposingTask)
                        occ = relOccDecomposed;
                    else
                        occ = relOccOther;
                } else if (relOccDecomposed != ETERNAL) {
                    occ = relOccDecomposed;
                } else if (relOccOther != ETERNAL) {
                    occ = relOccOther;
                }

                if (occ == ETERNAL) {
                    return noTemporalBasis(derived);
                }

            }

            occReturn[0] = occ;

            return derived;

        }

    }

    @Nullable
    static Term resolve(@NotNull PremiseEval p, Term t) {
        return p.resolve(productNormalize(unneg(t)));
    }

    @Nullable
    static Compound noTemporalBasis(@NotNull Compound derived) {
        if (Param.DEBUG_EXTRA)
            throw new InvalidTermException(derived.op(), derived.dt(), derived.terms(),
                    "no basis for relating other occurrence to derived");
        else
            return null;
    }


//        int shift = 0;
//        int matchedDerived = -1, matchedOther = -1;
//
//        if (dtDecomposed != DTERNAL) {
//
//
//            //shift to occurrence time of the subterm within the decomposed term's task
//
//
//            for (int i = 0; i < decomposedTerm.size(); i++) {
//                Term dct = decomposedTerm.term(i);
//                Term rdt = p.resolve(dct);
//                if (rdt==null)
//                    continue;
//
//                if (rdt.equals(derived)) {
//                    int st = decomposedTerm.subtermTime(dct);
//                    if (st != DTERNAL) {
//                        shift += st;
//                        matchedDerived = i;
//                    }
//                }
//
//                if (rdt.equals(otherTerm)) {
//                    int st = decomposedTerm.subtermTime(dct);
//                    if (st != DTERNAL) {
//                        shift -= st;
//                        matchedOther = i;
//                    }
//                }
//            }
//
//        }
//
////        long occ;
////        //long rOcc = ETERNAL;
////
////            /*if (dOcc == ETERNAL) {
////                //rOcc = oOcc;
////            }
////
////            else*/
////        if (occOther!=ETERNAL && occDecomposed!=ETERNAL) {
////            //conflict, use dOcc by default
////            occ = occDecomposed;
////        }
////        else if (occOther != ETERNAL && occDecomposed == ETERNAL) {
////            occ = occOther;
////        }
////        else if (occDecomposed != ETERNAL && occOther == ETERNAL) {
////
////
////            if (shift < 0) {
////                if (((dtDecomposed > 0) && (matchedDerived < matchedOther))||
////                        ((dtDecomposed < 0) && (matchedDerived > matchedOther))){
////                    shift *= -1;
////                }
////            }
////
////            occ = occDecomposed;
////        } else {
////            //should not happen, prevented by condition above
////            occ = ETERNAL;
////            shift = 0;
////        }
////
////            /*else if (dOcc!=ETERNAL && oOcc!=ETERNAL && dOcc!=oOcc) {
////                //both specify a possible occurrence time to use; base occurrence time according to the most confident
////                //float dConf = decomposeTask ? prem.task().conf() : (premBelief !=null ? premBelief.conf() : 0);
////                //if (dConf > other.conf())
////                    oOcc = (oOcc + dOcc)/2; //TODO interpolate?
////            }*/
////
////
////        if (occOther!=ETERNAL) {
////            occReturn[0] = occ + shift;
////
////        }
//
//        return derived;
//    }

    @NotNull
    static Compound dtExact(@NotNull Compound derived, @NotNull long[] occReturn, @NotNull PremiseEval p, boolean taskOrBelief) {

        Term dtTerm = taskOrBelief ? $.unneg(p.taskTerm) : p.beliefTerm;

        Task t = p.task;
        Task b = p.belief;
        long tOcc = t.occurrence();
        if (!taskOrBelief && b != null) {
            //if (b.occurrence()!=ETERNAL) {
            int derivedInT = dtTerm.subtermTime(derived);
            if (derivedInT == DTERNAL && derived.op() == Op.IMPL) {
                //try to find the subtermTime of the implication's subject
                derivedInT = dtTerm.subtermTime(derived.term(0));
            }

            if (derivedInT == DTERNAL) {
                derivedInT = 0;
            }

            if (tOcc != ETERNAL)
                occReturn[0] = tOcc + derivedInT;

//            } else if (t.occurrence()!=ETERNAL) {
//                //find the offset of the task term within the belief term, and then add the task term's occurrence
//                occReturn[0] =
//            }
        } else {
            occReturn[0] = tOcc; //the original behavior, but may not be right
        }


        if (dtTerm instanceof Compound) {
            int dtdt = ((Compound) dtTerm).dt();
            return deriveDT(derived, +1, p, dtdt, occReturn);
        } else
            return derived;
    }


    /**
     * dt is supplied by Task
     */
    TimeFunctions dtTask = (@NotNull Compound derived, @NotNull PremiseEval p, @NotNull Conclude d, long[] occReturn, float[] confScale) ->
            dtTaskOrBelief(derived, p, occReturn, earliestOccurrence, true, false);

    TimeFunctions dtTaskEnd = (@NotNull Compound derived, @NotNull PremiseEval p, @NotNull Conclude d, long[] occReturn, float[] confScale) ->
            dtTaskOrBelief(derived, p, occReturn, earliestOccurrence, true, true);

    /**
     * dt is supplied by Belief
     */
    TimeFunctions dtBelief = (@NotNull Compound derived, @NotNull PremiseEval p, @NotNull Conclude d, long[] occReturn, float[] confScale) ->
            dtTaskOrBelief(derived, p, occReturn, earliestOccurrence, false, false);
    TimeFunctions dtBeliefEnd = (@NotNull Compound derived, @NotNull PremiseEval p, @NotNull Conclude d, long[] occReturn, float[] confScale) ->
            dtTaskOrBelief(derived, p, occReturn, earliestOccurrence, false, true);

    @NotNull
    static Compound dtTaskOrBelief(@NotNull Compound derived, @NotNull PremiseEval p, long[] occReturn, @NotNull OccurrenceSolver s, boolean taskOrBelief/*, boolean shiftToPredicate*/, boolean end) {

        long o = p.occurrenceTarget(s);
//        if (o != ETERNAL) {
//            long taskDT;
//            if (taskOrBelief) {
//                taskDT = premise.task().term().dt();
//            } else {
//                taskDT = premise.belief().term().dt();
//            }
//            if (taskDT != DTERNAL) {
//                if (end && taskDT > 0) {
//                    o += taskDT;
//                } else if (!end && taskDT < 0) {
//                    o -= taskDT;
//                }
//            }
//        }
        if (o != ETERNAL) {
            if (taskOrBelief && end) {
                //long taskDT = (taskOrBelief ? premise.task() : premise.belief()).term().dt();

                long ddt = ((Compound) $.unneg(p.taskTerm)).dt();
                if (ddt != DTERNAL)
                    o += ddt;
            } else if (taskOrBelief && !end) {
                //NOTHING NECESSARY
                /*long ddt = premise.task().term().dt();
                if (ddt != DTERNAL)
                    o -= ddt;*/
            } else {

                //Task belief = premise.belief();
                Compound beliefTerm = (Compound) p.beliefTerm;
                //if (belief != null) {
                long ddt = beliefTerm.dt();
                if (ddt != DTERNAL) {
                    if (!taskOrBelief && !end) {
                        o -= ddt;
                    } else if (!taskOrBelief && end) {
                        o += ddt;
                    }
                }
                //}
            }

        }

        occReturn[0] = o;

        return derived;
    }

    @NotNull
    static Compound deriveDT(@NotNull Compound derived, int polarity, @NotNull PremiseEval p, int eventDelta, @NotNull long[] occReturn) {
        int dt;
        dt = eventDelta == DTERNAL ? DTERNAL : eventDelta * polarity;

        return dt(derived, dt, p, occReturn);
    }

    /**
     * combine any existant DT's in the premise (assumes both task and belief are present)
     */
    @NotNull TimeFunctions dtCombine = (@NotNull Compound derived, @NotNull PremiseEval p, @NotNull Conclude d, long[] occReturn, float[] confScale) -> {

        Task task = p.task;

        int taskDT = ((Compound) $.unneg(p.taskTerm)).dt();
        Term bt = p.beliefTerm;

        int beliefDT = (bt instanceof Compound) ? ((Compound) bt).dt() : DTERNAL;

        int eventDelta;
        if (taskDT == DTERNAL && beliefDT == DTERNAL) {
            eventDelta = DTERNAL;
        } else if (taskDT != DTERNAL && beliefDT != DTERNAL) {

            Task belief = p.belief;

            if (belief != null && task.isBeliefOrGoal() && belief.isBeliefOrGoal()) {
                //blend task and belief's DT's weighted by their relative confidence
                /*float taskConf = task.confWeight();
                eventDelta = Math.round(Util.lerp(
                        taskDT,
                        beliefDT,
                        taskConf / (taskConf + belief.confWeight())
                ));*/
                eventDelta = chooseByConf(task, belief, p).dt();
//
//                //reduce confidence by the total change proportion
//                confScale[0] = eventDelta / (Math.abs(eventDelta-taskDT) + Math.abs(eventDelta-beliefDT)

                //choose dt from task with more confidence
                //eventDelta = task.conf() > belief.conf() ? taskDT : beliefDT;


            } else {
                eventDelta = taskDT;
            }

        } else if (taskDT == DTERNAL) {
            eventDelta = beliefDT;
        } else /*if (beliefDT == DTERNAL)*/ {
            eventDelta = taskDT;
        }

        occReturn[0] = p.occurrenceTarget(earliestOccurrence); //(t, b) -> t >= b ? t : b); //latest occurring one

        return deriveDT(derived, 1, p, eventDelta, occReturn);
    };

    TimeFunctions occMerge = (derived, p, d, occReturn, confScale) -> {
//        long taskOcc = p.task.occurrence();
//        long beliefOcc = p.belief.occurrence();
//        if (taskOcc == Tense.ETERNAL) {
//            occReturn[0] = beliefOcc;
//        } else if (beliefOcc == Tense.ETERNAL) {
//            occReturn[0] = taskOcc;
//        } else {
//            //merge in proportion to their conf
//            //double tConf = p.task.confWeight();
//            //double bConf = p.belief.confWeight();
//            //double newOcc = Util.lerp(taskOcc, beliefOcc, tConf / (bConf + tConf));
//
//            ;
//        }
        occReturn[0] = chooseByConf(p.task, p.belief, p).occurrence();
        return derived;
    };

//    Temporalize AutoSimple = (derived, p, d, occReturn) -> {
//
//        ConceptProcess premise = p.premise;
//
//        long occ = premise.occurrenceTarget((t, b) -> t); //reset
//
//        occReturn[0] = occ;
//
//        return derived;
//    };


    /**
     * "automatic" implementation of Temporalize, used by default. slow and wrong about 25..30% of the time sux needs rewritten or replaced
     * apply temporal characteristics to a newly derived term according to the premise's
     */
    @NotNull TimeFunctions Auto = (derived, p, d, occReturn, confScale) -> {


        @NotNull PremiseRule rule = d.rule;
        Term tp = rule.getTask();
        Term bp = rule.getBelief();

        //ConceptProcess premise = p.premise;

        Task task = p.task;
        Task belief = p.belief;

        long occ = chooseByConf(task, belief, p).occurrence(); //reset

        Compound tt = (Compound) $.unneg(p.taskTerm);
        Term bb = p.beliefTerm; // belief() != null ? belief().term() : null;

        int td = tt.dt();
        int bd = bb instanceof Compound ? ((Compound) bb).dt() : DTERNAL;

        int t = DTERNAL;

        Term cp = d.conclusionPattern; //TODO this may be a wrapped immediatefunction?

        if (derived.op().temporal && cp instanceof Compound) {

            Compound ccc = (Compound) cp;
            Term ca = ccc.term(0);

            //System.out.println(tt + " "  + bb);

        /* CASES:
            conclusion pattern size 1
                equal to task subterm
                equal to belief subterm
                unique term
            conclusion pattern size 2 (a, b)
                a equal to task subterm
                b equal to task subterm
                a equal to belief subterm
                b equal to belief subterm

         */
            int s = cp.size();
            if (s == 2) {
                Term cb = ccc.term(1);

                //chained relations
                if (td != DTERNAL && bd != DTERNAL && (tp.size() == 2) && (bp.size() == 2)) {
                    Compound tpp = (Compound) tp;
                    Compound bpp = (Compound) bp;

                    if (derivationMatch(tpp.term(1), bpp.term(0))) {
                        t = td + bd;

                        //chained inner
                        if (!derivationMatch(cb, bpp.term(1))) {
                            t = -t; //invert direction
                        }
                    } else if (derivationMatch(tpp.term(0), bpp.term(1))) {
                        //chain outer
                        t = td + bd; //?? CHECK
                    } else if (derivationMatch(tpp.term(0), bpp.term(0))) {
                        //common left
                        t = td - bd;
                    } else if (derivationMatch(tpp.term(1), bpp.term(1))) {
                        //common right
                        t = bd - td;
                    } else {
                        //throw new RuntimeException("unhandled case");
                        t = (bd + td) / 2; //???
                    }

                }

                long to = task.occurrence();
                long bo = belief != null ? belief.occurrence() : ETERNAL;

                int occDiff = (to != ETERNAL && bo != ETERNAL) ? (int) (bo - to) : 0;

                if (td == DTERNAL && bd == DTERNAL) {

                    int aTask = tp.subtermTime(ca, DTERNAL);
                    int aBelief = bp.subtermTime(ca, DTERNAL);
                    int bTask = tp.subtermTime(cb, DTERNAL);
                    int bBelief = bp.subtermTime(cb, DTERNAL);

                    if (belief != null) {

                        boolean reversed = false;
                    /* reverse subterms if commutive and the terms are opposite the corresponding pattern */
                        if (derived.op().commutative) {
                            if (!derivationMatch(
                                    p.resolve(((Compound) cp).term(0)),
                                    derived.term(0))) {
                                occDiff = -occDiff;
                                reversed = true;
                            }
                        }


                        if (aTask != DTERNAL && aBelief == DTERNAL &&
                                bBelief != DTERNAL && bTask == DTERNAL) {
                            //forward: task -> belief
                            //t = (int) (task.occurrence() - belief().occurrence());
                            t = occDiff;
                            if (reversed) occ -= t;
                            else occ += t;

                        } else if (aTask == DTERNAL && aBelief != DTERNAL &&
                                bBelief == DTERNAL && bTask != DTERNAL) {
                            //reverse: belief -> task
                            t = -occDiff;
                            //t = (int) (belief().occurrence() - task.occurrence());
                            //t = (int) (task.occurrence() - belief().occurrence());

                            if (!reversed) {
                                occ -= t;
                            } else {
                                occ += t;
                            }


                        } else {

                            //both ITERNAL

                            if ((to != ETERNAL) && (bo != ETERNAL)) {
                                t = occDiff;
                                if (reversed) occ -= t;
                                else occ += t;
                            }

                        }
                    }

                } else if (td == DTERNAL && bd != DTERNAL) {
                    //belief has dt
                    t = bd;// + occDiff;
                    //TODO align
                } else if (td != DTERNAL && bd == DTERNAL) {
                    //task has dt
                    t = td + occDiff;
                    //occ += t; //TODO check this alignment

                } else {
                    //t = occDiff;
                    //throw new RuntimeException("unhandled case");
                    //???
                    //t = (td+bd)/2;
                }
            }


//            if (t!=ITERNAL) {
//                long ot = taskPattern.subtermTime(cp, t);
//                long ob = beliefPattern.subtermTime(cp, t);
//
//                System.out.println(ot + " " + ob);
//
//            }

            //System.out.println(derived + " " + a + ":"+ aTask + "|" + aBelief + ", " + b + ":" + bTask + "|" + bBelief);


        }


        //apply occurrence shift
        if (occ > Tense.TIMELESS) {

            Term T = resolve(p, tt);
            Term B = bb != null ? resolve(p, bb) : null;
            Term C = derived;

            if (belief != null) {
                //TODO cleanup simplify this is messy and confusing

                if (task.isEternal() && !belief.isEternal()) {
                    //find relative time of belief in the task, relative time of the conclusion, and subtract
                    //the occ (=belief time's)
                    long timeOfBeliefInTask = T.subtermTime(B, td);
                    long timeOfDerivedInTask = T.subtermTime(C, td);
                    if (timeOfDerivedInTask != DTERNAL && timeOfBeliefInTask != DTERNAL)
                        occ += (timeOfDerivedInTask - timeOfBeliefInTask);
                    else if (timeOfDerivedInTask != DTERNAL)
                        occ += timeOfDerivedInTask;
                } else if (!task.isEternal() && belief.isEternal()) {
                    long timeOfTaskInBelief = B.subtermTime(T, bd);
                    long timeOfDerivedInBelief = B.subtermTime(C, bd);

                    if (timeOfTaskInBelief != DTERNAL && timeOfDerivedInBelief != DTERNAL)
                        occ += (timeOfDerivedInBelief - timeOfTaskInBelief);
                    else if (timeOfDerivedInBelief != DTERNAL)
                        occ += timeOfDerivedInBelief;
                    else {
                        long timeOfDerivedInTask = T.subtermTime(C, td);
                        if (timeOfDerivedInTask != DTERNAL) {
                            occ += timeOfDerivedInTask;
                        } else {
                            //??
                        }
                    }
                } else if (!task.isEternal() && !belief.isEternal()) {
                    //throw new RuntimeException("ambiguous task or belief");

                    //long ot = T.subtermTime(C, td);
                    //long ob = B.subtermTime(C, bd);
                    //if (t!=ITERNAL)
                    //    occ -= t;
                }
            } else {

                if (!task.isEternal()) {
                    long timeOfDerivedInTask = T.subtermTime(C, td);
                    if (timeOfDerivedInTask != DTERNAL)
                        occ += timeOfDerivedInTask;
                } else {

                    int ot = tp.subtermTime(cp, td);
                    int ob = bp.subtermTime(cp, bd);

                    if (ot != DTERNAL) {
                        if (tp instanceof Compound) {
                            Compound ctp = (Compound) tp;
                            if (derivationMatch(ctp.term(0), cp)) {
                                ot -= td;
                            }
                        }
                        occ += ot; //occ + ot;
                    } else if (ob != DTERNAL) {

                        if (belief.occurrence() != task.occurrence()) { //why?
                            if (bp instanceof Compound) {
                                Compound cbp = (Compound) bp;
                                if (!derivationMatch(cbp.term(1), cp)) {
                                    ob -= bd;
                                }
                            }
                        }

                        occ += ob;

                    } else {
                        //neither, remain eternal
                        throw new RuntimeException("unhandled case");
                    }
                }
            }


        }
        //}
        //}


        if ((t != DTERNAL) && (t != derived.dt())) {
        /*derived = (Compound) p.premise.nar.memory.index.newTerm(derived.op(), derived.relation(),
                t, derived.subterms());*/

            if (derived.size() == 2 || t == 0)
                derived = dt(derived, t, p, occReturn);

//            int nt = derived.t();
//            if (occ > TIMELESS) {
//                if (Math.signum(t) != Math.signum(nt)) {
//                    //re-align the occurrence
//                    occ -= t;
//                } else {
//                    occ -= nt;
//                }
//            }
        }


        occReturn[0] = occ;


        return derived;

    };

    @NotNull
    static Compound dt(@NotNull Compound derived, int dt, @NotNull PremiseEval p, long[] occReturn) {
        Op o = derived.op();
        if (!o.temporal) {
            dt = DTERNAL;
        }
        if (!o.temporal && dt != DTERNAL && dt != 0 && occReturn[0] != ETERNAL) {
            //something got reduced to a non-temporal, so shift it to the midpoint of what the actual term would have been:
            occReturn[0] += dt / 2;
            dt = DTERNAL;
        }
        if (derived.dt() != dt) {
            @NotNull Term n = $.terms.the(o, dt, derived.subterms());
            if (!(n instanceof Compound))
                throw new InvalidTermException(o, dt, derived.subterms(), "Untemporalizable to new DT");
            return (Compound) n;
        } else {
            return derived;
        }
    }
}
