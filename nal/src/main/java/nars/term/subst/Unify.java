package nars.term.subst;

import jcog.Util;
import jcog.version.VersionMap;
import jcog.version.Versioned;
import jcog.version.Versioning;
import nars.Op;
import nars.Param;
import nars.derive.constraint.MatchConstraint;
import nars.derive.mutate.Termutator;
import nars.index.term.NewCompound;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.var.AbstractVariable;
import nars.term.var.CommonVariable;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

import static nars.Op.Null;


/* recurses a pair of compound term tree's subterms
across a hierarchy of sequential and permutative fanouts
where valid matches are discovered, backtracked,
and collected until power is depleted.



https://github.com/rupertlssmith/lojix/blob/master/lojix/prolog/src/main/com/thesett/aima/logic/fol/prolog/PrologUnifier.java
https://github.com/rupertlssmith/lojix/blob/master/lojix/wam_prolog/src/main/com/thesett/aima/logic/fol/wam/compiler/WAMInstruction.java
see this code for a clear explanation of what a prolog unifier does.
this code does some additional things but shares a general structure with the lojix code which i just found now
So it can be useful for a more easy to understand rewrite of this class TODO


*/
public abstract class Unify extends Versioning implements Subst {

    protected final static Logger logger = LoggerFactory.getLogger(Unify.class);

    public Random random;

    @Nullable
    public final Op type;

    @NotNull
    public final Set<Termutator> termutes = new LinkedHashSet();

//    @NotNull
//    public final TermIndex terms;

    @NotNull
    public final VersionMap<Term, Term> xy;

    /**
     * temporal tolerance; if -1, then it is not tested
     */
    public int dur = -1;

    /** whether the variable unification allows to happen in reverse (a variable in Y can unify a constant in X) */
    public boolean varSymmetric = true;

//    /**
//     * free variables remaining unassigned, for counting
//     */
//    protected final Versioned<Set<Term>> free;


    /**
     * @param type       if null, unifies any variable type.  if non-null, only unifies that type
     * @param random
     */
    protected Unify(@Nullable Op type, Random random, int stackMax, int initialTTL) {
        super(stackMax, initialTTL);

//        this.terms = terms;

        this.random = random;
        this.type = type;

        xy = new ConstrainedVersionMap(this, Param.UnificationVariableCapInitial);
        //this.free = new Versioned<>(this, 4); //task, belief, subIfUnifies + ?
        //this.freeCount = new Versioned<>(versioning, 8);

    }

    /**
     * spend an amount of TTL; returns whether it is still live
     */
    public final boolean use(int cost) {
        return ((ttl -= cost) > 0);
    }


    /**
     * called each time all variables are satisfied in a unique way
     *
     * @return whether to continue on any subsequent matches
     */
    public abstract void tryMatch();

    public final void tryMutate(Termutator[] chain, int next) {
        if (!use(Param.TTL_MUTATE))
            return;

        if (++next < chain.length) {
            chain[next].mutate(this, chain, next);
        } else {
            tryMatch(); //end of chain
        }
    }

    @Nullable
    @Override
    public Term xy(Term x0) {
        return xy.get(x0);

//        Term xy = x0, y = null;
//        while ((xy = this.xy.get(xy)) != null) { //completely dereference
//            y = xy;
//        }
//        return y;

//        //SAFE VERSION:
//        Term xy = x0, y0 = null, y = null;
//        while ((xy = this.xy.get(xy))!=null) { //completely dereference
//            if (y0!=null)
//                return y0;
//            y0 = y;
//            y = xy;
//        }
//        return y;

//        Term y0 = xy.get(x0);
//        if (y0 == null)
//            return null;
//        else {
//            Term y1 = xy.get(y0);
//            if (y1 == null)
//                return y0;
//            else
//                return y1;
//        }
    }


