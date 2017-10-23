package nars.derive;

import nars.Narsese;
import nars.index.term.PatternIndex;
import nars.term.Compound;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static nars.time.Tense.XTERNAL;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PatternCompoundTest {

    final PatternIndex i = new PatternIndex();

    @Test
    public void testPatternCompoundWithXTERNAL() throws Narsese.NarseseException {
        Compound p = (Compound) i.get($("((x) ==>+- (y))"), true).term();
        assertEquals(PatternCompound.PatternCompoundSimple.class, p.getClass());
        assertEquals(XTERNAL, p.dt());
    }

    @Test
    public void testEqualityWithNonPatternDT() throws Narsese.NarseseException {
        for (String s : new String[] { "(a ==> b)", "(a ==>+1 b)", "(a &&+1 b)" }) {
            Compound t = $(s);
            Compound p = (Compound) i.get(t, true).term();
            assertEquals(PatternCompound.PatternCompoundSimple.class, p.getClass());
            assertEquals(t.dt(), p.dt());
            assertEquals(t, p);
        }
    }
}