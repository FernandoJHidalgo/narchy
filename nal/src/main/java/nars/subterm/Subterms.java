package nars.subterm;

import com.google.common.base.Joiner;
import com.google.common.io.ByteArrayDataOutput;
import jcog.TODO;
import jcog.Util;
import jcog.data.bit.MetalBitSet;
import jcog.list.FasterList;
import nars.$;
import nars.Op;
import nars.subterm.util.DisposableTermList;
import nars.subterm.util.TermList;
import nars.term.*;
import nars.unify.Unify;
import nars.unify.match.EllipsisMatch;
import nars.unify.mutate.CommutivePermutations;
import nars.util.term.transform.MapSubst;
import nars.util.term.transform.TermTransform;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.predicate.primitive.IntObjectPredicate;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.RoaringBitmap;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;


/**
 * Methods common to both Term and Subterms
 * T = subterm type
 */
public interface Subterms extends Termlike, Iterable<Term> {


    default Op op() {
        return null;
    }



    static int hash(List<Term> term) {
        int n = term.size();
        int h = 1;
        for (int i = 0; i < n; i++)
            h = Util.hashCombine(h, term.get(i));
        return h;
    }
    default boolean isSorted() {
        int s = subs();
        if (s < 2) return true;



        for (int i = 1; i < s; i++) {
            if (sub(i - 1).compareTo(sub(i)) >= 0)
                return false;
        }
        return true;
    }
    static int hash(Subterms container) {
        return container.intifyShallow(Util::hashCombine, 1);
    }


    /**
     * returns sorted ready for commutive; null if nothing in common
     */
    static @Nullable MutableSet<Term> intersect(/*@NotNull*/ Subterms a, /*@NotNull*/ Subterms b) {
        if ((a.structure() & b.structure()) > 0) {

            Set<Term> as = a.toSet();
            MutableSet<Term> ab = b.toSet(as::contains);
            if (ab != null)
                return ab;
        }
        return null;
    }

    /**
     * recursively
     */
    /*@NotNull*/
    static boolean hasCommonSubtermsRecursive(/*@NotNull*/ Term a, /*@NotNull*/ Term b, boolean excludeVariables) {

        Subterms aa = a.subterms();
        Subterms bb = b.subterms();

        int commonStructure = aa.structure() & bb.structure();
        if (excludeVariables)
            commonStructure = commonStructure & ~(Op.VariableBits);

        if (commonStructure == 0)
            return false;

        Set<Term> scratch = new UnifiedSet<>(4);
        aa.recurseTermsToSet(commonStructure, scratch, true);
        return bb.recurseTermsToSet(commonStructure, scratch, false);
    }

    /*@NotNull*/
    static boolean commonSubterms(/*@NotNull*/ Compound a, /*@NotNull*/ Compound b, boolean excludeVariables) {

        int commonStructure = a.structure() & b.structure();
        if (excludeVariables)
            commonStructure = commonStructure & ~(Op.VariableBits);

        if (commonStructure == 0)
            return false;

        Set<Term> scratch = new UnifiedSet(a.subs());
        a.termsToSet(commonStructure, scratch, true);
        return b.termsToSet(commonStructure, scratch, false);

    }


    static String toString(/*@NotNull*/ Iterable<? extends Term> subterms) {
        return '(' + Joiner.on(',').join(subterms) + ')';
    }

    static int compare(/*@NotNull*/ Subterms a, /*@NotNull*/ Subterms b) {

        if (a.equals(b)) return 0;

        int s;
        int diff;
        if ((diff = Integer.compare((s = a.subs()), b.subs())) != 0)
            return diff;


        if ((diff = Integer.compare(a.structure(), b.structure())) != 0)
            return diff;


        Term inequalVariableX = null, inequalVariableY = null;

        for (int i = 0; i < s; i++) {
            Term x = a.sub(i);
            Term y = b.sub(i);
            if (x instanceof Variable && y instanceof Variable) {
                if (inequalVariableX == null && !x.equals(y)) {

                    inequalVariableX = x;
                    inequalVariableY = y;
                }
            } else {
                int d = x.compareTo(y);
                if (d != 0) {
                    return d;
                }
            }
        }


        if (inequalVariableX != null) {
            return inequalVariableX.compareTo(inequalVariableY);
        } else {
            return 0;
        }
    }

