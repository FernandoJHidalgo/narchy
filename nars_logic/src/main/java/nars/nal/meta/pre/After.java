//package nars.nal.meta.pre;
//
//import nars.Premise;
//import nars.nal.PremiseMatch;
//import nars.nal.meta.AtomicBooleanCondition;
//import nars.task.Temporal;
//
///**
// * After(%X,%Y) Means that
// * %X is after %Y
// * TODO use less confusing terminology and order convention
// */
//public class After extends AtomicBooleanCondition<PremiseMatch> {
//
//    public static final After the = new After();
//
//    protected After() {
//    }
//
//    @Override
//    public String toString() {
//        return "After";
//    }
//
//    @Override
//    public boolean booleanValueOf(PremiseMatch m) {
//        Premise premise = m.premise;
//        if (premise.isEvent()) {
//            //measured from belief to task, so it's relative to task
//            int tDelta = -((Temporal) premise.getTask()).tDelta((Temporal) premise.getBelief());
//            m.tDelta.set(tDelta);
//            return true;
//        }
//        return false;
//    }
// }
