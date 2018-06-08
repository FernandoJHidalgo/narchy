package nars.derive;

import jcog.Util;
import jcog.data.ArrayHashSet;
import jcog.math.random.SplitMix64Random;
import jcog.pri.Prioritized;
import nars.*;
import nars.control.Cause;
import nars.derive.premise.PreDerivation;
import nars.derive.step.Occurrify;
import nars.op.SubIfUnify;
import nars.op.Subst;
import nars.subterm.Subterms;
import nars.task.NALTask;
import nars.task.proxy.TaskWithTerm;
import nars.term.*;
import nars.term.anon.Anon;
import nars.term.anon.CachedAnon;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.control.PrediTerm;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.func.TruthOperator;
import nars.util.TimeAware;
import nars.util.term.TermHashMap;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.set.primitive.ImmutableLongSet;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static nars.Op.*;
import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.TIMELESS;


/**
 * evaluates a premise (task, belief, termlink, taskLink, ...) to derive 0 or more new tasks
 */
public class Derivation extends PreDerivation {


    public static final Atomic Task = Atomic.the("task");
    public static final Atomic Belief = Atomic.the("belief");
    final static int ANON_INITIAL_CAPACITY = 16;
    private final static BiFunction<Task, Task, Task> DUPLICATE_DERIVATION_MERGE = (pp, tt) -> {
        pp.priMax(tt.pri());
        ((NALTask) pp).causeMerge(tt);
        if (pp.isCyclic() && !tt.isCyclic()) {

            pp.setCyclic(false);
        }
        return pp;
    };
    private static final Atomic _tlRandom = (Atomic) $.the("termlinkRandom");
    public final ArrayHashSet<Premise> premiseBuffer =

            new ArrayHashSet<>();
    public final Anon anon =

            new CachedAnon(ANON_INITIAL_CAPACITY, 16 * 1024);
    /**
     * temporary buffer for derivations before input so they can be merged in case of duplicates
     */
    private final Map<Task, Task> derivedTasks = new HashMap<>();
    private final SubIfUnify uniSubAny = new SubIfUnify(this);
    private final Functor polarizeFunc = new Functor.AbstractInlineFunctor2("polarize") {
        @Override
        protected Term apply(Term subterm, Term whichTask) {
            if (subterm instanceof Bool)
                return subterm;

            Truth compared;
            if (whichTask.equals(Task)) {
                compared = taskTruth;
            } else {
                compared = beliefTruth;
            }
            if (compared == null)
                return Null;
            else
                return compared.isNegative() ? subterm.neg() : subterm;
        }
    };
    public NAR nar;
    private final Functor.LambdaFunctor termlinkRandomProxy = Functor.f1("termlinkRandom", (x) -> {
        x = anon.get(x);
        if (x == null)
            return Null;

        Term y = $.func(_tlRandom, x).eval(nar);
        if (y != null && y.op().conceptualizable)
            return anon.put(y);
        return Null;
    });
    /**
     * temporary un-transform map
     */
    public Map<Term, Term> untransform = new UnifiedMap<>() {
        @Override
        public Term put(Term key, Term value) {
            if (key.equals(value))
                return null;

            return super.put(key, value);
        }
    };
    private final Subst uniSub = new Subst("substitute") {

        @Override
        public @Nullable Term apply(Evaluation e, Subterms xx) {
            Term input = xx.sub(0);
            Term replaced = xx.sub(1);
            Term replacement = xx.sub(2);
            if (replaced instanceof Atom) {

                replaced = anon.put(replaced);
            }

            Term y = super.apply(xx, input, replaced, replacement);

            if (y != null && !(y instanceof Bool)) {


                untransform.put(y, input);


            }
            return y;
        }
    };
    public long[] concOcc;
    /**
     * mutable state
     */
    public Truth concTruth;
    public byte concPunc;
    public Term derivedTerm;
    public Task _task;
    public Task _belief;
    /**
     * cached values ==========================================
     */
    public int termVolMax;
    public float confMin;
    public Task task;
    public Task belief;
    /**
     * current MatchTerm to receive matches at the end of the Termute chain; set prior to a complete match by the matchee
     */
    public PrediTerm<Derivation> forEachMatch;
    /**
     * current NAR time, set at beginning of derivation
     */
    public long time = ETERNAL;
    /**
     * evidential overlap
     */
    public boolean overlapDouble, overlapSingle;
    /**
     * the base priority determined by the task and/or belief (tasks) of the premise.
     * note: this is not the same as the premise priority, which is determined by the links
     * and has already affected the selection of those links to create derived premises.
     * instead, the derived tasks are budgeted according to the priorities of the
     * parent task(s) NOT the links.  this allows the different budget 'currencies' to remain
     * separate.
     */
    public float pri;
    public short[] parentCause;
    public boolean single;
    public float parentComplexitySum;
    public float premiseEviSingle;
    public float premiseEviDouble;
    public Occurrify occ = new Occurrify(this);
    /**
     * whether either the task or belief are events and thus need to be considered with respect to time
     */
    public boolean temporal;
    public boolean eternal;
    /**
     * original non-anonymized tasks
     */
    public TruthOperator truthFunction;
    public int ditherTime;
    public Deriver deriver;
    /**
     * precise time that the task and belief truth are sampled
     */
    public long taskAt, beliefAt;
    private ImmutableMap<Term, Termed> derivationFunctors;
    private Term _beliefTerm;
    private long[] evidenceDouble, evidenceSingle;
    private int taskUniques;
    private ImmutableLongSet taskStamp;