    /**
     * unifies the next component, which can either be at the start (true, false), middle (false, false), or end (false, true)
     * of a matching context
     * <p>
     * setting finish=false allows matching in pieces before finishing
     */
    public boolean unify(Term x, Term y, boolean finish) {

        //accumulate any new free variables in this next matched term
//        Set<Term> freeX = freeVariables(x);
////        if (null == free.set(freeX)) //plus and not equals because this may continue from another unification!!!!!
////            return false;
//        if (freeX.isEmpty())
//            return x.equals(y);

        //assert (unassigned.isEmpty() ) : "non-purposeful unification";
        //this.freeCount.add( newFree.size() );

        if (x.unify(y, this)) {
            if (finish) {
                tryMatches();
            }
            return true;
        }

        return false;
    }

//    /**
//     * computes a lazy set with the new free variables added by the incoming term, to continue
//     * from a previous partial unification if necessary.
//     */
//    Set<Term> freeVariables(@NotNull Term x) {
//        Set<Term> prevFree = free.get();
//        Set<Term> nextFree = x.varsUnique(type, prevFree != null ?  prevFree : Collections.emptySet());
//        return concat(prevFree, nextFree);
//    }


    void tryMatches() {
        int ts = termutes.size();
        if (ts > 0) {

            //TODO use Termutator[] not List
            Termutator[] t = termutes.toArray(new Termutator[ts]);

            termutes.clear();

            //shuffle the ordering of the termutes themselves
            if (ts > 1)
                Util.shuffle(t, random);

            tryMutate(t, -1); //start combinatorial recurse

        } else {
            tryMatch(); //go directly to conclusion
        }

//        if (matched.size()>1)
//            System.out.println(matched);

//        matched.clear();

    }

//    private void tryMatch() {
//
//
////        if (freeCount.get() > 0) {
////            //quick test for no assignments
////            return;
////        }
//
//        //filter incomplete matches by detecting them here
//        //TODO use a counter to measure this instead of checking all the time
////        Iterator<Map.Entry<Term, Versioned<Term>>> ee = xy.map.entrySet().iterator();
////        while (ee.hasNext()) {
////            Map.Entry<Term, Versioned<Term>> e = ee.next();
////            Versioned<Term> v = e.getValue();
////            if ((v == null || v.get() == null) && matchType(e.getKey()))
////                return;
////        }
//
//
////        Set<Term> free = this.free.get();
////        Term[][] match = new Term[free.size()][];
////        int m = 0;
////        for (Term x : free) {
////            Term y = xy(x);
////            if (y == null)
////                return;
////            match[m++] = new Term[]{x, y};
////        }
////        Arrays.sort(match, matchElementComparator); //sort by key
//
////        if (!matched.add(((ConstrainedVersionMap)xy).snapshot()))
////            return; //already seen
//
//        onMatch( /*match*/);
//
//
//    }

    //final static Comparator<Term[]> matchElementComparator = Comparator.comparing(v -> v[0]);


    @Override
    public String toString() {
        return xy + "@" + ((Versioning<Term>) this).ttl;
    }


    /**
     * whether the op is assignable
     */
    public final boolean matchType(/*@NotNull*/ Op oy) {
        Op t = this.type;
        return t == null ?
                oy.var : //any variable
                oy == t; //the specified type
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException("slow");
    }


    /**
     * returns true if the assignment was allowed, false otherwise
     * args should be non-null. the annotations are removed for perf reasons
     */
    public final boolean putXY(final /*@NotNull*/ Term x, final /*@NotNull*/ Term y) {
//        assert(!x0.equals(y)): "attempted to explicitly create a cycle";

        Term y0 = xy(x);

        if (y0 != null) {
//            if (y0.equals(x))
//                return true;
//            else

                //return y0.equals(y);// || unify(y0, y);
                return y==y0;
        } else /*if (matchType(x0))*/ {

            return xy.tryPut(x, y);

        }

    }

    /** stack counter, not time */
    public final int now() {
        return this.size;
    }

    /** returns the updated value */
    public int addTTL(int x) {
        return this.ttl += x;
    }

