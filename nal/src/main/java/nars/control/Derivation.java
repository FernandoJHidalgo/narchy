package nars.control;

import jcog.Util;
import jcog.math.ByteShuffler;
import jcog.pri.Pri;
import jcog.version.Versioned;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.derive.PrediTerm;
import nars.derive.TemporalizeDerived;
import nars.derive.rule.PremiseRule;
import nars.op.Subst;
import nars.op.data.differ;
import nars.op.data.intersect;
import nars.op.data.union;
import nars.task.DerivedTask;
import nars.task.ITask;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.subst.Unify;
import nars.truth.Stamp;
import nars.truth.Truth;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.RoaringBitmap;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import static nars.Op.*;
import static nars.op.SubstUnified.uniSubAny;
import static nars.time.Tense.ETERNAL;


/**
 * evaluates a premise (task, belief, termlink, taskLink, ...) to derive 0 or more new tasks
 */
public class Derivation extends Unify {

    public static final Atomic _taskTerm = Atomic.the("_taskTerm");
    public static final Atomic _beliefTerm = Atomic.the("_beliefTerm");
    private final Functor.LambdaFunctor polarizeFunc;

    public NAR nar;

    public final Versioned<Term> derivedTerm;

    public final ByteShuffler shuffler = new ByteShuffler(64);

    /**
     * temporary buffer for derivations before input so they can be merged in case of duplicates
     */
    public final Map<DerivedTask, DerivedTask> derivations = new LinkedHashMap<>();

    private ImmutableMap<Term, Termed> derivationFunctors;

    public float truthResolution;

    /**
     * cached values ==========================================
     */
    public int termVolMax;
    public float confMin;

    public Truth concTruth;
    public byte concPunc;
    public float concConfFactor;
    public final long[] concOcc = new long[2];

//    /**
//     * the current premise being evaluated in this context TODO make private again
//     */
//    public Premise premise;

    /**
     * current MatchTerm to receive matches at the end of the Termute chain; set prior to a complete match by the matchee
     */
    public PrediTerm<Derivation> forEachMatch;
    /**
     * op ordinals: 0=task, 1=belief
     */
    public byte termSub0op;
    public byte termSub1op;


    /**
     * structs, 0=task, 1=belief
     */
    public int termSub0Struct;
    public int termSub1Struct;

    /**
     * current NAR time, set at beginning of derivation
     */
    public long time = ETERNAL;

    public Truth taskTruth, beliefTruth, beliefTruthRaw;
    public Term taskTerm;
    public Term beliefTerm;
    public Task task;
    public Task belief;
    public byte taskPunct;

    /**
     * whether either the task or belief are events and thus need to be considered with respect to time
     */
    public boolean temporal;


    /**
     * evidential overlap
     */
    public float overlapDouble, overlapSingle;

    public float premisePri;
    public short[] parentCause;

    public PrediTerm<Derivation> deriver;
    public boolean single;
    public TemporalizeDerived temporalize;
    public int parentComplexity;

    /**
     * choices mapping the available post targets
     */
    public final RoaringBitmap preToPost = new RoaringBitmap();

    public float premiseConfSingle;
    public float premiseConfDouble;
    private long[] evidenceDouble, evidenceSingle;


//    private transient Term[][] currentMatch;

//    public /*static*/ final Cache<Transformation, Term> transformsCache; //works in static mode too
//    /*static*/ {
//    }

//    final MRUCache<Transformation, Term> transformsCache = new MRUCache<>(Param.DERIVATION_THREAD_TRANSFORM_CACHE_SIZE);

