package nars.term;

import jcog.data.ArrayHashSet;
import jcog.list.FasterList;
import jcog.util.ArrayIterator;
import jcog.util.CartesianIterator;
import jcog.version.VersionMap;
import jcog.version.Versioning;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.concept.Operator;
import nars.subterm.Subterms;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.control.AbstractPred;
import nars.unify.match.EllipsisMatch;
import nars.util.term.transform.DirectTermTransform;
import nars.util.term.transform.TermTransform;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import static nars.Op.*;

public class Evaluation {

    private static final Functor TRUE = new Functor.TheAbstractInlineFunctor1Inline("\"" + True + '"', x->null);

//    static final ThreadLocal<Evaluation> eval = ThreadLocal.withInitial(Evaluation::new);

    private List<Predicate<VersionMap<Term, Term>>[]> proc = null;

    Versioning v;

    VersionMap<Term, Term> subst;

    boolean wrapBool = false;

    public Evaluation() {

    }

    protected void ensureReady() {
        if (v == null) {
             v = new Versioning<>(16, 128);
             subst = new VersionMap<>(v);
             proc = new FasterList<>(1);
        }
    }


    public static Evaluation start(boolean wrapBool) {
        return new Evaluation().wrapBool(wrapBool);
    }

    public static ArrayHashSet<Term> solveAll(Evaluation e, Term x, NAR nar) {
        return solveAll(e, x, nar.functors, true);
    }

    public static ArrayHashSet<Term> solveAll(Term x, NAR nar) {
        return solveAll(null, x, nar);
    }

    static final TermTransform trueUnwrapper = new TermTransform.NegObliviousTermTransform() {
        @Override
        public @Nullable Term transformCompoundUnneg(Compound x) {
            if (Operator.func(x).equals(TRUE)) {
                return Operator.arg(x, 0).transform(this);
            }
            return TermTransform.NegObliviousTermTransform.super.transformCompoundUnneg(x);
        }

        @Override
        public boolean eval() {
            return false;
        }
    };
    public static ArrayHashSet<Term> solveAll(Evaluation e, Term x, TermContext context, boolean wrapBool) {
        ArrayHashSet<Term> all = new ArrayHashSet<>(1);
        Evaluation.solve(e, x, wrapBool, context, (y) -> {
            y = possiblyNeedsEval(y) ? y.transform(trueUnwrapper) : y;


            all.add(y);

            return true;
        });
        return !all.isEmpty() ? all : ArrayHashSet.EMPTY;
    }

    public static boolean solve(@Nullable Evaluation e, Term x, boolean wrapBool, TermContext context, Predicate<Term> each) {
        Term y = needsEvaluation(x, context);
        if (y == null)
            return each.test(x);
        else
            return (e != null ? e.wrapBool(wrapBool) : Evaluation.start(wrapBool)).get(y, each);
    }

    @Nullable
    private static Term needsEvaluation(Term x, TermContext context) {

        if (!possiblyNeedsEval(x))
            return null;

        MyFunctorResolver ft = new MyFunctorResolver(context);
        Term y = x.transform(ft);

        if (y == Null) {
            return Null;
        }

        if (!ft.hasFunctor) {
            return null;
        }

        //TODO add flag if all functors involved are InlineFunctor

        return y;
    }

    public static boolean possiblyNeedsEval(Term x) {
        return x.hasAll(Op.FuncBits);
    }

    public static Term solveAny(Term x, Evaluation e, TermContext context, Random random) {
        ArrayHashSet<Term> results = solveAll(e, x, context, false);
        return results.get(random);
    }

    protected Term eval(Term x) {
        return boolWrap(x, _eval(x));
    }

    protected Term boolWrap(Term x, Term y) {
        if (!(y instanceof Bool) || !wrapBool) {
            return y; //no change
        }

        if (y == Null)
            return Null;
        else {
            // if (y == True || y == False || y.hasAny(Op.BOOL)) {
            {
//                    boolean hasFalse = y == False; || y.ORrecurse(t -> t == False);
//            if (hasFalse)
//                z = False; //TODO maybe record what part causes the falsity

                //determined absolutely true or false: implies that this is the answer to a question
                return $.funcFast(TRUE, y == False ? x.neg() : x);

                //return hasFalse ? False : True;
            }
        }
    }