    public boolean relevantVariables(Termlike xsubs, Termlike ysubs) {
        return relevantVariables(xsubs) || (varSymmetric && relevantVariables(ysubs));
    }

    public boolean relevantVariables(Termlike x) {
        return type == null ?
                x.varPattern() > 0 || x.hasAny(Op.VAR_DEP.bit | Op.VAR_INDEP.bit | Op.VAR_QUERY.bit) :
                x.hasAny(type);
    }

    private class ConstrainedVersionMap extends VersionMap<Term, Term> {
        public ConstrainedVersionMap(Versioning versioning, int mapCap) {
            super(versioning, mapCap, 1 /* maybe 2 is necessary for full 2nd-layer unification */);
        }

//        @Nullable
//        @Override
//        public Term remove(Object key) {
//            Versioned<Term> x = map.remove(key);
//            if (x == null)
//                return null;
//            ConstrainedVersionedTerm cx = (ConstrainedVersionedTerm)x;
//            if (((ConstrainedVersionedTerm) x).forMatchedType)
//                assigned--;
//
//            return x.get();
//        }

//        @Override
//        public boolean tryPut(Term key, @NotNull Term value) {
//            int beforePut = matchType(key) ? now() : Integer.MAX_VALUE;
//            if (super.tryPut(key, value)) {
//                if (now() > beforePut) { //detects change and not just an equals() match
////                    int nextUnassigned = freeCount.get() - 1;
////
////                    if (nextUnassigned < 0)
////                        return false;
////                    //assert(nextUnassigned >= 0): "underflow";
////
////                    freeCount.add(nextUnassigned);
//                }
//                return true;
//            }
//            return false;
//        }

        @NotNull
        @Override
        public Versioned newEntry(Term key) {
            return new ConstrainedVersionedTerm(elementStackSizeDefault);
        }

        public NewCompound snapshot() {
            NewCompound s = new NewCompound(null, xy.map.size() * 2);
            xy.forEach((x, y) -> {
                s.add(x);
                if (y == null)
                    y = Null; //HACK this should have been handled by the variable count
                s.add(y);
            });
            return s.commit();
        }
    }

    final class ConstrainedVersionedTerm extends Versioned<Term> {



        /**
         * lazyily constructed
         */
        Versioned<MatchConstraint> constraints;

//        /**
//         * divide constraints into two classes: fast and slow,
//         * fast ieally are checked first
//         */
//        Versioned<MatchConstraint> fastConstraints = null;

        ConstrainedVersionedTerm(int stackSize) {
            super(Unify.this, stackSize);
        }

//        @Override
//        public void pop() {
//            if (forMatchedType) {
//                if (get() != null) {
//                    freeCount.add(freeCount.get()+1); //relase assigned variable
//                }
//            }
//
//            super.pop();
//        }

        @Nullable
        @Override
        public Versioned<Term> set(/*@NotNull*/ Term next) {
            return valid(next) ? super.set(next) : null;
        }

        private boolean valid(/*@NotNull*/ Term x) {
            if (constraints != null) {
                int s = constraints.size();
                for (int i = 0; i < s; i++) {
                    MatchConstraint cc = constraints.get(i);
                    if (cc==null)
                        throw new RuntimeException("fix");
                    assert(cc!=null);
                    if (cc.invalid(x, Unify.this))
                        return false;
                }
            }
            return true;
        }

        public boolean constrain(MatchConstraint m) {

            if (constraints == null)
                constraints = new Versioned(Unify.this, 2);

            return constraints.set(m) != null;
        }


//
//        boolean isFast(MatchConstraint m) {
//            return !(m instanceof CommonalityConstraint);
//        }

//        Versioned newConstraints() {
//            return new Versioned(versioning, MaxMatchConstraintsPerVariable);
//        }
    }


    public boolean constrain(MatchConstraint... cc) {
        for (MatchConstraint m : cc) {
            Versioned<Term> v = xy.getOrCreateIfAbsent(m.target);
            if (!((ConstrainedVersionedTerm) v).constrain(m))
                return false;
        }
        return true;
    }

}


