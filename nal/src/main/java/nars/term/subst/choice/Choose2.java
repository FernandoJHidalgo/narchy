package nars.term.subst.choice;

import nars.nal.meta.match.Ellipsis;
import nars.nal.meta.match.EllipsisMatch;
import nars.term.Term;
import nars.term.container.ShuffledSubterms;
import nars.term.container.TermContainer;
import nars.term.subst.FindSubst;
import nars.util.data.array.IntArrays;
import nars.util.math.Combinations;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;

/**
 * Created by me on 12/22/15.
 */
public class Choose2 extends Termutator {

    @NotNull
    final Combinations comb;
    @NotNull
    private final Term[] yFree;
    @NotNull
    private final Term[] x;
    @NotNull
    private final Ellipsis xEllipsis;
    @NotNull
    private final FindSubst f;
    @NotNull
    private final ShuffledSubterms yy;

    @NotNull
    @Override
    public String toString() {

        return "Choose2{" +
                "yFree=" + Arrays.toString(yFree) +
                ", xEllipsis=" + xEllipsis +
                ", x=" + x[0] + ',' + x[1] +
                '}';

    }

    public Choose2(@NotNull FindSubst f, @NotNull Ellipsis xEllipsis, @NotNull Collection<Term> x, @NotNull Collection<Term> yFreeSet) {
        super(xEllipsis);
        this.f = f;
        this.xEllipsis = xEllipsis;
        this.x = x.toArray(new Term[x.size()]);

        int yFreeSize = yFreeSet.size();
        this.yFree = yFreeSet.toArray(new Term[yFreeSize]);
        this.yy = new ShuffledSubterms(f.random, this.yFree);

        this.comb = new Combinations(yFreeSize, 2);
    }

    @Override
    public int getEstimatedPermutations() {
        return comb.getTotal()*2;
    }

    @Override
    public void run(FindSubst versioneds, Termutator[] chain, int current) {

        @NotNull Combinations ccc = this.comb;
        ccc.reset();

        boolean phase = true;

        int start = f.now();
        @NotNull ShuffledSubterms yy = this.yy;

        Term[] m = new Term[this.yy.size()-2];

        Ellipsis xEllipsis = this.xEllipsis;
        FindSubst f = this.f;
        Term[] x = this.x;

        int[] c = null;
        while (ccc.hasNext() || !phase) {

            c = phase ? ccc.next() : c;
            phase = !phase;

            int c0 = c[0];
            int c1 = c[1];
            IntArrays.reverse(c); //swap to try the reverse next iteration

            Term y1 = yy.term(c0);

            if (f.match(x[0], y1)) {

                Term y2 = yy.term(c1);

                if (f.match(x[1], y2) &&
                        f.putXY(xEllipsis, EllipsisMatch.match(TermContainer.except(yy, y1, y2, m)))) {

                    next(f, chain, current);
                }

            }

            f.revert(start);


        }

    }

}