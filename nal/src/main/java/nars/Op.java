package nars;


import jcog.data.ArrayHashSet;
import jcog.list.FasterList;
import nars.op.SetFunc;
import nars.op.mental.AliasConcept;
import nars.subterm.ArrayTermVector;
import nars.subterm.Neg;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.atom.Int;
import nars.term.compound.CachedCompound;
import nars.term.compound.util.Conj;
import nars.term.var.NormalizedVariable;
import nars.term.var.UnnormalizedVariable;
import nars.term.var.VarDep;
import nars.time.Tense;
import nars.unify.Unify;
import nars.unify.match.EllipsisMatch;
import nars.unify.match.Ellipsislike;
import nars.util.term.TermBuilder;
import nars.util.term.builder.InterningTermBuilder;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.RoaringBitmap;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Arrays.copyOfRange;
import static nars.term.Terms.sorted;
import static nars.time.Tense.*;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 * NAL symbol table
 */
public enum Op {


    ATOM(".", Op.ANY_LEVEL, OpType.Other),

    NEG("--", 1, Args.One) {
        public Term compound(int dt, Term[] u) {

            if (u.length != 1)
                throw new RuntimeException("negation requires one subterm");
            return Neg.the(u[0]);
        }

    },

    INH("-->", 1, OpType.Statement, Args.Two) {
        @Override
        public Term compound(int dt, Term[] u) {
            assert (u.length == 2);
            return statement(this, dt, u[0], u[1]);
        }
    },
    SIM("<->", true, 2, OpType.Statement, Args.Two) {
        @Override
        public Term compound(int dt, Term[] u) {
            if (u.length == 1) {
                assert (this == SIM);
                return u[0] == Null ? Null : True;
            } else {
                assert (u.length == 2);
                return statement(this, dt, u[0], u[1]);
            }
        }
    },

    /**
     * extensional intersection
     */
    SECTe("&", true, 3, Args.GTETwo) {
        @Override
        public Term compound(int dt, Term[] u) {
            return intersect(/*Int.intersect*/(u),
                    SECTe,
                    SETe,
                    SETi);
        }
    },

    /**
     * intensional intersection
     */
    SECTi("|", true, 3, Args.GTETwo) {
        @Override
        public Term compound(int dt, Term[] u) {
            return intersect(/*Int.intersect*/(u),
                    SECTi,
                    SETi,
                    SETe);
        }
    },

    /**
     * extensional difference
     */
    DIFFe("~", false, 3, Args.Two) {
        @Override
        public Term compound(int dt, Term[] u) {
            return differ(this, u);
        }
    },

    /**
     * intensional difference
     */
    DIFFi("-", false, 3, Args.Two) {
        @Override
        public Term compound(int dt, Term[] u) {
            return differ(this, u);
        }
    },

    /**
     * PRODUCT
     * classically this is considered NAL4 but due to the use of functors
     * it is much more convenient to classify it in NAL1 so that it
     * along with inheritance (INH), which comprise the functor,
     * can be used to compose the foundation of the system.
     */
    PROD("*", 1, Args.GTEZero),


