package nars.derive;

import nars.Op;
import nars.The;
import nars.derive.match.Ellipsis;
import nars.derive.match.EllipsisMatch;
import nars.derive.mutate.Choose1;
import nars.derive.mutate.Choose2;
import nars.derive.mutate.CommutivePermutations;
import nars.term.Compound;
import nars.term.GenericCompoundDT;
import nars.term.Term;
import nars.term.container.TermContainer;
import nars.term.subst.Unify;
import org.jetbrains.annotations.Nullable;

import java.lang.instrument.Instrumentation;
import java.util.SortedSet;
import java.util.TreeSet;


@Deprecated
abstract public class PatternCompound extends GenericCompoundDT {

//    final int sizeCached;
//    final int structureNecessary;
//    private final boolean commutative; //cached
//    transient final private Op op; //cached
//    private final int minVolumeNecessary;
//    private final int size;

//    @Nullable public final Set<Variable> uniqueVars;

    PatternCompound(/*@NotNull*/ Op op, int dt, TermContainer subterms) {
        super((Compound) op.the(subterms.theArray()), dt);
    }

    abstract protected static class PatternCompoundWithEllipsis extends PatternCompound {

        final Ellipsis ellipsis;
        final int structureRequired;

        PatternCompoundWithEllipsis(/*@NotNull*/ Op seed, int dt, Ellipsis ellipsis, TermContainer subterms) {
            super(seed, dt, subterms);

            this.ellipsis = ellipsis;
            this.structureRequired = subterms.structure() & ~(Op.VariableBits);
        }

        abstract protected boolean matchEllipsis(TermContainer y, Unify subst);

        @Override
        public final boolean unify(Term y, Unify subst) {
            return y.hasAll(structureRequired) && op() == y.op() && matchEllipsis(y.subterms(), subst);
        }


    }


    public static class PatternCompoundWithEllipsisLinear extends PatternCompoundWithEllipsis {

        public PatternCompoundWithEllipsisLinear(/*@NotNull*/ Op op, int dt, Ellipsis ellipsis, TermContainer subterms) {
            super(op, dt, ellipsis, subterms);
        }

        @Override
        protected boolean matchEllipsis(TermContainer y, Unify subst) {
            return matchEllipsedLinear(y, subst);
        }

        /**
         * non-commutive compound match
         * X will contain at least one ellipsis
         * <p>
         * match subterms in sequence
         * <p>
         * WARNING this implementation only works if there is one ellipse in the subterms
         * this is not tested for either
         */
        final boolean matchEllipsedLinear(TermContainer Y, Unify u) {

            int i = 0, j = 0;
            int xsize = subs();
            int ysize = Y.subs();

            //TODO check for shim and subtract xsize?

            while (i < xsize) {
                Term x = sub(i++);

                if (x instanceof Ellipsis) {
                    int available = ysize - j;

                    Term eMatched = u.xy(x); //EllipsisMatch, or null
                    if (eMatched == null) {

                        //COLLECT
                        if (i == xsize) {
                            //SUFFIX
                            if (!ellipsis.validSize(available))
                                return false;

                            return ellipsis.unify(EllipsisMatch.match(Y, j, j + available), u);

                        } else {
                            //PREFIX the ellipsis occurred at the start and there are additional terms following it
                            //TODO
                            return false;
                        }
                    } else {
                        //assert(false): "TODO check this case in PatternCompound ellipsis linear";

                        if (eMatched instanceof EllipsisMatch) {
                            EllipsisMatch ex = (EllipsisMatch) eMatched;
                            if (!ex.linearMatch(Y, j, u))
                                return false;
                            j += ex.subs();
                        } else {
                            //it is a single ellipsis term to unify against
                            if (!sub(j).unify(eMatched, u))
                                j++;
                        }
                    }
                    //previous match exists, match against what it had
//                        if (i == xsize) {
//                        //SUFFIX - match the remaining terms against what the ellipsis previously collected
//                        //HACK this only works with EllipsisMatch type
//                        Term[] sp = ((EllipsisMatch) eMatched).term;
//                        if (sp.length!=available)
//                            return false; //incorrect size
//
//                        //match every item
//                        for (Term aSp : sp) {
//                            if (!match(aSp, Y.term(j++)))
//                                return false;
//                        }
//                        } else {
//                            //TODO other cases
//                            return false;
//                        }
//                    }
                } else {
                    if (ysize <= j || !x.unify(Y.sub(j++), u))
                        return false;
                }
            }

            return true;
        }


    }


    public static final class PatternCompoundWithEllipsisCommutive extends PatternCompoundWithEllipsis {


//        /** the components of this pattern compound other than the ellipsis "*/
//        final ImmutableSet<Term> fixed;

        public PatternCompoundWithEllipsisCommutive(Op op, int dt, Ellipsis ellipsis, TermContainer subterms) {
            super(op, dt, ellipsis, subterms);

//            MutableSet<Term> f = new UnifiedSet();
//            subterms.forEach(s -> {
//                if (!s.equals(ellipsis))
//                    f.add(s);
//            });
//            this.fixed = f.toImmutable();

        }

