package nars.util.term.transform;

import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.compound.util.Image;
import org.jetbrains.annotations.Nullable;

/** procedure for Compound term Normalization */
public class CompoundNormalization extends VariableNormalization {

    private final Term root;

    public CompoundNormalization(Term root, byte varOffset) {
        super(root.vars() /* estimate */, varOffset);
        this.root = root;
    }

    @Override
    public @Nullable Term transformCompoundUnneg(Compound x) {
        if (!x.equals(root)) {
            Term y = Image.imageNormalize(x);
            if (y!=x) {
                Termed yy = apply(y);
                if (yy != null)
                    return yy.term();
            }
        }
        return super.transformCompoundUnneg(x);
    }
















}
