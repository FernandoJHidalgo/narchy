package nars.subterm;

import com.google.common.io.ByteArrayDataOutput;
import jcog.Util;
import nars.Op;
import nars.term.Term;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

abstract public class AbstractUnitSubterm implements Subterms {

    abstract public Term sub();

    @Override
    public final void append(ByteArrayDataOutput out) {
        out.writeByte(1);
        sub().appendTo(out);
    }

    @Override
    public String toString() {
        return "(" + sub() + ')';
    }

    @Override
    public Term[] arrayClone() {
        return new Term[] { sub() };
    }

    @Override
    public boolean OR(Predicate<Term> p) {
        return p.test(sub());
    }
    @Override
    public boolean AND(Predicate<Term> p) {
        return p.test(sub());
    }

    @Override public boolean isTemporal() {
        return hasAny(Op.Temporal) && sub().isTemporal();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Subterms)) return false;
        Subterms s = (Subterms)obj;
        return
                (s.subs() == 1) &&
                (sub().equals(s.sub(0)));
    }

    @Override
    public int hashCode() {
        return Util.hashCombine1(sub());
    }

    @Override
    public int volume() {
        return 1+sub().volume();
    }

    @Override
    public int complexity() {
        return 1+sub().complexity();
    }

    @Override
    public int structure() {
        return sub().structure();
    }

    @Override
    public final Term sub(int i) {
        if (i!=0) throw new ArrayIndexOutOfBoundsException();
        return sub();
    }

    public final Term[] termsExcept(int i) {
        assert(i==0);
        return Op.EmptyTermArray;
    }

    @Override
    public final int subs() {
        return 1;
    }

    @Override
    public boolean isNormalized() {
        return sub().isNormalized();
    }

    @Override
    public void setNormalized() {
        sub().subterms().setNormalized();
    }

    @Override
    public final void forEach(Consumer<? super Term> c) {
        c.accept(sub());
    }

    @Override
    public void forEach(Consumer<? super Term> c, int start, int stop) {
        if (start!=0 ||
                stop!=1)
            throw new ArrayIndexOutOfBoundsException();
        c.accept(sub());
    }

    @Override
    public Stream<Term> subStream() {
        return Stream.of(sub());
    }

    @Override
    public Iterator<Term> iterator() {
        return com.google.common.collect.Iterators.singletonIterator(sub());
    }

    @Override
    public final int intifyRecurse(IntObjectToIntFunction<Term> reduce, int v) {
        return sub().intifyRecurse(reduce, v);
    }

    @Override
    public final int intifyShallow(IntObjectToIntFunction<Term> reduce, int v) {
        return reduce.intValueOf(v, sub());
    }

}