    /**
     * conjunction
     */
    CONJ("&&", true, 5, Args.GTETwo) {
        @Override
        public Term compound(int dt, Term[] u) {
            final int n = u.length;
            switch (n) {

                case 0:
                    return True;

                case 1:
                    Term only = u[0];
                    if (only instanceof EllipsisMatch) {

                        return compound(dt, ((EllipsisMatch) only).arrayShared());
                    } else {


                        return only instanceof Ellipsislike ?
                                compound(CONJ, dt, only)
                                :
                                only;
                    }

            }

            int trues = 0;
            for (Term t : u) {
                if (t == Null || t == False)
                    return t;
                else if (t == True)
                    trues++;
            }

            if (trues > 0) {

                int sizeAfterTrueRemoved = u.length - trues;
                switch (sizeAfterTrueRemoved) {
                    case 0:

                        return True;
                    case 1: {

                        for (Term uu : u) {
                            if (uu != True) {
                                assert (!(uu instanceof Ellipsislike)) : "if this happens, TODO";
                                return uu;
                            }
                        }
                        throw new RuntimeException("should have found non-True term to return");
                    }
                    default: {
                        Term[] y = new Term[sizeAfterTrueRemoved];
                        int j = 0;
                        for (int i = 0; j < y.length; i++) {
                            Term uu = u[i];
                            if (uu != True)
                                y[j++] = uu;
                        }
                        assert (j == y.length);

                        u = y;
                    }
                }
            }


            switch (dt) {
                case DTERNAL:
                case 0:
                    if (u.length == 2) {


                        Term a = u[0];
                        Term b = u[1];
                        if (Terms.commonStructure(a, b)) {
                            if (a.equals(b))
                                return u[0];
                            if (a.equalsNeg(b))
                                return False;
                        }

                        if (!a.hasAny(Op.CONJ.bit) && !b.hasAny(Op.CONJ.bit)) {


                            return compound(CONJ, dt, sorted(u[0], u[1]));
                        }

                    }


                    assert u.length > 1;
                    Conj c = new Conj();
                    long sdt = dt == DTERNAL ? ETERNAL : 0;
                    for (Term x : u) {
                        if (!c.add(sdt, x))
                            break;
                    }
                    return c.term();

                case XTERNAL:


                    int ul = u.length;
                    if (ul > 1) {
                        boolean unordered = false;
                        for (int i = 0; i < ul - 1; i++) {
                            if (u[i].compareTo(u[i + 1]) > 0) {
                                unordered = true;
                                break;
                            }
                        }
                        if (unordered) {
                            u = u.clone();
                            if (ul == 2) {

                                Term u0 = u[0];
                                u[0] = u[1];
                                u[1] = u0;
                            } else {
                                Arrays.sort(u);
                            }
                        }

                    }

                    switch (ul) {
                        case 0:
                            return True;

                        case 1:
                            return u[0];

                        case 2: {


                            Term a = u[0];
                            if (a.op() == CONJ && a.dt() == XTERNAL && a.subs() == 2) {
                                Term b = u[1];

                                int va = a.volume();
                                int vb = b.volume();

                                if (va > vb) {
                                    Term[] aa = a.subterms().arrayShared();
                                    int va0 = aa[0].volume();
                                    int va1 = aa[1].volume();
                                    int vamin = Math.min(va0, va1);


                                    if ((va - vamin) > (vb + vamin)) {
                                        int min = va0 <= va1 ? 0 : 1;

                                        Term[] xu = {CONJ.compound(XTERNAL, new Term[]{b, aa[min]}), aa[1 - min]};
                                        Arrays.sort(xu);
                                        return compound(CONJ, XTERNAL, xu);
                                    }
                                }

                            }
                            break;
                        }

                        default:
                            break;
                    }

                    if (u.length > 1) {
                        return compound(CONJ, XTERNAL, u);
                    } else {
                        return u[0];
                    }

                default: {
                    if (n != 2) {
                        if (Param.DEBUG_EXTRA)
                            throw new RuntimeException("temporal conjunction with n!=2 subterms");
                        return Null;
                    }

                    return Conj.conjMerge(u[0], u[1], dt);
                }
            }

        }

    },


    /**
     * intensional set
     */
    SETi("[", true, 2, Args.GTEOne) {
        @Override
        public boolean isSet() {
            return true;
        }
    },

    /**
     * extensional set
     */
    SETe("{", true, 2, Args.GTEOne) {
        @Override
        public boolean isSet() {
            return true;
        }
    },


    /**
     * implication
     */
    IMPL("==>", 5, OpType.Statement, Args.Two) {
        @Override
        public Term compound(int dt, Term... u) {
            assert (u.length == 2);
            return statement(this, dt, u[0], u[1]);
        }
    },


    VAR_DEP('#', Op.ANY_LEVEL, OpType.Variable),
    VAR_INDEP('$', 5 /*NAL5..6 for Indep Vars */, OpType.Variable),
    VAR_QUERY('?', Op.ANY_LEVEL, OpType.Variable),
    VAR_PATTERN('%', Op.ANY_LEVEL, OpType.Variable),

    INT("+", Op.ANY_LEVEL, OpType.Other),

    BOOL("B", Op.ANY_LEVEL, OpType.Other),


    /**
     * for ellipsis, when seen as a term
     */

    ;


    /**
     * does this help?  Op.values() bytecode = INVOKESTATIC
     * but accessing this is GETSTATIC
     */
    public static final Op[] ops = Op.values();

    public static final String DISJstr = "||";
    public static final int StatementBits = Op.or(Op.INH, Op.SIM, Op.IMPL);
    public static final int FuncBits = Op.or(Op.ATOM, Op.INH, Op.PROD);
    public static final int FuncInnerBits = Op.or(Op.ATOM, Op.PROD);
    public static final byte BELIEF = '.';
    public static final byte QUESTION = '?';
    public static final byte GOAL = '!';
    public static final byte QUEST = '@';
    public static final byte COMMAND = ';';
    public static final String TENSE_PAST = ":\\:";
    public static final String TENSE_PRESENT = ":|:";
    public static final String TENSE_FUTURE = ":/:";
    public static final String TENSE_ETERNAL = ":-:";
    public static final String TASK_RULE_FWD = "|-";
    public static final char BUDGET_VALUE_MARK = '$';
    public static final char TRUTH_VALUE_MARK = '%';
    public static final char VALUE_SEPARATOR = ';';
    public static final char ARGUMENT_SEPARATOR = ',';
    public static final char SET_INT_CLOSER = ']';
    public static final char SET_EXT_CLOSER = '}';
    public static final char COMPOUND_TERM_OPENER = '(';
    public static final char COMPOUND_TERM_CLOSER = ')';
    @Deprecated
    public static final char OLD_STATEMENT_OPENER = '<';
    @Deprecated
    public static final char OLD_STATEMENT_CLOSER = '>';
    public static final char STAMP_OPENER = '{';
    public static final char STAMP_CLOSER = '}';
    public static final char STAMP_SEPARATOR = ';';
    public static final char STAMP_STARTER = ':';
    /**
     * bitvector of non-variable terms which can not be part of a goal term
     */
    public static final int NonGoalable = or(IMPL);
    public static final int varBits = Op.or(VAR_PATTERN, VAR_DEP, VAR_QUERY, VAR_INDEP);
    /**
     * Image index ("imdex") symbol for products, and anonymous variable in products
     */
    public final static char ImdexSym = '_';
    public static final Atomic Imdex =
            new UnnormalizedVariable(Op.VAR_DEP, String.valueOf(ImdexSym)) {

                final int RANK = Term.opX(VAR_PATTERN, (short) 20 /* different from normalized variables with a subOp of 0 */);

                @Override
                public Term the() {
                    return this;
                }

                @Override
                public int opX() {
                    return RANK;
                }
            };
    public static final char TrueSym = '†';
    public static final char FalseSym = 'Ⅎ';
    public static final char NullSym = '☢';

