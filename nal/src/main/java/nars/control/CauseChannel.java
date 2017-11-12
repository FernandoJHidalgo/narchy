package nars.control;

import com.google.common.collect.Streams;
import jcog.pri.Priority;
import nars.NAR;
import nars.task.ITask;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * metered and mixable extension of Cause base class
 */
public class CauseChannel<X extends Priority> extends Cause implements Consumer<X> {

    /**
     * linear gain control
     */
    public float preBias, preAmp = 1;

//    /** in-bound traffic statistics */
//    public final AtomicSummaryStatistics traffic = new AtomicSummaryStatistics();

    final Consumer<X> target;

    public CauseChannel(short id, Object idObj, Consumer<X> target) {
        super(id, idObj);
        this.target = target;
    }

    @Override
    public String toString() {
        return name.toString();
    }

    public final void input(Iterable<? extends X> xx) {
        input(xx.iterator());
    }

    public void input(Iterator<? extends X> xx) {
        xx.forEachRemaining(this);
    }

    public void input(Stream<? extends X> x) {
        x.forEach(this);
    }

    public void input(X... x) {

        for (X p : x)
            input(p);
    }

    public void input(X x) {
        if (x!=null && process(x))
            target.accept(x);
    }

    protected boolean process(X x) {

        float p = x.pri();
        if (p != p) return false;

        //traffic.accept(p);

        if (preBias != 0 || preAmp != 1) {
            x.setPri(preBias + p * preAmp);
        }
        return true;
    }

    @Override
    public void accept(@Nullable X x) {
        input(x);
    }

    public CauseChannel pre(float bias, float amplitude) {
        return preAmp(amplitude).preBias(bias);
    }

    public CauseChannel preBias(float bias) {
        this.preBias = bias;
        return this;
    }

    public CauseChannel preAmp(float amp) {
        this.preAmp = amp;
        return this;
    }

    public static class TaskChannel extends CauseChannel<ITask> {

        private final Predicate<ITask> each;
        private final NAR nar;

        public TaskChannel(NAR nar, short id, Object idObj, Consumer<ITask> each) {
            super(id, idObj, null);
            this.nar = nar;
            this.each = (ITask x)-> {
                if (x!=null && process(x)) {
                    each.accept(x);
                    return true;
                }
                return false;
            };
        }

        @Override
        public void input(ITask x) {
            input(Stream.of(x));
        }

        @Override
        public void input(ITask... x) {
            input(Stream.of(x));
        }

        @Override
        public void input(Iterator<? extends ITask> xx) {
            input(Streams.stream(xx));
        }

        @Override
        public void input(Stream<? extends ITask> x) {
            nar.input(x.filter(each));
        }
    }
}