    /**
     * if using this, must set: nar, index, random, DerivationBudgeting
     */
    public Derivation() {
        super(
                null /* any var type */
                //VAR_PATTERN
                , null, Param.UnificationStackMax, 0);


//        Caffeine cb = Caffeine.newBuilder().executor(nar.exe);
//            //.executor(MoreExecutors.directExecutor());
//        int cs = Param.DERIVATION_TRANSFORM_CACHE_SIZE_PER_THREAD;
//        if (cs == -1)
//            cb.softValues();
//        else
//            cb.maximumSize(cs);
//
//        //cb.recordStats();
//
//        transformsCache = cb.builder();

        //final Functor substituteIfUnifiesDep = new substituteIfUnifiesDep(this);

        polarizeFunc = Functor.f2("polarize", (subterm, whichTask) -> {
            Truth compared;
            if (whichTask.equals(PremiseRule.Task)) {
                compared = taskTruth;
            } else {
                compared = beliefTruth;
            }
            if (compared == null)
                return Null;
            else
                return compared.isNegative() ? subterm.neg() : subterm;
        });


        derivedTerm = new Versioned(this, 3);
    }

    ImmutableMap<Term, Termed> functors(Termed... t) {
        java.util.Map<Term, Termed> m = new HashMap(t.length + 2);
        for (Termed x : t) {
            m.put(x.term(), x);
        }
        m.put(_taskTerm, () -> taskTerm);
        m.put(_beliefTerm, () -> beliefTerm);
        return Maps.immutable.ofMap(m);
    }

    /**
     * only returns derivation-specific functors.  other functors must be evaluated at task execution time
     */
    @Override
    public final Termed apply(Term x) {

        if (x instanceof Atom) {
            Termed f = derivationFunctors.get(x);
            if (f != null)
                return f;
        }

        return super.apply(x);
    }

    public Derivation cycle(NAR nar, PrediTerm<Derivation> deriver) {
        NAR pnar = this.nar;
        if (pnar != nar) {
            init(nar);
        }

        long now = nar.time();
        if (now != this.time) {
            this.time = now;
            this.dur = nar.dur();
            this.truthResolution = nar.truthResolution.floatValue();
            this.confMin = nar.confMin.floatValue();
            this.termVolMax = nar.termVolumeMax.intValue();
            //transformsCache.cleanUp();
        }
        this.deriver = deriver;
        return this;
    }

    public void init(NAR nar) {
        this.clear();
        this.nar = nar;
        this.random = nar.random();
        this.derivationFunctors = functors(
                new uniSubAny(this),
                polarizeFunc,
                Subst.the,
                union.the,
                differ.the,
                intersect.the,
                nar.get(Atomic.the("dropAnyEvent")),
                nar.get(Atomic.the("dropAnySet")),
                nar.get(Atomic.the("conjEvent")),
                nar.get(Atomic.the("conjDropIfEarliest")),
                nar.get(Atomic.the("ifConjCommNoDepVars")),
                nar.get(Atomic.the("without")),
                nar.get(Atomic.the("indicesOf")),
                nar.get(Atomic.the("substDiff"))
        );
    }

