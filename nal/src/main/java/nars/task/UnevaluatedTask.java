package nars.task;

import jcog.data.list.FasterList;
import nars.NAR;
import nars.Task;
import nars.task.util.InvalidTaskException;
import nars.term.Term;
import nars.truth.Truth;

/** task which bypasses the evaluation procedure on input.
 *  this is faster but also necessary when
 *  something is specified in the task that evaluation
 *  otherwise would un-do.
 */
public class UnevaluatedTask extends NALTask {

    public UnevaluatedTask(Term t, byte punct, Truth truth, long creation, long start, long end, long[] stamp) {
        super(t, punct, truth, creation, start, end,
                stamp /* TODO use an implementation which doenst need an array for this */);
    }

    public UnevaluatedTask(Term c, Task xx, Truth t) throws InvalidTaskException {
        super(c, xx.punc(), t, xx.creation(), xx.start(), xx.end(), xx.stamp());
    }

    @Override
    public ITask next(NAR n) {
        //HACK, for ensuring the operator invocation etc
        FasterList<ITask> q = new FasterList(1);
        preProcess(n, term(), q);
        assert(q.size()==1);
        return q.get(0);
    }
}
