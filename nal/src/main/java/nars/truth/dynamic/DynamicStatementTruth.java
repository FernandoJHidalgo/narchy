package nars.truth.dynamic;

import jcog.Util;
import jcog.WTF;
import jcog.data.list.FasterList;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.task.util.TaskRegion;
import nars.term.Term;
import nars.term.util.Conj;
import nars.term.util.Image;
import nars.time.Tense;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static nars.Op.*;
import static nars.term.atom.Bool.Null;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;

public class DynamicStatementTruth {

    /**
     * statement component
     */
    protected static Term stmtDecomposeStructural(Op superOp, boolean subjOrPred, Term subterm, Term common) {
        return stmtDecompose(superOp, subjOrPred, subterm, common, DTERNAL, false, true);
    }

    /**
     * statement component (temporal)
     */
    private static Term stmtDecompose(Op superOp, boolean subjOrPred, Term subterm, Term common, int dt, boolean negate, boolean structural) {
        Term s, p;

        boolean outerNegate = false;
        if (structural && subterm.op() == NEG) {
            outerNegate = true;
            subterm = subterm.unneg();
        }

        if (subjOrPred) {
            s = subterm.negIf(negate);
            p = common;
        } else {
            s = common;
            p = subterm.negIf(negate);
        }
        assert (!(s == null || p == null));

        Term y;
        if (dt == DTERNAL) {
            y = superOp.the(s, p);
        } else {
            assert (superOp == IMPL);
            y = superOp.the(s, dt, p);
        }

        if (!y.op().conceptualizable)
            return Null; //throw new WTF();

        return y.negIf(outerNegate);

    }

    /**
     * statement common component
     */
    protected static Term stmtCommon(boolean subjOrPred, Term superterm) {
        return subjOrPred ? superterm.sub(1) : superterm.sub(0);
    }

    @Nullable
    private static Term[] stmtReconstruct(boolean subjOrPred, List<Task> components) {

        //extract passive term and verify they all match (could differ temporally, for example)
        Term[] common = new Term[1];
        if (!((FasterList<Task>) components).allSatisfy(tr -> {
            Term uu = tr.term().unneg();
            Term tt = subjOrPred ? uu.sub(1) : uu.sub(0);
            Term p = common[0];
            if (p == null) {
                common[0] = tt;
                return true;
            } else {
                return p.equals(tt);
            }
        }) || (common[0] == null))
            return null; //differing passive component; TODO this can be detected earlier, before truth evaluation starts

        return Util.map(0, components.size(), Term[]::new, tr ->
                //components.get(tr).task().term().sub(subjOrPred ? 0 : 1)
                subSubjPredWithNegRewrap(!subjOrPred, components.get(tr))
        );
    }

    private static Term subSubjPredWithNegRewrap(boolean subjOrPred, TaskRegion tr) {
        Term t = tr.task().term();
        boolean neg = t.op() == NEG;
        if (neg) {
            t = t.unneg();
        }

        Term tt = t.sub(subjOrPred ? 1 : 0 /* reverse */);
        if (neg) {

            //assert(!subjOrPred && t.op()==IMPL); //impl predicate

            tt = tt.neg();
        }
        return tt;
    }

