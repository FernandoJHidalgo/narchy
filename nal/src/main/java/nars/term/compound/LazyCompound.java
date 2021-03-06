package nars.term.compound;

import com.google.common.primitives.Ints;
import jcog.WTF;
import jcog.data.byt.DynBytes;
import jcog.util.ArrayUtils;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.util.builder.TermBuilder;
import nars.term.util.builder.TermConstructor;
import nars.term.util.map.ByteAnonMap;
import nars.unify.ellipsis.EllipsisMatch;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.Function;

import static nars.Op.*;
import static nars.term.atom.Bool.Null;
import static nars.term.atom.Bool.True;
import static nars.time.Tense.DTERNAL;

/**
 * memoizable supplier of compounds
 * fast to construct but not immediately usable (or determined to be valid)
 * without calling the .the()
 * consists of a tape flat linear tape of instructions which
 * when executed construct the target
 */
public class LazyCompound {
    final static int INITIAL_CODE_SIZE = 16;

    ByteAnonMap sub = null;
    final DynBytes code = new DynBytes(INITIAL_CODE_SIZE);

    final static int INITIAL_ANON_SIZE = 8;
    private boolean changed;

    public LazyCompound() {

    }

    public boolean updateMap(Function<Term, Term> m) {
        if (sub == null) return false;
        return sub.updateMap(m);
    }

    public final LazyCompound compoundStart(Op o) {
        compoundStart(o, DTERNAL);
        return this;
    }


    /**
     * append compound
     */
    public final LazyCompound compound(Op o, Term... subs) {
        return compound(o, DTERNAL, subs);
    }

    /**
     * append compound
     */
    public LazyCompound compound(Op o, int dt, Term... subs) {
        int n = subs.length;
        assert (n < Byte.MAX_VALUE);
        return compoundStart(o, dt).subsStart((byte) n).subs(subs).compoundEnd(o);
    }

    public LazyCompound compoundEnd(Op o) {
        return this;
    }


    public LazyCompound subsStart(byte subterms) {
        code.writeByte(subterms);
        return this;
    }

    public LazyCompound compoundStart(Op o, int dt) {
        DynBytes c = this.code;

        byte oid = o.id;

        if (!o.temporal)
            c.writeByte(oid);
        else
            c.writeByteInt(oid, dt);

//        else
//            assert (dt == DTERNAL);

        return this;
    }

    final static byte MAX_CONTROL_CODES = (byte) Op.ops.length;


    public final LazyCompound negStart() {
        compoundStart(NEG, DTERNAL);
        return this;
    }


    /**
     * add an already existent sub
     */
    public LazyCompound append(Term x) {
        if (x instanceof Atomic || x instanceof EllipsisMatch) {
            return appendAtomic(x);
        } else {
            return append((Compound) x);
        }
    }

    protected final LazyCompound append(Compound x) {
        Op o = x.op();
        if (o == NEG) {
            return negStart().append(x.unneg()).compoundEnd(NEG);
        } else {
            return compoundStart(o, x.dt()).appendSubterms(x.subterms()).compoundEnd(o);
        }
    }

    public LazyCompound appendSubterms(Subterms s) {
        return subsStart((byte) s.subs()).subs(s).subsEnd();
    }

    protected LazyCompound appendAtomic(Term x) {
        code.writeByte(MAX_CONTROL_CODES + intern(x));
        return this;
    }

    private byte intern(Term x) {
        //assert(!(x == null || x == Null || x.op() == NEG));
        return sub().intern(x);
    }

    final LazyCompound subs(Iterable<Term> subs) {
        subs.forEach(this::append);
        return this;
    }

    final LazyCompound subs(Term... subs) {
        for (Term x : subs)
            append(x);
        return this;
    }


    private ByteAnonMap sub() {
        ByteAnonMap sub = this.sub;
        return this.sub == null ? (this.sub = new ByteAnonMap(INITIAL_ANON_SIZE)) : sub;
    }

    public final Term get() {
        return get(Op.terms);
    }

    /**
     * run the construction process
     */
    public Term get(TermBuilder b) {

        DynBytes c = code;
        if (code == null)
            return Null; //nothing
        else {
//            if (sub != null)
//                sub.readonly(); //optional
            return getNext(b, c.arrayDirect(), new int[]{0, c.len});
        }
    }

