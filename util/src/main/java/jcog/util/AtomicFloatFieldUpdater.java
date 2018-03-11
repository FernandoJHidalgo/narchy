package jcog.util;

import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import static java.lang.Float.floatToIntBits;
import static java.lang.Float.intBitsToFloat;

/** @see AtomicFloat */
public class AtomicFloatFieldUpdater<X>  {

    private final AtomicIntegerFieldUpdater<X> updater;

    /** for whatever reason, the field updater needs constructed from within the target class
     * so it must be passed as a parameter here.
     */
    public AtomicFloatFieldUpdater(AtomicIntegerFieldUpdater<X> u) {
        this.updater = u;
    }


    public void set(X x, float value) {
        updater.set(x, floatToIntBits(value));
    }

    public void add(X x, float add) {
        updater.updateAndGet(x, v -> floatToIntBits(intBitsToFloat(v) + add));
    }

    public float updateAndGet(X x, FloatToFloatFunction f) {
        return intBitsToFloat(updater.updateAndGet(x,
                v -> floatToIntBits(f.valueOf(intBitsToFloat(v)))
        ));
    }

    public void addUpdate(X x, float add, Runnable r) {
        updater.updateAndGet(x, v -> {
            r.run();
            return floatToIntBits(intBitsToFloat(v) + add);
        });
    }

    public float getAndSet(X x, float value) {
        return intBitsToFloat(updater.getAndSet(x, floatToIntBits(value)));
    }

    public float getAndZero(X x) {
        return intBitsToFloat(updater.getAndSet(x, ZERO));
    }

    public float get(X x) {
        return intBitsToFloat(updater.get(x));
    }

    private final static int ZERO = floatToIntBits(0f);

    public void zero(X v, FloatConsumer with) {
        this.updater.getAndUpdate(v, x->{
            with.accept(intBitsToFloat(x));
            return AtomicFloatFieldUpdater.ZERO;
        });
    }

    /** if the current value is actually zero, the consumer is not called and nothing needs updated.
     * should be faster than zero(v,with) when a zero value is expected
     */
    public void zeroIfNonZero(X v, FloatConsumer with) {
        this.updater.getAndUpdate(v, x->{
            if (x != AtomicFloatFieldUpdater.ZERO) {
                with.accept(intBitsToFloat(x));
            }
            return AtomicFloatFieldUpdater.ZERO;
        });
    }

    public float getAndZero(X v, FloatConsumer with) {
        return intBitsToFloat(this.updater.getAndUpdate(v, (x)->{ with.accept(intBitsToFloat(x)); return AtomicFloatFieldUpdater.ZERO; } ));
    }

    public boolean compareAndSet(X x, float expected, float newvalue) {
        return updater.compareAndSet(x, floatToIntBits(expected), floatToIntBits(newvalue));
    }



//    public void addAndGet(X v, float x) {
//        this.updater.updateAndGet(v, (i)-> floatToIntBits(intBitsToFloat(i) + x ));
//    }
}
