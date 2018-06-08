package nars.unify.mutate;

import nars.$;
import nars.subterm.ShuffledSubterms;
import nars.subterm.Subterms;
import nars.term.atom.Atom;
import nars.unify.Unify;

/**
 * Created by me on 12/22/15.
 */
public final class CommutivePermutations extends Termutator.AbstractTermutator {


    private final Subterms y;
    private final Subterms x;

    final static Atom COMMUTIVE_PERMUTATIONS = $.the(CommutivePermutations.class);

    /**
     * NOTE X and Y should be pre-sorted using Terms.sort otherwise diferent permutations of the same
     * values could result in duplicate termutes HACK
     */
    public CommutivePermutations(Subterms X, Subterms Y) {
        super(COMMUTIVE_PERMUTATIONS, $.sFast(X), $.sFast(Y));

        this.x = X;
        this.y = Y;

        int xs = X.subs();
        assert(xs > 1);
        assert(xs == Y.subs());
        



    }

    @Override
    public int getEstimatedPermutations() {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void mutate(Unify f, Termutator[] chain, int current) {
        int start = f.now();

        ShuffledSubterms p = new ShuffledSubterms(x, f.random);


        while (p.shuffle()) {

            if (p.unifyLinear(y, f)) {
                if (!f.tryMutate(chain, current))
                    break;
            }

            if (!f.revertLive(start))
                break;
        }


    }


}
