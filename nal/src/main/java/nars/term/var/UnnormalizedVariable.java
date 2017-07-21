package nars.term.var;

import com.google.common.io.ByteArrayDataOutput;
import nars.$;
import nars.Op;
import nars.index.term.TermContext;
import nars.term.Term;
import nars.term.atom.AtomicToString;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;

import static nars.IO.SPECIAL_OP;

/**
 * Unnormalized, labeled variable
 */
public class UnnormalizedVariable extends AtomicToString implements Variable {

    @NotNull
    public final Op type;
    @Deprecated private final String str;

    @Override public int opX() { return Term.opX(op(), 10);    }

    public UnnormalizedVariable(@NotNull Op type, @NotNull String label) {
        super(type, label);
        this.str = label;
        this.type = type;
    }

    @Override
    public final int id() {
        throw new UnsupportedOperationException();
    }

    final @Override public boolean equals(Object u) {
        if (this == u) return true;

//        if (u instanceof AbstractVariable)
//            if (u.toString().equals(str))
//                System.out.println(this + " and " + u + " equal by string");
//            //throw new UnsupportedOperationException();
//        }

        //prevent comparison with AbstractVariable
        if (u instanceof UnnormalizedVariable) {
            return toString().equals(((UnnormalizedVariable) u).toString());
        }

        return false;
    }


    @Override
    public final int complexity() {
        return 0;
    }

    @NotNull
    @Override
    public final Op op() {
        return type;
    }


    @Override
    public final int varIndep() {
        return type == Op.VAR_INDEP ? 1 : 0;
    }

    @Override
    public final int varDep() {
        return type == Op.VAR_DEP ? 1 : 0;
    }

    @Override
    public final int varQuery() {
        return type == Op.VAR_QUERY ? 1 : 0;
    }

    @Override
    public final int varPattern() {
        return type == Op.VAR_PATTERN ? 1 : 0;
    }

    @Override
    public final int vars() {
        // pattern variable hidden in the count 0
        return type == Op.VAR_PATTERN ? 0 : 1;
    }

    /** produce a normalized version of this identified by the serial integer */
    public @NotNull Variable normalize(int serial) {
        return $.v(type, serial);
    }

    @NotNull
    @Override
    public final String toString() {
        return str;
    }

    @Override
    public boolean unify(@NotNull Term y, @NotNull Unify subst) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Term eval(TermContext index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void append(ByteArrayDataOutput out) {
        out.writeByte(SPECIAL_OP);
        String s = toString();
        out.writeShort(s.length());
        out.write(s.getBytes()); //HACK
        //byte[] b = bytes();
        //out.writeShort(b.length);

    }

}