    /**
     * a and b must be instances of input, and output must be of size input.length-2
     */
    /*@NotNull*/
    static Term[] except(/*@NotNull*/ Subterms input, Term a, Term b, /*@NotNull*/ Term[] output) {


        int j = 0;
        int l = input.subs();
        for (int i = 0; i < l; i++) {
            Term x = input.sub(i);
            if ((x != a) && (x != b))
                output[j++] = x;
        }

        if (j != output.length)
            throw new RuntimeException("permute underflow");

        return output;
    }

    /*@NotNull*/
    @Override
    default Iterator<Term> iterator() {
        throw new TODO();
    }



    default boolean subEquals(int i, /*@NotNull*/ Term x) {
        return subs() > i && sub(i).equals(x);
    }


    default /*@NotNull*/ SortedSet<Term> toSetSorted() {
        TreeSet u = new TreeSet();
        forEach(u::add);
        return u;
    }

    default /*@NotNull*/ SortedSet<Term> toSetSortedExcept(Predicate<Term> t) {
        TreeSet u = new TreeSet();
        forEach(x -> {
            if (!t.test(x)) u.add(x);
        });
        return u;
    }

    default /*@NotNull*/ FasterList<Term> toList() {
        FasterList<Term> u = new FasterList(subs());
        forEach(u::add);
        return u;
    }

    /**
     * @return a Mutable Set, unless empty
     */
    default /*@NotNull*/ MutableSet<Term> toSet() {
        int s = subs();
        UnifiedSet u = new UnifiedSet(s * 2);
        if (s > 0) {
            forEach(u::add);

        }
        return u;


    }

    default @Nullable MutableSet<Term> toSet(Predicate<Term> ifTrue) {
        int s = subs();
        if (s > 0) {
            UnifiedSet<Term> u = null;
            for (int i = 0; i < s; i++) {
                /*@NotNull*/
                Term x = sub(i);
                if (ifTrue.test(x)) {
                    if (u == null)
                        u = new UnifiedSet<>((s - i) * 2);
                    u.add(x);
                }
            }
            if (u != null) {

                return u;
            }
        }

        return null;


    }

    /**
     * by default this does not need to do anything
     * but implementations can cache the normalization
     * in a boolean because it only needs done once.
     */
    default void setNormalized() {

    }

    /**
     * assume its normalized if no variables are present
     */
    default boolean isNormalized() {
        return !hasVars();
    }

    /**
     * gets the set of unique recursively contained terms of a specific type
     * TODO generalize to a provided lambda predicate selector
     */
    /*@NotNull*/
    default Set<Term> recurseTermsToSet(Op onlyType) {
        if (onlyType != null && !hasAny(onlyType))
            return Sets.mutable.empty();

        Set<Term> t = new HashSet(volume());


        recurseTerms(
                tt -> tt.hasAny(onlyType),
                tt -> {
                    if (tt.op() == onlyType)
                        t.add(tt);
                    return true;
                }, null);
        return t;
    }


    /**
     * returns whether the set operation caused a change or not
     */
    /*@NotNull*/
    default boolean termsToSet(int inStructure, /*@NotNull*/ Collection<Term> t, boolean addOrRemoved) {
        boolean r = false;

        int l = subs();
        for (int i = 0; i < l; i++) {
            /*@NotNull*/
            Term s = sub(i);
            if (inStructure == -1 || ((s.structure() & inStructure) > 0)) {
                r |= (addOrRemoved) ? t.add(s) : t.remove(s);
                if (!addOrRemoved && r)
                    return true;
            }
        }
        return r;
    }

    /*@NotNull*/
    default boolean recurseTermsToSet(int inStructure, /*@NotNull*/ Collection<Term> t, boolean addOrRemoved) {
        final boolean[] r = {false};
        Predicate<Term> selector = s -> {

            if (!addOrRemoved && r[0])
                return false;

            if (inStructure == -1 || ((s.structure() & inStructure) > 0)) {
                r[0] |= (addOrRemoved) ? t.add(s) : t.remove(s);
            }

            return true;
        };


        recurseTerms(
                (inStructure != -1) ?
                        p -> p.hasAny(inStructure)
                        :
                        any -> true,
                selector, null);


        return r[0];
    }