    /**
     * if using this, must set: nar, index, random, DerivationBudgeting
     */
    public Derivation() {
        super(

                VAR_PATTERN
                , null, Param.UnificationStackMax, 0,
                new TermHashMap()

        );


        this.random =

                new SplitMix64Random(1);


    }

    private void init(NAR nar) {

        this.clear();

        this.nar = nar;

        Termed[] derivationFunctors = new Termed[]{
                uniSubAny,
                uniSub,
                polarizeFunc,
                termlinkRandomProxy

        };

        Map<Term, Termed> m = new HashMap<>(derivationFunctors.length + 2);

        for (Termed x : derivationFunctors)
            m.put(x.term(), x);


        this.derivationFunctors = Maps.immutable.ofMap(m);

    }

    @Override
    public Term resolve(Term x) {
        return x instanceof Variable ?
                super.resolve(x) :
                x;
    }

    /**
     * setup for a new derivation.
     * returns false if the premise is invalid to derive
     * <p>
     * this is optimized for repeated use of the same task (with differing belief/beliefTerm)
     */
    public boolean reset(Task _task, final Task _belief, Term _beliefTerm) {

        this.termutes.clear();

        reset();

        this.derivedTerm = null;


        if (taskUniques > 0 && this._task != null && this._task.term().equals(_task.term())) {


            anon.rollback(taskUniques);


        } else {

            anon.clear();
            this.taskTerm = anon.put(_task.term());


            this.taskUniques = anon.uniques();
        }

        assert (taskTerm != null) : (_task + " could not be anonymized: " + _task.term().anon() + " , " + taskTerm);


        if (this._task == null || !Arrays.equals(this._task.stamp(), _task.stamp())) {
            this.taskStamp = Stamp.toSet(_task);
        }

        if (this._task == null || this._task != _task) {


            this._task = _task;

            this.task = new TaskWithTerm(taskTerm, _task);

            this._taskStruct = taskTerm.structure();
            this._taskOp = taskTerm.op().id;
        }

        long tAt = _task.start();


        if (tAt == TIMELESS)
            throw new RuntimeException();

        this.taskAt = tAt;
        this.taskPunc = _task.punc();

        if ((taskPunc == BELIEF || taskPunc == GOAL)) {
            if ((this.taskTruth = _task.truth()) == null)
                return false;
        } else {
            this.taskTruth = null;
        }


        if (_belief != null) {
            this.beliefTruthDuringTask = _belief.truth(_task.start(), _task.end(), dur);
            this.beliefTruth = _belief.truth();

            if (beliefTruth != null || beliefTruthDuringTask != null) {
                this._belief = _belief;
            } else {
                this._belief = null;
            }
        } else {
            this._belief = null;
        }

        if (this._belief != null) {

            beliefTerm = anon.put(this._beliefTerm = _belief.term());
            this.belief = new TaskWithTerm(beliefTerm, _belief);
            this.beliefAt = _belief.start();
        } else {

            this.beliefTerm = anon.put(this._beliefTerm = _beliefTerm);
            if (beliefTerm.op() == NEG)
                throw new RuntimeException("should not be NEG: " + beliefTerm);
            this.beliefAt = TIMELESS;
            this.belief = null;
            this.beliefTruth = this.beliefTruthDuringTask = null;
        }

        assert (beliefTerm != null) : (_beliefTerm + " could not be anonymized");
        this._beliefStruct = beliefTerm.structure();
        this._beliefOp = beliefTerm.op().id;

        this.forEachMatch = null;
        this.concTruth = null;
        this.concPunc = 0;
        this.truthFunction = null;
        this.single = false;
        this.evidenceDouble = evidenceSingle = null;


        return true;
    }