    public static final char imIntSym = '\\';
    public static final char imExtSym = '/';

    /**
     * absolutely nonsense
     */
    public static final Bool Null = new Bool(String.valueOf(Op.NullSym)) {

        final int rankBoolNull = Term.opX(BOOL, (short) 0);

        @Override
        public final int opX() {
            return rankBoolNull;
        }

        @Override
        public Term neg() {
            return this;
        }

        @Override
        public boolean equalsNeg(Term t) {
            return false;
        }

        @Override
        public boolean equalsNegRoot(Term t) {
            return false;
        }

        @Override
        public Term unneg() {
            return this;
        }
    };
    /**
     * tautological absolute true
     */
    public static final Bool True = new Bool(String.valueOf(Op.TrueSym)) {

        final int rankBoolTrue = Term.opX(BOOL, (short) 2);

        @Override
        public final int opX() {
            return rankBoolTrue;
        }

        @Override
        public Term neg() {
            return False;
        }

        @Override
        public boolean equalsNeg(Term t) {
            return t == False;
        }

        @Override
        public boolean equalsNegRoot(Term t) {
            return t == False;
        }

        @Override
        public Term unneg() {
            return True;
        }
    };
    /**
     * tautological absolute false
     */
    public static final Bool False = new Bool(String.valueOf(Op.FalseSym)) {

        final int rankBoolFalse = Term.opX(BOOL, (short) 1);

        @Override
        public final int opX() {
            return rankBoolFalse;
        }

        @Override
        public boolean equalsNeg(Term t) {
            return t == True;
        }

        @Override
        public boolean equalsNegRoot(Term t) {
            return t == True;
        }

        @Override
        public Term neg() {
            return True;
        }

        @Override
        public Term unneg() {
            return True;
        }
    };
    public static final VarDep imInt = new ImDep((byte) 126, (byte) '\\');
    public static final VarDep imExt = new ImDep((byte) 127, (byte) '/');
    public static final int DiffBits = Op.DIFFe.bit | Op.DIFFi.bit;
    public static final int SectBits = or(Op.SECTe, Op.SECTi);
    public static final int SetBits = or(Op.SETe, Op.SETi);
    public static final int Temporal = or(Op.CONJ, Op.IMPL);

    public static final Atom BELIEF_TERM = (Atom) Atomic.the(String.valueOf((char) BELIEF));
    public static final Atom GOAL_TERM = (Atom) Atomic.the(String.valueOf((char) GOAL));
    public static final Atom QUESTION_TERM = (Atom) Atomic.the(String.valueOf((char) QUESTION));
    public static final Atom QUEST_TERM = (Atom) Atomic.the(String.valueOf((char) QUEST));
    public static final Atom QUE_TERM = (Atom) Atomic.the(String.valueOf((char) QUESTION) + String.valueOf((char) QUEST));

    public static final Term[] EmptyTermArray = new Term[0];
    public static final Subterms EmptySubterms = new ArrayTermVector(EmptyTermArray);
    public static final Term EmptyProduct = new CachedCompound.SimpleCachedCompound(Op.PROD, EmptySubterms);
    public static final Term EmptySet = new CachedCompound.SimpleCachedCompound(Op.SETe, EmptySubterms);
    public static final int VariableBits = or(Op.VAR_PATTERN, Op.VAR_INDEP, Op.VAR_DEP, Op.VAR_QUERY);
    public static final int[] NALLevelEqualAndAbove = new int[8 + 1];
    static final ImmutableMap<String, Op> stringToOperator;
    /**
     * ops across which reflexivity of terms is allowed
     */
    final static int relationDelimeterStrong = Op.or(Op.PROD, Op.NEG);
    public static final Predicate<Term> recursiveCommonalityDelimeterStrong =
            c -> !c.isAny(relationDelimeterStrong);
    /**
     * allows conj
     */
    final static int relationDelimeterWeak = relationDelimeterStrong | Op.or(Op.CONJ);
    public static final Predicate<Term> recursiveCommonalityDelimeterWeak =
            c -> !c.isAny(relationDelimeterWeak);
    /**
     * specifier for any NAL level
     */
    private static final int ANY_LEVEL = 0;
    private static final int InvalidImplicationSubj = or(IMPL);
    public static TermBuilder terms =
            new InterningTermBuilder();
    public static int ConstantAtomics = Op.ATOM.bit | Op.INT.bit;