    @Override
    default boolean containsRecursively(/*@NotNull*/ Term y, boolean root, Predicate<Term> subTermOf) {

        if (!impossibleSubTerm(y)) {
            int s = subs();
            for (int i = 0; i < s; i++) {
                Term x = sub(i);
                if (x == y || (root ? x.equalsRoot(y) : x.equals(y)) || x.containsRecursively(y, root, subTermOf))
                    return true;
            }
        }
        return false;
    }

    default boolean equalTerms(/*@NotNull*/ Subterms c) {
        int s = subs();
        if (s != c.subs())
            return false;
        for (int i = 0; i < s; i++) {
            if (!sub(i).equals(c.sub(i)))
                return false;
        }
        return true;
    }

    default boolean equalTerms(/*@NotNull*/ Term[] c) {
        int s = subs();
        if (s != c.length)
            return false;
        for (int i = 0; i < s; i++) {
            if (!sub(i).equals(c[i]))
                return false;
        }
        return true;
    }

    default void copyInto(Collection<Term> target) {
        forEach(target::add);
    }


//
//    /**
//     * if subterms are already sorted, returns arrayShared().
//     * otherwise a sorted clone is returned.
//     */
//    default Term[] arraySharedSorted(boolean dedup) {
//        Term[] aa = arrayShared();
//        if (dedup) {
//            Term[] ss = Terms.sorted();
//            if (ss == aa)
//                return aa;
//            else
//                return ss;
//        } else {
//            if (Util.isSorted(aa))
//                return aa;
//            else {
//                Term[] ss = aa.clone();
//                Arrays.sort(ss);
//                return ss;
//            }
//        }
//    }




    /*@NotNull*/
    default Term[] terms(/*@NotNull*/ IntObjectPredicate<Term> filter) {
        List<Term> l = $.newArrayList(subs());
        int s = subs();
        int added = 0;
        for (int i = 0; i < s; i++) {
            Term t = sub(i);
            if (filter.accept(i, t)) {
                l.add(t);
                added++;
            }
        }
        return added == 0 ? Op.EmptyTermArray : l.toArray(new Term[added]);
    }


    default void forEach(Consumer<? super Term> action, int start, int stop) {
        for (int i = start; i < stop; i++)
            action.accept(sub(i));
    }

    @Override
    default void forEach(Consumer<? super Term> action) {
        forEach(action, 0, subs());
    }

    /**
     * follows normal indexOf() semantics; -1 if not found
     */
    default int indexOf(/*@NotNull*/ Term t) {
        if (!impossibleSubTerm(t)) {
            int s = subs();
            for (int i = 0; i < s; i++) {
                if (t.equals(sub(i)))
                    return i;
            }
        }
        return -1;
    }


    /**
     * of all the matches to the predicate, chooses one at random and returns its index
     */
    default int indexOf(Predicate<Term> t, Random r) {
        IntArrayList a = indicesOf(t);
        return (a == null) ? -1 :
                a.get(a.size() == 1 ? 0
                        : r.nextInt(a.size()));

    }

    @Nullable
    default IntArrayList indicesOf(Predicate<Term> t) {
        IntArrayList a = null;
        int s = subs();
        for (int i = 0; i < s; i++) {
            if (t.test(sub(i))) {
                if (a == null)
                    a = new IntArrayList(1);
                a.add(i);
            }
        }
        return a;
    }

    /**
     * allows the subterms to hold a different hashcode than hashCode when comparing subterms
     */
    default int hashCodeSubterms() {
        return hashCode();
    }




    default boolean unifyLinearSimple(Subterms Y, /*@NotNull*/ Unify u) {


        int s = subs();
        for (int i = 0; i < s; i++) {
            if (!sub(i).unify(Y.sub(i), u))
                return false;
        }
        return true;

    }

