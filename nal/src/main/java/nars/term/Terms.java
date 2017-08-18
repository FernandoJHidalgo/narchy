package nars.term;

import jcog.Texts;
import jcog.Util;
import jcog.data.sorted.SortedList;
import jcog.list.FasterList;
import nars.Op;
import nars.index.term.TermIndex;
import nars.term.atom.Atom;
import nars.term.atom.Bool;
import nars.term.container.TermContainer;
import nars.term.transform.Retemporalize;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;
import org.eclipse.collections.impl.bag.mutable.HashBag;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import static nars.Op.*;
import static nars.time.Tense.DTERNAL;

/**
 * Static utility class for static methods related to Terms
 * <p>
 * Also serves as stateless/memory-less transient static (on-heap) TermIndex
 *
 * @author me
 */
public enum Terms {
    ;

    /**
     * TODO decide on some reasonable coding scheme for bundling these numeric values
     * into 32-bit or 64-bit fields/arrays
     */
    public static int hashVar(@NotNull Op type, int id) {
        return (type.id << 16) | id;
    }

    /**
     * computes the content hash while accumulating subterm metadata summary fields into int[] meta
     */
    public static int hashSubterms(@NotNull Term[] term, @NotNull int[] meta) {

        /*
        int result = 1;
        for (Object element : a)
            result = 31 * result + (element == null ? 0 : element.hashCode());
        return result;
         */
        int result = 1;
        for (int i = 0; i < term.length; i++) {
            Term t = term[i];
            //result = 31 /*Util.PRIME1 */ * result + t.init(meta);
            t.init(meta);
            result = Util.hashCombine(t.hashCode(), result);
        }
        return result;
    }

    /**
     * should be consistent with the other hash method(s)
     */
    public static int hashSubterms(@NotNull TermContainer container) {
        int h = 1;
        int s = container.size();
        for (int i = 0; i < s; i++) {
            h = container.sub(i).hashCode() + h * 31 /*Util.PRIME1 */;
        }
        return h;
    }

//    @Deprecated
//    public static boolean equalSubTermsInRespectToImageAndProduct(@Nullable Term a, @Nullable Term b) {
//
//        /*if (a == null || b == null) {
//            return false;
//        } else {*/
//        Op o = a.op();
//        boolean equalOps = (o == b.op());
//
//        if (equalOps) {
//
//            switch (o) {
//                case INH:
//                    return equalSubjectPredicateInRespectToImageAndProduct((Compound) a, (Compound) b);
//
//                case SIM:
//                    //only half seems necessary:
//                    //boolean y = equalSubjectPredicateInRespectToImageAndProduct((Compound) b, (Compound) a);
//                    return equalSubjectPredicateInRespectToImageAndProduct((Compound) a, (Compound) b);
//
//                default:
//                    if (Terms.equalAtemporally(a, b))
//                        return false;
//                    break;
//            }
//        }
//
//
//        if ((a instanceof Compound) && (b instanceof Compound)) {
//            //both are compounds
//
//
//            Compound A = ((Compound) a);
//            Compound B = ((Compound) b);
//            int aLen = A.size();
//            if (aLen != B.size()) {
//                return false;
//            } else {
//
//                //match all subterms
//                for (int i = 0; i < aLen; i++) {
//                    if (!equalSubTermsInRespectToImageAndProduct(A.get(i), B.get(i)))
//                        return false;
//                }
//                return true;
//            }
//        }
//
//        return false;
//
//
//    }
//
//
//    public static boolean equalSubjectPredicateInRespectToImageAndProduct(@NotNull Compound A, @NotNull Compound B) {
//
//        if (A.equals(B)) {
//            return true;
//        }
//
//
//        if (/*!hasAny(as, Op.PRODUCT) || */!A.hasAny(Op.ImageBits))
//            return false;
//
//        if (/*!hasAny(bs, Op.PRODUCT) || */!B.hasAny(Op.ImageBits))
//            return false;
//
////        if (!A.hasAny(Op.PRODUCT) || !B.hasAny(Op.PRODUCT) || !A.hasAny(Op.ImageBits) || !B.hasAny(Op.ImageBits)) {
////            //product and one of either image types
////            return false; //the remaining comparisons are unnecessary
////        }
//
//        Term subjA = subj(A);
//        Term predA = pred(A);
//        Term subjB = subj(B);
//        Term predB = pred(B);
//
//        Term ta = null, tb = null; //the compound term to put itself in the comparison set
//        Term sa = null, sb = null; //the compound term to put its components in the comparison set
//
//        Op sao = subjA.op();
//        Op sbo = subjB.op();
//        Op pao = predA.op();
//        Op pbo = predB.op();
//
//
//        if ((sao == PROD) && (pbo == IMGe)) {
//            ta = predA;
//            sa = subjA;
//            tb = subjB;
//            sb = predB;
//        }
//
//        if ((sbo == PROD) && (pao == IMGe)) {
//            ta = subjA;
//            sa = predA;
//            tb = predB;
//            sb = subjB;
//        }
//
//        if ((pao == IMGe) && (pbo == IMGe)) {
//            ta = subjA;
//            sa = predA;
//            tb = subjB;
//            sb = predB;
//        }
//
//        if ((sao == IMGi) && (sbo == IMGi)) {
//            ta = predA;
//            sa = subjA;
//            tb = predB;
//            sb = subjB;
//        }
//
//        if ((pao == PROD) && (sbo == IMGi)) {
//            ta = subjA;
//            sa = predA;
//            tb = predB;
//            sb = subjB;
//        }
//
//        if ((pbo == PROD) && (sao == IMGi)) {
//            ta = predA;
//            sa = subjA;
//            tb = subjB;
//            sb = predB;
//        }
//
//        if (ta != null) {
//            //original code did not check relation index equality
//            //https://code.google.com/p/open-nars/source/browse/trunk/nars_core_java/nars/language/CompoundTerm.java
//            //if (requireEqualImageRelation) {
//            //if (sa.op().isImage() && sb.op().isImage()) {
//            Compound csa = (Compound) sa;
//            Compound csb = (Compound) sb;
//
//            return csa.dt() == csb.dt() && containsAll(csa, ta, csb, tb);
//        } else {
//            return false;
//        }
//
//    }
//
//    private static boolean containsAll(@NotNull TermContainer sat, Term ta, @NotNull TermContainer sbt, Term tb) {
//        //set for fast containment check
//        Set<Term> componentsA = sat.toSet();
//        componentsA.add(ta);
//
//        //test A contains B
//        if (!componentsA.contains(tb))
//            return false;
//
//        Term[] sbtt = sbt.toArray();
//        for (Term x : sbtt) {
//            if (!componentsA.contains(x))
//                return false;
//        }
//
//        return true;
//    }