    static {
        for (Op o : Op.values()) {
            int l = o.minLevel;
            if (l < 0) l = 0;
            for (int i = l; i <= 8; i++) {
                NALLevelEqualAndAbove[i] |= o.bit;
            }
        }

        final Map<String, Op> _stringToOperator = new HashMap<>(values().length * 2);


        for (Op r : Op.values()) {
            _stringToOperator.put(r.toString(), r);

        }
        stringToOperator = Maps.immutable.ofMap(_stringToOperator);


    }

    public final Atom strAtom;
    public final boolean indepVarParent;
    public final boolean depVarParent;
    /**
     * whether it is a special or atomic term that isnt conceptualizable.
     * negation is an exception to this, being unconceptualizable itself
     * but it will have conceptualizable=true.
     */
    public final boolean conceptualizable;
    public final boolean beliefable, goalable;
    /**
     * string representation
     */
    public final String str;
    /**
     * character representation if symbol has length 1; else ch = 0
     */
    public final char ch;
    public final OpType type;
    /**
     * arity limits, range is inclusive >= <=
     * TODO replace with an IntPredicate
     */
    public final int minSize, maxSize;
    /**
     * minimum NAL level required to use this operate, or 0 for N/A
     */
    public final int minLevel;
    public final boolean commutative;
    public final boolean temporal;
    /**
     * 1 << op.ordinal
     */
    public final int bit;
    public final boolean var;
    public final boolean atomic;
    public final boolean statement;
    /**
     * whether this involves an additional numeric component: 'dt' (for temporals) or 'relation' (for images)
     */
    public final boolean hasNumeric;

    /*
    used only by Termlike.hasAny
    public static boolean hasAny(int existing, int possiblyIncluded) {
        return (existing & possiblyIncluded) != 0;
    }*/
    public final byte id;

    Op(char c, int minLevel, OpType type) {
        this(c, minLevel, type, Args.None);
    }

    Op(String s, boolean commutative, int minLevel, IntIntPair size) {
        this(s, commutative, minLevel, OpType.Other, size);
    }

    Op(char c, int minLevel, OpType type, IntIntPair size) {
        this(Character.toString(c), minLevel, type, size);
    }


    Op(String string, int minLevel, IntIntPair size) {
        this(string, minLevel, OpType.Other, size);
    }


    Op(String string, int minLevel, OpType type) {
        this(string, false /* non-commutive */, minLevel, type, Args.None);
    }

    Op(String string, int minLevel, OpType type, IntIntPair size) {
        this(string, false /* non-commutive */, minLevel, type, size);
    }

    Op(String string, boolean commutative, int minLevel, OpType type, IntIntPair size) {

        this.id = (byte) (ordinal());
        this.str = string;
        this.ch = string.length() == 1 ? string.charAt(0) : 0;
        this.strAtom = ch != '.' ? (Atom) Atomic.the('"' + str + '"') : null /* dont compute for ATOM, infinite loops */;

        this.commutative = commutative;
        this.minLevel = minLevel;
        this.type = type;


        this.minSize = size.getOne();
        this.maxSize = size.getTwo();

        this.var = (type == OpType.Variable);

        boolean isImpl = str.equals("==>");
        this.statement = str.equals("-->") || isImpl || str.equals("<->");
        boolean isConj = str.equals("&&");
        this.temporal = isConj || isImpl;


        this.hasNumeric = temporal;


        this.bit = (1 << ordinal());

        final Set<String> ATOMICS = Set.of(".", "+", "B");
        this.atomic = var || ATOMICS.contains(str);


        conceptualizable = !var &&
                !str.equals("B") /* Bool */


        ;

        goalable = conceptualizable && !isImpl;

        beliefable = conceptualizable;

        indepVarParent = isImpl;
        depVarParent = isConj;

    }

    /**
     * TODO option for instantiating CompoundLight base's in the bottom part of this
     */
    public static Term dt(Compound base, int nextDT) {


        return base.op().compound(nextDT, base.arrayShared());


    }


    public static boolean hasAny(int existing, int possiblyIncluded) {
        return (existing & possiblyIncluded) != 0;
    }

    public static boolean hasAll(int existing, int possiblyIncluded) {
        return ((existing | possiblyIncluded) == existing);
    }

    public static boolean isTrueOrFalse(Term x) {
        return x == True || x == False;
    }


    public static boolean concurrent(int dt) {
        return (dt == DTERNAL) || (dt == 0);
    }


    public static boolean hasNull(Term[] t) {
        for (Term x : t)
            if (x == Null)
                return true;
        return false;
    }

