package nars.time;

import com.conversantmedia.util.concurrent.ConcurrentQueue;
import com.conversantmedia.util.concurrent.MultithreadConcurrentQueue;
import com.netflix.servo.util.Clock;
import jcog.list.FasterList;
import nars.NAR;
import nars.task.NativeTask.SchedTask;
import org.jetbrains.annotations.Nullable;

import javax.measure.Quantity;
import java.io.Serializable;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Time state
 */
public abstract class Time implements Clock, Serializable {



    final AtomicLong scheduledNext = new AtomicLong(Long.MIN_VALUE);

    final static int MAX_PRE_SCHED = 4 * 1024;
    final ConcurrentQueue<SchedTask> preSched =
            //new DisruptorBlockingQueue<>(MAX_QUEUED);
            new MultithreadConcurrentQueue<>(MAX_PRE_SCHED);

    //new ArrayBlockingQueue<>(MAX_QUEUED);
    //final ConcurrentQueue<SchedTask> pendingSched =
    //new MultithreadConcurrentQueue(MAX_QUEUED);

    final PriorityQueue<SchedTask> scheduled =
            //final MinMaxPriorityQueue<SchedTask> scheduled =
            //MinMaxPriorityQueue.orderedBy((SchedTask a, SchedTask b) -> {
            new PriorityQueue<>();


    public void clear(NAR n) {
        synchronized(scheduled) {
            synch(n);
            //pendingSched.clear();
            scheduled.clear();
        }
    }

    /**
     * called when memory reset
     */
    public abstract void reset();


    /**
     * time elapsed since last cycle
     */
    public abstract long sinceLast();

    /**
     * returns a new stamp evidence id
     */
    public abstract long nextStamp();


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


    public void runAt(long whenOrAfter, Runnable then) {
        runAt(new SchedTask(whenOrAfter, then));
    }

    public void runAt(long whenOrAfter, Consumer<NAR> then) {
        runAt(new SchedTask(whenOrAfter, then));
    }

    private void runAt(SchedTask event) {

        //try {
            if (!preSched.offer(event)) {
                throw new RuntimeException(this + " overflow"); //TODO handle better
            }
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }

        long w = event.when;
        scheduledNext.updateAndGet(z -> Math.min(z, w));
    }


    @Nullable
    private List<SchedTask> exeScheduled() {

        //        now = now();
//        SchedTask firstQuick = scheduled.peek(); //it's safe to call this outside synchronized block for speed
//        if (firstQuick == null || firstQuick.when > now)
//            return null; //too soon for the next one


        long now = now();
        long nextScheduled = scheduledNext.get();
        if ((now < nextScheduled) || !(scheduledNext.compareAndSet(nextScheduled, Long.MAX_VALUE)))
            return null;


        try  {

            //preSched.drainTo(scheduled);

            List<SchedTask> pending =
                    //new LinkedList();
                    new FasterList(8);

            int s = 0;
            SchedTask p;
            while ((p = preSched.poll())!=null && s++ <= MAX_PRE_SCHED) { //limit by MAX in case the preSched continues filling while this is being processed
                if (p.when <= now)
                    pending.add(p); //bypass the queue
                else
                    scheduled.offer(p);
            }



            SchedTask next;
            while (((next = scheduled.peek()) != null) && (next.when <= now)) {
                SchedTask actualNext = scheduled.poll();
                assert (next == actualNext);
                pending.add(next);
            }

            long nextNextWhen = next!=null ? next.when : Long.MAX_VALUE; //if emptied the priority queue, delay indefintely. otherwise delay until the time of the next item not dequeued now
            scheduledNext.updateAndGet(z -> Math.min(z, nextNextWhen ));

            return pending.isEmpty() ? null : pending;

        } catch (Throwable t) {
            //logger.error() //TODO
            t.printStackTrace();
            scheduledNext.set(now); //try again immediately
            return null;
        }


    }

    public void cycle(NAR n) {
        synch(n);
    }


    /**
     * flushes the pending work queued for the current time
     */
    public void synch(NAR n) {
        List<SchedTask> l = exeScheduled();
        if (l!=null)
            n.input(l);
    }


    /** returns a string containing the time elapsed/to the given time */
    public String durationToString(long target) {
        long now = now();
        return durationString(now - target);
    }

    protected abstract String durationString(long time);

    public long toCycles(Quantity q) {
        throw new UnsupportedOperationException("Only in RealTime implementations");
    }
}
