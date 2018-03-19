package nars.concept.dynamic;

import nars.NAR;
import nars.Op;
import nars.Task;
import nars.table.TemporalBeliefTable;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.truth.Truth;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.CONJ;
import static nars.time.Tense.XTERNAL;


/** computes dynamic truth according to implicit truth functions
 *  determined by recursive evaluation of the compound's sub-component's truths
 */
public class DynamicTruthBeliefTable extends DynamicBeliefTable {

    private final DynamicTruthModel model;


    public DynamicTruthBeliefTable(Term c, TemporalBeliefTable t, DynamicTruthModel model, boolean beliefOrGoal) {
        super(c, beliefOrGoal, t);
        this.model = model;
    }

    @Override
    public boolean isEmpty() {
        /** since this is a dynamic evaluation, we have to assume it is not empty */
        return false;
    }

    public DynTruth truth(long start, long end, Term template, NAR n) {
        return model.eval(template, beliefOrGoal, start, end, n);
    }

    @Override
    protected Task taskDynamic(long start, long end, @NotNull final Term _template, NAR nar) {

        Term template = template(start, end, _template, nar);
        if (template == null)
            return null;

        DynTruth yy = truth(start, end, template, nar);
        if (yy != null) {
            return yy.task(template, model, beliefOrGoal, nar);
        } else {
            return null;
        }
    }



    @Override
    protected @Nullable Truth truthDynamic(long start, long end, NAR nar) {
        DynTruth d = model.eval(term, beliefOrGoal, start, end, nar);
        if (d!=null)
            return d.truth(term, model, beliefOrGoal, nar);
        else
            return null;
    }


    @Override
    @Nullable
    protected Term template(long start, long end, Term template, NAR nar) {
        Op templateOp = template.op();
        if (this.term != null && templateOp != this.term.op())
            return null; //template doesnt match this (quick op test)

        int templateSubs = template.subs();
        assert (templateSubs > 1);
        boolean temporal = templateOp.temporal;
        if (temporal) {
            int d = template.dt();
            if (d == XTERNAL) {
                int e = matchDT(start, end, templateSubs > 2, nar);
                assert (e != XTERNAL);
                Term next = template.dt(e);

                if ((next.subs() < templateSubs || next.dt() == XTERNAL)) {

                    /*if no dt can be calculated, return
                              0 or some non-zero value (ex: 1, end-start, etc) in case of repeating subterms. */
                    int artificialDT;


                    if (templateSubs == 2) {
                        if (start != end && end - start < Integer.MAX_VALUE) {
                            if (end != start) {
                                artificialDT = (int) (end - start);
                            } else {
                                artificialDT =
                                        (template.sub(0).unneg().equals(template.sub(1).unneg())) ?
                                                nar.dur() :
                                                0; //ok for simultaneous
                            }
                        } else {
                            artificialDT = nar.dur();
                        }


                    } else /*(if (templateSubs > 2)*/ {
                        assert (templateSubs > 2);
                        artificialDT = 0; //commutive conjunction
                    }

                    next = template.dt(artificialDT);

                    if (next.subs() < templateSubs || next.dt() == XTERNAL) {
                        next = template;
                        if (next.subs() == 2) {

                            //possibly pulled an internal XTERNAL to the outside, so try artificializing this as well
                            int limit = 2;
                            int nextDT = XTERNAL;
                            do {
                                next = next.dt(artificialDT);
                                if (next instanceof Bool)
                                    return null;
                            } while (limit-- > 0 && (nextDT = next.dt()) == XTERNAL);

                            if (nextDT == XTERNAL)
                                return null; //give up

                        } else {
                            //create a random sequence of the terms, separated by artificial DT's
                            assert (templateOp == CONJ);
                            Term[] subs = template.subterms().arrayClone();
                            ArrayUtils.shuffle(subs, nar.random());
                            int dur = nar.dur();
                            next = subs[0];
                            for (int k = 1; k < subs.length; k++) {
                                next = Op.conjMerge(next, 0, subs[k], dur);
                                if (next instanceof Bool)
                                    return null; //it is probably possible to find another solution with a different shuffle
                            }
                        }
                    }
                }

                template = next;
            }
        }
        return template;
    }


}
//    /** prepare a term, if necessary, for use as template  */
//    private Term template(Term template, long start, long end, NAR nar) {
//        if (template.dt() == XTERNAL) {
//            int newDT = matchDT(template, start, end);
//            template = template.dt(newDT);
//        }
//
//        //still XTERNAL ? try using start..end as a dt
//        if (start!=DTERNAL && template.dt() == XTERNAL && template.subs()==2) {
//            template = template.dt((int) (end-start));
//        }
//
//        Retemporalize retemporalizeMode =
//                template.subterms().OR(Term::isTemporal) ?
//                    Retemporalize.retemporalizeXTERNALToZero  //dont unnecessarily attach DTERNALs to temporals
//                        :
//                    Retemporalize.retemporalizeXTERNALToDTERNAL //dont unnecessarily create temporals where DTERNAL could remain
//        ;
//        template = template.temporalize(retemporalizeMode);
//        if (template == null)
//            return null;
//
//
//        if (!template.conceptual().equals(term))
//            return null; //doesnt correspond to this concept anyway
//
//        return template;
//
////        if (t2 == null) {
////
////
////
////            //for some reason, retemporalizing to DTERNAL failed (ex: conj collision)
////            //so as a backup plan, use dt=+/-1
////            int dur = nar.dur();
////            Random rng = nar.random();
////            t2 = template.temporalize(new Retemporalize.RetemporalizeFromToFunc(XTERNAL,
////                    () -> rng.nextBoolean() ? +dur : -dur));
////        }
//////        if (t2!=null && t2.dt()==XTERNAL) {
//////            return template(t2, start, end ,nar);//temporary
//////            //throw new RuntimeException("wtf xternal");
//////        }
////
////        return t2;
//    }