package nars.term.compound;

import nars.$;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;

/** use with extreme caution when op is not PROD */
public class LightCompound implements SeparateSubtermsCompound, AbstractLightCompound {

    final Subterms subs;
    private final int hash;
    protected final byte op;


    public LightCompound(Op o, Term... s) {
        this(o, $.vFast(s));
    }

    public LightCompound(Op o, Subterms s) {
        this.op = o.id;
        this.subs = s;
        this.hash = s.hashWith(o);
    }
    @Override
    public boolean equals(Object obj) {
        return (this == obj) ||
                ((obj instanceof Compound) &&
                        hash == obj.hashCode() &&
                        Compound.equals(this, (Term) obj));
    }

    @Override
    public final int hashCode() {
        return hash;
    }


    @Override
    public String toString() {
        return Compound.toString(this);
    }

    @Override
    public final Op op() {
        return Op.ops[op];
    }


    @Override
    public final Subterms subterms() {
        return subs;
    }



}