    @NotNull
    public static Term[] reverse(@NotNull Term[] arg) {
        int l = arg.length;
        Term[] r = new Term[l];
        for (int i = 0; i < l; i++) {
            r[i] = arg[l - i - 1];
        }
        return r;
    }


    /**
     * warning may rearrange items in the input
     */
    public static Term[] sorted(@NotNull Term... arg) {
        int len = arg.length;
        switch (len) {

            case 0:
                return Term.EmptyArray;

            case 1:
                return arg; //new Term[] { arg[0] };
            case 2:
                Term a = arg[0];
                Term b = arg[1];
                int c = a.compareTo(b);


                if (c < 0) return arg; //same as input //new Term[]{a, b};
                else if (c > 0) return new Term[]{b, a};
                else /*if (c == 0)*/ return new Term[]{a}; //equal

                //TODO fast sorted array for arg.length == 3 ?

            default:
                SortedList<Term> x = new SortedList<>(arg, new Term[arg.length]);
                return x.toArrayRecycled(Term[]::new);

            //return sortUniquely(arg); //<- may also work but seems slower

        }
    }

    public static void printRecursive(@NotNull PrintStream out, @NotNull Term x) {
        printRecursive(out, x, 0);
    }

    static void printRecursive(@NotNull PrintStream out, @NotNull Term x, int level) {
        //indent
        for (int i = 0; i < level; i++)
            out.print("  ");

        out.print(x);
        out.print(" (");
        out.print(x.op() + "[" + x.getClass().getSimpleName() + "] ");
        out.print("c" + x.complexity() + ",v" + x.volume() + ",dt=" + x.dt() + ",dtRange=" + x.dtRange() + " ");
        out.print(Integer.toBinaryString(x.structure()) + ')');
        out.println();


        for (Term z : x.subterms())
            printRecursive(out, z, level + 1);

    }

    /**
     * for printing complex terms as a recursive tree
     */
    public static void printRecursive(Term x, @NotNull Consumer<String> c) {
        printRecursive(x, 0, c);
    }

    public static void printRecursive(Term x, int level, @NotNull Consumer<String> c) {
        //indent
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < level; i++)
            line.append("  ");

        line.append(x);


        for (Term z : x.subterms())
            printRecursive(z, level + 1, c);


