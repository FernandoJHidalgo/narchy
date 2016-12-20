package nars.util;

import jcog.Util;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import org.apache.commons.math3.stat.Frequency;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by me on 10/31/16.
 */
public class TaskStatistics {
    final AtomicInteger i = new AtomicInteger(0);

    final Frequency clazz = new Frequency();
    final Frequency volume = new Frequency();
    final Frequency rootOp = new Frequency();
    final Frequency punc = new Frequency();
    final Frequency eviLength = new Frequency();

    final Frequency freq = new Frequency();
    final Frequency conf = new Frequency();
    final Frequency pri = new Frequency();
    final Frequency qua = new Frequency();

    public TaskStatistics add(NAR nar) {
        nar.tasks.forEach(this::add);
        return this;
    }

    public TaskStatistics add(Concept c) {
        c.forEachTask(true, true, true, true, this::add);
        return this;
    }

    public TaskStatistics add(Iterable<Task> c) {
        c.forEach(this::add);
        return this;
    }

    public void add(Task t) {

        if (t.isDeleted())
            return;

        i.incrementAndGet();
        //complexity.addValue(c.complexity());
        volume.addValue(t.volume());
        rootOp.addValue(t.op());
        clazz.addValue(t.getClass().toString());
        punc.addValue(t.punc());
        eviLength.addValue(t.evidence().length);

        if (t.isBeliefOrGoal()) {
            freq.addValue(Util.round(t.freq(), 0.1f));
            conf.addValue(Util.round(t.conf(), 0.1f));
        }
        pri.addValue(Util.round(t.pri(), 0.1f));
        qua.addValue(Util.round(t.qua(), 0.1f));

    }

    public void print(PrintStream out) {
        out.println("-------------------------------------------------");
        out.println("Total Tasks:\n" + i.get());

        out.println("\npunc:\n" + punc);
        out.println("\nrootOp:\n" + rootOp);
        out.println("\nvolume:\n" + volume);
        out.println("\nevidence:\n" + eviLength);
        out.println("\nclass:\n" + clazz);

        out.println("\nfreq:\n" + freq);
        out.println("\nconf:\n" + conf);
        out.println("\npri:\n" + pri);
        out.println("\nqua:\n" + qua);

    }


    public void print() {
        print(System.out);
    }
}
