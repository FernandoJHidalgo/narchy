package nars.control;

import jcog.exe.Can;
import nars.NAR;
import nars.task.ITask;
import nars.task.NativeTask;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * instruments the runtime resource consumption of its iteratable procedure.
 * this determines a dynamically adjusted strength parameter
 * that the implementation can use to modulate its resource needs.
 * these parameters are calculated in accordance with
 * other instances in an attempt to achieve a fair and
 * predictable (ex: linear) relationship between its scalar value estimate
 * and the relative system resources it consumes.
 */
abstract public class Causable extends NARService {

    private static final Logger logger = LoggerFactory.getLogger(Causable.class);


    public final Can can;

    private final AtomicBoolean busy;

    protected Causable(NAR nar) {
        super(nar);
        busy = singleton() ? new AtomicBoolean(false) : null;

        can = new MyCan(nar, term().toString());
    }

    @Override
    public String toString() {
        return can.toString();
    }

    @Override
    protected void start(NAR nar) {
        super.start(nar);

        synchronized (nar.focus) {
            nar.focus.add(this);
        }

    }

    @Override
    protected void stop(NAR nar) {

        synchronized (nar.focus) {
            nar.focus.remove(this);
        }

        super.stop(nar);
    }

    /**
     * if true, allows multiple threads to execute on this instance
     */
    public boolean singleton() {
        return true;
    }

    public final int run(NAR n, int iterations) {

        if (singleton() && !busy.compareAndSet(false, true)) {
            return 0; //another thread running in here
        }

        Throwable error = null;
        int completed = 0;
        try {
            long start = System.nanoTime();
            try {
                completed = next(n, iterations);
                assert (completed >= 0);
            } catch (Throwable t) {
                error = t;
            }
            long end = System.nanoTime();

            can.update(completed, value(), (end - start) / 1.0E9);
        } catch (Exception e) {
            logger.error("{} {}", this, e);
        } finally {
            if (busy != null)
                busy.set(false);
        }

        if (error != null) {
            logger.error("{} {}", this, error);
        }

        return completed;
    }

//
//    protected void learn(long waitNS, float workAllowed, float workDone, long runtimeNS) {
//        //double cycleTime = runtimeNS + waitNS;
//        //double dutyCycle = runtimeNS / cycleTime; //estimate
//        workEstimator.learn(workDone, runtimeNS);
//    }


    /**
     * returns iterations actually completed
     */
    protected abstract int next(NAR n, int iterations);

    /**
     * returns a system estimated value of invoking this. between 0..1.0
     */
    public abstract float value();

//    public final static class InvokeCause extends NativeTask {
//
//        public final Causable cause;
//        public final int iterations;
//
//        private InvokeCause(Causable cause, int iterations) {
//            assert (iterations > 0);
//            this.cause = cause;
//            this.iterations = iterations;
//        }
//        //TODO deadline? etc
//
//        @Override
//        public String toString() {
//            return cause + ":" + iterations + "x";
//        }
//
//        @Override
//        public @Nullable Iterable<? extends ITask> run(NAR n) {
//            cause.run(n, iterations);
//            return null;
//        }
//    }

    private final class MyCan extends Can {
        private final NAR nar;

        public MyCan(NAR nar, String id) {
            super(id);
            this.nar = nar;
        }

//        @Override
//        public void commit() {
//            int ii = iterations();
//            if (ii > 0)
//                nar.exe.add(new InvokeCause(Causable.this, ii));
//        }
    }
}