        /**
         * commutive compound match: Y into X which contains one ellipsis
         * <p>
         * X pattern contains:
         * <p>
         * one unmatched ellipsis (identified)
         * <p>                    //HACK should not need new list
         * <p>
         * zero or more "constant" (non-pattern var) terms
         * all of which Y must contain
         * <p>
         * zero or more (non-ellipsis) pattern variables,
         * each of which may be matched or not.
         * matched variables whose resolved values that Y must contain
         * unmatched variables determine the amount of permutations/combinations:
         * <p>
         * if the number of matches available to the ellipse is incompatible with the ellipse requirements, fail
         * <p>
         * (total eligible terms) Choose (total - #normal variables)
         * these are then matched in revertable frames.
         * <p>
         * *        proceed to collect the remaining zero or more terms as the ellipse's match using a predicate filter
         *
         * @param y the compound being matched to this
         */
        @Override
        protected boolean matchEllipsis(TermContainer y, Unify u) {
            //return subst.matchEllipsedCommutative(
            //        this, ellipsis, y
            //);
            //public final boolean matchEllipsedCommutative(Compound X, Ellipsis Xellipsis, Compound Y) {

            SortedSet<Term> xFixed = new TreeSet();//$.newHashSet(0); //Global.newHashSet(0);


            final Ellipsis ellipsis = this.ellipsis;

            SortedSet<Term> yFree = y.toSortedSet();

            int s = subs();

            for (int k = 0; k < s; k++) {


                Term x = sub(k);

                if (x.equals(ellipsis)) {
                    Term v = u.xy(x);
                    if (v != null) {
                        if (v instanceof EllipsisMatch) {
                            return ((EllipsisMatch) v).rematch(y, yFree);
                        } else {
                            Term vv = v; //single-term matched for the ellipsis, so wont be EllipsisMatch instance
                            if (!yFree.remove(vv))
                                return false;
                        }
                    }

                    continue;
                }

                //find (randomly) at least one element of 'y' which unifies with this fixed variable
                //if one matches, remove it from yFree
                //if none match, fail
                //TODO this should be part of the termutator since there could be more than one match
//                int dir = subst.random.nextBoolean() ? +1 : -1;
//                int u = subst.random.nextInt(s);
//                boolean matched = false;
//                for (int w = 0; w < s; w++, u+=dir) {
//                    if (u == -1) u = s - 1;
//                    else if (u == s) u = 0;
//                    //if (!yFree.contains(yu)) continue //?? would this help?
//                    Term yu = y.sub(u);
//                    if (subst.putXY(x, yu)) {
//                        matched = true;
//                        yFree.remove(yu);
//                        break;
//                    }
//                }
//                if (!matched)
//                    return false;

                /*if (v instanceof EllipsisMatch) {

                    //assume it's THE ellipsis here, ie. x == xEllipsis by testing that Y contains all of these
                    if (!((EllipsisMatch) v).addWhileMatching(y, alreadyInY, ellipsis.sizeMin())) {
                        return false;
                    } else {
                        //Xellipsis = null;
                        ellipsisMatched = true;
                        break; //continued below
                    }


                } else */
//                if (v != null) {
//
//                    if (!yFree.remove(v)) {
//                        //required but not actually present in Y
//                        return false;
//                    }
//
//                } else {

                Op xo = x.op();
                boolean xConst = !u.matchType(xo);
                if (!xConst) {
                    //try to resolve an already assigned and thus resolvable to constant
                    @Nullable Term previouslyMatched = u.xy(x);
                    if (previouslyMatched != null) {
                        x = previouslyMatched;
                    }

                    xFixed.add(x); //x is a variable which must be termuted when everything non-X is assigned

                } else {

                    if (yFree.remove(x)) {
                        //matched constant
                        //xFixed.remove(x); //<- probably not necessary
                    } else {
                        if ((u.type == null && (x.vars()+x.varPattern()==0)) || (u.type!=null && !x.hasAny(u.type)))
                            return false; //unmatched constant offers no possibility of eventual unification

                        xFixed.add(x);
                    }

//                    if (!yFree.remove(x))
//                        xFixed.add(x);
                }

                //         }


            }

            final int xs = xFixed.size();
            int ys = yFree.size();
            int numRemainingForEllipsis = ys - xs;

            //if not invalid size there wouldnt be enough remaining matches to satisfy ellipsis cardinality
            boolean vs = ellipsis.validSize(numRemainingForEllipsis);
            if (!vs)
                return false;

            switch (xs) {
                case 0: //match everything to everything

                    Term match = ys > 0 ? EllipsisMatch.match(yFree) : EllipsisMatch.empty;

                    if (subs() == 1 || match.subs()==0 || xFixed.isEmpty()) {
                        return this.ellipsis.unify(match, u);
                    } else {
                        //permute may be necessary to unify the correct dep/indep terms for 2nd layer
                        if (xFixed.size()==match.subs())
                            return u.termutes.add(new CommutivePermutations(
                                    The.subterms(xFixed),
                                    match.subterms()));
                        else
                            return false; //?
                    }

                case 1:
                    Term x0 = xFixed.first();
                    if (yFree.size() == 1) {
                        return this.ellipsis.unify(EllipsisMatch.empty, u) && x0.unify(yFree.first(), u);
                    } else {
                        return u.termutes.add(new Choose1(this.ellipsis, x0, yFree));
                    }

                case 2:
                    return u.termutes.add(new Choose2(this.ellipsis, u, xFixed, yFree));

                default:
                    //3 or more combination
                    throw new RuntimeException("unimpl: " + xs + " arity combination unimplemented");
            }


        }

    }


}
