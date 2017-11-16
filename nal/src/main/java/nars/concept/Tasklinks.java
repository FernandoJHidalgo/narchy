package nars.concept;

import jcog.bag.Bag;
import jcog.pri.PLinkUntilDeleted;
import jcog.pri.PriReference;
import jcog.pri.Prioritized;
import nars.NAR;
import nars.Task;
import org.apache.commons.lang3.mutable.MutableFloat;

import java.util.Collection;

public class Tasklinks {

    public static void linkTask(Task t, Concept cc, NAR nar) {
        float p = t.pri();
        if (p == p)
            linkTask(t, p, cc, nar);
    }

    public static void linkTask(Task t, float activationApplied, Concept cc, NAR nar) {


        Bag<Task, PriReference<Task>> tl = cc.tasklinks();


        MutableFloat overflow = new MutableFloat();
        tl.put(
                new PLinkUntilDeleted<>(t, activationApplied), overflow
                //new PLink<>(t, activationApplied), overflow
        );

        activationApplied -= overflow.floatValue();

        if (activationApplied >= Prioritized.EPSILON_VISIBLE) {
            nar.eventTask.emit(t);
        }

        float conceptActivation = activationApplied * nar.evaluate(t.cause());

        nar.activate(cc, conceptActivation);

        if (conceptActivation > 0)
            nar.emotion.onActivate(t, conceptActivation, cc, nar);
    }

    public static void linkTask(Task task, Collection<Concept> targets) {
        int numSubs = targets.size();
        if (numSubs == 0)
            return;

        float tfa = task.priElseZero();
        float tfaEach = tfa / numSubs;


        for (Concept target : targets) {

            target.tasklinks().putAsync(
                    new PLinkUntilDeleted(task, tfaEach)
            );
//                target.termlinks().putAsync(
//                        new PLink(task.term(), tfaEach)
//                );


        }
    }
}