    /**
     * tasklink/termlink scope
     */
    public void set(Premise p, Task belief, Term beliefTerm) {

        if (revert(0)) {
            //remove common variable entries because they will just consume memory if retained as empty
//            xy.map.keySet().removeIf(k -> {
//                return !(k instanceof AbstractVariable) || k instanceof CommonVariable;
//            });
//            xy.map.clear();
        }
        xy.map.clear();
        termutes.clear();
        preToPost.clear();
        //assert(termutes.isEmpty() && preToPost.isEmpty());
        this.forEachMatch = null;
        this.temporalize = null;
        this.concTruth = null;
        this.concPunc = 0;
        this.single = false;
        this.evidenceDouble = evidenceSingle = null;

        final Task task = this.task = p.task;


        Term taskTerm = task.term();
        this.taskTerm = taskTerm;


        this.termSub0Struct = taskTerm.structure();
        this.termSub0op = taskTerm.op().id;


        this.belief = belief;

        Term bt = beliefTerm.unneg();
        assert (!(bt instanceof Bool));

        this.concOcc[0] = this.concOcc[1] = ETERNAL;

//        int ttv = taskTerm.vars();
//        if (ttv > 0 && bt.vars() > 0) {
//            bt = bt.normalize(ttv); //shift variables up to be unique compared to taskTerm's
//        }
        this.beliefTerm = bt;
        this.parentComplexity =
                //Util.sum(
                Math.max(
                        taskTerm.complexity(), bt.complexity()
                );


        switch (this.taskPunct = task.punc()) {
            case QUESTION:
            case QUEST:
                this.taskTruth = null;
                break;
            default:
                this.taskTruth = task.truth();
        }

        long[] taskStamp = task.stamp();
        this.overlapSingle = Stamp.cyclicity(taskStamp);

        if (belief != null) {
            this.beliefTruthRaw = belief.truth();

            /** to compute the time-discounted truth, find the minimum distance
             *  of the tasks considering their dtRange
             */
            if (!belief.isEternal()) {

                //project belief truth to task's time
                long beliefTruthTime = task.isEternal() ?
                        ETERNAL :
                        //task.start();
                        belief.nearestTimeBetween(task.start(), task.end());
                //nar.time(); //now

                this.beliefTruth = belief.truth(beliefTruthTime, dur, confMin);
            } else {
                this.beliefTruth = beliefTruthRaw;
            }

            long[] beliefStamp = belief.stamp();
            this.overlapDouble =
                    //Math.min(1, Util.sum(
                    Util.or(
                            //Util.max(
                            overlapSingle,
                            Stamp.overlapFraction(taskStamp, beliefStamp),
                            Stamp.cyclicity(beliefStamp)
                    );
        } else {
            this.beliefTruth = this.beliefTruthRaw = null;
            this.overlapDouble = 0;
        }


        this.termSub1Struct = beliefTerm.structure();

        Op bOp = beliefTerm.op();
        this.termSub1op = bOp.id;

        this.temporal = (!task.isEternal() || taskTerm.isTemporal()) ||
                (belief != null && (!belief.isEternal() || beliefTerm.isTemporal()));

        this.parentCause = belief != null ?
                Cause.zip(task, belief) :
                task.cause();

        this.premisePri =
                //p.priElseZero(); //use the premise pri directly
                belief == null ? task.priElseZero() : Param.TaskBeliefDerivation.apply(task.priElseZero(), belief.priElseZero());

        //float parentValue = nar.evaluate(parentCause); /* value of the parent cause as a multiplier */
        //this.premisePri *= parentValue;


        this.premiseConfSingle = this.taskTruth != null ? taskTruth.conf() : 0;
        this.premiseConfDouble = beliefTruth != null ?
                Math.min(premiseConfSingle, beliefTruth.conf()) : //to be fair to the lesser confidence
                premiseConfSingle;

    }

    public void derive(int ttl) {
        setTTL(ttl);
        assert (ttl > 0);
        deriver.test(this);
    }

    @Override
    public final void tryMatch() {


        int now = now();
        try {
            //xy.replace(nar::applyTermIfPossible); //resolve to an abbreviation or other indexed term
            forEachMatch.test(this);
        } finally {
            revert(now); //undo any changes applied in conclusion
        }

    }

    @Nullable
    public long[] evidenceSingle() {
        if (evidenceSingle == null) {
            evidenceSingle = Stamp.cyclic(task.stamp());
        }
        return evidenceSingle;
    }

    @Nullable
    public long[] evidenceDouble() {
        if (evidenceDouble == null) {
            float te, be, tb;
            if (task.isBeliefOrGoal()) {
                //for belief/goal use the relative conf
                te = taskTruth.conf();
                be = beliefTruth != null ? beliefTruth.conf() : 0; //beliefTruth can be zero in temporal cases
                tb = te / (te + be);
            } else {
                //for question/quest, use the relative priority
                te = task.priElseZero();
                be = belief.priElseZero();
                tb = te + be;
                tb = tb < Pri.EPSILON ? 0.5f : te / tb;
            }
            return evidenceDouble = Stamp.zip(task.stamp(), belief.stamp(), tb);
        } else {
            return evidenceDouble;
        }
    }

