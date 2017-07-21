package nars.derive.match;

import nars.$;
import nars.Op;
import nars.index.term.NonInternable;
import nars.term.Compound;
import nars.term.Term;
import nars.term.container.TermContainer;
import nars.term.var.AbstractVariable;
import nars.term.var.UnnormalizedVariable;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class Ellipsis extends AbstractVariable implements Ellipsislike, NonInternable {

    private final int minArity;


//    /** a placeholder that indicates an expansion of one or more terms that will be provided by an Ellipsis match.
//     *  necessary for terms which require > 1 argument but an expression that will expand one ellipsis variable will not construct a valid prototype of it
//     *  ex:
//     *    (|, %A, ..)
//     *
//     *
//     * IMPORTANT: InvisibleAtom's default compareTo of -1
//     * ensures this will appear always at the end of any ordering */
//    public static final ShadowAtom Shim = new ShadowAtom("..") {
//        @Override public Op op() {
//            return Op.INTERVAL;
//        }
//    };

//    @NotNull
//    public abstract Variable clone(AbstractVariable newVar, VariableNormalization normalizer);


    public final int sizeMin() {
        return minArity;
    }


    //public final Variable target;

    public static class EllipsisPrototype extends UnnormalizedVariable implements Ellipsislike {

        public final int minArity;

        public EllipsisPrototype(@NotNull Op type, @NotNull UnnormalizedVariable target, int minArity) {
            super(type, target.toString()
                    + ".." + (minArity == 0 ? '*' : '+'));
            this.minArity = minArity;

        }


        @Override
        public
        @Deprecated
        @NotNull Variable normalize(int serial) {
            return make(serial, minArity);
        }

        @NotNull
        public static Ellipsis make(int serial, int minArity) {
            @NotNull AbstractVariable v = $.v(Op.VAR_PATTERN, serial);
            if (minArity == 0) {
                return new EllipsisZeroOrMore(v);
            } else if (minArity == 1) {
                return new EllipsisOneOrMore(v);
            } else {
                throw new RuntimeException("invalid ellipsis minArity");
            }
        }
    }

//    public static class EllipsisTransformPrototype extends UnnormalizedVariable {
//
//        //public final Variable name;
//        public final Term from, to;
//
//        public EllipsisTransformPrototype(/*Op type, */@NotNull Variable name, Term from, Term to) {
//            super(Op.VAR_PATTERN,
//                    name.toString()
//                            + ".." + from + '=' + to + "..+");
//            //this.name = name;
//            this.from = from;
//            this.to = to;
//        }
//
//        @Override public @NotNull Variable normalize(int serial) {
//            //throw new RuntimeException("n/a");
//            return this; //ignore
//        }
//
//    }


    protected Ellipsis(@NotNull AbstractVariable target, int minArity) {
        this(target, minArity, target.id);
    }

    protected Ellipsis(@NotNull AbstractVariable target, int minArity, int id) {
        super(target.op(), hash(id, minArity));
        this.minArity = minArity;
    }

    private static int hash(int id, int minArity) {
        //the 30th bit (arbitrarily chosen here) is what will store the 1-bit minArity value. provided id's should all have that bit free
        if (minArity > 1 || ((id & (1 << 30)) != 0)) throw new UnsupportedOperationException();
        if (minArity==1)
            id |= (1 << 30);
        return id;
    }

    @Override
    public int structure() {
        return 0;
    }



//    @Override
//    public final boolean equals(Object obj) {
//        return //((obj instanceof Ellipsis) && id == ((Ellipsis)obj).id)
//                //||
//                ((obj instanceof Variable) && id == ((Variable)obj).id)
//                ;
//    }
//



    /** this needs to use .term(x) instead of Term[] because of shuffle terms */
    @Nullable public static Ellipsis firstEllipsis(@NotNull TermContainer x) {
        if (x.varPattern()==0)
            return null;

        int xsize = x.size();
        for (int i = 0; i < xsize; i++) {
            Term xi = x.sub(i);
            if (xi instanceof Ellipsis) {
                return (Ellipsis) xi;
            }
        }
        return null;
    }

    @Nullable public static Ellipsis firstEllipsisRecursive(@NotNull Term x) {
        if (x instanceof Ellipsis)
            return (Ellipsis)x;
        else if (x instanceof Compound)
            return firstEllipsisRecursive((TermContainer)x);
        else
            return null;
    }

    /** this needs to use .term(x) instead of Term[] because of shuffle terms */
    @Nullable public static Ellipsis firstEllipsisRecursive(@NotNull TermContainer x) {
        int xsize = x.size();
        for (int i = 0; i < xsize; i++) {
            Term xi = x.sub(i);
            Ellipsis ex = firstEllipsisRecursive(xi);
            if (ex!=null)
                return ex;
        }
        return null;
    }

    @Nullable
    public static Ellipsis firstEllipsis(@NotNull Term[] xx) {
        for (Term x : xx) {
            if (x instanceof Ellipsis) {
                return (Ellipsis) x;
            }
        }
        return null;
    }


//    /** recursively */
//    public static boolean containsEllipsis(Term _x) {
//        if (_x instanceof Compound) {
//            Compound x = (Compound)_x;
//            int xs = x.size();
//
//            for (int i = 0; i < xs; i++) {
//                Term y = x.term(i);
//                if (y instanceof Ellipsis) return true;
//                if (y instanceof Compound) {
//                    if (containsEllipsis(y))
//                        return true;
//                }
//            }
//        }
//        return false;
//    }

//    public static int numUnmatchedEllipsis(Compound x, FindSubst ff) {
//
//        int xs = x.size();
//
//        Map<Term, Term> xy = ff.xy;
//        if (xy.isEmpty()) {
//            //map is empty so return total # ellipsis
//            return numEllipsis(x);
//        }
//
//        int n = 0;
//        for (int i = 0; i < xs; i++) {
//            Term xt = x.term(i);
//            if (xt instanceof Ellipsis) {
//                if (!xy.containsKey(xt))
//                    n++;
//            }
//        }
//        return n;
//    }

//    public static int numEllipsis(TermContainer x) {
//        int xs = x.size();
//        int n = 0;
//        for (int i = 0; i < xs; i++) {
//            if (x.term(i) instanceof Ellipsis)
//                n++;
//        }
//        return n;
//    }

//    public static int numNonEllipsisSubterms(@NotNull Compound x) {
//        int xs = x.size();
//        int n = xs;
//        for (int i = 0; i < xs; i++) {
//            Term xt = x.term(i);
//
//            if (xt instanceof Ellipsis)
//                n--;
//        }
//        return n;
//    }



//    public ShadowProduct match(ShortSet ySubsExcluded, Compound y) {
//        Term ex = this.expression;
//        if ((ex == PLUS) || (ex == ASTERISK)) {
//            return matchRemaining(y, ySubsExcluded);
//        }
//
//        throw new RuntimeException("unimplemented expression: " + ex);
//
////        else if (ex instanceof Operation) {
////
////            Operation o = (Operation) ex;
////
////            //only NOT implemented currently
////            if (!o.getOperatorTerm().equals(NOT)) {
////                throw new RuntimeException("ellipsis operation " + expression + " not implemented");
////            }
////
////            return matchNot(o.args(), mapped, y);
////        }
//    }


//    private static Term matchNot(Term[] oa, Map<Term, Term> mapped, Compound Y) {
//
//        if (oa.length!=1) {
//            throw new RuntimeException("only 1-arg not() implemented");
//        }
//
//        Term exclude = oa[0];
//
//        final int ysize = Y.size();
//        Term[] others = new Term[ysize-1];
//        int k = 0;
//        for (int j = 0; j < ysize; j++) {
//            Term yt = Y.term(j);
//            if (!mapped.get(exclude).equals(yt))
//                others[k++] = yt;
//        }
//        return Product.make(others);
//    }

    //
//    @Deprecated public boolean valid(int numNonVarArgs, int ysize) {
//        int collectable = ysize - numNonVarArgs;
//        return valid(collectable);
//    }


    public final boolean validSize(int collectable) {
        return collectable >= minArity;
    }

    @NotNull
    @Override
    public
    Op op() {
        return Op.VAR_PATTERN;
    }

    @Override
    public int varPattern() {
        return 1;
    }

    @Override
    public int vars() {
        return 0;
    } //varpattern doesnt count as var

    //    public static Ellipsis getFirstUnmatchedEllipsis(Compound X, Subst ff) {
//        final int xsize = X.size();
//        for (int i = 0; i < xsize; i++) {
//            Term xi = X.term(i);
//            if (xi instanceof Ellipsis) {
//                if (ff.getXY(X)==null)
//                    return (Ellipsis) xi;
////                else {
////                    System.err.println("already matched");
////                }
//            }
//        }
//        return null;
//    }
//    public static Term getFirstNonEllipsis(Compound X) {
//        int xsize = X.size();
//        for (int i = 0; i < xsize; i++) {
//            Term xi = X.term(i);
//            if (!(xi instanceof Ellipsis)) {
//                return xi;
//            }
//        }
//        return null;
//    }

//    public static ArrayEllipsisMatch matchRemaining(Compound Y, ShortSet ySubsExcluded) {
//        return EllipsisMatch.matchedSubterms(Y, (index, term) ->
//                !ySubsExcluded.contains((short)index) );
//    }
//
//    public static ArrayEllipsisMatch matchedSubterms(Compound Y) {
//        Term[] arrayGen =
//                !(Y instanceof Sequence) ?
//                        Y.terms() :
//                        ((Sequence)Y).toArrayWithIntervals();
//
//        return new ArrayEllipsisMatch(arrayGen);
//    }


    //    public static RangeTerm rangeTerm(String s) {
//        int uscore = s.indexOf("_");
//        if (uscore == -1) return null;
//        int periods = s.indexOf("..");
//        if (periods == -1) return null;
//        if (periods < uscore) return null;
//
//        String prefix = s.substring(0, uscore);
//        int from = Integer.parseInt( s.substring(uscore, periods) );
//        String to = s.substring(periods+2);
//        if (to.length() > 1) return null;
//
//        return new RangeTerm(prefix, from, to.charAt(0));
//    }
}
