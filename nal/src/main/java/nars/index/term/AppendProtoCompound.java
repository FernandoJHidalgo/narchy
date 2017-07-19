package nars.index.term;

import jcog.byt.DynBytes;
import jcog.util.ArrayPool;
import nars.IO;
import nars.Op;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Terms;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.time.Tense.DTERNAL;

/**
 * a lightweight, prototype compound, not fully constructed.
 * <p>
 * it is constructed incrementally by appending additional subterms.
 * <p>
 * it accumulates a rolling hash so it may be used as a key in a hash Map
 * <p>
 * TODO:
 * VolumeLimitedIncrementalProtoCompound extends AppendProtoCompound
 * etc...
 */
public class AppendProtoCompound extends /*HashCached*/DynBytes implements ProtoCompound {

    public final Op op;

    @Nullable
    protected Term[] subs;

    int size;

    int hash;


    public AppendProtoCompound(Op op, @NotNull Term[] prepopulated) {
        super();
        this.op = op;
        this.subs = prepopulated; //zero-copy direct usage
        this.size = prepopulated.length;
    }

    /**
     * hash will be modified for each added subterm
     *
     * @param initial_capacity estimated size, but will grow if exceeded
     */
    public AppendProtoCompound(Op op, int initial_capacity) {
        super();
        this.op = op;
        if (initial_capacity > 0)
            this.subs = new Term[initial_capacity];
    }

    public AppendProtoCompound(Op op, TermContainer subterms) {
        this(op, subterms.size());
        for (int i = 0; i < (size = subs.length); i++) /* (has been set in superconstructor)*/
            subs[i] = subterms.sub(i);

    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public Op op() {
        return op;
    }

    /**
     * use only during actual build step; should not be called while being compared
     */
    @Override
    public Term[] subterms() {
        compact(); //compact the key

        Term[] tt;
        int s = this.size;
        @NotNull Term[] ss = this.subs;
        if (ss.length == s) {
            tt = ss;
            //return ss; //dont reallocate it's just fine to share
        } else {
            tt = Arrays.copyOfRange(ss, 0, s); //trim
        }

        this.subs = null; //clear refernce to the array from this point

        return tt;
    }

    @Override
    public Term[] toArray() {
        return subs; //.clone();
    }

//    @Override
//    public AppendProtoCompound commit(int commuteForDT) {
//        boolean commute = false;
//        if (commuteForDT != DTERNAL && op != null) {
//            commute = subs.length > 1 && op.commutative;
//            if (commute && op.temporal && !Op.concurrent(commuteForDT))
//                commute = false; //dont pre-commute
//        }
//        return commit(commute);
//    }

    /**
     * hashes and prepares for use in hashmap
     */
    public AppendProtoCompound commit() {

//        if (commute) {
//            subs = Terms.sorted(subs);
//            size = subs.length;
//        }

        int volume = 0;
        for (Term x : subs)
            volume += x.volume();

        ArrayPool<byte[]> bytePool = ArrayPool.bytes();
        byte[] bbTmp = bytePool.getMin(volume * 32 /* estimate */);
        try {

            this.bytes = bbTmp;
            //this.bytes = new byte[subs.length * 8 /* estimate */];

            writeByte(op != null ? op.id : Byte.MAX_VALUE);
            for (Term x : subs)
                appendKey(x);

            compress();
            compact(bbTmp, false);

        } finally {
            bytePool.release(bbTmp);
        }

        this.hash = hash(0, len);
        if (this.hash == 0) this.hash = 1;
        return this;
    }

    @Override
    public int hashCode() {
        int h = this.hash;
        //assert(h!=0);
        return h;
    }

    @Override
    public boolean AND(@NotNull Predicate<Term> t) {
        for (Term x : subs) {
            //if (x == null)
            //  break;
            if (t.test(x)) return false;
        }
        return true;
    }

    @Override
    public boolean OR(@NotNull Predicate<Term> t) {
        for (Term x : subs) {
            //if (x == null)
            //  break;
            if (t.test(x)) return true;
        }
        return false;
    }

    public boolean add(@NotNull Termed x) {
        int c = subs.length;
        int len = this.size;
        if (c == len) {
            ensureCapacity(len, len + Math.max(1, (len / 2)));
        }

        _add(x.term());

        return true;
    }

    protected void _add(@NotNull Term x) {
        subs[size++] = x;
    }

    private void appendKey(@NotNull Term x) {
        try {
            IO.writeTerm(this, x);
            writeByte(0); //separator
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void ensureCapacity(int newCapacity) {
        int s = this.size;
        if (s < newCapacity)
            ensureCapacity(s, newCapacity);
    }

    private void ensureCapacity(int curCap, int newCapacity) {
        Term[] newItems = new Term[newCapacity];
        System.arraycopy(this.subs, 0, newItems, 0, Math.min(curCap, newCapacity));
        subs = newItems;
    }

    public void addAll(@NotNull Term[] u) {
        int ul = u.length;
        if (ul > 0) {
            ensureCapacity(size + ul);
            for (Term x : u)
                _add(x);
        }
    }

//    @Override
//    public boolean equals(Object obj) {
//        AppendProtoCompound x = (AppendProtoCompound) obj;
//        return x.hash == hash && x.op == op && x.dt == dt && Arrays.equals(bytes, x.bytes);
//                //Util.equalArraysDirect(subs, x.subs);
//    }

    @Override
    public Term sub(int i) {
        return subs[i];
    }


    @Override
    public String toString() {
        return "AppendProtoCompound{" +
                "op=" + op +
                ", subs=" + Arrays.toString(Arrays.copyOfRange(subs, 0, size)) + //HACK use more efficient string method
                '}';
    }

    @Override
    public void forEach(Consumer<? super Term> action, int start, int stop) {
        throw new UnsupportedOperationException("TODO");
    }


    @NotNull
    @Override
    public Iterator<Term> iterator() {
        throw new UnsupportedOperationException("TODO");
    }


}