    /**
     * const/variable phase version
     */
    default boolean unifyLinear(Subterms Y, /*@NotNull*/ Unify u) {
        int s = subs();


        Term[] deferredPairs = null;
        int dynPairs = 0;
        for (int i = 0; i < s; i++) {
            Term xi = sub(i);
            Term yi = Y.sub(i);

            if (xi == yi)
                continue;

            boolean now = (i == s - 1) || (u.constant(xi) && u.constant(yi));

            if (now) {

                if (!xi.unify(yi, u))
                    return false;
            } else {
                if (deferredPairs == null)
                    deferredPairs = new Term[(s - i) * 2];

                deferredPairs[dynPairs++] = yi;
                deferredPairs[dynPairs++] = xi;
            }
        }


        if (deferredPairs != null) {

            do {
                if (!deferredPairs[--dynPairs].unify(deferredPairs[--dynPairs], u))
                    return false;
            } while (dynPairs > 0);
        }

        return true;
    }

    /**
     * the term that y is from may not be commutive in some temporal cases like
     * (x &&+1 x). special care is needed to preserve the appearance of
     * both subterms unlike how creating a Set would remove the duplicate.
     */
    default boolean unifyCommute(Subterms y, boolean yCommutative, /*@NotNull*/ Unify u) {

        int xv = u.vars(this);
        int yv = u.vars(y);
        if (yCommutative) {
            if (xv == 0 && yv == 0) {
                if (u.constant(this) && u.constant(y))
                    return y.equals(this);
            }
        }

        int s = subs();
        final int originalS = s;
        if ((xv == 0 && yv == s) || (xv == s && yv == 0)) {


            u.termutes.add(new CommutivePermutations(this, y));
            return true;
        }


        final SortedSet<Term> xx = toSetSorted();

        if (!yCommutative) {

            assert (s == 2);
            Term y0 = y.sub(0);
            boolean y0inX = xx.contains(y0);
            Term y1 = y.sub(1);
            boolean y1inX = xx.contains(y1);

            boolean y0c = u.constant(y0);
            boolean y1c = u.constant(y1);

            if (y0inX && y1inX && (y0c && y1c)) {
                return true;
            }

            if ((!y0inX && !y1inX) || (!y0c && !y1c)) {

                if (u.constant(this) && (y0c && y1c))
                    return false;


                u.termutes.add(new CommutivePermutations(this, y));
                return true;
            } else {
                if (y0c && y0inX) {
                    xx.remove(y0);
                    //re: java.util.NoSuchElementException
                    //if xx.isempty then return y0.equals(y1) //removed both so must match unified both
                    return xx.first().unify(y1, u);
                }
                if (y1c && y1inX) {
                    xx.remove(y1);
                    return xx.first().unify(y0, u);
                }

                if (u.constant(sub(0)) && u.constant(sub(1)))
                    return false;
                else {

                    u.termutes.add(new CommutivePermutations(this, y));
                    return true;
                }
            }

        } else {

            SortedSet<Term> yy = y.toSetSorted();
            MutableSet<Term> xy = Sets.intersect(xx, (Set) yy);
            if (!xy.isEmpty()) {


                xy.removeIf(z -> !u.constant(z));
                if (!xy.isEmpty()) {

                    xx.removeAll(xy);
                    yy.removeAll(xy);
                    s = xx.size();
                    if (s != yy.size()) {
                        throw new RuntimeException("set size should have remained equal");
                    }
                }
            }


            if (s == 1) {


                return xx.first().unify(yy.first(), u);

            } else if (originalS == s) {

                u.termutes.add(new CommutivePermutations(this, y));
                return true;
            } else {

                u.termutes.add(new CommutivePermutations(
                        $.vFast(Terms.sorted(xx.toArray(Op.EmptyTermArray))),
                        $.vFast(Terms.sorted(yy.toArray(Op.EmptyTermArray)))
                ));
                return true;

            }
        }


    }


    default Term[] termsExcept(RoaringBitmap toRemove) {
        int numRemoved = toRemove.getCardinality();
        int size = subs();
        int newSize = size - numRemoved;
        Term[] t = new Term[newSize];
        int j = 0;
        for (int i = 0; i < size; i++) {
            if (!toRemove.contains(i))
                t[j++] = sub(i);
        }
        return (t.length == 0) ? Op.EmptyTermArray : t;
    }