    /**
     * called after protoderivation has returned some possible Try's
     */
    public void derive(int ttl) {


        this.parentComplexitySum =
                Util.sum(

                        taskTerm.voluplexity(), beliefTerm.voluplexity()
                );


        this.overlapSingle = task.isCyclic();

        if (_belief != null) {

            /** to compute the time-discounted truth, find the minimum distance
             *  of the tasks considering their dtRange
             */


            long[] beliefStamp = _belief.stamp();
            this.overlapDouble = Stamp.overlapsAny(this.taskStamp, beliefStamp);


        } else {
            this.overlapDouble = false;
        }


        this.eternal = task.isEternal() && (_belief == null || _belief.isEternal());
        this.temporal = !eternal || (taskTerm.isTemporal() || (_belief != null && beliefTerm.isTemporal()));

        this.parentCause = _belief != null ?
                Cause.sample(Param.causeCapacity.intValue(), _task, _belief) :
                _task.cause();

        float taskPri = _task.priElseZero();
        this.pri =
                _belief == null ?
                        Param.TaskToDerivation.valueOf(taskPri) :
                        Param.TaskBeliefToDerivation.apply(taskPri, _belief.priElseZero());


        this.premiseEviSingle = taskTruth != null ? taskTruth.evi() : Float.NaN;
        this.premiseEviDouble = beliefTruth != null ?

                premiseEviSingle + beliefTruth.evi() :
                premiseEviSingle;


        setTTL(ttl);


        deriver.prioritize.premise(this);

        deriver.rules.can.test(this);


    }

    @Override
    public final void tryMatch() {


        forEachMatch.test(this);



        /* finally {
            revert(now);
        }*/

    }

    /**
     * accelerated version that executes inline functors
     */
    @Override
    public @Nullable Term transformedCompound(Compound x, Op op, int dt, Subterms xx, Subterms yy) {

        if (op == INH && yy.subs() == 2 && yy.hasAll(Op.PROD.bit | Op.ATOM.bit)) {
            Term pred;
            if ((pred = yy.sub(1)) instanceof Functor.InlineFunctor) {
                Term args = yy.sub(0);
                if (args.op() == PROD) {
                    Term v = ((Functor.InlineFunctor) pred).apply(args);
                    if (v != null)
                        return v;

                }
            }
        }

        return super.transformedCompound(x, op, dt, xx, yy);
    }

    /**
     * only returns derivation-specific functors.  other functors must be evaluated at task execution time
     */
    @Override
    public final Term transformAtomic(Term atomic) {

        if (atomic instanceof Atom) {
            Termed f = derivationFunctors.get(atomic);
            if (f != null)
                return (Term) f;
        }

        if (atomic instanceof Bool)
            return atomic;

        if (atomic instanceof Functor)
            return atomic;


        if (atomic instanceof Variable) {
            Term y = xy(atomic);
            if (y != null)
                return y;

        }

        return atomic;


    }

    public Derivation cycle(NAR nar, Deriver deri) {
        TimeAware pnar = this.nar;
        if (pnar != nar) {
            init(nar);
        }

        long now = nar.time();
        if (now != this.time) {
            this.time = now;
            this.dur = deri.dur();
            this.ditherTime = nar.dtDither();
            this.confMin = nar.confMin.floatValue();
            this.termVolMax = nar.termVolumeMax.intValue();
            this.random.setSeed(nar.random().nextLong());

        }

        this.deriver = deri;

        return this;
    }

    @Nullable
    public long[] evidenceSingle() {
        if (evidenceSingle == null) {
            evidenceSingle = task.stamp();
        }
        return evidenceSingle;
    }

    @Nullable
    public long[] evidenceDouble() {
        if (evidenceDouble == null) {
            float te, be, tb;
            if (task.isBeliefOrGoal()) {

                te = taskTruth.evi();
                be = beliefTruth != null ? beliefTruth.evi() : 0;
                tb = te / (te + be);
            } else {

                te = task.priElseZero();
                be = belief.priElseZero();
                tb = te + be;
                tb = tb < Prioritized.EPSILON ? 0.5f : te / tb;
            }
            return evidenceDouble = Stamp.zip(task.stamp(), belief.stamp(), tb);
        } else {
            return evidenceDouble;
        }
    }

    @Override
    public String toString() {
        return _task + " " + (_belief != null ? _belief : _beliefTerm)
                + ' ' + super.toString();
    }

    /**
     * include any .clear() for data structures in case of emergency we can continue to assume they will be clear on next run()
     */
    @Override
    public void clear() {
        anon.clear();
        taskUniques = 0;
        premiseBuffer.clear();
        derivedTasks.clear();
        untransform.clear();
        termutes.clear();
        time = ETERNAL;
        super.clear();
    }

    public Task add(Task t) {
        return derivedTasks.merge(t, t, DUPLICATE_DERIVATION_MERGE);
    }

    public Term untransform(Term t) {


        return t.replace(untransform);
    }

    public int flush(Consumer<Collection<Task>> target) {
        int s = derivedTasks.size();
        if (s > 0) {
            nar.emotion.deriveTask.increment(s);
            target.accept(derivedTasks.values());
            derivedTasks.clear();
        }
        return s;
    }

    public final boolean revertLive(int before, int cost) {
        ttl -= cost;
        return revertLive(before);
    }

    public Occurrify occ(Term pattern) {
        occ.reset(pattern.hasAny(NEG));
        return occ;
    }


}


