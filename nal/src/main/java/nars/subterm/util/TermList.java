package nars.subterm.util;

import jcog.list.FasterList;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;

import java.util.Collection;
import java.util.List;

/** mutable subterms, used in intermediate operations */
public class TermList extends FasterList<Term> implements Subterms {


    public TermList() { super(); }

    public TermList(int initialCapacity) {
        super(0, new Term[initialCapacity]);
    }

    public TermList(Term[] direct) {
        super(direct.length, direct);
    }

    public TermList(Collection<Term> copied) {
        this(copied.toArray(Op.EmptyTermArray));
    }

    public TermList(Iterable<Term> copied) {
        super(0);
        copied.forEach(this::add);
    }

    @Override
    public int hashCode() {
        return Subterms.hash((List)this);
    }


    @Override
    public FasterList<Term> toList() {
        return clone();
    }

    @Override
    public Term sub(int i) {
        return get(i);
    }

    @Override
    public Term[] arrayClone() {
        return toArray(new Term[size()]);
    }

    /** creates an immutable instance of this */
    public Subterms theSubterms() {
        return Op.terms.subterms(this);
    }

    @Override
    public int subs() {
        return size;
    }

    @Override
    public String toString() {
        return Subterms.toString(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        
        if ((obj instanceof TermList)) {
            return fastListEquals(((TermList)obj));
        } else {
            if (hashCode()!=obj.hashCode())
                return false;
            return ((Subterms)obj).equalTerms(this);
        }
    }













    public void addAll(Subterms x, int xStart, int xEnd) {
        ensureCapacity(xEnd-xStart);
        for (int i = xStart; i < xEnd; i++) {
            addWithoutResizeCheck(x.sub(i));
        }
    }

    /** use this only if the TermList is done being modified */
    public Term[] arraySharedKeep() {
        return toArrayRecycled(Term[]::new);
    }

    @Override protected Term[] newArray(int newCapacity) {
        return new Term[newCapacity];
    }

}
