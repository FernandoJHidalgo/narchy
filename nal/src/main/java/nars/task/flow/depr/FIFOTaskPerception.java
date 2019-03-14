//package nars.task.flow;
//
//import nars.NAR;
//import nars.task.Task;
//import org.jetbrains.annotations.NotNull;
//
//import java.util.ArrayDeque;
//import java.util.Deque;
//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.function.Consumer;
//import java.util.function.Predicate;
//
///**
// * accumulates a buffer of iasks which can be delivered at a specific rate.
// * <p>
// * consists of 2 buffers which are sampled in some policy each cycle
// * <p>
// * "input" - a dequeue in which input tasks are appended
// * in the order they are received
// * <p>
// * "newTasks" - a priority buffer, emptied in batches,
// * in which derived tasks and other feedback accumulate
// * <p>
// * Sub-interfaces
// * <p>
// * Storage
// * <p>
// * Delivery (procedure for cyclical input policy
// */
//public class FIFOTaskPerception extends TaskPerception {
//
//    /**
//     * max # of inputs to perceive per cycle; -1 means unlimited (attempts to drains input to empty each cycle)
//     */
//    public final AtomicInteger inputsPerCycleMax = new AtomicInteger(1);
//
//    /* ?? public interface Storage { void put(Task t); }*/
//
//    //public final ItemAccumulator<Task> newTasks;
//
//    public final Deque<Task> buffer = new ArrayDeque();
//
//    /**
//     * determines if content can enter
//     */
//    protected final Predicate<Task> filter;
//
//    public FIFOTaskPerception(@NotNull NAR nar, Predicate<Task> filter, Consumer<Task[]> receiver) {
//        super(nar, receiver);
//        this.filter = filter;
//    }
//
//    public FIFOTaskPerception(@NotNull NAR nar, Predicate<Task> filter) {
//        super(nar);
//        this.filter = filter;
//    }
//    public FIFOTaskPerception(@NotNull NAR nar) {
//        this(nar, null);
//    }
//
////    @Override
////    public void forEach(@NotNull Consumer<? super Task> each) {
////        buffer.forEach(each);
////    }
//
//    @Override
//    public void accept(@NotNull Task t) {
//        Predicate<Task> f = this.filter;
//        if (f == null || f.test(t)) {
//
//            if (t.isDeleted())
//                throw new RuntimeException("task Deleted");
//
//            buffer.add(t);
//        }
//    }
//
//    @Override
//    public void clear() {
//        buffer.clear();
//    }
//
//
//
//    //        @Override
////        public void accept(Task t) {
////            if (t.isInput())
////                percepts.add(t);
////            else {
//////                if (t.getParentTask() != null && t.getParentTask().getTerm().equals(t.getTerm())) {
//////                } else {
////                    newTasks.add(t);
////                }
////            }
////        }
//
//    /** sends the next batch of tasks to the receiver */
//    @Override
//    public void nextFrame(@NotNull Consumer<Task[]> receiver) {
//
//
//        int s = buffer.size();
//        int n = Math.min(s, inputsPerCycleMax.get()); //counts down successful sends
//        int r = n; //actual cycles counted
//
//
//        Task[] tt = new Task[1];
//
//        //n will be equal to or greater than r
//        for (; n > 0 && r > 0; r--) {
//            Task t = buffer.removeFirst();
//
//            if (t.isDeleted()) {
//                //the task became Deleted while this was in the buffer. no need to repeat Memory.removed
//                continue;
//            }
//
//            tt[0] = t;
//            receiver.accept( tt ); //inefficient
//            n--;
//        }
//
//    }
//
////        protected void runNewTasks() {
////            runNewTasks(newTasks.size()); //all
////        }
////
////        protected void runNewTasks(int max) {
////
////            int numNewTasks = Math.min(max, newTasks.size());
////            if (numNewTasks == 0) return;
////
////            //queueNewTasks();
////
////            for (int n = newTasks.size() - 1; n >= 0; n--) {
////                Task highest = newTasks.removeHighest();
////                if (highest == null) break;
////                if (highest.isDeleted()) continue;
////
////                run(highest);
////            }
////            //commitNewTasks();
////        }
//
//
//}