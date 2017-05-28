package jcog.pri.mix;

import jcog.Util;
import jcog.data.FloatParam;
import jcog.math.AtomicSummaryStatistics;
import jcog.meter.event.PeriodMeter;
import jcog.pri.Pri;
import jcog.pri.Prioritized;
import jcog.pri.Priority;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/** a sink channel (ie. target/destination) for streams of Priority instances,
 *      with mix controls */
public class PSink<K,P extends Priority> extends FloatParam implements Function<P,P>, Consumer<P> {

    public final AtomicSummaryStatistics priMeterIn, priMeterOut;
    public final K source;
    private final Consumer<P> target;

    float minThresh = Pri.EPSILON;


    PSink(K source, Consumer<P> target) {
        super(1f, 0f, 2f);
        this.source = source;
        priMeterIn = new AtomicSummaryStatistics();
        priMeterOut = new AtomicSummaryStatistics();
        this.target = target;
    }

    public Stream<P> apply(Stream<P> x) {
        return x.peek(this);
    }

    @Override public P apply(P x) {
        accept(x);
        return x;
    }

    public P[] apply(P[] x) {
        for (P y : x)
            accept(y);
        return x;
    }


    public final void input(Stream<P> x) {
        x.map(this).forEach( target );
    }

    public final void input(P[] x) {
        for (P p : x)
            input(p);
    }

    public final void input(P x) {
        target.accept(apply(x));
    }



    @Override
    public void accept(P pp) {
        if (pp == null)
            return;

        float p = pp.pri();
        if (p!=p)
            return;

        float g = floatValue();
        if (p >= 0 && g >= 0) {
            float pg = p * g;
            if (pg >= minThresh) {
                pp.setPri(p * g);
                target.accept(pp);
                priMeterIn.accept(p);
                priMeterOut.accept(p * g);
            }
        }

    }

    /** reset gathered statistics */
    public void commit() {
        priMeterIn.clear();
        priMeterOut.clear();
        //quaMeter.clear();
    }
}
