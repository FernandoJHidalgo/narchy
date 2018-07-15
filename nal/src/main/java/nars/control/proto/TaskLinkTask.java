package nars.control.proto;

import jcog.Util;
import jcog.pri.ScalarValue;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.link.Tasklinks;
import nars.task.AbstractTask;
import nars.task.ITask;
import org.jetbrains.annotations.Nullable;

public class TaskLinkTask extends AbstractTask {

    private final Task task;
    private final Concept concept;
    private final float pri;

    public TaskLinkTask(Task task) {
        this(task, task.priElseZero());
    }

    public TaskLinkTask(Task task, float pri) {
        this(task, pri, null);
    }

    public TaskLinkTask(Task task, @Nullable Concept c) {
        this(task, task.priElseZero(), c);

    }

    public TaskLinkTask(Task task, float pri, @Nullable Concept c) {
        this.task = task;
        this.concept = c;

        /** non-zero for safety */
        this.pri = Math.max(Util.notNaN(pri), ScalarValue.EPSILON);
    }

    @Override
    public ITask next(NAR n) {

        Concept c = this.concept;
        if (c == null) {
            c = task.concept(n, true);
            if (c == null)
                return null;
        }

        n.activate(c, pri);

        Tasklinks.linkTask(task, pri, c, n);

        n.emotion.onInput(task, n);
        n.emotion.onActivate(task, pri);

        return null;
    }

    @Override
    public String toString() {
        return "TaskLink(" + task + ",$" + pri + ")";
    }

}