    private static Term differ(/*@NotNull*/ Op op, Term... t) {


        switch (t.length) {
            case 1:
                Term single = t[0];
                if (single instanceof EllipsisMatch) {
                    return differ(op, ((Subterms) single).arrayShared());
                }
                return single instanceof Ellipsislike ?
                        compound(op, DTERNAL, single) :
                        Null;
            case 2:
                Term et0 = t[0], et1 = t[1];

                if (et0 == Null || et1 == Null)
                    return Null;


                if (et0.equalsRoot(et1))
                    return False;


                Op o0 = et0.op();
                if (et1.equalsNegRoot(et0)) {
                    if (o0 == NEG || et0 == False)
                        return False;
                    else
                        return True;
                }

                if (isTrueOrFalse(et0) || isTrueOrFalse(et1))
                    return Null;

                Op o1 = et1.op();

                //((--,X)~(--,Y)) reduces to (Y~X)
                if (o0 == NEG && o1==NEG) {
                    //un-neg and swap order
                    Term x = et0.unneg();
                    et0 = et1.unneg();
                    o0 = et0.op();
                    et1 = x;
                    o1 = et1.op();
                }



                if (et0.containsRecursively(et1, true, recursiveCommonalityDelimeterWeak)
                        || et1.containsRecursively(et0, true, recursiveCommonalityDelimeterWeak))
                    return Null;

                if (op == DIFFe && et0 instanceof Int.IntRange && o1 == INT) {
                    Term simplified = ((Int.IntRange) et0).subtract(et1);
                    if (simplified != Null)
                        return simplified;
                }


                Op set = op == DIFFe ? SETe : SETi;
                if ((o0 == set && o1 == set)) {
                    return differenceSet(set, et0, et1);
                } else {
                    return differenceSect(op, et0, et1);
                }


        }

        throw new Term.InvalidTermException(op, t, "diff requires 2 terms");

    }

    private static Term differenceSect(Op diffOp, Term a, Term b) {


        Op ao = a.op();
        if (((diffOp == DIFFi && ao == SECTe) || (diffOp == DIFFe && ao == SECTi)) && (b.op() == ao)) {
            Subterms aa = a.subterms();
            Subterms bb = b.subterms();
            MutableSet<Term> common = Subterms.intersect(aa, bb);
            if (common != null) {
                int cs = common.size();
                if (aa.subs() == cs || bb.subs() == cs)
                    return Null;
                return ao.the(common.with(
                        diffOp.the(ao.the(aa.termsExcept(common)), ao.the(bb.termsExcept(common)))
                ));
            }
        }


        if (((diffOp == DIFFi && ao == SECTi) || (diffOp == DIFFe && ao == SECTe)) && (b.op() == ao)) {
            Subterms aa = a.subterms();
            Subterms bb = b.subterms();
            MutableSet<Term> common = Subterms.intersect(aa, bb);
            if (common != null) {
                int cs = common.size();
                if (aa.subs() == cs || bb.subs() == cs)
                    return Null;
                return ao.the(common.collect(Term::neg).with(
                        diffOp.the(ao.the(aa.termsExcept(common)), ao.the(bb.termsExcept(common)))
                ));
            }
        }

        return compound(diffOp, DTERNAL, a, b);
    }

    /*@NotNull*/
    public static Term differenceSet(/*@NotNull*/ Op o, Term a, Term b) {


        if (a.equals(b))
            return Null;

        if (o == INT) {
            if (!(a instanceof Int.IntRange))
                return Null;
            else {
                Term aMinB = ((Int.IntRange) a).subtract(b);
                if (aMinB != Null) {
                    if (a.equals(aMinB))
                        return Null;
                    return aMinB;
                }
            }
        }


        int size = a.subs();
        Collection<Term> terms = o.commutative ? new TreeSet() : new FasterList(size);

        for (int i = 0; i < size; i++) {
            Term x = a.sub(i);
            if (!b.contains(x)) {
                terms.add(x);
            }
        }

        int retained = terms.size();
        if (retained == size) {
            return a;
        } else if (retained == 0) {
            return Null;
        } else {
            return o.the(DTERNAL, terms);
        }

    }


    /**
     * decode a term which may be a functor, return null if it isnt
     */
    @Nullable
    public static <X> Pair<X, Term> functor(Term maybeOperation, Function<Term, X> invokes) {
        if (maybeOperation.hasAll(Op.FuncBits)) {
            Term c = maybeOperation;
            if (c.op() == INH) {
                Term s0 = c.sub(0);
                if (s0.op() == PROD) {
                    Term s1 = c.sub(1);
                    if (s1 instanceof Atomic /*&& s1.op() == ATOM*/) {
                        X i = invokes.apply(s1);
                        if (i != null)
                            return Tuples.pair(i, s0);
                    }
                }
            }
        }
        return null;
    }


    static boolean in(int needle, int haystack) {
        return (needle & haystack) == needle;
    }

    public static int or(/*@NotNull*/ Op... o) {
        int bits = 0;
        for (Op n : o)
            bits |= n.bit;
        return bits;
    }


