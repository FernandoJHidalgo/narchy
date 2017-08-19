package nars.time;

import com.google.common.collect.MinMaxPriorityQueue;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.MoreExecutors;
import com.netflix.servo.util.Clock;
import nars.NAR;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Time state
 */
public abstract class Time implements Clock, Serializable {

    final MinMaxPriorityQueue<LongObjectPair<Runnable>> scheduled =
            MinMaxPriorityQueue.orderedBy((LongObjectPair<Runnable> a, LongObjectPair<Runnable> b) -> {
                int c = Longs.compare(a.getOne(), b.getOne());
                if (c == 0)
                    return Integer.compare(System.identityHashCode(a.getTwo()), System.identityHashCode(b.getTwo())); //maintains uniqueness in case they occupy the same time
                else
                    return c;
            }).create();

    //Timer real = new Timer("Realtime");

    /**
     * called when memory reset
     */
    public abstract void clear();

    /**
     * returns the current time, as measured in units determined by this clock
     */
    @Override
    public abstract long now();

    /**
     * returns a new stamp evidence id
     */
    public abstract long nextStamp();

    /**
     * called each cycle
     */
    protected abstract void update();



    /**
     * the default duration applied to input tasks that do not specify one
     * >0
     */
    public abstract int dur();

    /**
     * set the duration, return this
     *
     * @param d, d>0
     */
    public abstract Time dur(int d);

    public long[] nextInputStamp() {
        return new long[]{nextStamp()};
    }


    public void at(long whenOrAfter, Runnable then) {
        synchronized (scheduled) {
            scheduled.add(PrimitiveTuples.pair(whenOrAfter, then));
        }
    }

    public void exeScheduled(Executor exe) {

        List<Runnable> pending = new LinkedList();

        synchronized (scheduled) {
            LongObjectPair<Runnable> next;
            long now = now();
            while ((next = scheduled.peek()) != null) {
                if (next.getOne() <= now) {
                    scheduled.poll();
                    pending.add(next.getTwo());
                } else {
                    break; //wait till another time
                }
            }
        }

        //incase execution is synchronous, do it outside the synchronized block here to prevent deadlock.
        pending.forEach(exe::execute);
    }

    public void cycle(NAR n) {
        update();
        exeScheduled(n.exe);
    }


    /** flushes the pending work queued for the current time */
    public synchronized void synch() {
        exeScheduled(MoreExecutors.directExecutor());
    }

}
