package nars.task.signal;

import nars.NAR;
import nars.concept.Concept;
import nars.link.Tasklinks;
import nars.table.DefaultBeliefTable;
import nars.term.Term;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;
import static nars.time.Tense.XTERNAL;

/**
 * TODO implement Task directly avoiding redudant fields that this overrides
 */
public class TruthletTask extends SignalTask {

    public Truthlet truthlet;

    public TruthletTask(Term t, byte punct, Truthlet truth, NAR n) {
        this(t, punct, truth, n.time(), n.time.nextStamp());
    }

    public TruthletTask(Term t, byte punct, Truthlet truth, long creation, long stamp) {
        super(t, punct, truth, creation, XTERNAL, XTERNAL, stamp);
        assert (punct == BELIEF || punct == GOAL);
        this.truthlet = truth;
    }


    /**
     * should be called only from the stretch procedure
     */
    void updateTime(Concept c, long nextStart, long nextEnd) {
        if (nextStart == start() && nextEnd == end())
            return; //no change
        else
            update(c, (tt)-> tt.truthlet.setTime(nextStart, nextEnd));
    }

    public void update(NAR n, Consumer<TruthletTask> t) {
        Concept c = concept(n, true);
        if (c != null) {

            update(c, t);

        }
    }

    public void update(Concept c, Consumer<TruthletTask> t) {
        ((DefaultBeliefTable)c.table(punc)).temporal.update(this, ()->{
            t.accept(TruthletTask.this);
        });
    }


    @Override
    public long start() {
        return truthlet.start();
    }

    @Override
    public long end() {
        return truthlet.end();
    }


    public float eviInteg() {
        long s = start();
        long e = end();

        long dt = Math.max(1, e - s);
        float es = evi(s, 1);
        if (e!=s) {
            float ee = evi(e, 1);
            return dt * (es + ee)/2f /* linear trapezoid */;
        } else {
            return dt * es /* point sample */;
        }
    }

    @Override
    public boolean intersects(long start, long end) {
        return truthlet.intersects(start, end);
    }

    @Nullable
    public final Truth truth(long when, long dur, float minConf) {
        Truth t = truth(when);
        if (t != null) {
            if (minConf == 0 || t.conf() >= minConf)
                return t;
        }
        return null;
    }
//    @Override
//    public float freq() {
//        return 0.5f;
//    }
//
//    @Override
//    public float conf() {
//        return super.conf();
//    }
//
//    @Override
//    public float coordF(boolean maxOrMin, int dimension) {
//        switch (dimension) {
//            case 0:
//                return maxOrMin ? end() : start();
//            case 1:
//            case 2:
//                return maxOrMin ? 0 : 1; //entire range, by default
//            default:
//                throw new UnsupportedOperationException();
//        }
//    }

    @Override
    public @Nullable Truth truth(long when, long dur) {
        return truth(when);
    }


    public @Nullable Truth truth(long when) {
        float[] tl = truthlet.truth(when);
        float f = tl[0];
        if (f!=f)
            return null;
        float e = tl[1];
        if (e > 0)
            return new PreciseTruth(f, e /* evi */, false);
        else
            return null;
    }

    @Override
    public float evi(long when, long dur) {
        return truthlet.truth(when)[1];
    }

    public void updateEnd(Concept c, long nextEnd) {
        updateTime(c, start(), nextEnd);
    }

    public void truth(Truthlet newTruthlet, boolean relink, NAR n) {
        if (truthlet!=newTruthlet) {
            Concept c = concept(n, true);
            if (c != null) {
                update(c, (tt) -> {
                    tt.truthlet = newTruthlet;
                    if (relink) {
                        Tasklinks.linkTask(this, pri, c, n);
                    }
                });
            }
        }
    }

}