    /*@NotNull*/
    static Term statement(/*@NotNull*/ Op op, int dt, final Term subject, final Term predicate) {

        if (subject == Null || predicate == Null)
            return Null;

        boolean dtConcurrent = concurrent(dt);
        if (dtConcurrent) {
            if (subject.equals(predicate))
                return True;


        }

        if (op == INH || op == SIM) {
            if (isTrueOrFalse(subject)) {

                return Null;
            }
            if (op == SIM && isTrueOrFalse(predicate)) {


                return Null;
            }
        }

        if (op == IMPL) {


            if (subject == True)
                return predicate;
            if (subject == False)
                return Null;

            if (predicate instanceof Bool)
                return Null;


            if (predicate.op() == NEG) {

                return IMPL.compound(dt, subject, predicate.unneg()).neg();
            }


            if (subject.hasAny(InvalidImplicationSubj))
                return Null;


            switch (predicate.op()) {
                case IMPL: {
                    return IMPL.compound(predicate.dt(), CONJ.compound(dt, new Term[]{subject, predicate.sub(0)}), predicate.sub(1));
                }


            }


            if (dt != XTERNAL && subject.dt() != XTERNAL && predicate.dt() != XTERNAL) {

                ArrayHashSet<LongObjectPair<Term>> se = new ArrayHashSet<>(4);
                subject.eventsWhile((w, t) -> {
                    se.add(PrimitiveTuples.pair(w, t));
                    return true;
                }, 0, true, true, false, 0);

                FasterList<LongObjectPair<Term>> pe = new FasterList(4);
                int pre = subject.dtRange();
                boolean dtNotDternal = dt != DTERNAL;
                int edt = pre + (dtNotDternal ? dt : 0);

                final boolean[] peChange = {false};


                boolean contradiction = !predicate.eventsWhile((w, t) -> {
                    LongObjectPair<Term> x = PrimitiveTuples.pair(w, t);
                    if (se.contains(x)) {

                        peChange[0] = true;
                    } else if (se.contains(pair(w, t.neg()))) {
                        return false;
                    } else {
                        pe.add(x);
                    }
                    return true;
                }, edt, true, true, false, 0);

                if (contradiction)
                    return False;


                if ((dt == DTERNAL || dt == 0)) {
                    for (ListIterator<LongObjectPair<Term>> pi = pe.listIterator(); pi.hasNext(); ) {
                        LongObjectPair<Term> pex = pi.next();
                        Term pext = pex.getTwo();
                        if (pext.op() == CONJ) {
                            int pdt = pext.dt();
                            if (pdt == DTERNAL || pdt == 0) {
                                long at = pex.getOne();

                                RoaringBitmap pextRemovals = null;
                                Subterms subPexts = pext.subterms();
                                int subPextsN = subPexts.subs();

                                for (ListIterator<LongObjectPair<Term>> si = se.listIterator(); si.hasNext(); ) {
                                    LongObjectPair<Term> sse = si.next();
                                    if (sse.getOne() == at) {


                                        Term sset = sse.getTwo();

                                        for (int i = 0; i < subPextsN; i++) {
                                            Term subPext = subPexts.sub(i);
                                            Term merge = CONJ.compound(dt, new Term[]{sset, subPext});
                                            if (merge == Null) return Null;
                                            else if (merge == False) {

                                                return False;
                                            } else if (merge.equals(sset)) {

                                                if (pextRemovals == null)
                                                    pextRemovals = new RoaringBitmap();
                                                pextRemovals.add(i);
                                            } else {

                                            }
                                        }
                                    }
                                }
                                if (pextRemovals != null) {
                                    if (pextRemovals.getCardinality() == subPextsN) {

                                        pi.remove();
                                    } else {
                                        pi.set(pair(at, CONJ.compound(pdt, subPexts.termsExcept(pextRemovals))));
                                    }
                                    peChange[0] = true;
                                }
                            }
                        }
                    }
                }


                if (pe.isEmpty())
                    return True;


                if (peChange[0]) {

                    int ndt = dtNotDternal ? (int) pe.minBy(LongObjectPair::getOne).getOne() - pre : DTERNAL;
                    Term newPredicate;
                    if (pe.size() == 1) {
                        newPredicate = pe.getOnly().getTwo();
                    } else if (predicate.dt() == DTERNAL) {

                        Conj c = new Conj();
                        for (int i = 0, peSize = pe.size(); i < peSize; i++) {
                            if (!c.add(ETERNAL, pe.get(i).getTwo()))
                                break;
                        }
                        newPredicate = c.term();
                    } else {
                        newPredicate = Conj.conj(pe);
                    }

                    return IMPL.compound(ndt, new Term[]{subject, newPredicate});
                }


            }


        }


        if ((dtConcurrent || op != IMPL) && (!subject.hasAny(Op.VAR_PATTERN) && !predicate.hasAny(Op.VAR_PATTERN))) {

            Predicate<Term> delim = (op == IMPL) ?
                    recursiveCommonalityDelimeterStrong : Op.recursiveCommonalityDelimeterWeak;


            if ((containEachOther(subject, predicate, delim))) {

                return Null;
            }
            boolean sa = subject instanceof AliasConcept.AliasAtom;
            if (sa) {
                Term sd = ((AliasConcept.AliasAtom) subject).target;
                if (sd.equals(predicate) || containEachOther(sd, predicate, delim))
                    return Null;
            }
            boolean pa = predicate instanceof AliasConcept.AliasAtom;
            if (pa) {
                Term pd = ((AliasConcept.AliasAtom) predicate).target;
                if (pd.equals(subject) || containEachOther(pd, subject, delim))
                    return Null;
            }
            if (sa && pa) {
                if (containEachOther(((AliasConcept.AliasAtom) subject).target, ((AliasConcept.AliasAtom) predicate).target, delim))
                    return Null;
            }

        }


        return compound(op, dt, subject, predicate);
    }

