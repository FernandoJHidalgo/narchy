package nars.term;

import jcog.Util;
import nars.IO;
import nars.Op;
import nars.Param;
import nars.derive.PatternCompound;
import nars.term.container.TermContainer;
import nars.term.transform.CompoundTransform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.CONJ;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

public class GenericCompoundDT /*extends ProxyTerm<Compound>*/ implements Compound, CompoundDT {

    /**
     * numeric (term or "dt" temporal relation)
     */
    public final int dt;
    private final int hashDT;
    protected final Compound ref;

    public GenericCompoundDT(Compound base, int dt) {
        this.ref = base;

        if (!(dt == XTERNAL || Math.abs(dt) < Param.DT_ABS_LIMIT))
            throw new InvalidTermException(base.op(), dt, base.subterms(), "exceeded DT limit");

        if (Param.DEBUG_EXTRA) {

            assert (getClass() != GenericCompoundDT.class /* a subclass */ || dt != DTERNAL);

            Op op = base.op();

            @NotNull TermContainer subterms = base.subterms();
            int size = subterms.subs();

            if (op.temporal && (op != CONJ && size != 2))
                throw new InvalidTermException(op, dt, "Invalid dt value for operator", subterms.arrayClone());

            if (dt != XTERNAL && op.commutative && size == 2) {
                if (sub(0).compareTo(sub(1)) > 0)
                    throw new RuntimeException("invalid ordering");
            }

        }


        this.dt = dt;

        assert dt != DTERNAL || this instanceof PatternCompound : "use GenericCompound if dt==DTERNAL";

        assert dt == DTERNAL || dt == XTERNAL || (Math.abs(dt) < Param.DT_ABS_LIMIT) : "abs(dt) limit reached: " + dt;

        int baseHash = base.hashCode();
        this.hashDT = dt != DTERNAL ? Util.hashCombine(baseHash, dt) : baseHash;
    }

    @Override
    public int varQuery() {
        return ref.varQuery();
    }

    @Override
    public int varDep() {
        return ref.varDep();
    }

    @Override
    public int varIndep() {
        return ref.varIndep();
    }

    @Override
    public int varPattern() {
        return ref.varPattern();
    }


    @Override
    public boolean contains(Term t) {
        return ref.subterms().contains(t);
    }

    @Override
    public /*@NotNull*/ Op op() {
        return ref.op();
    }


    @Override
    public final Term dt(int dt) {
        return dt == this.dt ? this : Compound.super.dt(dt);
    }

    @Override
    public int structure() {
        return ref.structure();
    }

    @NotNull
    @Override
    public String toString() {
        return IO.Printer.stringify(this).toString();
    }

//    @Override
//    public Term sub(int i, Term ifOutOfBounds) {
//        return ref.sub(i, ifOutOfBounds);
//    }

//    @Override
//    public final int hashCodeSubTerms() {
//        return ref.hashCodeSubTerms();
//    }

//    @Override
//    public Term conceptual() {
//        return Compound.super.conceptual();
//        //return ref.conceptual();
//    }


//    @Override
//    @Nullable
//    public final Term root() {
//        return Compound.super.root();
//        //return ref.root();
//    }

    public @Nullable Term transform(CompoundTransform t, Compound parent) {
        return transform(t);
    }

    @Override
    public @Nullable Term transform(CompoundTransform t) {
        return t.transform(this, op(), dt);
    }


    @Override
    public boolean equals(Object that) {
        if (this == that) return true;

        if (!(that instanceof Term) || hashDT != that.hashCode())
            return false;

        if (that instanceof GenericCompoundDT) {
            GenericCompoundDT cthat = (GenericCompoundDT) that;
            if (dt != cthat.dt) return false;
            Compound thatRef = cthat.ref;
            Compound myRef = this.ref;
            if (myRef == thatRef) {
                return true;
            } else if (myRef.equals(thatRef)) {
//                this.ref = thatRef; //share
                return true;
            } else {
                return false;
            }
        } else {

            return Compound.equals(this, (Term)that);
        }
    }


    //    @Override
//    public boolean equals(Object obj) {
//        if (this == obj) return true;
//
//        if (obj instanceof GenericCompoundDT) {
//
//            GenericCompoundDT d = (GenericCompoundDT) obj;
//
//            Compound ref = this.ref;
//            Compound dref = d.ref;
//
//            if (!Param.CompoundDT_TermSharing) {
//
//                //compares hash and dt first, but doesnt share
//                return (hashDT == d.hashDT && dt == d.dt && ref.equals(d.ref));
//
//            } else {
//
//                if (ref == dref) {
//                    //ok
//                } else if (ref.equals(dref)) {
//                    //share equivalent instance, prefer to maintain a normalized term as it is likely used elsewhere (ie. in task content)
//                    if (ref.isNormalized()) {
//                        d.ref.setNormalized(); //though we will overwrite this next, in case it's shared elsewhere it will now also be known normalized
//                        d.ref = ref;
//                    } else if (d.ref.isNormalized()) {
//                        ref.setNormalized();  //though we will overwrite this next, in case it's shared elsewhere it will now also be known normalized
//                        this.ref = d.ref;
//                    } else {
//                        d.ref = ref;
//                    }
//
//
//                } else {
//                    return false;
//                }
//
//                return (hashDT == d.hashDT && dt == d.dt);
//            }
//
//        } else if (obj instanceof ProxyCompound) {
//            return equals(((ProxyCompound) obj).ref);
//        }
//
//        return false;
//    }

    @Override
    public TermContainer subterms() {
        return ref.subterms();
    }

    @Override
    public int subs() {
        return ref.subs();
    }

    @Override
    public int volume() {
        return ref.volume();
    }

    @Override
    public int complexity() {
        return ref.complexity();
    }

    @Override
    public final int hashCode() {
        return hashDT;
    }

    @Override
    public final int dt() {
        return dt;
    }





    //    @Override
//    public Compound dt(int nextDT) {
//        if (nextDT == this.dt)
//            return this;
//
//        return compoundOrNull($.the(op(), nextDT, toArray()));
//
////        if (o.commutative && !Op.concurrent(this.dt) && Op.concurrent(nextDT)) {
////            //HACK reconstruct with sorted subterms. construct directly, bypassing ordinary TermBuilder
////            TermContainer ms = subterms();
////            //@NotNull TermContainer st = ms;
//////            if (!st.isSorted()) {
//////                Term[] ts = Terms.sorted(ms.toArray());
//////                if (ts.length == 1) {
//////                    if (o == CONJ)
//////                        return compoundOrNull(ts[0]);
//////                    return null;
//////                }
//////
//////                TermContainer tv;
//////                if (ms.equalTerms(ts))
//////                    tv = ms; //share
//////                else
//////                    tv = TermVector.the(ts);
//////
////                /*GenericCompound g =*/ return compoundOrNull($.the(o, nextDT, ms.toArray())); //new GenericCompound(o, tv);
//////                if (nextDT != DTERNAL)
//////                    return new GenericCompoundDT(g, nextDT);
//////                else
//////                    return g;
//////            }
////
////        }
////        return ref.dt(nextDT);
//    }
}
