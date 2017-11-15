package nars.term.container;

import com.google.common.collect.Iterators;
import nars.Op;
import nars.derive.match.EllipsisMatch;
import nars.term.Term;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.eclipse.collections.impl.factory.Sets;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Size 1 TermVector
 */
public class TermVector1 extends TermVector {

    public final Term sub;


    public TermVector1(Term sub) {
        super(sub);
        assert(!(sub instanceof EllipsisMatch));

        this.sub = sub;
    }


    @Override
    public final boolean isTemporal() {
        return sub.isTemporal();
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        else if (obj instanceof TermVector) {
            TermVector t = (TermVector)obj;
            if (hash!=t.hash || t.subs()!=1)
                return false;

            if (t.sub(0).equals(sub)) {
                equivalentTo(t);
                return true;
            }

            return false;
        }
        else if (obj instanceof TermContainer && hash== obj.hashCode()) {
            TermContainer t = (TermContainer) obj;
            if (t.subs() == 1 && sub.equals(t.sub(0))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equalTerms(Term[] c) {
        return c.length == 1 && sub.equals(c[0]);
    }

    @NotNull
    @Override
    public Term[] toArray() {
        return new Term[]{sub};
    }

    @Override
    public final Term sub(int i) {
        if (i != 0)
            throw new ArrayIndexOutOfBoundsException();
        return sub;
    }

    @Override
    public boolean subIs(int i, Op o) {
        return i == 0 && sub.op() == o;
    }

    @Override
    public boolean subEquals(int i, Term x) {
        return i == 0 && sub.equals(x);
    }


    @Override
    public final int subs() {
        return 1;
    }

    @Override
    @NotNull
    public Set<Term> toSet() {
        return Sets.mutable.of(sub);
    }

    @Override
    public @NotNull Set<Term> toSet(Predicate<Term> ifTrue) {
        return ifTrue.test(sub) ? toSet() : Collections.emptySet();
    }

    @NotNull
    @Override
    public String toString() {
        return "(" + sub + ')';
    }

    @Override
    public void recurseTerms(Consumer<Term> v) {
        sub.recurseTerms(v);
    }

    @Override
    public Iterator<Term> iterator() {
        return Iterators.singletonIterator(sub);
    }

//    @NotNull
//    @Override
//    public Term[] toArray() {
//        return subtermsArray();
//    }
//
//    @NotNull
//    @Override
//    public <T> T[] toArray(T[] ts) {
//        throw new UnsupportedOperationException();
//    }


//    public boolean containsAll(Collection<?> collection) {
//        int cs = collection.size();
//        switch (cs) {
//            case 0:
//                throw new UnsupportedOperationException(); //?
//            case 1:
//                return the.equals(collection.iterator().next());
//        }
//        return false;
//    }

    @Override
    public void forEach(Consumer<? super Term> action, int start, int stop) {
        if (start != 0 || stop != 0)
            throw new ArrayIndexOutOfBoundsException();
        forEach(action);
    }

    @Override
    public void forEach(Consumer<? super Term> action) {
        action.accept(sub);
    }

//    @Override public boolean equalTo(@NotNull TermContainer b) {
//        return //(hashCode() == b.hashCode()) &&
//                (b.size()==1) &&
//                the.equals(b.sub(0));
//    }


    @Override
    public final boolean contains(Term t) {
        return sub.equals(t);
    }

    @Override
    public boolean OR(Predicate<Term> p) {
        return p.test(sub);
    }

    @Override
    public boolean AND(Predicate<Term> p) {
        return p.test(sub);
    }

    @Override
    public int varIndep() {
        return sub.varIndep();
    }

    @Override
    public int varDep() {
        return sub.varDep();
    }

    @Override
    public int varQuery() {
        return sub.varQuery();
    }

    @Override
    public int varPattern() {
        return sub.varPattern();
    }

    @Override
    public int vars() {
        return sub.vars();
    }


}