    private Term getNext(TermBuilder b, byte[] ii, int[] range) {
        int from;
        byte ctl = ii[(from = range[0]++)];
        //System.out.println("ctl=" + ctl + " @ " + from);

        Term next;
        if (ctl < MAX_CONTROL_CODES) {
            Op op = Op.ops[ctl];
            if (op == NEG)
                next = getNext(b, ii, range).neg();
            else {


                if (op.atomic)
                    throw new WTF(); //alignment error or something

                int dt;
                if (op.temporal) {
                    int p = range[0];
                    range[0] += 4;
                    dt = Ints.fromBytes(ii[p++], ii[p++], ii[p++], ii[p/*++*/]);
                } else
                    dt = DTERNAL;

                byte subterms = ii[range[0]++];
                if (subterms == 0) {
                    if (op == PROD)
                        next = EmptyProduct;
                    else if (op == CONJ)
                        next = True;
                    else {
                        throw new WTF();
                    }
                } else {
                    Term[] s = getNext(b, subterms, ii, range);
                    if (s == null)
                        return Null;
                    else {

//                        for (Term x : s) if (x == null) throw new NullPointerException();

                        if (op==INH && evalInline() && s[1] instanceof Functor.InlineFunctor && s[0].op()==PROD) {

                            Term z = ((Functor.InlineFunctor)s[1]).applyInline(s[0].subterms());
                            if (z == null)
                                return Null;
                            next = z;

                            //TODO if Functor.isDeterministic { replaceAhead...

                        } else {

                            next = op.the(b, dt, s);

                            assert (next != null);

                            if (next != Null)
                                replaceAhead(ii, range, from, next);

                        }

                    }
                }
            }


        } else {
            next = next(b, ctl);
            //skip zero padding suffix
            while (range[0] < range[1] && code.at(range[0]) == 0) {
                ++range[0];
            }
        }

        return next;


    }

    protected boolean evalInline() {
        return true;
    }

    private void replaceAhead(byte[] ii, int[] range, int from, Term next) {
        int to = range[0];
        int end = range[1];
        int span = to - from;
        if (end - to >= span) {
            //search for repeat occurrences of the start..end sequence after this
            int afterTo = to;
            byte n = 0;
            do {
                int match = ArrayUtils.nextIndexOf(ii, afterTo, end, ii, from, to);

                if (match != -1) {
                    //System.out.println("repeat found");
                    if (n == 0)
                        n = (byte) (MAX_CONTROL_CODES + intern(next)); //intern for re-substitute
                    code.set(match, n);

                    code.fillBytes((byte) 0, match + 1, match + span); //zero padding, to be ignored
                    afterTo = match + span;
                } else
                    break;

            } while (afterTo < end);
        }
    }

    protected Term next(TermConstructor b, byte ctl) {
        Term n = sub.interned((byte) (ctl - MAX_CONTROL_CODES));
        if (n == null)
            throw new NullPointerException();
        return n;
    }

    @Nullable
    private Term[] getNext(TermBuilder b, byte n, byte[] ii, int[] range) {
        Term[] t = null;
        //System.out.println(range[0] + ".." + range[1]);
        for (int i = 0; i < n; ) {
            //System.out.println("\t" + s + "\t" + range[0] + ".." + range[1]);
            if (range[0] >= range[1])
                throw new ArrayIndexOutOfBoundsException();
            //return Arrays.copyOfRange(t, 0, i); //hopefully this is becaues of an ellipsis that got inlined and had no effect

            Term y;
            if ((y = getNext(b, ii, range)) == Null)
                return null;

            if (y instanceof EllipsisMatch) {
                //expand
                int en = y.subs();
                n += en - 1;
                if (t == null)
                    t = (n == 0) ? EmptyTermArray : new Term[n];
                else if (t.length != n) {
                    t = Arrays.copyOf(t, n);
                }
                if (en > 0) {
                    for (Term e : ((EllipsisMatch) y)) {
                        if (e == null || e == Null)
                            throw new NullPointerException();
                        t[i++] = e;
                    }
                }
            } else {
                if (t == null)
                    t = new Term[n];
                if (y == null)
                    throw new NullPointerException(); //WTF
                t[i++] = y;
            }
        }
        return t;
    }

