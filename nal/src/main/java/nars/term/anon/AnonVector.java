package nars.term.anon;

import com.google.common.io.ByteArrayDataOutput;
import jcog.data.byt.DynBytes;
import jcog.util.ArrayUtils;
import nars.Op;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.subterm.TermVector;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Predicate;

import static nars.Op.NEG;
import static nars.term.anon.AnonID.*;

/**
 * a vector which consists purely of AnonID terms
 */
public class AnonVector extends TermVector implements Subterms.SubtermsBytesCached {

    /*@Stable*/
    private final short[] subterms;

    private AnonVector(short[] s) {
        super(AnonID.subtermMetadata(s));
        this.subterms = s;
        testIfInitiallyNormalized();
    }

    protected void testIfInitiallyNormalized() {
        if (vars() == 0 || testIfInitiallyNormalized(subterms))
            setNormalized();
    }

    /**
     * assumes the array contains only AnonID instances
     */
    public AnonVector(Term... s) {
        super(s);

        boolean hasNeg = anyNeg();

        short[] t = subterms = new short[s.length];
        for (int i = 0, sLength = s.length; i < sLength; i++) {
            Term ss = s[i];
            boolean neg = hasNeg && ss.op() == NEG;
            if (neg)
                ss = ss.unneg();
            short tt = ((AnonID) ss).anonID;
            t[i] = neg ? ((short)-tt) : tt;
        }

        testIfInitiallyNormalized();
    }


//        @Override
//    public @Nullable Subterms transformSubs(TermTransform f) {
//        @Nullable Subterms s = super.transformSubs(f);
//        if (s!=this && equals(s))
//            return this; //HACK
//        else
//            return s;
//    }

    @Override
    public Subterms replaceSub(Term from, Term to, Op superOp) {


        short fid = AnonID.id(from);
        if (fid == 0)
            return this; //no change

        boolean found = false;
        if (fid > 0) {
            //find positive or negative subterm
            for (short x: subterms) {
                if (Math.abs(x) == fid) {
                    found = true;
                    break;
                }
            }
        } else {
            //find exact negative only
            for (short x: subterms) {
                if (x == fid) {
                    found = true;
                    break;
                }
            }
        }
        if (!found)
            return this;


        short tid = AnonID.id(to);
        if (tid != 0) {
            assert (from != to);
            short[] a = this.subterms.clone();
            if (fid > 0) {
                for (int i = 0, aLength = a.length; i < aLength; i++) { //replace positive or negative, with original polarity
                    short x = a[i];
                    if (x == fid) a[i] = tid;
                    else if (-x == fid) a[i] = (short) -tid;
                }
            } else {
                for (int i = 0, aLength = a.length; i < aLength; i++) //replace negative only
                    if (a[i] == fid) a[i] = tid;
            }
            return new AnonVector(a);
        } else {
            int n = subs();
            Term[] tt = new Term[n];
            short[] a = this.subterms;
            if (fid > 0) {
                for (int i = 0; i < n; i++) { //replace positive or negative, with original polarity
                    short x = a[i];
                    Term y;
                    if (x == fid) y = (to);
                    else if (-x == fid) y = (to.neg());
                    else y = (idToTerm(x));
                    tt[i] = (y);
                }
            } else {
                for (int i = 0; i < n; i++) { //replace negative only
                    tt[i] = (a[i] == fid ? to : idToTerm(a[i]));
                }

            }
            return new TermList(tt);
        }
    }

    @Override
    public final Term sub(int i) {
        return idToTerm(subterms[i]);
    }

    @Override
    public int subs(Op matchingOp) {
        short match;
        switch (matchingOp) {
            case NEG:
                return subsNeg();
            case ATOM:
                match = AnonID.ATOM_MASK;
                break;
            case VAR_PATTERN:
                match = AnonID.VARPATTERN_MASK;
                break;
            case VAR_QUERY:
                match = AnonID.VARQUERY_MASK;
                break;
            case VAR_DEP:
                match = AnonID.VARDEP_MASK;
                break;
            case VAR_INDEP:
                match = AnonID.VARINDEP_MASK;
                break;
            default:
                return 0;
        }
        int count = 0;
        for (short s: subterms) {
            if (s > 0 && idToMask(s) == match)
                count++;
        }
        return count;
    }

