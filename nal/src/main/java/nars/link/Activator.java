package nars.link;

import jcog.pri.OverflowDistributor;
import nars.NAR;
import nars.concept.Concept;
import nars.task.AbstractTask;
import nars.task.ITask;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;

/** accumulates/buffers/collates a stream of concept activations and termlinkages
 *  to be applied in a batch as a batch
 *
 *  this task instance represents the drainage operation
 *  which is recyclable and will be recycled, and is thread-safe for simultaneous drainage from multiple threads.
 *
 *  it can be drained while being populated from different threads.
 *
 *  TODO use non-UnitPri entries and then allow this to determine a global amplitude factor via adaptive dynamic range compression of priority
 * */
public class Activator extends AbstractTask {


    /** pending concept activation collation */
    final ConcurrentHashMap<Term, Activate> concepts = new ConcurrentHashMap(1024);

//    /** pending termlinking collation */
//    final ConcurrentHashMap<TermLinkage, TermLinkage> termlink = new ConcurrentHashMap(1024);
    private final boolean deferredOrInline;

    public Activator(boolean deferredOrInline) {
        this.deferredOrInline = deferredOrInline;
    }

//    /** implements a plus merge (with collected refund)
//     * TODO detect priority clipping (@1.0) statistic
//     * */
//    public void linkPlus(Concept source, Term target, float pri, @Nullable NumberX refund) {
//        float overflow = termlink.computeIfAbsent(new TermLinkage(source, target), (cc)-> cc)
//                .priAddOverflow(pri);
//        if (overflow > Float.MIN_NORMAL && refund!=null)
//            refund.add(overflow);
//    }

    public boolean isEmpty() {
        return concepts.isEmpty(); /* && termlink.isEmpty();*/
    }

    public Concept activate(Termed tgtTerm, float pri, NAR nar, @Nullable OverflowDistributor<Concept> overflow) {

        @Nullable Concept x = nar.concept(tgtTerm, true);
        if (x == null)
            return null;
        return activate(x, pri, overflow);
    }

    public final Concept activate(Concept x, float pri) {
        return activate(x, pri, null);
    }

    public Concept activate(Concept x, float pri, @Nullable OverflowDistributor<Concept> overflow) {
        Activate a = concepts.computeIfAbsent(x.term(), t -> new Activate(x));

        if (overflow!=null)
            overflow.overflow(x, a, pri);

        return x;
    }

    public void commit(NAR n) {
        //if (!isEmpty()) {
            //deferred
        if (deferredOrInline) {
            n.input(this);
        } else {
            //inline
            ITask.run(this, n);
        }

    }

//    private static final class TermLinkage extends UnitPri implements Comparable<TermLinkage> {
//
//        public final static Comparator<TermLinkage> preciseComparator = Comparator
//            .comparing((TermLinkage x)->x.concept.term())
//            .thenComparingDouble((TermLinkage x)->-x.pri()) //descending
//            .thenComparingInt((TermLinkage x)->x.hashTarget) //at this point the order doesnt matter so first decide by hash
//            .thenComparing((TermLinkage x)->x.target);
//
//        /** fast and approximately same semantics of the sort as the preciseComparator:
//         *     soruce concept -> pri -> target
//         */
//        public final static Comparator<TermLinkage> sloppyComparator = Comparator
//                .comparingInt((TermLinkage x)->x.hashSource)
//                .thenComparingDouble((TermLinkage x)->-x.pri()) //descending
//                .thenComparingInt((TermLinkage x)->x.hashTarget) //at this point the order doesnt matter so first decide by hash
//                .thenComparing(System::identityHashCode);
//
//        public final Concept concept;
//        public final Term target;
//        public final int hashSource, hashTarget;
//
//        TermLinkage(Concept source, Term target) {
//            this.concept = source;
//            this.target = target;
//            this.hashSource = source.hashCode();
//            this.hashTarget = target.hashCode();
//        }
//
//        @Override
//        public int hashCode() {
//            return hashSource ^ hashTarget;
//        }
//
//        @Override
//        public boolean equals(Object obj) {
//            if (this == obj) return true;
//            TermLinkage x = (TermLinkage) obj;
//            return x.hashSource == hashSource && x.hashTarget == hashTarget && x.target.equals(target) && x.concept.equals(concept);
//
//        }
//
//        @Override
//        public String toString() {
//            return "termlink(" + concept + ',' + target + ',' + pri() + ')';
//        }
//
//
//
//        @Override
//        public int compareTo(Activator.TermLinkage x) {
//            //return comparator.compare(this, x);
//            return sloppyComparator.compare(this, x);
//        }
//    }


    @Override
    public ITask next(NAR nar) {


        concepts.values().removeIf(a -> {
            nar.eventActivate.emit(a.clone());
            return true;
        });

//        int n = termlink.size();
//        if (n > 0) {
//            //drain at most n items from the concurrent map to a temporary list, sort it,
//            //then insert PLinks into the concept termlinks bag as they will be sorted into sequences
//            //of the same concept.
//            SortedList<TermLinkage> l = drainageBuffer(n);
//
//
//            Iterator<TermLinkage> ii = termlink.keySet().iterator();
//            while (ii.hasNext() && n-- > 0) {
//                TermLinkage x = ii.next();
//                ii.remove();
//
//                l.add(x);
//
//            }
//
//
//            //l.clearReallocate(1024, 8);
//            l.clear();
//        }

        return null;
    }

//    final static ThreadLocal<SortedList<TermLinkage>> drainageBuffers = ThreadLocal.withInitial(()->new SortedList<>(16));
//
//    /** provide a list to be used as a pre-insertion drainage buffer */
//    protected static SortedList<TermLinkage> drainageBuffer(int n) {
//        SortedList<TermLinkage> b = drainageBuffers.get();
//        b.ensureCapacity(n);
//        return b;
//    }


}
