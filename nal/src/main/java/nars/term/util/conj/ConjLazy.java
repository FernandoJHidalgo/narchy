package nars.term.util.conj;

import jcog.data.set.LongObjectArraySet;
import nars.Task;
import nars.term.Term;
import nars.term.util.builder.TermBuilder;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;

import static nars.Op.CONJ;
import static nars.term.atom.Bool.*;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;

/**
 * prepares construction of a conjunction target from components,
 * in the most efficient method possible according to them.
 * it is lighter weight than Conj.java in buffering / accumulating
 * events prior to complete construction.
 */
public class ConjLazy extends LongObjectArraySet<Term> implements ConjBuilder {


    public ConjLazy(int expectedSize) {
        super(expectedSize);
    }

    public ConjLazy() {
        this(4);
    }

    /** TODO add support for supersampling to include task.end() features */
    public static Term sequence(Task[] events) {
        int eventsSize = events.length;
        switch (eventsSize) {
            case 0:
                return True;
            case 1:
                return sequenceTerm(events[0]);
        }

        ConjLazy ce = new ConjLazy(eventsSize);

        for (Task o : events) {
            if (!ce.add(o.start(), sequenceTerm(o))) {
                break;
            }
        }

        return ce.term();
    }

    private static Term sequenceTerm(Task o) {
        return o.term().negIf(o.isNegative());
    }

    /**
     * consistent with ConjBuilder - semantics slightly different than superclass and List.addAt: returns true only if False or Null have been added; a duplicate value returns true
     */
    @Override
    public boolean addEvent(long when, Term t) {
        boolean result = true;
        if (t == True)
            return true; //ignore

        if (t == False || t == Null) {
            clear(); //fail
            result = false;
        }

        super.add(when, t);

        return result;
    }

    @Override
    public int eventOccurrences() {
        int s = size();
        if (s == 0) return 0;
        else if (s == 1) return 1;
        else if (s == 2) {
            //quick tests
            if (when[0] == when[1]) return 1;
            else return 2;
        } else if (s == 3) {
            //quick tests
            boolean a01 = when[0] == when[1];
            if (a01) {
                if (when[1] == when[2]) return 1;
                else return 2;
            }
            return when[1] == when[2] ? 2 : 3;
        }

        LongHashSet h = new LongHashSet(s);
        for (int i = 0; i < s; i++) {
            h.add(when[i]);
        }
        return h.size();
    }

    @Override
    public int eventCount(long w) {
        int s = size();
        int c = 0;
        for (int i = 0; i < s; i++)
            if (this.when[i] == w)
                c++;
        return c;
    }

    @Override
    public final LongIterator eventOccIterator() {
        return longIterator();
    }

    public static ConjLazy events(Term conj) {
        return events(conj, 0);
    }

    public static ConjLazy events(Term conj, long occOffset) {
        ConjLazy l = new ConjLazy(conj.op() == CONJ ? 4 : 1);
        conj.eventsWhile(l::add,
                occOffset, true, true, false);
        return l;
    }

    @Override
    protected final Object[] newArray(int newCapacity) {
        return new Term[newCapacity];
    }

    @Override
    public void negateEvents() {
        replaceAll(Term::neg);
    }

    @Override
    public Term term(TermBuilder B) {
        int n = size();
        switch (n) {
            case 0:
                return True;
            case 1:
                return get(0);
            case 2: {
                long w0 = when[0], w1 = when[1];

                if (w0 == ETERNAL && w1 != ETERNAL)
                    w0 = w1 = 0; //quick promote to parallel
                else if (w1 == ETERNAL && w0 != ETERNAL)
                    w0 = w1 = 0; //quick promote to parallel

                if (w0 == w1) {
                    Term a = items[0], b = items[1];
                    if (a.equals(b))
                        return a; //quick test
                    else
                        return CONJ.the(B, (w0 == ETERNAL) ? DTERNAL : 0, a, b);
                }
                break;
            }
            default: {
                long w0 = when[0];
                boolean parallel = true;
                for (int i = 1, whenLength = when.length; i < whenLength; i++) {
                    if (when[i] != w0) {
                        parallel = false;
                        break; //difference
                    }
                }
                //all same time
                if (parallel)
                    return CONJ.the(B, (w0 == ETERNAL) ? DTERNAL : 0, this);
            }
        }

        //failsafe impl:
        ConjBuilder c = new Conj(n);
        for (int i = 0; i < n; i++) {
            Term t = this.get(i);
            if (!c.add(when[i], t))
                break;
        }
        return c.term(B);
    }


}