    public static boolean containEachOther(Term x, Term y, Predicate<Term> delim) {
        int xv = x.volume();
        int yv = y.volume();
        boolean root = false;
        if (xv == yv)
            return Terms.commonStructure(x, y) &&
                    (x.containsRecursively(y, root, delim) || y.containsRecursively(x, root, delim));
        else if (xv > yv)
            return x.containsRecursively(y, root, delim);
        else
            return y.containsRecursively(x, root, delim);
    }


    @Nullable
    public static Op the(String s) {
        return stringToOperator.get(s);
    }

    public static Object theIfPresent(String s) {
        Op x = stringToOperator.get(s);
        if (x != null)
            return x;
        else
            return s;
    }

    private static Term intersect(Term[] t, /*@NotNull*/ Op intersection, /*@NotNull*/ Op setUnion, /*@NotNull*/ Op setIntersection) {

        int trues = 0;
        for (Term x : t) {
            if (x == True) {

                trues++;
            } else if (x == Null || x == False) {
                return Null;
            }
        }
        if (trues > 0) {
            if (trues == t.length) {
                return True;
            } else if (t.length - trues == 1) {

                for (Term x : t) {
                    if (x != True)
                        return x;
                }
            } else {

                Term[] t2 = new Term[t.length - trues];
                int yy = 0;
                for (Term x : t) {
                    if (x != True)
                        t2[yy++] = x;
                }
                t = t2;
            }
        }

        switch (t.length) {

            case 0:
                throw new RuntimeException();

            case 1:

                Term single = t[0];
                if (single instanceof EllipsisMatch) {
                    return intersect(((Subterms) single).arrayShared(), intersection, setUnion, setIntersection);
                }
                return single instanceof Ellipsislike ?
                        compound(intersection, DTERNAL, single) :
                        single;

            case 2:
                return intersect2(t[0], t[1], intersection, setUnion, setIntersection);
            default:

                Term a = intersect2(t[0], t[1], intersection, setUnion, setIntersection);

                Term b = intersect(copyOfRange(t, 2, t.length), intersection, setUnion, setIntersection);

                return intersect2(a, b,
                        intersection, setUnion, setIntersection
                );
        }

    }

    /*@NotNull*/
    @Deprecated
    private static Term intersect2(Term term1, Term term2, /*@NotNull*/ Op intersection, /*@NotNull*/ Op setUnion, /*@NotNull*/ Op setIntersection) {

        if (term1.equals(term2))
            return term1;

        Op o1 = term1.op();
        Op o2 = term2.op();

        if ((o1 == setUnion) && (o2 == setUnion)) {

            return SetFunc.union(setUnion, term1.subterms(), term2.subterms());
        }


        if ((o1 == setIntersection) && (o2 == setIntersection)) {

            return SetFunc.intersect(setIntersection, term1.subterms(), term2.subterms());
        }

        if (o2 == intersection && o1 != intersection) {

            Term x = term1;
            term1 = term2;
            term2 = x;
            o2 = o1;
            o1 = intersection;
        }


        TreeSet<Term> args = new TreeSet<>();
        if (o1 == intersection) {
            ((Iterable<Term>) term1).forEach(args::add);
            if (o2 == intersection)
                ((Iterable<Term>) term2).forEach(args::add);
            else
                args.add(term2);
        } else {
            args.add(term1);
            args.add(term2);
        }

        int aaa = args.size();
        if (aaa == 1)
            return args.first();
        else {
            return compound(intersection, DTERNAL, args.toArray(Op.EmptyTermArray));
        }
    }

    public static boolean goalable(Term c) {
        return !c.hasAny(Op.NonGoalable);
    }

    /**
     * returns null if not found, and Null if no subterms remain after removal
     */
    @Nullable
    public static Term without(Term container, Predicate<Term> filter, Random rand) {


        Subterms cs = container.subterms();

        int i = cs.indexOf(filter, rand);
        if (i == -1)
            return Null;


        switch (cs.subs()) {
            case 1:
                return Null;
            case 2:

                Term remain = cs.sub(1 - i);
                Op o = container.op();
                return o.isSet() ? o.the(remain) : remain;
            default:
                return container.op().compound(container.dt(), cs.termsExcept(i));
        }

    }

