package nars.concept.sensor;

import jcog.Util;
import jcog.math.FloatRange;
import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.attention.AttnBranch;
import nars.concept.PermanentConcept;
import nars.concept.TaskConcept;
import nars.link.TermLinker;
import nars.table.dynamic.SensorBeliefTables;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;


/**
 * primarily a collector for belief-generating time-changing (live) input scalar (1-d real value) signals
 *
 *
 * In vector analysis, a scalar quantity is considered to be a quantity that has magnitude or size, but no motion. An example is pressure; the pressure of a gas has a certain value of so many pounds per square inch, and we can measure it, but the notion of pressure does not involve the notion of movement of the gas through space. Therefore pressure is a scalar quantity, and it's a gross, external quantity since it's a scalar. Note, however, the dramatic difference here between the physics of the situation and mathematics of the situation. In mathematics, when you say something is a scalar, you're just speaking of a number, without having a direction attached to it. And mathematically, that's all there is to it; the number doesn't have an internal structure, it doesn't have internal motion, etc. It just has magnitude - and, of course, location, which may be attachment to an object.
 * http://www.cheniere.org/misc/interview1991.htm#Scalar%20Detector
 */
public class Signal extends TaskConcept implements Sensor, FloatFunction<Term>, FloatSupplier, PermanentConcept {

    public final AttnBranch attn;
    final short cause;

    /**
     * update directly with next value
     */
    public static Function<FloatSupplier, FloatFloatToObjectFunction<Truth>> SET = (conf) ->
            ((prev, next) -> next == next ? $.t(next, conf.asFloat()) : null);
    /**
     * first order difference
     */
    public static Function<FloatSupplier, FloatFloatToObjectFunction<Truth>> DIFF = (conf) ->
            ((prev, next) -> (next == next) ? ((prev == prev) ? $.t((next - prev) / 2f + 0.5f, conf.asFloat()) : $.t(0.5f, conf.asFloat())) : $.t(0.5f, conf.asFloat()));

    public final FloatSupplier source;

    private volatile float currentValue = Float.NaN;

    public Signal(Term term, FloatSupplier signal, NAR n) {
        this(term, n.newCause(term).id, signal, n);
    }

    public Signal(Term term, short cause, FloatSupplier signal, NAR n) {
        this(term, cause, signal, n.conceptBuilder.termlinker(term), n);
    }
    public Signal(Term term, short cause, FloatSupplier signal, TermLinker linker, NAR n) {
        this(term, cause, BELIEF, signal, linker, n);
    }

    private Signal(Term term, short cause, byte punc, FloatSupplier signal, TermLinker linker, NAR n) {
        super(term,
                punc == BELIEF ? new SensorBeliefTables(term, true) : n.conceptBuilder.newTable(term, true),
                punc == GOAL ? new SensorBeliefTables(term, false) : n.conceptBuilder.newTable(term, false),
                linker,
                n.conceptBuilder);

        this.source = signal;
        this.cause = cause;

        this.attn = newAttn(term);

        ((SensorBeliefTables) beliefs()).resolution(FloatRange.unit(n.freqResolution));

        n.on(this);
    }

    protected AttnBranch newAttn(Term term) {
        return new AttnBranch(term, List.of(this));
    }


    @Override
    public Iterable<Termed> components() {
        return List.of(this);
    }

    @Override
    public final FloatRange resolution() {
        return ((SensorBeliefTables) beliefs()).resolution();
    }

    @Override
    public float floatValueOf(Term anObject /* ? */) {
        return this.currentValue;
    }


    @Override
    public float asFloat() {
        return currentValue;
    }

    @Nullable
    public void update(long start, long end, FloatFloatToObjectFunction<Truth> truther, FloatSupplier pri, short cause, NAR n) {

        float prevValue = currentValue;

        float nextValue = currentValue = source.asFloat();

        ((SensorBeliefTables) beliefs()).add(
                nextValue == nextValue ? truther.value(prevValue, nextValue) : null,
                        start, end, pri, cause, n);
    }


    public Signal resolution(float r) {
        resolution().set(r);
        return this;
    }
    public Signal setResolution(FloatRange r) {
        ((SensorBeliefTables) beliefs()).resolution(r);
        return this;
    }

    @Override
    public void sense(long prev, long now, NAR nar) {
        update(prev, now,
                (tp, tn) -> $.t(Util.unitize(tn), nar.confDefault(BELIEF)),
                attn::elementPri,
                cause, nar);
    }


}