        c.accept(line.toString());
    }


    public static int maxLevel(@NotNull Term term) {
        int[] max = {0};
        term.recurseTerms((t) -> {
            int m = t.op().minLevel;
            if (m > max[0])
                max[0] = m;
        });
        return max[0];
    }

    @Nullable
    public static Term[] concat(@Nullable Term[] a, @NotNull Term... b) {

        if (a == null) {
            return null;
        }

        if (a.length == 0) return b;
        if (b.length == 0) return a;

        int L = a.length + b.length;

        //TODO apply preventUnnecessaryDeepCopy to more cases

        Term[] arr = new Term[L];

        int l = a.length;
        System.arraycopy(a, 0, arr, 0, l);
        System.arraycopy(b, 0, arr, l, b.length);

        return arr;
    }


    /**
     * returns lev distance divided by max(a.length(), b.length()
     */
    public static float levenshteinDistancePercent(@NotNull CharSequence a, @NotNull CharSequence b) {
        int len = Math.max(a.length(), b.length());
        if (len == 0) return 0f;
        return Texts.levenshteinDistance(a, b) / ((float) len);
    }


    @Nullable
    public static Atom atomOr(@Nullable Term possiblyCompound, Atom other) {
        return (possiblyCompound instanceof Atom) ? (Atom) possiblyCompound : other;
    }

    @Nullable
    public static Atom atomOrNull(@Nullable Term t) {
        return atomOr(t, null);
    }

    /**
     * dangerous because some operations involving concepts can naturally reduce to atoms, and using this interprets them as non-existent
     */
    @Nullable
    @Deprecated
    public static Compound compoundOrNull(@Nullable Term t) {
        if (t instanceof Compound) return (Compound) t;
        else
            return null;
    }

    @Nullable
    public static <T extends Term> T normalizedOrNull(@Nullable Term t) {
        return (T) normalizedOrNull(t, TermIndex.retemporalizeXTERNALToDTERNAL);
    }

    @Nullable
    public static Term normalizedOrNull(@Nullable Term t, Retemporalize r) {

        if (t.isTemporal()) {
            //the compound indicated a potential dt, but the premise was actually atemporal;
            // this indicates a temporal placeholder (XTERNAL) in the rules which needs to be set to DTERNAL
            return TermIndex.retemporalize(t, r); //retemporalize does normalize at the end
        } else {
            return t.normalize();
        }
    }


    /**
     * detects a negated conjunction of negated subterms:
     * (--, (&&, --A, --B, .., --Z) )
     */
    public static boolean isDisjunction(@NotNull Compound c) {
        if (c.dt() == DTERNAL && c.op() == NEG && c.subIs(0, CONJ)) {
            return allNegated(c.sub(0).subterms());
        }
        return false;
    }


    public static boolean allNegated(@NotNull TermContainer subterms) {
        return subterms.hasAny(Op.NEG) && subterms.AND((Term t) -> t.op() == NEG);
    }


