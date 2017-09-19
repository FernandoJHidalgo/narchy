package nars.derive.constraint;

import nars.$;
import nars.term.Term;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;

public final class StructureInclusionConstraint extends MatchConstraint {

    private final int structure;

    public StructureInclusionConstraint(Term target, int opVector) {
        super("StructIncl", target, $.the(opVector));
        this.structure = opVector;
    }

    @Override
    public boolean invalid(@NotNull Term y, @NotNull Unify f) {
        return !y.hasAny(structure);
    }

    @Override
    public float cost() {
        return 0.1f;
    }
}