    public static int conjEarlyLate(Term x, boolean earlyOrLate) {
        assert (x.op() == CONJ);
        int dt = x.dt();
        switch (dt) {
            case DTERNAL:
            case XTERNAL:
            case 0:
                return earlyOrLate ? 0 : 1;

            default: {


                return (dt < 0) ? (earlyOrLate ? 1 : 0) : (earlyOrLate ? 0 : 1);
            }
        }
    }

    /**
     * direct constructor
     * no reductions or validatios applied
     * use with caution
     */
    public static Term compound(Op o, int dt, Term... u) {
        return terms.compound(o, dt, u);
    }

    public final Term the(Subterms s) {
        return the(s.arrayShared());
    }

    public final Term the(/*@NotNull*/ Term... u) {
        return compound(DTERNAL, u);
    }

    @Override
    public String toString() {
        return str;
    }

    /**
     * writes this operator to a Writer in (human-readable) expanded UTF16 mode
     */
    public final void append(Compound c, Appendable w) throws IOException {
        append(c.dt(), w, false);
    }

    /**
     * writes this operator to a Writer in (human-readable) expanded UTF16 mode
     */
    public final void append(int dt, Appendable w, boolean invertDT) throws IOException {


        if (dt == 0) {

            String s;
            switch (this) {
                case CONJ:
                    s = ("&|");
                    break;
                case IMPL:
                    s = ("=|>");
                    break;


                default:
                    throw new UnsupportedOperationException();
            }
            w.append(s);
            return;
        }

        boolean hasTime = dt != Tense.DTERNAL;
        if (hasTime)
            w.append(' ');

        char ch = this.ch;
        if (ch == 0)
            w.append(str);
        else
            w.append(ch);

        if (hasTime) {

            if (invertDT)
                dt = -dt;

            if (dt > 0) w.append('+');
            String ts;
            if (dt == XTERNAL)
                ts = "-";
            else
                ts = Integer.toString(dt);
            w.append(ts).append(' ');

        }
    }

    public final Term[] sortedIfNecessary(int dt, Term[] u) {
        if (commutative && commute(dt, u.length)) {
            return sorted(u);
        }
        return u;
    }

    public static boolean commute(int dt, int subterms) {
        return subterms > 1 && Op.concurrent(dt);
    }

    public final Term the(/*@NotNull*/ Collection<Term> sub) {
        return the(DTERNAL, sub);
    }

    public final Term the(int dt, /*@NotNull*/ Collection<Term> sub) {
        return compound(dt, sub.toArray(EmptyTermArray));
    }

    /**
     * alternate method args order for 2-term w/ infix DT
     */
    public final Term the(Term a, int dt, Term b) {
        return compound(dt, a, b);
    }

    /**
     * entry point into the term construction process.
     * this call tree eventually ends by either:
     * - instance(..)
     * - reduction to another term or True/False/Null
     */
    public Term compound(int dt, Term... u) {
        return terms.compound(this, dt, u);
    }

    /**
     * true if matches any of the on bits of the vector
     */
    public final boolean in(int vector) {
        return in(bit, vector);
    }

    public boolean isSet() {
        return false;
    }

    public boolean isAny(int bits) {
        return ((bit & bits) != 0);
    }

    /**
     * top-level Op categories
     */
    public enum OpType {
        Statement,
        Variable,
        Other
    }

    enum Args {
        ;
        static final IntIntPair None = pair(0, 0);
        static final IntIntPair One = pair(1, 1);
        static final IntIntPair Two = pair(2, 2);

        static final IntIntPair GTEZero = pair(0, Param.COMPOUND_SUBTERMS_MAX);
        static final IntIntPair GTEOne = pair(1, Param.COMPOUND_SUBTERMS_MAX);
        static final IntIntPair GTETwo = pair(2, Param.COMPOUND_SUBTERMS_MAX);

    }

    final static class ImDep extends VarDep {

        private final String str;
        private final char symChar;
        private final int rank;

        public ImDep(byte id, byte sym) {
            super(id);
            this.str = String.valueOf((char) sym);
            this.symChar = (char) sym;
            this.rank = Term.opX(VAR_DEP, (short) id);
        }

        @Override
        public Term concept() {
            return Null;
        }

        @Override
        public int opX() {
            return rank;
        }

        @Override
        public @Nullable NormalizedVariable normalize(byte vid) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean unify(Term y, Unify u) {
            return y == this;
        }

        @Override
        public boolean unifyReverse(Term x, Unify u) {
            return x == this;
        }

        @Override
        public final void appendTo(Appendable w) throws IOException {
            w.append(symChar);
        }

        @Override
        public final String toString() {
            return str;
        }

    }

    public static class InvalidPunctuationException extends RuntimeException {
        public InvalidPunctuationException(byte c) {
            super("Invalid punctuation: " + c);
        }
    }


}
