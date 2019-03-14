package nars.nal.op;

import nars.Op;
import nars.term.atom.AtomicStringConstant;
import org.jetbrains.annotations.NotNull;


public abstract class TermTransformOperator extends AtomicStringConstant implements TermTransform {



    public TermTransformOperator(String name) {
        super("^" + name);
    }

    @NotNull
    @Override
    public Op op() {
        return Op.OPER;
    }
}
