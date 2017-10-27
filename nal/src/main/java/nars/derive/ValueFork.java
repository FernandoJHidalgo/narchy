package nars.derive;

import jcog.Util;
import nars.$;
import nars.control.Cause;
import nars.control.Derivation;
import nars.derive.op.UnifyTerm;
import org.eclipse.collections.api.block.function.primitive.ByteToFloatFunction;
import org.roaringbitmap.RoaringBitmap;

import java.util.List;
import java.util.function.Function;

/**
 * AIKR value-determined fork (aka choice-point)
 */
public class ValueFork extends Fork {

    final Taskify[] conc;
//    final ValueCache values;

    /**
     * the term which a derivation will encounter signaling
     * that it may continue here after evaluating it among other choices
     */
    public final ValueBranch valueBranch;
    private final RoaringBitmap downstream;

    /**
     * the causes that this is responsible for, ie. those that may be caused by this
     */
    public final Cause[] causes;


    public static ValueFork the(PrediTerm[] branches, List<ValueFork> choices, RoaringBitmap downstream) {
        int branchID = choices.size();
        ValueBranch valueBranch = new ValueBranch(branchID, downstream);
        ValueFork v = new ValueFork(branches, valueBranch, downstream);
        choices.add(v);
        return v;
    }

    protected ValueFork(PrediTerm[] branches, ValueBranch branch, RoaringBitmap downstream) {
        super(branches);

        this.valueBranch = branch;
        this.downstream = downstream;

        conc = new Taskify[branches.length];
        int n = 0;
        for (PrediTerm b : branches) {
            PrediTerm fin = AndCondition.last(b);
            UnifyTerm.UnifySubtermThenConclude u = (UnifyTerm.UnifySubtermThenConclude) fin;
            conc[n++] = ((Taskify) (AndCondition.last(u.eachMatch)));
        }

        causes = Util.map(c -> c.channel, new Cause[n], conc);
//        values = new ValueCache(c -> c::value, causes);
    }

    @Override
    public PrediTerm<Derivation> transform(Function<PrediTerm<Derivation>, PrediTerm<Derivation>> f) {
        return new ValueFork(PrediTerm.transform(f, branches), valueBranch, downstream);
    }

    /**
     * The number of distinct byte values.
     */
    private static final int NUM_BYTE_VALUES = 1 << 8;

    /**
     * modified from jdk9 source:
     * Sorts the specified range of the array.
     *
     * @param a     the array to be sorted
     * @param left  the index of the first element, inclusive, to be sorted
     * @param right the index of the last element, inclusive, to be sorted
     */
    static void sort(byte[] a, int left, int right, ByteToFloatFunction v) {
//        // Use counting sort on large arrays
//        if (right - left > COUNTING_SORT_THRESHOLD_FOR_BYTE) {
//            int[] count = new int[NUM_BYTE_VALUES];
//
//            for (int i = left - 1; ++i <= right;
//                 count[a[i] - Byte.MIN_VALUE]++
//                    )
//                ;
//            for (int i = NUM_BYTE_VALUES, k = right + 1; k > left; ) {
//                while (count[--i] == 0) ;
//                byte value = (byte) (i + Byte.MIN_VALUE);
//                int s = count[i];
//
//                do {
//                    a[--k] = value;
//                } while (--s > 0);
//            }
//        } else { // Use insertion sort on small arrays
        for (int i = left, j = i; i < right; j = ++i) {
            byte ai = a[i + 1];
            while (v.valueOf(ai) < v.valueOf(a[j])) {
                a[j + 1] = a[j];
                if (j-- == left) {
                    break;
                }
            }
            a[j + 1] = ai;
        }
//        }
    }


    @Override
    public boolean test(Derivation d) {

        int before = d.now();

        int branches = this.branches.length;
        if (branches == 1) {
            this.branches[0].test(d);
            return d.revertLive(before);
        } else {

            final boolean[] continued = {true};
            Util.selectRouletteUnique(branches, d.random, branches, (i) -> causes[i].amp(), (b) -> {

                this.branches[b].test(d);

                if (!d.revertLive(before)) {
                    continued[0] = false;
                    return false;
                }
                return true;
            });
            return continued[0];

            //RANDOM
            //ByteShuffler b = d.shuffler;
            //byte[] order = b.shuffle(d.random, branches, true); //must get a copy because recursion will re-use the shuffler's internal array


//            //BY VALUE
//            byte[] order = new byte[branches];
//            for (byte i = 0; i < branches; i++)
//                order[i] = i;
            //sort(order, 0, branches-1, a -> -causes[a].value());

//            int ttl = d.ttl;

//            for (int i = 0; i < branches; i++) {
//
////                int subTTL = Math.round(ttl * (value[i] / valueTotal));
////                int reserve = d.getAndSetTTL(subTTL) - subTTL;
//
//                this.branches[order[i]].test(d);
//
////                d.addTTL(reserve);
//
//                if (!d.revertLive(before))
//                    return false;
//            }
//            return true;
        }
    }


    /**
     * remembers the possiblity of a choice which "can" be pursued
     * (ie. according to value rank)
     */
    static class ValueBranch extends AbstractPred<Derivation> {

        public final int id;

        /**
         * global cause channel ID's that this leads to
         */
        private final RoaringBitmap downstream;

        protected ValueBranch(int id, RoaringBitmap downstream) {
            super($.func("can", /*$.the(id),*/ $.sete(downstream)));

            this.id = id;
            this.downstream = downstream;
        }

        @Override
        public boolean test(Derivation derivation) {
            derivation.preToPost.add(id);
            return true;
        }
    }


//        @Override
//        public String toString() {
//            return id + "(to=" + cache.length + ")";
//        }
}