    public boolean changed() {
        return changed;
    }

    public void setChanged(boolean c) {
        changed = c;
    }

    public int pos() {
        return code.len;
    }

    public void rewind(int pos) {
        code.len = pos;
    }

    public LazyCompound subsEnd() {
        return this;
    }

//    /**
//     * ability to lazily evaluate and rewrite functors
//     */
//    public static class LazyEvalCompound extends LazyCompound {
//
//        ShortArrayList inhStack = null;
//
//        private static class Eval extends UnnormalizedVariable {
//            final Functor.InlineFunctor f;
//            byte arity;
//            final byte[] args;
//
//            public Eval(Functor.InlineFunctor f, byte arity, byte[] args) {
//                super(Op.VAR_PATTERN, ";");
//                this.arity = arity;
//                this.f = f;
//                this.args = args;
//            }
//
//            @Override
//            public boolean equals(Object o) {
//                return this == o;
//            }
//
//            @Override
//            public int hashCode() {
//                return System.identityHashCode(this);
//            }
//
//            @Override
//            public String toString() {
//                return f + "<" + Arrays.toString(args) + ">";
//            }
//
//            public Term eval(TermBuilder b, LazyCompound c) {
//                Term[] a = c.getNext(b, arity, args, new int[]{0, args.length});
//                if (a == null) {
//                    return Null;
//                } else {
//                    Term t = f.applyInline($.vFast(a));
//                    return t;
//                }
//            }
//        }
//
//        @Override
//        public LazyCompound compoundStart(Op o, int dt) {
//
//            if (o == INH) {
//                if (inhStack == null)
//                    inhStack = new ShortArrayList(8);
//
//                inhStack.add((short) code.length()); //record compound at this position
//            }
//
//            return super.compoundStart(o, dt);
//        }
//
//        @Override
//        public LazyCompound compoundEnd(Op o) {
//            if (o == INH) {
//                //assert(inhStack!=null);
//                inhPop();
//            }
//            return super.compoundEnd(o);
//        }
//
//        private void inhPop() {
//            inhStack.removeAtIndex(inhStack.size() - 1);
//        }
//
//        @Override
//        protected LazyCompound append(Atomic x) {
//            if (x instanceof Functor.InlineFunctor) {
//                //
//                // ((arg1,arg,...)-->F)
//                //
//                //scan backwards, verifying preceding product arguments contained within inheritance
//                if (inhStack != null && !inhStack.isEmpty()) {
//                    DynBytes c = this.code;
//                    int lastInh = inhStack.getLast();
//
//                    byte[] cc = c.arrayDirect();
//                    int lastProd = lastInh + 2;
//                    if (c.length() >= lastProd && cc[lastProd] == PROD.id) {
//                        int pos = c.length();
//                        return append(evalLater((Functor.InlineFunctor) x, lastProd, pos /* after adding the functor atomic */));
//                    }
//
//                }
//
//            }
//
//            return super.append(x);
//        }
//
//        @Override
//        public void rewind(int pos) {
//            if (pos != pos()) {
//                while (inhStack != null && !inhStack.isEmpty() && inhStack.getLast() > pos)
//                    inhStack.removeAtIndex(inhStack.size() - 1);
//                super.rewind(pos);
//            }
//        }
//
//        /**
//         * deferred functor evaluation
//         */
//        private Eval evalLater(Functor.InlineFunctor f, int start, int end) {
//            DynBytes c = code;
//            Eval e = new Eval(f, code.at(start + 1), c.subBytes(start + 2, end));
//            rewind(start - 2);
//            //inhPop();
//
//            return e;
//        }
//
//        @Override
//        protected Term next(TermBuilder b, byte ctl) {
//            Term t = super.next(b, ctl);
//            if (t instanceof Eval) {
//                t = ((Eval) t).eval(b, this);
//            }
//            if (t == null)
//                throw new NullPointerException();
//            return t;
//        }
//
//
//    }


//    static class Int1616 extends Int {
//
//        public Int1616(short low, short hi) {
//            super(low | (hi << 16));
//        }
//
//        public short low() {
//            return (short) (id & 0xffff);
//        }
//        public short high() {
//            return (short) ((id >> 16) & 0xffff);
//        }
//
//    }

}
