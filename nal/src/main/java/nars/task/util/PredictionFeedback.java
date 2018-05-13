package nars.task.util;

import jcog.math.Longerval;
import nars.NAR;
import nars.Task;
import nars.task.signal.SignalTask;

public class PredictionFeedback {

    //final BeliefTable table;

    static final boolean delete =
            true;
            //false;

//    /**
//     * punish any held non-signal beliefs during the current signal task which has just been input.
//     * time which contradict this sensor reading, and reward those which it supports
//     */
//    static public void feedbackSignal(SignalTask x, BeliefTable table, NAR nar) {
//        if (x == null)
//            return;
//
//        int dur = nar.dur();
//        long predictionLimit = nar.time() - dur / 2;
//
//        long start = x.start();
//        long end = x.end();
//
//
//        float fThresh = nar.freqResolution.floatValue();
//
//        List<Task> trash = new FasterList<>(8);
//
//        ((DefaultBeliefTable) table).temporal.whileEach(start, end, (y)->{
//            //if (!(y instanceof SignalTask)) {
//            if (y.end() < predictionLimit)
//                trash.add(y);
//            //}
//            return true; //continue
//        });
//
//
//        //test evidences etc outside of critical section that would lock the RTreeBeliefTable
//        trash.forEach(y-> {
//            if (absorb(x, y, start, end, dur, fThresh, nar)) {
//                table.removeTask(y);
//            }
//        });
//
//    }




    /**
     * TODO handle stretched tasks
     */
    public static boolean absorbNonSignal(Task y, long seriesStart, long seriesEnd, NAR nar) {

        long end = y.end();
        if (end >= seriesEnd)
            return false; //dont absorb if at least part of the task predicts the future

        if (Longerval.intersectLength(y.start(), end, seriesStart, seriesEnd)!=-1) {
            //intersects with data, and does not predict past its future
            y.delete();
            return true;
        }

        return false;
//        long start = y.start();

//        return !table.series.whileEach(start, end, true, existing -> {
//            //TODO or if the cause is purely this Cause id (to include pure revisions of signal tasks)
//            if (existing instanceof SignalTask) {
//                if (absorb((SignalTask)existing, y))
//                    return false; //eliminated; done
//            }
//            return true; //continue
//        });

    }

//    private static boolean signalOrRevisedSignalAbsorbs(Task existing, Task y) {
//        if (existing instanceof SignalTask)
//            return true;
//        if (existing.isCyclic())
//            return false;
//        if (existing.confMax() < y.confMin() || existing.originality() < y.originality())
//            return false;
//
//        return true;
//
////        short[] cc = existing.cause();
////        int n = cc.length;
////        switch (n) {
////            case 0: return false;
////            case 1: return cc[0] == cause;
////            default:
////                return false;
//////                for (short x : cc)
//////                    if (x != cause)
//////                        return false;
//////                return true;
////        }
//    }

//    /** true if next is stronger than current */
//    private static float strength(Task x, long start, long end, int dur) {
//        return
//                (x.evi(start,dur)+x.evi(end,dur)) //sampled at start & end
//        ;
//    }





    /**
     * rewards/punishes the causes of this task,
     * then removes it in favor of a stronger sensor signal
     * returns whether the 'y' task was absorbed into 'x'
     */
    static boolean absorb(SignalTask x, Task y) {
        if (x.intersects(y)) {
            if (delete) {
                y.delete(/*fwd: x*/); //forward to the actual sensor reading
            }
            //MetaGoal.Accurate.learn(y.cause(), value, nar.causes);
            return true;
        }
        return false;

//        //maybe also factor originality to prefer input even if conf is lower but has more originality thus less chance for overlap
//        float yEvi = Revision.eviInteg(y, start, end, dur); //TODO cache either if possible
//        float xEvi = Revision.eviInteg(x, start, end, dur); //TODO cache either if possible
//
//        float error = Math.abs(x.freq() - y.freq());
//        float coherence;
//        if (error <= fThresh) {
//            coherence = +1;
//        } else {
//            coherence = -error;
//        }
//        float value = coherence * yEvi/(yEvi + xEvi);
//        if (Math.abs(value) > Float.MIN_NORMAL) {
//            MetaGoal.Accurate.learn(y.cause(), value, nar.causes);
//        }
//
//        if (delete) {
//            y.delete(/*fwd: x*/); //forward to the actual sensor reading
//            return true;
//        } else {
//            return false; //keep if correct and stronger
//        }
    }

//    private static float error(Task x, Task y, long start, long end, int dur) {
//        //maybe also factor originality to prefer input even if conf is lower but has more originality thus less chance for overlap
//
//        float yEvi = y.evi(start, end, dur);
//        float xEvi = x.evi(start, end, dur);
//
//
//        return Math.abs(x.freq() - y.freq());
//    }
}