    @Nullable
    protected static Term stmtReconstruct(Term superterm, List<Task> components, boolean subjOrPred, boolean union, boolean subjNeg) {
        Term superSect = superterm.sub(subjOrPred ? 0 : 1);
        Op op = superterm.op();
        if (union) {
            if (superSect.op() == NEG) {
                if (op == CONJ || op == IMPL /* will be subj only, pred is auto unnegated */)
                    superSect = superSect.unneg();
                else {
                    throw new WTF();
                }

            }
        }

        //may not be correct TODO
//        if (!Param.DEBUG) {
//            //elide reconstruction when superterm will not differ by temporal terms
//            //TODO improve
//            if (superSect.subs() == components.size() && ((FasterList<Task>) components).allSatisfy(t -> t != null && !((Task) t).term().sub(subjOrPred ? 0 : 1).isTemporal())) {
//                if (!superSect.isTemporal())
//                    return superterm;
//            }
//        }


        Term sect;
        int outerDT;
        if (op == IMPL) {
            //IMPL: compute innerDT for the conjunction
            Conj c = new Conj();
            for (TaskRegion x : components) {
                Term xx = ((Task) x).term();

                boolean forceNegate = false;
                if (xx.op() == NEG) {

//                    if (op == IMPL) {
                    if (xx.unneg().op() == IMPL) {
                        xx = xx.unneg();
                        forceNegate = true;
                    } else {
                        if (!subjOrPred) {
                            //assume this is the reduced (true ==> --x)
                            c.add(ETERNAL, xx.neg());
                            continue;
                        } else {
                            throw new WTF();
                        }
                    }
                } else if (xx.op() != IMPL) {
                    if (!subjOrPred) {
                        //assume this is the reduced (true ==> x)
                        c.add(ETERNAL, xx);
                        continue;
                    } else {
                        //throw new WTF();
                        return null;
                    }
                }

                int tdt = xx.dt();
                if (!c.add(tdt == DTERNAL ? ETERNAL : -tdt, xx.sub(subjOrPred ? 0 : 1).negIf(union ^ forceNegate)))
                    break;
            }

            sect = c.term();
            if (sect == Null)
                return null; //but allow other Bool's

            long cs = c.shift();
            if (cs == DTERNAL || cs == ETERNAL) {
                outerDT = DTERNAL; //some temporal information destroyed
            } else {
                long shift = -cs - sect.eventRange();
                outerDT = Tense.occToDT(shift);
            }

        } else {

            Term[] subs = stmtReconstruct(subjOrPred, components);
            if (subs == null)
                return null;

            if (union && superSect.op() == CONJ) {
                for (int i = 0, subsLength = subs.length; i < subsLength; i++) {
                    subs[i] = subs[i].neg();
                }
            }

            sect = superSect.op().the(subs);
            outerDT = DTERNAL;
        }

        Term common = superterm.sub(subjOrPred ? 1 : 0);

        if (union || (subjOrPred && subjNeg)) {
            if (op == CONJ || op == IMPL /* but not Sect's */)
                sect = sect.neg();
        }

        return subjOrPred ? op.the(sect, outerDT, common) : op.the(common, outerDT, sect);
    }

    static public boolean decomposeImplConj(Term superterm, long start, long end, AbstractDynamicTruth.ObjectLongLongPredicate<Term> each, Term common, Term decomposed, boolean subjOrPred, boolean negateComponents) {

//        int outerDT = superterm.dt();
        long is, ie;
        if (start == ETERNAL) {
            is = ie = ETERNAL;
        } else {
            is = start;
            ie = end + decomposed.eventRange();// + outerDT;
        }

        return DynamicConjTruth.ConjIntersection.components(decomposed, is, ie, (what, s, e) -> {
            //TODO fix
            int innerDT = (s == ETERNAL) ? DTERNAL : Tense.occToDT(
                    //(e-s)-outerDT
                    e - s
            );
            Term i;
            if (subjOrPred) {
                if (negateComponents)
                    what = what.neg();
                i = IMPL.the(what, innerDT, common);
            } else {
                i = IMPL.the(common, innerDT, what);
                if (negateComponents)
                    i = i.neg();
            }
            return each.accept(i, s, e);
        });
//        int innerDT = decomposed.dt();
//        int decRange;
//        switch (outerDT) {
//            case DTERNAL:
//                decRange = 0;
//                break;
//            case XTERNAL:
//                decRange = XTERNAL;
//                break;
//            default:
//                decRange = decomposed.eventRange();
//                break;
//        }
//        boolean startSpecial = (start == ETERNAL || start == XTERNAL);
//        //TODO use dynamic conjunction decompose which provides factoring
//        Op superOp = superterm.op();
//        return decomposed.eventsWhile((offset, y) -> {
//                    boolean ixTernal = startSpecial || offset == ETERNAL || offset == XTERNAL;
//
//                    long subStart = ixTernal ? start : start + offset;
//                    long subEnd = end;
//                    if (subEnd < subStart) {
//                        //swap
//                        long x = subStart;
//                        subStart = subEnd;
//                        subEnd = x;
//                    }
//
//                    int occ = (outerDT != DTERNAL && decRange != XTERNAL) ? occToDT(decRange - offset + outerDT) : XTERNAL;
//                    Term x = stmtDecompose(op, subjOrPred, y, common,
//                            ixTernal ? DTERNAL : occ, negateComponents, false);
//
//                    if (x == Null || x.unneg().op()!=superOp)
//                        return false;
//
//                    return each.accept(x, subStart, subEnd);
//                }
//                , outerDT == DTERNAL ? ETERNAL : 0, innerDT == 0,
//                innerDT == DTERNAL,
//                innerDT == XTERNAL, 0);
    }

    static class DynamicInhSectTruth extends AbstractSectTruth {

        final boolean subjOrPred;

        private DynamicInhSectTruth(boolean union, boolean subjOrPred) {
            super(union);
            this.subjOrPred = subjOrPred;
        }

        @Override
        public boolean acceptComponent(Term superTerm, Term componentTerm, Task componentTask) {
            return componentTask.op() == superTerm.op();
        }

