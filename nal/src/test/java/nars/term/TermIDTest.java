package nars.term;

import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Created by me on 6/3/15.
 */
public class TermIDTest {

    final NAR nar = NARS.shell();


    /* i will make these 3 pass soon, this is an improvement on the representation
    that will make these tests pass once implemented. */

    // '&&' 'a' ',' 'b' ')'
    @Test
    public void testInternalRepresentation28() {
        testBytesRepresentation("(a&&b)", 5);
    }

    @Test
    public void testInternalRepresentation28cc() {
        testBytesRepresentation("((--,(b,c))&&a)", 5);
    }

//    // '--', 'a'
//    @Test
//    public void testInternalRepresentation29() {
//        testBytesRepresentation("(--,a)", 2);
//    }

    // '*' 'a' ',' 'b' ')'
    @Test
    public void testInternalRepresentation2z() {
        testBytesRepresentation("(a,b)", 5);
    }


    /**
     * tests whether NALOperators has been reduced to the
     * compact control character (8bits UTF) that represents it
     */

    @Test
    public void testInternalRepresentation23() {
        testBytesRepresentation("x", 1);
    }

    @Test
    public void testInternalRepresentation24() {
        testBytesRepresentation("xyz", 3);
    }

    @Test
    public void testInternalRepresentation25() {
        testBytesRepresentation("\u00ea", 2);
    }

    @Test
    public void testInternalRepresentation26() {
        testBytesRepresentation("xyz\u00e3", 3 + 2);
    }

    //  '-->' 'a' ','  'b' ')' == 5
    @Test
    public void testInternalRepresentation27() {
        testBytesRepresentation("(a-->b)", 5);
    }

//    @Test
//    public void testInternalRepresentationImage1() {
//        for (char t : new char[]{'/', '\\'}) {
//            testBytesRepresentation("(" + t + ",_,a)", 3 + 1);
//            testBytesRepresentation("(" + t + ",_,a,b)", 5 + 1);
//            testBytesRepresentation("(" + t + ",a,_,b)", 5 + 1);
//            testBytesRepresentation("(" + t + ",a,b,_)", 5 + 1);
//        }
//    }


    //@Test public void testInternalRepresentation2() { testInternalRepresentation("<a && b>", 5); }


    @NotNull
    public Term testBytesRepresentation(@NotNull String expectedCompactOutput, int expectedLength) {
        try {
            return testBytesRepresentation(
                    null,
                    expectedCompactOutput,
                    expectedLength);
        } catch (Narsese.NarseseException e) {
            fail(e);
            return null;
        }
    }

    @NotNull
    public Term testBytesRepresentation(@Nullable String expectedCompactOutput, @NotNull String expectedPrettyOutput, int expectedLength) throws Narsese.NarseseException {
        //UTF8Identifier b = new UTF8Identifier(expectedPrettyOutput);
        Termed i = $.$(expectedPrettyOutput);
        //byte[] b = i.bytes();
        //byte[] b = i.bytes();

        if (expectedCompactOutput != null)
            assertEquals(expectedCompactOutput, i.toString());

        areEqualAndIfNotWhy(expectedPrettyOutput, i.toString());


        //assertEquals(expectedCompactOutput + " ---> " + Arrays.toString(b), expectedLength, b.length);
        return i.term();
    }

    public void areEqualAndIfNotWhy(@NotNull String a, @NotNull String b) {
        assertEquals(charComparison(a, b), a, b);
    }

    @NotNull
    public static String charComparison(@NotNull String a, @NotNull String b) {
        return Arrays.toString(a.toCharArray()) + " != " + Arrays.toString(b.toCharArray());
    }

//    @Test public void testComparingStringAndUtf8Atoms() {
//        testStringUtf8Equal("x");
//        testStringUtf8Equal("xy");
//        testStringUtf8Equal("xyz");
//        testTermInEqual(new StringAtom("x"), new Utf8Atom("y"));
//        testTermInEqual($.$("$x"), new Utf8Atom("x"));
//        testTermInEqual($.$("$x"), new StringAtom("x"));
//    }
//
//    public void testStringUtf8Equal(String id) {
//        StringAtom s = new StringAtom(id);
//        Utf8Atom u = new Utf8Atom(id);
//
//        assertEquals(id, u.toString());
//        assertEquals(id, s.toString());
//        assertEquals(Op.ATOM, u.op());
//
//        testTermEqual(u, s);
//        assertEquals(0, u.compareTo(s));
//        assertEquals(0, s.compareTo(u));
//        assertEquals(id.hashCode(), s.hashCode());
//        assertEquals(u.hashCode(), s.hashCode());
//
//    }
//
//    public void testTermInEqual(Term u, Term s) {
//
//        int us = u.compareTo(s);
//        assertNotEquals(0, us);
//        assertEquals(-us, s.compareTo(u));
//        assertNotEquals(u.hashCode(), s.hashCode());
//
//    }
//    public void testTermEqual(Term u, Term s) {
//
//        assertEquals(u.op(), s.op());
//
//        assertEquals(u.hashCode(), s.hashCode());
//
//        assertEquals(u, s);
//
//
//    }
}
