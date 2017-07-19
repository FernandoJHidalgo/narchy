package nars.derive.meta;

import jcog.Texts;
import nars.control.premise.Derivation;
import nars.derive.meta.op.MatchTerm;
import org.fusesource.jansi.Ansi;

public class DebugDerivationPredicate extends InstrumentedDerivationPredicate {

    public DebugDerivationPredicate(PrediTerm<Derivation> inner) {
        super(inner);
    }

    @Override
    protected void onEnter(PrediTerm<Derivation> p, Derivation d) {

    }

    protected void onExit(PrediTerm<Derivation> p, Derivation d, boolean returnValue, Throwable thrown, long nanos) {
        if (p instanceof Fork) {

        } else if (p instanceof AndCondition) {

        } else {

            Ansi.Color fg;
            if (p instanceof MatchTerm) {
                fg = Ansi.Color.MAGENTA;
            } else {
                fg = Ansi.Color.YELLOW;
            }
            Ansi ansi = Ansi.ansi();
            String pAnsi = ansi
                    .a(Texts.iPad(nanos, 6) + "nS ")
                    .a(' ')
                    .a(d.toString())
                    .a(' ')
                    //.a(p.getClass().getSimpleName())                    .a(' ')
                    .fg(fg).a(p.toString()).fg(Ansi.Color.DEFAULT)
                    .a(' ')
                    .a((thrown != null ? (" "+thrown) : ' '))
                    .fg(returnValue ? Ansi.Color.GREEN : Ansi.Color.RED)
                    //.bg( thrown!=null ? Ansi.Color.YELLOW : Ansi.Color.BLACK )
                    .a(returnValue ? "TRUE" : "FALS").fg(Ansi.Color.DEFAULT).newline().toString();

            System.out.print(pAnsi);
        }
    }
}