    default Term[] termsExcept(MetalBitSet toRemove) {
        int numRemoved = toRemove.cardinality();
        int size = subs();
        int newSize = size - numRemoved;
        Term[] t = new Term[newSize];
        int j = 0;
        for (int i = 0; i < size; i++) {
            if (!toRemove.get(i))
                t[j++] = sub(i);
        }
        return t.length == 0 ? Op.EmptyTermArray : t;
    }

    default Term[] termsExcept(Collection<Term> except) {
        FasterList<Term> fxs = new FasterList<>(subs());
        forEach(t -> {
            if (!except.contains(t))
                fxs.add(t);
        });
        return fxs.toArrayRecycled(Term[]::new);
    }


    /**
     * match a range of subterms of Y.
     * WARNING: provides a shared (non-cloned) copy if the entire range is selected
     */
    /*@NotNull*/
    default Term[] toArraySubRange(int from, int to) {
        if (from == 0 && to == subs()) {
            return arrayShared();

        } else {

            int s = to - from;

            Term[] l = new Term[to - from];

            int y = from;
            for (int i = 0; i < s; i++) {
                l[i] = sub(y++);
            }

            return l;
        }
    }



    default boolean recurseTerms(Predicate<Term> aSuperCompoundMust, Predicate<Term> whileTrue, Term parent) {
        return AND(s -> whileTrue.test(s) && s.recurseTerms(aSuperCompoundMust, whileTrue, parent));
    }

    default Subterms reversed() {
        return subs() > 1 ? new ReverseSubterms(this) : this;
    }

    default Subterms sorted() {
        return isSorted() ? this : Op.terms.newSubterms(Terms.sorted(arrayShared()));
    }

    default Term[] termsExcept(int i) {
        return ArrayUtils.remove(arrayShared(), Term[]::new, i);
    }

    default int hashWith(Op op) {
        return Util.hashCombine(this.hashCodeSubterms(), op.id);
    }

    @Nullable
    default Term[] termsExcept(Term x) {
        int index = indexOf(x);
        return (index == -1) ? null : termsExcept(index);
    }

    default void append(ByteArrayDataOutput out) {
        out.writeByte(subs());
        forEach(t -> t.appendTo(out));
    }

    default Subterms replaceSubs(Term from, Term to) {
        if (!impossibleSubTerm(from)) {

            return transformSubs(new MapSubst.MapSubst1(from, to));
        } else {
            return this;
        }
    }

    /**
     * returns 'x' unchanged if no changes were applied,
     * returns 'y' if changes
     * returns null if untransformable
     */
    @Nullable
    default Subterms transformSubs(TermTransform f) {
        int s = subs();

        TermList y = null;

        for (int i = 0; i < s; i++) {

            Term xi = sub(i);

            Term yi = xi.transform(f);

            if (yi == null)
                return null;

            if (yi instanceof EllipsisMatch) {
                EllipsisMatch xe = (EllipsisMatch) yi;
                int xes = xe.subs();

                if (y == null) {
                    y = new DisposableTermList(s - 1 + xes /*estimate */, i);
                }

                if (xes > 0) {
                    for (int j = 0; j < xes; j++) {
                        @Nullable Term k = xe.sub(j).transform(f);
                        if (k == null) {
                            return null;
                        } else {
                            y.add(k);
                        }
                    }
                }

            } else {

                if (xi != yi /*&& (yi.getClass() != xi.getClass() || !xi.equals(yi))*/) {

//                    if (!(yi instanceof Functor) && xi.equals(yi))
//                        System.err.println(xi + " " + yi);

                    if (y == null) {
                        y = new DisposableTermList(s, i);
                    }
                }

                if (y != null)
                    y.add(yi);

            }

        }

        if (y != null) {


            int ys = y.size();
            for (int i = 0; i < ys; i++) {
                if (y.get(i) == null)
                    y.set(i, sub(i));
                else
                    break; //stop at first non-null subterm
            }
            return y;
        } else
            return this;
    }


}
