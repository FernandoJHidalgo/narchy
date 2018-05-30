package nars.op;

import jcog.Paper;
import jcog.Util;
import jcog.decide.Roulette;
import jcog.list.FasterList;
import jcog.memoize.HijackMemoize;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.bag.leak.LeakBack;
import nars.term.Term;
import nars.term.Variable;
import nars.term.anon.Anom;
import nars.term.anon.Anon;
import nars.term.atom.Int;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.IntObjectPair;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.function.Function;

import static nars.Op.*;
import static nars.time.Tense.DTERNAL;
import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * introduces arithmetic relationships between differing numeric subterms
 * responsible for showing the reasoner mathematical relations between
 * numbers appearing in compound terms.
 *
 * TODO
 *      greater/less than comparisons
 *
 *
 */
@Paper
public class ArithmeticIntroduction extends LeakBack {

    public static Term apply(Term x, Random random) {
        return apply(x, true, random);
    }

    public static Term apply(Term x, boolean eternal, Random random) {
        return apply(x, null, eternal, random);
    }

    public static Term apply(Term x, @Nullable Anon anon, boolean eternal, Random random) {
        if (anon == null && !x.hasAny(INT) || x.complexity() < 3)
            return x;

        
        IntHashSet ints = new IntHashSet(4);
        x.recurseTerms(t->t.hasAny(Op.INT), t -> {
            if (t instanceof Anom) {
                t = anon.get(t);
            }
            if (t instanceof Int) {
                ints.add(((Int) t).id);
            }
            return true;
        }, x);

        int ui = ints.size();
        if (ui <= 1)
            return x; 

        int[] ii = ints.toSortedArray();  

        List<IntObjectPair<List<Pair<Term, Function<Term, Term>>>>> mmm = mods(ii);

        

        

        


        int choice = Roulette.selectRoulette(mmm.size(), c -> mmm.get(c).getTwo().size(), random);

        IntObjectPair<List<Pair<Term, Function<Term, Term>>>> m = mmm.get(choice);

        Term baseTerm = Int.the(m.getOne());
        if (anon!=null)
            baseTerm = anon.put(baseTerm);

        Variable V = $.varDep("b");
        Term yy = x.replace(baseTerm, V);

        for (Pair<Term, Function<Term, Term>> s : m.getTwo()) {
            Term s0 = s.getOne();
            Term s1 = s.getTwo().apply(V);
            if (anon!=null)
                s0 = anon.put(s0); 
            yy = yy.replace(s0, s1);
        }

        Term equality =
                SIM.the(baseTerm, V);
                /** $.func(Builtin.EQUAL, Terms.sorted(baseTerm, V)); */

        Term y =
                CONJ.the(yy, eternal ? DTERNAL : 0, equality);
                
                

        if (y.op()!=CONJ) {
        
            return null; 
        }

        if (x.isNormalized()) {
            y = y.normalize();
        }
        return y;
    }

    final static class IntArrayListCached extends IntArrayList {
        private final int hash;

        public IntArrayListCached(int[] ii) {
            super(ii);
            int hash = ii[0];
            for (int i = 1; i < ii.length; i++)
                hash = Util.hashCombine(hash, ii[i]);
            this.hash = hash;
        }

        public int[] toArray() {
            return items;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    static final HijackMemoize<IntArrayListCached,List<IntObjectPair<List<Pair<Term, Function<Term, Term>>>>>>
        modsCache = new HijackMemoize<>(ArithmeticIntroduction::_mods, 512, 3);

    static List<IntObjectPair<List<Pair<Term, Function<Term, Term>>>>> mods(int[] ii) {


        return modsCache.apply(new IntArrayListCached(ii));
    }

    static List<IntObjectPair<List<Pair<Term, Function<Term, Term>>>>> _mods(IntArrayListCached iii) {
        
        

        int[] ii = iii.toArray();

        IntObjectHashMap<List<Pair<Term, Function<Term,Term>>>> mods = new IntObjectHashMap<>(ii.length);


        
        for (int a = 0; a < ii.length; a++) {
            int ia = ii[a];
            for (int b = 0; b < ii.length; b++) {
                if (a == b) continue;

                int ib = ii[b];


                int BMinA = ib - ia;
                if (ia == -ib) {
                    
                    maybe(mods, ia).add(pair(
                            Int.the(ib), v-> $.func(MathFunc.mul, v,Int.NEG_ONE)
                    ));



                } else if (ia!=0 && Math.abs(ia)!=1 && ib!=0 && Math.abs(ib)!=1 && Util.equals(ib/ia, (float)ib /ia, Float.MIN_NORMAL)) {

                    
                    maybe(mods, ia).add(pair(
                            Int.the(ib), v->$.func(MathFunc.mul, v, $.the(ib/ia))
                    ));
                } else if (ia < ib) { 

                    maybe(mods, ia).add(pair(
                            Int.the(ib), v-> $.func(MathFunc.add, v, $.the(BMinA))
                    ));





                }









            }
        }
        return !mods.isEmpty() ? mods.keyValuesView().toList() : List.of();
    }

    public static List<Pair<Term, Function<Term, Term>>> maybe(IntObjectHashMap<List<Pair<Term, Function<Term, Term>>>> mods, int ia) {
        return mods.getIfAbsentPut(ia, FasterList::new);
    }

    public static final Logger logger = LoggerFactory.getLogger(ArithmeticIntroduction.class);


    public ArithmeticIntroduction(int taskCapacity, NAR n) {
        super(taskCapacity, n);
    }

    @Override
    protected boolean preFilter(Task next) {
        return next.term().hasAny(Op.INT);
    }
    @Override
    protected float pri(Task t) {
        float p = super.pri(t);
        Term tt = t.term();
        int numInts = tt.intifyRecurse((n, sub) -> sub.op() == INT ? n + 1 : n, 0);
        assert(numInts > 0);
        if (numInts < 2)
            return Float.NaN;

        
        float intTerms = numInts / ((float)tt.volume());
        return p * intTerms;
    }
    @Override
    protected float leak(Task xx) {
        Term x = xx.term();
        Term y = apply(x, xx.isEternal(), nar.random());
        if (y!=null && !y.equals(x) && y.op().conceptualizable) {
            Task yy = Task.clone(xx, y);
            
            if (yy!=null) {
                input(yy);
                return 1;
            }
        } else {


        }

        return 0;
    }



}