    private int subsNeg() {
        int count = 0;
        for (short s: subterms) {
            if (s < 0)
                count++;
        }
        return count;
    }


    @Override
    public final int subs() {
        return subterms.length;
    }

    private int indexOf(short id) {

//        if (id < 0 && !anyNeg())
//            return -1;

        return ArrayUtils.indexOf(subterms, id);
    }

// TODO TEST
//    private int indexOf(short id, int after) {
//        return ArrayUtils.indexOf(subterms, id, after+1);
//    }

    private int indexOf(AnonID t, boolean neg) {
        return indexOf(t.anonID(neg));
    }

    public int indexOf(AnonID t) {
        return indexOf(t.anonID);
    }

    @Override
    public int indexOf(Term t) {
        short tid = AnonID.id(t);
        return tid != 0 ? indexOf(tid) : -1;
    }

// TODO TEST
//    @Override
//    public int indexOf(Term t, int after) {
//        throw new TODO();
//    }

    private int indexOfNeg(Term x) {
        short tid = AnonID.id(x);
        return tid != 0 ? indexOf((short) -tid) : -1;
    }

    @Override
    public boolean contains(Term x) {
        return indexOf(x) != -1;
    }

    @Override
    public boolean containsNeg(Term x) {
        return indexOfNeg(x) != -1;
    }

    @Override
    public boolean containsRecursively(Term x, boolean root, Predicate<Term> inSubtermsOf) {
        if (x.op() == NEG) {
            if (anyNeg()) {
                Term tt = x.unneg();
                if (tt instanceof AnonID) {
                    return indexOf((AnonID) tt, true) != -1;
                }
            }
        } else {
            if (x instanceof AnonID) {
                short aid = ((AnonID) x).anonID;
                boolean hasNegX = false;
                for (short xx : this.subterms) {
                    if (xx == aid)
                        return true; //found positive
                    else if (xx == -aid)
                        hasNegX = true; //found negative, but keep searching for a positive first
                }
                if (hasNegX)
                    return (inSubtermsOf.test(x.neg()));

//                return (indexOf((AnonID) x) != -1)
//                        ||
//                        (anyNeg() && indexOf((AnonID) x, true) != -1 && inSubtermsOf.test(x.neg()));
            }

        }
        return false;
    }

    private boolean anyNeg() {
        return (structure & NEG.bit) != 0;
    }

    @Override
    public Iterator<Term> iterator() {
        return new AnonArrayIterator(subterms);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;

        if (this.hash != obj.hashCode()) return false;

        if (obj instanceof AnonVector) {
            return Arrays.equals(subterms, ((AnonVector) obj).subterms);
        }

        if (obj instanceof Subterms) {


            Subterms ss = (Subterms) obj;
            int s = subterms.length;
            if (ss.subs() != s)
                return false;
            for (int i = 0; i < s; i++) {
                if (!sub(i).equals(ss.sub(i)))
                    return false;
            }
            return true;

        }
        return false;
    }


    private transient byte[] bytes = null;

    @Override
    public void appendTo(ByteArrayDataOutput out) {
        if (bytes==null) {
            short[] ss = subterms;
            out.writeByte(ss.length);
            for (short s : ss) {
                if (s < 0) {
                    out.writeByte(Op.NEG.id);
                    s = (short) -s;
                }
                idToTermPos(s).appendTo(out);
            }
        } else {
            out.write(bytes);
        }
    }

    @Override
    public void bytes(DynBytes builtWith) {
        if (bytes == null)
            bytes = builtWith.arrayCopy(1 /* skip op byte */);
    }

    @Override
    public @Nullable Term subSub(int start, int end, byte[] path) {
        byte z = path[start];
        if (subterms.length <= z)
            return null;

        switch (end-start) {
            case 1:
                return sub(z);
            case 2:
                if (path[start+1]==0) {
                    short a = this.subterms[z];
                    if (a < 0)
                        return AnonID.idToTerm((short) -a); //if the subterm is negative its the only way to realize path of length 2
                }
                break;
        }
        return null;
    }
}
