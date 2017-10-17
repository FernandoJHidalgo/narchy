package jcog.pri;


import jcog.Texts;
import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

import static jcog.Util.lerp;
import static jcog.Util.sum;

/**
 * something which has a priority floating point value
 *      stores priority with 32-bit float precision
 *      restricted to 0..1.0 range
 *      NaN means it is 'deleted' which is a valid and testable state
 */
public interface Prioritized extends Deleteable {

    /**
     * a value in range 0..1.0 inclusive.
     * if the value is NaN, then it means this has been deleted
     */
    float pri();

    /**
     * common instance for a 'Deleted budget'.
     */
    Prioritized Deleted = new PriRO(Float.NaN);
    /**
     * common instance for a 'full budget'.
     */
    Prioritized One = new PriRO(1f);
    /**
     * common instance for a 'half budget'.
     */
    Prioritized Half = new PriRO(0.5f);
    /**
     * common instance for a 'zero budget'.
     */
    Prioritized Zero = new PriRO(0);
    /**
     * default minimum difference necessary to indicate a significant modification in budget float number components
     */
    float EPSILON = 0.0001f;

//    /**
//     * decending order (highest first)
//     */
//    Comparator<Prioritized> IdentityComparator = (Prioritized a, Prioritized b) -> {
//        if (a == b) return 0;
//
//        float ap = a.priElseNeg1();
//        float bp = b.priElseNeg1();
//
//        int q = Float.compare(bp, ap);
//        if (q == 0) {
//            //if still not equal, then use system identiy
//            return Integer.compare(System.identityHashCode(a), System.identityHashCode(b));
//        }
//        return q;
//    };

    static String toString(Prioritized b) {
        return toStringBuilder(null, Texts.n4(b.pri())).toString();
    }

    @NotNull
    static StringBuilder toStringBuilder(@Nullable StringBuilder sb, String priorityString) {
        int c = 1 + priorityString.length();
        if (sb == null)
            sb = new StringBuilder(c);
        else {
            sb.ensureCapacity(c);
        }

        sb.append('$')
                .append(priorityString);
        //.append(Op.BUDGET_VALUE_MARK);

        return sb;
    }

    @NotNull
    static Ansi.Color budgetSummaryColor(@NotNull Prioritized tv) {
        int s = (int) Math.floor(tv.priElseZero() * 5);
        switch (s) {
            default:
                return Ansi.Color.DEFAULT;

            case 1:
                return Ansi.Color.MAGENTA;
            case 2:
                return Ansi.Color.GREEN;
            case 3:
                return Ansi.Color.YELLOW;
            case 4:
                return Ansi.Color.RED;

        }
    }

    static <X extends Priority> void normalize(X[] xx, float target) {
        int l = xx.length;
        assert (target == target);
        assert (l > 0);

        float ss = sum(Prioritized::priElseZero, xx);
        if (ss <= Pri.EPSILON)
            return;

        float factor = target / ss;

        for (X x : xx)
            x.priMult(factor);

    }

    default float priElse(float valueIfDeleted) {
        float p = pri();
        return p == p ? p : valueIfDeleted;
    }


    default float priElseZero() {
        float p = pri();
        return p == p ? p : 0;
        //return priElseZero();
    }
    default float priElseNeg1() {
        float p = pri();
        return p == p ? p : -1;
        //return priSafe(-1);
    }

    @Override
    default boolean isDeleted() {
        float p = pri();
        return p!=p; //fast NaN check
    }

    @NotNull Appendable toBudgetStringExternal();

    @NotNull StringBuilder toBudgetStringExternal(StringBuilder sb);

    @NotNull String toBudgetString();

    @NotNull String getBudgetString();






//    static void normalizePriSum(@NotNull Iterable<? extends Prioritized> l, float total) {
//
//        float priSum = Prioritized.priSum(l);
//        float mult = total / priSum;
//        for (Prioritized b : l) {
//            b.priMult(mult);
//        }
//
//    }
//
//    /**
//     * randomly selects an item from a collection, weighted by priority
//     */
//    static <P extends Prioritized> P selectRandomByPriority(@NotNull NAR memory, @NotNull Iterable<P> c) {
//        float totalPriority = priSum(c);
//
//        if (totalPriority == 0) return null;
//
//        float r = memory.random.nextFloat() * totalPriority;
//
//        P s = null;
//        for (P i : c) {
//            s = i;
//            r -= s.priElseZero();
//            if (r < 0)
//                return s;
//        }
//
//        return s;
//
//    }

}