    protected Term _eval(Term c) {
        Op o = c.op();
        if (o == NEG) {
            Term xu = c.unneg();
            Term yu = _eval(xu);
            if (xu != yu)
                return yu.neg();
            return c; //unchanged
        }

        /*if (hasAll(opBits))*/

        /*if (subterms().hasAll(opBits))*/

        Subterms uu = c.subterms();
        Term[] xy = null;


        int ellipsisAdds = 0, ellipsisRemoves = 0;

        boolean evalConjOrImpl = wrapBool ? false : o == CONJ || o == IMPL;
        int polarity = 0;

        for (int i = 0, n = uu.subs(); i < n; i++) {
            Term xi = xy != null ? xy[i] : uu.sub(i);
            Term yi = possiblyNeedsEval(xi) ? _eval(xi) : xi;
            if (yi == Null)
                return Null;
            if (evalConjOrImpl && yi instanceof Bool && (i == 0 /* impl subj only */ || o == CONJ)) {
                if (yi == True) {
                    polarity = +1;
                } else /*if (yi == False)*/ {
                    if (o == IMPL)
                        return Null;
                    polarity = -1;
                    break;
                }
            } else {
                if (xi != yi) {
                    yi = boolWrap(xi, yi);
                }
            }

            if (xi != yi) {
                if (yi == null) {

                } else {


                    if (yi instanceof EllipsisMatch) {
                        int ys = yi.subs();
                        ellipsisAdds += ys;
                        ellipsisRemoves++;
                    }

                    if (xi!=yi) {
                        if (xy == null) {
                            xy = ((Compound) c).arrayClone();
                        }
                        xy[i] = yi;
                    }
                }
            }
        }

        if (polarity != 0) {
            if (o == CONJ) {
                if (polarity < 0) {
                    return False; //short circuit
                }
            } else if (o == IMPL) {
                assert(polarity > 0); //False and Null already handled
                return xy[1];
            }
        }


        Term u;
        if (xy != null) {
            if (ellipsisAdds > 0) {

                xy = EllipsisMatch.flatten(xy, ellipsisAdds, ellipsisRemoves);
            }

            u = o.compound(c.dt(), xy);
            o = u.op();
            uu = u.subterms();
        } else {
            u = c;
        }


        if (o == INH && uu.hasAll(Op.FuncInnerBits)) {
            Term pred, subj;
            if ((pred = uu.sub(1)) instanceof Functor && (subj = uu.sub(0)).op() == PROD) {

                Term v = ((BiFunction<Evaluation, Subterms, Term>) pred).apply(this, subj.subterms());
                if (v != null) {
                    if (v instanceof AbstractPred) {
                        u = $.the(((Predicate) v).test(null));
                    } else {
                        u = v;
                    }
                } /* else v == null, no change */
            }
        }

//        if (u != c && (u.equals(c) && u.getClass() == c.getClass()))
//            return c;

        return u;
    }

    public Evaluation wrapBool(boolean wrapBool) {
        this.wrapBool = wrapBool;
        return this;
    }

    private Evaluation clear() {
        if (v!=null) {
            proc.clear();
            v.reset();
            subst.clear();
        }
        return this;
    }

    public boolean get(Term _x, Predicate<Term> each) {

        Term x = eval(_x);

        int np = procs();
        if (np == 0)
            return each.test(x); //done

        Iterator<Predicate<VersionMap<Term, Term>>[]> pp;

        Iterable[] aa = new Iterable[np];
        for (int i = 0; i < np; i++)
            aa[i] = ArrayIterator.iterable(proc.get(i));

        pp = new CartesianIterator<Predicate<VersionMap<Term, Term>>>(Predicate[]::new, aa);

        int start = v.now();

        nextPermute:
        while (pp.hasNext()) {

            Term y;

            v.revert(start);

            Predicate<VersionMap<Term, Term>>[] n = pp.next();
            assert (n.length > 0);

            for (Predicate p : n) {
                if (!p.test(subst)) {
                    return each.test(False);
                    //continue nextPermute;
                }
            }

            y = x.replace(subst);
            if (y == null)
                continue;

            Term z = eval(y);

            int ps = procs();
            if (z != null && np == ps && !each.test(z))
                return false;

            if (np < ps) {
                int before = v.now();
                if (!get(z, each))
                    return false;
                v.revert(before);
            }

        }


        return true;
    }

    public int procs() {
        List<Predicate<VersionMap<Term, Term>>[]> p = this.proc;
        return p!=null ? p.size() : 0;
    }


    public void replace(Term x, Term xx) {
        replace(subst(x, xx));
    }

    public void replace(Term x, Term xx, Term y, Term yy) {
        replace(subst(x, xx, y, yy));
    }

    public void replace(Predicate... r) {
        ensureReady();
        proc.add(r);
    }

    private Predicate<VersionMap<Term, Term>> subst(Term x, Term xx) {
        return (m) -> {
            Term px = m.get(x);
            if (px != null) {
                return px.equals(xx);
            } else {
                m.tryPut(x, xx);
                return true;
            }
        };
    }

    public Predicate<VersionMap<Term, Term>> subst(Term x, Term xx, Term y, Term yy) {
        return (m) -> subst(x, xx).test(m) && subst(y, yy).test(m);
    }

    /**
     * interface necessary for evaluating terms
     */
    public interface TermContext extends Function<Term, Term> {


        /**
         * elides superfluous .term() call
         */
        default Term applyTermIfPossible(/*@NotNull*/ Term x, Op supertermOp, int subterm) {
            Term y = apply(x);
            return y != null ? y.term() : x;
        }


        class MapTermContext implements TermContext {
            private final ImmutableMap<Term, Term> resolvedImm;

            public MapTermContext(MutableMap<Term, Term> resolved) {
                this(resolved.toImmutable());
            }

            public MapTermContext(ImmutableMap<Term, Term> resolvedImm) {
                this.resolvedImm = resolvedImm;
            }

            @Override
            public Term apply(Term term) {
                if (term.op() == ATOM) {
                    Term r = resolvedImm.get(term);
                    if (r != null)
                        return r;
                }
                return term;
            }
        }
    }

    private static final class MyFunctorResolver implements DirectTermTransform {
        private final TermContext context;

        public boolean hasFunctor;

        MyFunctorResolver(TermContext context) {
            this.context = context;
        }

        @Override
        public boolean eval() {
            return false; //not at this stage
        }

        @Override
        public @Nullable Term transformAtomic(Atomic z) {
            if (z instanceof Functor) {
                hasFunctor = true;
                return z;
            }

            if (z.op() == ATOM) {
                Term zz = context.applyTermIfPossible(z, null, 0);
                if (zz instanceof Functor)
                    hasFunctor = true;
                return zz;
            }
            return z;
        }

    }
}
