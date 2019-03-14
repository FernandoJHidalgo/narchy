package nars.nlp;

import nars.NAR;
import nars.nar.Default;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by me on 7/9/16.
 */
public class NLPGenTest {

    final NLPGen g = new NLPGen();

    NAR n = new Default();

    @Test
    public void testSimple1() {
        assertEquals("a is b.", g.toString(n.task("(a --> b).")));
        assertEquals("a isn't b.", g.toString(n.task("(--,(a --> b)).")));
    }

    @Test
    public void testSimple2() {
        assertEquals("b same as a.", g.toString(n.task("(a <-> b).")));
    }

//    @Test
//    public void testSimple3() {
//        assertEquals("a isn't b.", g.toString(n.task("(--,(a --> b)).")));
//        assertEquals("(a is b) and (c is d).", g.toString(n.task("((a --> b) && (c --> d)).")));
//    }

}