        @Override
        public Term reconstruct(Term superterm, List<Task> components, NAR nar, long start, long end) {
            return reconstruct(superterm, components, subjOrPred, unionOrIntersection);
        }

        protected static Term reconstruct(Term superterm, List<Task> components, boolean subjOrPred, boolean union) {
            return stmtReconstruct(superterm, components, subjOrPred, union, false);
        }

        @Override
        public boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each) {



            Term decomposed = stmtCommon(!subjOrPred, superterm);
            if (!decomposed.op().isAny(Op.Sect)) {
                //try Image normalizing
                superterm = Image.imageNormalize(superterm);
                decomposed = stmtCommon(!subjOrPred, superterm);
            }

            if (decomposed.op().isAny(Op.Sect)) {
                Term common = stmtCommon(subjOrPred, superterm);

                Op op = superterm.op();

                return decomposed.subterms().AND(
                        y -> each.accept(stmtDecomposeStructural(op, subjOrPred, y, common), start, end)
                );
            }
            assert (false);
//                if (union) {
//                    if (decomposed.op() == NEG) {
//                        if (superterm.op() == IMPL) {
//                            decomposed = decomposed.unneg();
//                        } else {
//                            //leave as-is
//                            // assert (decomposed.op() == CONJ /* and not Sect/Union */) : "unneg'd decomposed " + decomposed + " in superterm " + superterm;
//                        }
//                    }
//                }


            return false;
        }


    }


    public static final AbstractDynamicTruth UnionSubj = new DynamicInhSectTruth(true, true);
    public static final AbstractDynamicTruth SectSubj = new DynamicInhSectTruth(false, true);


    public static final AbstractDynamicTruth UnionPred = new DynamicInhSectTruth(true, false);
    public static final AbstractDynamicTruth SectPred = new DynamicInhSectTruth(false, false);

    /**
     * according to composition rules, the intersection term is computed with union truth, and vice versa
     */
    public static final AbstractDynamicTruth SectImplSubj = new SectImplSubj();
//        public static final DynamicTruthModel SectImplSubjNeg = new SectImplSubj() {
//            @Override
//            public boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
//                return components(superterm, start, end, each, superterm.sub(0).unneg());
//            }
//
//            @Override
//            public Term reconstruct(Term superterm, List<Task> components, NAR nar) {
//                return stmtReconstruct(superterm, components, subjOrPred, union, true);
//
//            }
//        };

    public static final AbstractDynamicTruth UnionImplSubj = new UnionImplSubj();

    public static final AbstractDynamicTruth SectImplPred = new DynamicInhSectTruth(false, false) {
        @Override
        public boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
            Term common = stmtCommon(subjOrPred, superterm);
            Term decomposed = stmtCommon(!subjOrPred, superterm);
            return decomposeImplConj(superterm, start, end, each, common, decomposed, false, false);
        }
    };


//        public static final DynamicTruthModel SectRoot = new Intersection(false) {
//
//            @Override
//            public boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
//                assert (superterm.op() == SECTe);
//                return superterm.subterms().AND(s ->
//                        each.accept(s, start, end)
//                );
//            }
//
//            @Override
//            public Term reconstruct(Term superterm, List<Task> components, NAR nar) {
//
//                //TODO test if the superterm will be equivalent to the component terms before reconstructing
//                Term[] t = new Term[components.size()];
//                for (int i = 0, componentsSize = components.size(); i < componentsSize; i++) {
//                    t[i] = components.get(i).term();
//                }
//
//
//                return Op.SECTi.the(t);
//            }
//
//        };

    private static class SectImplSubj extends DynamicInhSectTruth {
        public SectImplSubj() {
            super(false, true);
        }

        @Override
        public boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
            return components(superterm, start, end, each, superterm.sub(0));
        }

        protected static boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each, Term subj) {
            return decomposeImplConj(superterm, start, end, each, superterm.sub(1), subj, true, false);
        }

        @Override
        public Term reconstruct(Term superterm, List<Task> components, NAR nar, long start, long end) {
            return reconstruct(superterm, components, true, false);
        }
    }

    private static class UnionImplSubj extends DynamicInhSectTruth {
        public UnionImplSubj() {
            super(true, true);
        }

        @Override
        public boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
            return components(superterm, start, end, each, superterm.sub(0).unneg());
        }

        protected boolean components(Term superterm, long start, long end, ObjectLongLongPredicate<Term> each, Term subj) {
            return decomposeImplConj(superterm, start, end, each, superterm.sub(1), subj, true, true);
        }

        @Override
        public Term reconstruct(Term superterm, List<Task> components, NAR nar, long start, long end) {
            return reconstruct(superterm, components, true, true);
        }
    }
}