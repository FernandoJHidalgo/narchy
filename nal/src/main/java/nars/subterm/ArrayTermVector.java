package nars.subterm;

import com.google.common.io.ByteArrayDataOutput;
import jcog.util.ArrayIterator;
import nars.Param;
import nars.term.Term;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.eclipse.collections.api.block.predicate.primitive.ObjectIntPredicate;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Holds a vector or tuple of terms.
 * Useful for storing a fixed number of subterms

 */
public class ArrayTermVector extends TermVector {

    /*@NotNull*/
    /*@Stable*/
    private final Term[] terms;

    public ArrayTermVector(/*@NotNull */Term... terms) {
         super(terms);
         this.terms = terms;
    }

    @Override
    public final int intifyShallow(IntObjectToIntFunction<Term> reduce, int v) {
        for (Term t : terms)
            v = reduce.intValueOf(v, t);
        return v;
    }

    @Override public final int intifyRecurse(IntObjectToIntFunction<Term> reduce, int v) {
        for (Term t : terms)
            v = t.intifyRecurse(reduce, v); 
        return v;
    }

    @Override
    public final boolean equals(/*@NotNull*/ Object obj) {
        if (this == obj) return true;

        if (!(obj instanceof Subterms))
            return false;
        Subterms that = (Subterms) obj;
        if (hash!= that.hashCodeSubterms())
            return false;

        if (obj instanceof ArrayTermVector) {
            
            ArrayTermVector v = (ArrayTermVector) obj;
            if (!Arrays.equals(terms, v.terms))
                return false;

            equivalentTo(v);
        } else {
            final Term[] x = this.terms;
            int s = x.length;
            if (s != that.subs())
                return false;
            for (int i = 0; i < s; i++)
                if (!x[i].equals(that.sub(i)))
                    return false;

            if (obj instanceof TermVector)
                equivalentTo((TermVector)obj);
        }



        return true;
    }


    @Override
    /*@NotNull*/ public final Term sub(int i) {
        return terms[i];
        
    }

    @Override public final Term[] arrayClone() {
        return terms.clone();
    }

    @Override
    public final Term[] arrayShared() {
        return Param.TERM_ARRAY_SHARE ? terms : terms.clone();
    }

    @Override
    public final int subs() {
        return terms.length;
    }

    @Override
    public final Iterator<Term> iterator() {
        return ArrayIterator.get(terms);
    }

    @Override
    public final void forEach(Consumer<? super Term> action, int start, int stop) {
        Term[] t = this.terms;
        for (int i = start; i < stop; i++) {
            action.accept(t[i]);
        }
    }

    @Override
    public final boolean OR(Predicate<Term> p) {
        Term[] t = this.terms;
        for (Term i : t)
            if (p.test(i))
                return true;
        return false;
    }

    @Override public final boolean AND(Predicate<Term> p) {
        Term[] t = this.terms;
        for (Term i : t)
            if (!p.test(i))
                return false;
        return true;
    }

    @Override
    public boolean ANDwith(ObjectIntPredicate<Term> p) {
        Term[] t = this.terms;
        for (int i = 0, tLength = t.length; i < tLength; i++) {
            if (!p.accept(t[i], i))
                return false;
        }
        return true;
    }
    
    public final void append(ByteArrayDataOutput out) {
        Term[] t = this.terms;
        out.writeByte(t.length);
        for (Term x : t)
            x.append(out);
    }
}
