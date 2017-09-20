package nars.term.container;

import jcog.list.ArrayIterator;
import nars.Op;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.function.Consumer;

import static nars.Op.Null;

/**
 * Size 1 TermVector
 */
public final class TermVector2 extends TermVector {

    public final Term x, y;

    /** uses an array argument so that the input array can be used directly without needing to create a new one when it calls the superclass constructor */
    public TermVector2(Term... xy) {
        super(xy);
        assert(xy.length == 2);
        this.x = xy[0];
        this.y = xy[1];
    }

    @NotNull
    @Override
    public Term[] toArray() {
        return new Term[] { x, y };
    }

    @Override
    public @NotNull Term sub(int i) {
        switch (i) {
            case 0: return x;
            case 1: return y;
            default:
                return Null; //throw new ArrayIndexOutOfBoundsException();
        }
    }

    @Override
    public boolean subIs(int i, Op o) {
        return sub(i).op()==o;
    }

    @Override
    public boolean subEquals(int i, Term maybeEquals) {
        return sub(i).equals(maybeEquals);
    }

    @Override
    public boolean equals(@NotNull Object obj) {
        if (this == obj) return true;
        if (obj instanceof TermContainer) {
            if (hash == obj.hashCode()) {
                TermContainer t = (TermContainer) obj;
                return (t.subs() == 2 && t.sub(0).equals(x) && t.sub(1).equals(y));
            }
        }
        return false;
    }

    @Override
    public int subs() {
        return 2;
    }

    @Override
    public Iterator<Term> iterator() {
        return new ArrayIterator(toArray());
    }

    @Override
    public void forEach(@NotNull Consumer<? super Term> action, int start, int stop) {
        if (start == stop) {
            action.accept( start == 0 ? x : y );
        } else if (start == 0 && stop == 1) {
            action.accept(x);
            action.accept(y);
        }
    }

    @Override
    public void forEach(@NotNull Consumer<? super Term> action) {
        action.accept(x);
        action.accept(y);
    }

    @Override public void recurseTerms(@NotNull Consumer<Term> v) {
        x.recurseTerms(v);
        y.recurseTerms(v);
    }

    @Override
    public boolean isDynamic() {
        return x.isDynamic() || y.isDynamic();
    }

}
