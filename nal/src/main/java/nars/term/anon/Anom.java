package nars.term.anon;

import jcog.Util;
import nars.IO;
import nars.Op;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Int;

import static nars.Op.ATOM;

/* indexed anonymous term */
public final class Anom extends Int implements AnonID {

    static final byte ANOM_SUBTYPE = 1;
    static final int ANOM_OPX = Term.opX(ATOM, ANOM_SUBTYPE);
    static final byte ANOM_HEADER = IO.opAndSubType(ATOM, ANOM_SUBTYPE);

    Anom(byte i) {
        super(i, new byte[] { ANOM_HEADER, i } );
    }

    @Override
    public int opX() {
        return ANOM_OPX;
    }

    @Override
    public /**/ Op op() {
        return ATOM;
    }

    @Override
    public int structure() {
        return ATOM.bit;
    }


    @Override
    public String toString() {
        return '_' +  Integer.toString(id);
    }

    @Override
    public short anonID() {
        return (short) id; 
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    
    @Override
    public int compareTo(Termed yy) {
        if (this == yy) return 0;

        Term y = yy.term();
        if (y instanceof Anom) {
            return Integer.compare(id, ((Int) y).id);
        } else {
            int vc = Integer.compare(y.volume(), this.volume());
            if (vc != 0)
                return vc;

            int oc = Integer.compare(this.opX(), y.opX());
            assert (oc != 0);
            return oc;
        }
        
    }

    static final Anom[] the = Util.map(0, (int) Byte.MAX_VALUE, (i) -> new Anom((byte) i), Anom[]::new);
    static {
        the[0] = null; 
    }

    public static Anom the(int i) {
        return the[i];
    }


}
