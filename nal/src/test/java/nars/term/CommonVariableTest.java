package nars.term;

import nars.$;
import nars.term.var.CommonVariable;
import nars.unify.UnifyAny;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Created by me on 9/9/15.
 */
class CommonVariableTest {


    private static final Variable p1 = $.varDep(1);
    private static final Variable p2 = $.varDep(2);
    private static final Variable p3 = $.varDep(3);
    private static final Variable c12 = CommonVariable.common(p1, p2);


    @Test
    void commonVariableTest1() {
        
        Variable p1p2 = CommonVariable.common(p1, p2);
        assertEquals("#\"#1#2\"", p1p2.toString());
        Variable p2p1 = CommonVariable.common(p2, p1);
        assertEquals("#\"#2#1\"", p2p1.toString());
    }
    @Test
    void testInvalid() {
        assertThrows(Throwable.class, ()-> {
            Variable p1p1 = CommonVariable.common(p1, p1);
            assertEquals("#x1y1", p1p1.toString());
        });
    }


    @Test
    void CommonVariableDirectionalityPreserved() {
        

        Variable c12_reverse = CommonVariable.common(p2, p1);

        assertNotEquals(c12, c12_reverse);
    }

    @Test
    void CommonVariableOfCommonVariable() {
        Variable c123 = CommonVariable.common( c12,  p3);
        assertEquals("#\"#_#1#2_#3\" class nars.term.var.CommonVariable", (c123 + " " + c123.getClass()));

        
        assertEquals("#\"#_#_#1#2_#3_#2\"", CommonVariable.common( c123, p2).toString());

    }

    @Test void testUnifyCommonVar_DepIndep() {
        UnifyAny u = new UnifyAny();
        assertTrue(
                $$("x($1,#1)").unify($$("x(#1,$1)"), u)
        );
        assertEquals("{$1=#\"#1$1\", #1=#\"#1$1\", #2=#\"#2$2\", $2=#\"#2$2\"}$0", u.toString());
        System.out.println(u);
    }
    //TODO dep/query
}