//    @Nullable
//    public static Term atemporalize(@NotNull Term c) {
//        if (c instanceof Compound)
//            return atemporalize((Compound)c);
//        return c;
//    }


    /**
     * returns the most optimal subterm that can be replaced with a variable, or null if one does not meet the criteria
     */
    @Nullable
    public static Term[] substMaximal(@NotNull Compound c, @NotNull Predicate<Term> include, int minCount, int minScore) {
        HashBag<Term> uniques = subtermScore(c,
                t -> include.test(t) ? t.volume() : 0 //sum by complexity if passes include filter
        );

        int s = uniques.size();
        if (s > 0) {
            MutableList<ObjectIntPair<Term>> u = uniques.topOccurrences(s);
            for (ObjectIntPair<Term> p : u) {
                int score = p.getTwo();
                if (score >= minScore) {
                    Term subterm = p.getOne();
                    int count = score / subterm.complexity(); //should be a whole number according to the above scoring policy
                    if (count >= minCount) {
                        return new Term[]{subterm};
                    }
                }
            }

        }

        return null;
    }

    /**
     * returns the most optimal subterm that can be replaced with a variable, or null if one does not meet the criteria
     */
    @Nullable
    public static Term[] substRoulette(@NotNull Compound c, @NotNull Predicate<Term> include, int minCount, Random rng) {
        HashBag<Term> uniques = subtermScore(c,
                t -> include.test(t) ? 1 : 0 //sum by complexity if passes include filter
        );

        int s = uniques.size();
        if (s > 0) {
            ObjectIntPair<Term>[] oi = new ObjectIntPair[s];
            final int[] j = {0};
            final int[] sum = {0};
            uniques.forEachWithOccurrences((Term t, int count) -> {
                if (count >= minCount) {
                    int score = count * t.volume();
                    oi[j[0]++] = PrimitiveTuples.pair(t, score);
                    sum[0] += score;
                }
            });

            int available = j[0];
            if (available == 1) {
                return new Term[]{oi[0].getOne()};
            } else if (available > 1) {
                int selected = Util.decideRoulette(j[0], (i) -> oi[i].getTwo(), sum[0], rng);
                return new Term[]{oi[selected].getOne()};
            }
        }

        return null;
    }

    /**
     * returns the most optimal subterm that can be replaced with a variable, or null if one does not meet the criteria
     */
    @Nullable
    public static FasterList<Term> substAllRepeats(@NotNull Term c, @NotNull ToIntFunction<Term> score, int minCount) {
        FasterList<Term> oi = getUniqueRepeats(c, score, minCount);
        if (oi == null || oi.isEmpty())
            return null;

        if (oi.size() > 1) {
            //keep only the unique subterms which are not contained by other terms in the list
            //renive terms which are contained by other terms in the list

            //oi.sort((a, b) -> Integer.compare(a.volume(), b.volume())); //sorted by volume

            oi.removeIf(b ->
                    oi.anySatisfy(
                            a ->
                                    (a != b) &&
                                            a.containsRecursively(b)
                    )
            );
        }

        return oi;
    }

    /**
     * returns a list but its contents will be unique
     */
    @Nullable
    static FasterList<Term> getUniqueRepeats(@NotNull Term c, @NotNull ToIntFunction<Term> score, int minCount) {
        HashBag<Term> uniques = Terms.subtermScore(c, score);
        int us = uniques.size();
        if (us == 0)
            return null;

        FasterList<Term> oi = new FasterList(0);

        uniques.forEachWithOccurrences((Term t, int count) -> {
            if (count >= minCount)
                oi.add(t);
        });

        return oi;
    }


    /**
     * counts the repetition occurrence count of each subterm within a compound
     */
    @NotNull
    public static HashBag<Term> subtermScore(@NotNull Term c, @NotNull ToIntFunction<Term> score) {
        HashBag<Term> uniques = new HashBag<>(c.volume() / 2);

        c.recurseTerms((Term subterm) -> {
            int s = score.applyAsInt(subterm);
            if (s > 0)
                uniques.addOccurrences(subterm, s);
        });

        return uniques;
    }

    public static Term[] sorted(Collection<Term> s) {
        if (s instanceof SortedSet)
            return sorted((Set) s);
        else {
            return sorted(s.toArray(new Term[s.size()]));
        }
    }

    /**
     * a Set is already duplicate free, so just sort it
     */
    public static Term[] sorted(Set<Term> s) {

        //1. deduplicated
        Term[] x = s.toArray(new Term[s.size()]);

//        //filter any True, False, or Null's - return as an innocuous one element poison-pill array
//        for (Term t : x) {
//            if (t instanceof SpecialAtom)
//                return new Term[] { t };
//        }

        //2. sorted
        if ((x.length >= 2) && (!(s instanceof SortedSet)))
            Arrays.sort(x);

        return x;
    }

    @Nullable
    public static Term subj(@NotNull Termed statement) {
        return statement.term().sub(0);
    }

    @Nullable
    public static Term pred(@NotNull Termed statement) {
        return statement.term().sub(1);
    }

    @NotNull
    public static Term[] neg(@NotNull Term... modified) {
        int l = modified.length;
        Term[] u = new Term[l];
        for (int i = 0; i < l; i++) {
            u[i] = NEG.the(modified[i]);
        }
        return u;
    }

    static final Comparator<Term> volumeComparator = Comparator.comparingInt(Term::volume);

    /**
     * for commutive conjunction
     *
     * @param dt will be either 0 or DTERNAL (commutive relation)
     */
    public static boolean flatten(@NotNull Op op, @NotNull Term[] u, int dt, ObjectByteHashMap<Term> s) {

        //sort by volume, decreasing first. necessary for proper subsumption of events into sibling sequence compounds that may contain them
        //may also provide some performance benefit for accelerated early termination in case of invalid construction attempts (ex: co-negation)

        u = u.clone(); //dont modify input, it will confuse callee's

        Arrays.sort(u, volumeComparator);

        for (Term x : u) {
            if (!flatten(op, dt, x, s))
                return false;
        }
        return true;
    }

    public static boolean flatten(@NotNull Op op, @NotNull TermContainer u, int dt, ObjectByteHashMap<Term> s) {
        int l = u.size();
        for (int i = 0; i < l; i++) {
            if (!flatten(op, dt, u.sub(i), s))
                return false;
        }
        return true;
    }

    public static boolean flattenMatchDT(int candidate, int target) {
        if (candidate == target) return true;
        return target == 0 && candidate == DTERNAL;
    }

    public static boolean flatten(@NotNull Op op, int dt, Term x, ObjectByteHashMap<Term> s) {
        if (x instanceof Bool) {

            if (x == True)
                return true; //silently ignore

            if (x == False)
                return s.getIfAbsentPut(True, (byte) -1) == (byte) +1; //attempt to mark a False, for the chance it may become inverted and disappear rather than aborting any further construction here

            return false; //x must be Null
        }

        Op xo = x.op();

        if ((xo == op) && (flattenMatchDT(x.dt(), dt))) {
            return flatten(op, x.subterms(), dt, s);

        } else {
            if (!testCoNegate(x, s))
                return false;

            if (x.op() == CONJ) {
                int xdt = x.dt();
                if (xdt != 0) {
                    //test for x's early subterm (left aligned t=0) in case it matches with a term in the superconjunction that x is a subterm of
                    Term early = x.sub(xdt > 0 ? 0 : 1);

                    //check if the early event is present, and if it is (with correct polarity) then include x without the early event
                    Term earlyUnneg = early.unneg();
                    byte earlyExisting = s.getIfAbsent(earlyUnneg, (byte) 0);
                    if (earlyExisting != 0) {
                        if (early.op() == NEG ^ (earlyExisting == -1))
                            return false; //wrong polarity
                        else {
                            //subsume the existing term by removing it from the list, since it is part of 'x' which has been added in entirity already
                            s.remove(earlyUnneg);
                        }

                    }

                }
            }

            return true;
        }

    }

    static boolean testCoNegate(Term x, ObjectByteHashMap<Term> s) {

        assert (x != Null);

        byte polarity;
        Term t;
        if (x.op() == NEG || x == False) {
            polarity = -1;
            t = x.unneg();
        } else {
            polarity = +1;
            t = x;
        }
        return s.getIfAbsentPut(t, polarity) == polarity;
    }

    @NotNull
    public static Term intersect(@NotNull Op o, @NotNull TermContainer a, @NotNull TermContainer b) {
        if (a.equals(b) && a instanceof Term)
            return (Term) a;

        Term[] c = TermContainer.intersect(a, b);
        return (c == null || c.length == 0) ? Null : (Compound) (o.the(c));
    }

    @NotNull
    public static Term union(@NotNull Op o, @NotNull TermContainer a, @NotNull TermContainer b) {
        boolean bothTerms = a instanceof Term && b instanceof Term;
        if (bothTerms && a.equals(b))
            return (Term) a;

        TreeSet<Term> t = new TreeSet<>();
        a.copyInto(t);
        b.copyInto(t);
        if (bothTerms) {
            int as = a.size();
            int bs = b.size();
            int maxSize = Math.max(as, bs);
            if (t.size() == maxSize) {
                //the smaller is contained by the larger other
                //so return an input value rather than constructing a duplicate
                return (Term) (as > bs ? a : b);
            }
        }
        return o.the(DTERNAL, t);
    }

    public static boolean reflex(@NotNull Term sub0, @NotNull Term sub1) {
        sub0 = sub0.unneg();
        sub1 = sub1.unneg();
        return sub0.equals(sub1) ||
                sub0.containsRecursively(sub1) ||
                sub1.containsRecursively(sub0);
    }

    interface SubtermScorer {
        int score(Compound superterm, Term subterm);
    }

}


//    private static boolean equalsAnonymous(@NotNull TermContainer a, @NotNull TermContainer b) {
//        if (a.volume() == b.volume()) {
//            int n = a.size();
//            if (n == b.size()) {
//                for (int i = 0; i < n; i++) {
//                    Term as = a.term(i);
//                    Term bs = b.term(i);
//                    //        if (as == bs) {
////            return true;
////        } else if (as instanceof Compound && bs instanceof Compound) {
////            return equalsAnonymous((Compound) as, (Compound) bs);
////        } else {
////            return as.equals(bs);
////        }
//                    if (!Terms.equalAtemporally(as, bs))
//                        return false;
//                }
//                return true;
//            }
//        }
//        return false;
//    }
