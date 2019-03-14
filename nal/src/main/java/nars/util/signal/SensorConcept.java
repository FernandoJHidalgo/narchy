package nars.util.signal;

import com.gs.collections.api.block.function.primitive.FloatFunction;
import com.gs.collections.api.block.function.primitive.FloatToObjectFunction;
import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.Symbols;
import nars.budget.policy.ConceptPolicy;
import nars.concept.table.BeliefTable;
import nars.task.DerivedTask;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.Truth;
import nars.util.data.Sensor;
import nars.util.math.FloatSupplier;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nars.nal.Tense.ETERNAL;


/**
 * primarily a collector for believing time-changing input signals
 */
public class SensorConcept extends WiredConcept implements FloatFunction<Term> {

    @NotNull
    protected final Sensor sensor;
    private FloatSupplier input;
    private float current = Float.NaN;

    public static final Logger logger = LoggerFactory.getLogger(SensorConcept.class);

    /** implicit motivation task */
    private Task desire = null;

    public SensorConcept(@NotNull String term, @NotNull NAR n, FloatSupplier input, FloatToObjectFunction<Truth> truth) throws Narsese.NarseseException {
        this($.$(term), n, input, truth);
    }

    public SensorConcept(@NotNull Compound term, @NotNull NAR n, FloatSupplier input, FloatToObjectFunction<Truth> truth)  {
        super(term, n);

        this.sensor = new Sensor(n, this, this, truth) {
            @Override
            public void input(Task t) {
                SensorConcept.this.input(t);
            }
        };

        this.input = input;

    }

    protected void input(Task t) {
        nar.inputLater(t);
    }


    /** originating from this sensor, or a future prediction */
    @Override
    public boolean validBelief(@NotNull Task t, @NotNull NAR nar) {
        return onlyDerivationsIfFuture(t, nar);
        //return true;
    }
    @Override
    public boolean validGoal(@NotNull Task t, @NotNull NAR nar) {
        return onlyDerivationsIfFuture(t, nar);
        //return true;
    }


    public static boolean onlyDerivationsIfFuture(@NotNull Task belief, @NotNull NAR nar) {
        if (!(belief instanceof DerivedTask))
            return true;

        long bocc = belief.occurrence();
        return (bocc!=ETERNAL && bocc > nar.time());
    }



    @Override
    final protected void beliefCapacity(ConceptPolicy p) {
        beliefCapacity(0, beliefCapacity, desire!=null ? 1 : 0, goalCapacity);
    }

    @Override
    final protected @NotNull BeliefTable newBeliefTable() {
        return newBeliefTable(0,beliefCapacity);
    }

    @Override
    final protected @NotNull BeliefTable newGoalTable() {
        return newGoalTable(desire!=null ? 1 : 0,goalCapacity);
    }


    /** async timing: only commits when value has changed significantly, and as often as necessary */
    @NotNull
    public SensorConcept async() {
        timing(0, 0);
        return this;
    }

    /** commits every N cycles only */
    @NotNull
    public SensorConcept every(int minCycles) {
        timing(minCycles, minCycles);
        return this;
    }

    
    /**
     * adjust min/max temporal resolution of feedback input
     * ex:
     *          min=0, max=2 : update every 2 cycles, or immediately if changed
     *          max=2, min=0 : update no sooner than 2 cycles
     *          max=2, min=4 : update no sooner than 2 cycles, and no later than 4
     */
    @NotNull
    public SensorConcept timing(int minCycles, int maxCycles) {
        sensor.minTimeBetweenUpdates(minCycles);
        sensor.maxTimeBetweenUpdates(maxCycles);
        return this;
    }

    public Task desire(Truth t) {
        if (this.desire==null || !this.desire.truth().equals(t)) {
            if (this.desire != null) {
                this.desire.delete();
            }

            if (t!=null) {
                this.desire = new MutableTask(term(), Symbols.GOAL, t).log("Sensor Goal");
                policy(policy()); //trigger capacity update
                sensor.nar.inputLater(this.desire);
            }
        }
        return this.desire;
    }


    public void setInput(FloatSupplier input) {
        this.input = input;
    }

    public final FloatSupplier getInput() {
        return input;
    }

    @Override
    public final float floatValueOf(Term anObject) {
        return this.current = input.asFloat();
    }

    @NotNull
    public SensorConcept resolution(float v) {
        sensor.resolution(v);
        return this;
    }

    @NotNull
    public SensorConcept pri(float v) {
        sensor.pri(v);
        return this;
    }

    public final float get() {
        return current;
    }

    @NotNull
    public <S extends SensorConcept> S punc(char c) {
        sensor.punc(c);
        return (S)this;
    }

}