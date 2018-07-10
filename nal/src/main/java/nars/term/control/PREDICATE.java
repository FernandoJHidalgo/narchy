package nars.term.control;

import com.google.common.collect.Iterables;
import jcog.TODO;
import jcog.Util;
import jcog.list.FasterList;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The PrediTerm - a term-identified predicate (boolean-returning) function of a state
 *
 * @param X the type of state that is relevant to implementations
 */
public interface PREDICATE<X> extends Term, Predicate<X> {


    /** suspect */
    Comparator<PREDICATE> sortByCostIncreasing = (a, b) -> {
        if (a.equals(b)) return 0;
        float ac = a.cost();
        float bc = b.cost();
        if (ac > bc) return +1;
        else if (ac < bc) return -1;
        else return a.compareTo(b);
    };
    PREDICATE[] EMPTY_PREDICATE_ARRAY = new PREDICATE[0];


    static <X> PREDICATE<X>[] transform(Function<PREDICATE<X>, PREDICATE<X>> f, PREDICATE[] cache) {
        return Util.map(x -> x.transform(f), new PREDICATE[cache.length], cache);
    }

    static <X> PREDICATE<X> compileAnd(PREDICATE<X>[] p) {
        switch (p.length) {
            case 0: return null;
            case 1: return p[0];
            default:
                FasterList<PREDICATE<X>> pp = new FasterList<>(p);
                pp.removeIf(x -> !x.remainInAND(p));
                return AND.the(pp);
        }
    }

    @Nullable
    static <X> PREDICATE<X> compileAnd(Collection<PREDICATE<X>> cond, @Nullable PREDICATE<X> conseq) {
        return compileAnd(
                    Iterables.toArray(
                            (conseq != null ? Iterables.concat(cond, List.of(conseq)) : cond),
                            PREDICATE.class)
            );
    }

    default PREDICATE<X> transform(Function<PREDICATE<X>, PREDICATE<X>> f) {
        return f != null ? f.apply(this) : this;
    }

    /**
     * a relative global estimate (against its possible sibling PrediTerm's)
     * of the average computational cost of running the test method
     * warning: these need to return constant values for sort consistency
     */
    default float cost() {
        throw new TODO();
    }

    /** optimization subsumption: determines whether this predicate
     * should remain when appearing in an AND condition of the
     * specified predicates.  one of the 'p' in the array will be this instance
     */
    default boolean remainInAND(PREDICATE[] p) {
        return true;
    }









}