    @Override
    public String toString() {
        return task + " " + (belief != null ? belief : beliefTerm)
                + ' ' + super.toString();
    }

    /**
     * include any .clear() for data structures in case of emergency we can continue to assume they will be clear on next run()
     */
    @Override
    public void clear() {
        derivations.clear();
        termutes.clear();
        preToPost.clear();
        time = ETERNAL;
        super.clear();
    }

    public int getAndSetTTL(int next) {
        int before = this.ttl;
        this.ttl = next;
        return before;
    }

    /**
     * called at the end of the cycle, input all generated derivations
     */
    public int commit(Consumer<Collection<DerivedTask>> target) {
        int s = derivations.size();
        if (s > 0) {
            nar.emotion.taskDerived.increment(s);
            target.accept(derivations.values());
            derivations.clear();
        }
        return s;
    }

    //    /**
//     * experimental memoization of transform results
//     */
//    @Override
//    @Nullable
//    public Term transform(@NotNull Term pattern) {
//
//        if (!Param.DERIVATION_TRANSFORM_CACHE) {
//            return super.transform(pattern); //xy.get(pattern); //fast variable resolution
//        }
//
//        Term y = xy(pattern);
//        if (y!=null) {
//            if (pattern instanceof Variable || y.vars(null) == 0) {
////                if (xy.get(y)!=null)
////                    System.out.println(y + " -> " + xy.get(y));
//
//                return y;
//            }
//            pattern = y;
//        }
//        if (pattern instanceof Atomic) {
////            if (xy.get(x)!=null)
////                System.out.println(x + " -> " + xy.get(x));
//            return pattern;
//        }
//
////        if (x.OR(xx -> xx == Null))
////            return Null;
//
//        Transformation key = Transformation.the((Compound) pattern, currentMatch);
//
//        //avoid recursive update problem on the single thread by splitting the get/put
//        Term value = transformsCache.getIfPresent(key);
//        if (value != null)
//            return value;
//
//        value = super.transform(key.pattern);
//        if (value == null)
//            value = Null;
//
//        transformsCache.put(key, value);
//
//        if (value == Null)
//            value = null;
//
//        return value;
//    }
//
//    final static class Transformation {
//        public final Compound pattern;
//        final byte[] assignments;
//        final int hash;
//
//        Transformation(Compound pattern, DynBytes assignments) {
//            this.pattern = pattern;
//            this.assignments = assignments.array();
//            this.hash = Util.hashCombine(assignments.hashCode(), pattern.hashCode());
//        }
//
//        @Override
//        public boolean equals(Object o) {
//            //if (this == o) return true;
//            Transformation tthat = (Transformation) o;
//            if (hash != tthat.hash) return false;
//            return pattern.equals(tthat.pattern) && Arrays.equals(assignments, tthat.assignments);
//        }
//
//        @Override
//        public int hashCode() {
//            return hash;
//        }
//
//        static Transformation the(@NotNull Compound pattern, @NotNull Term[][] match) {
//
//            //TODO only include in the key the free variables in the pattern because there can be extra and this will cause multiple results that could have done the same thing
//            //FasterList<Term> key = new FasterList<>(currentMatch.length * 2 + 1);
//
//            DynBytes key = new DynBytes((2 * match.length + 1) * 8 /* estimate */);
//            pattern.append((ByteArrayDataOutput) key); //in 0th
//            key.writeByte(0);
//            for (Term[] m : match) {
//                Term var = m[0];
//                if (pattern.containsRecursively(var)) {
//                    var.append((ByteArrayDataOutput) key);
//                    key.writeByte(0);
//                    m[1].append((ByteArrayDataOutput) key);
//                    key.writeByte(0);
//                }
//            }
//            return new Transformation(pattern, key);
//        }
//
//    }


}


