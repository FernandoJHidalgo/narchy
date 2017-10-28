package nars.term;

import nars.*;
import nars.concept.BaseConcept;
import nars.concept.Concept;
import nars.term.transform.Retemporalize;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.TreeSet;

import static nars.$.$;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;
import static org.junit.jupiter.api.Assertions.*;


public class TemporalTermTest {

    @NotNull
    final NAR n = NARS.shell();

    @Test
    public void parsedCorrectOccurrenceTime() throws Narsese.NarseseException {
        long now = n.time();
        Task t = n.inputAndGet("b:a. :\\:");
        assertEquals(now, t.creation());
        assertEquals(now - 1, t.start());
    }

    @Test
    public void testCoNegatedSubtermConcept() throws Narsese.NarseseException {
        assertEquals("((x) &&+- (x))", n.conceptualize($.$("((x) &&+10 (x))")).toString());

        assertEquals("((--,(x)) &&+- (x))", n.conceptualize($.$("((x) &&+10 (--,(x)))")).toString());
        assertEquals("((--,(x)) &&+- (x))", n.conceptualize($.$("((x) &&-10 (--,(x)))")).toString());

//        assertEquals("((x) <=>+- (x))", n.conceptualize(n.term("((x) <=>+10 (--,(x)))")).toString());
//        assertEquals("((x) <=>+- (x))", n.conceptualize(n.term("((x) <=>-10 (--,(x)))")).toString());

        assertEquals("((x) ==>+- (x))", n.conceptualize($.$("((x) ==>+10 (x))")).toString());
        assertEquals("((--,(x)) ==>+- (x))", n.conceptualize($.$("((--,(x)) ==>+10 (x))")).toString());
        assertEquals("((x) ==>+- (x))", n.conceptualize($.$("((x) ==>+10 (--,(x)))")).toString());
        assertEquals("((x) ==>+- (x))", n.conceptualize($.$("((x) ==>-10 (--,(x)))")).toString());

    }

    @Test
    public void testCoNegatedSubtermTask() throws Narsese.NarseseException {

        //allowed
        assertNotNull(Narsese.parse().task("((x) &&+1 (--,(x))).", n));

        //not allowed
        assertInvalidTask("((x) && (--,(x))).");
        assertInvalidTask("((x) &&+0 (--,(x))).");
    }

    @Test
    public void testInvalidInheritanceOfEternalAndItsTemporal() throws Narsese.NarseseException {
        assertEquals(
                Op.True,
                $("((a &&+1 b)-->(a && b))")
        );
        assertEquals(
                Op.True,
                $("(--(a &&+1 b) --> --(a && b))")
        );
        assertEquals( //since one is negative it's allowed
                "((--,(a &&+1 b))-->(a&&b))",
                $("(--(a &&+1 b)-->(a && b))").toString()
        );
        assertEquals( //since one is negative it's allowed
                "((a &&+1 b)-->(--,(a&&b)))",
                $("((a &&+1 b) --> --(a && b))").toString()
        );
        assertEquals(
                Op.True,
                $("((a && b)-->(a &&+1 b))")
        );
        assertEquals(
                Op.True,
                $("((a && b)<->(a &&+1 b))")
        );
    }

    @Test
    public void testInvalidInheritanceOfEternalAndItsTemporalDeeper() throws Narsese.NarseseException {

        Term b = $("(x,((--,R) &&+6 (--,happy)))");
        assertTrue(b.isTemporal());
        Term bx = b.root();
        assertEquals(XTERNAL, bx.sub(1).dt());
        assertFalse(bx.equals(b));

        Term a = $("(x,((--,R)&&(--,happy)))");
        Term ax = a.root();
        assertEquals(XTERNAL, ax.sub(1).dt());
        assertFalse(ax.equals(a));

        assertEquals(b.root(), a.root());
        assertEquals(a.root(), b.root());
        assertEquals(
                Op.True,
                $("((x,((--,R) &&+6 (--,happy)))-->(x,((--,R)&&(--,happy))))")
        );
    }

    public void assertInvalidTask(@NotNull String ss) {
        try {
            Narsese.parse().task(ss, n);
            fail("");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    @Test
    public void testEventsWithRepeatParallel() throws Narsese.NarseseException {

        assertEquals("[0:a, 0:b]",
                $("(a&|b)").eventList().toString());

        assertEquals(
                //"[0:(a&|b), 5:(b&|c)]",
                //"[a:0, b:0, (a&|b):0, b:5, c:5, (b&|c):5]",
                "[0:a, 0:b, 5:b, 5:c]",
                $("((a&|b) &&+5 (b&|c))").eventList().toString());

    }

    @Test
    public void testEventsWithXTERNAL() throws Narsese.NarseseException {
        //cant decompose
        assertEquals("[0:(x &&+- y)]", $("(x &&+- y)").eventList().toString());
        assertEquals("[0:(x &&+- y), 1:z]", $("((x &&+- y) &&+1 z)").eventList().toString());
    }

    @Test
    public void testEventsWithDTERNAL() throws Narsese.NarseseException {
        //cant decompose
        assertEquals("[0:(x&&y)]", $("(x && y)").eventList().toString());
        assertEquals("[0:(x&&y), 1:z]", $("((x && y) &&+1 z)").eventList().toString());
    }

    @Test
    public void testAtemporalization() throws Narsese.NarseseException {
        Term t = $.$("((x) ==>+10 (y))");
        Concept c = n.conceptualize(t);
        assertEquals("((x)==>(y))", c.toString());
    }

    @Test
    public void testAtemporalization2() throws Narsese.NarseseException {

        assertEquals("((--,y) &&+- y)", $.<Compound>$("(y &&+3 (--,y))").temporalize(Retemporalize.retemporalizeAllToXTERNAL).toString());
    }

    @Test
    public void testAtemporalization3() throws Narsese.NarseseException {

        assertEquals("(--,((x &&+- $1) ==>+- ((--,y) &&+- $1)))",
                $.<Compound>$("(--,(($1&&x) ==>+1 ((--,y) &&+2 $1)))").temporalize(Retemporalize.retemporalizeAllToXTERNAL).toString());
    }

    @Test
    public void testAtemporalization4() throws Narsese.NarseseException {
        //maintain temporal information that would otherwise be factored out if non-temporal

        assertEquals("((x &&+- $1)==>(y &&+- $1))",
                $.$("(($1 && x) ==>+1 ($1 &&+1 y))").root().toString());
    }


    @Test
    public void testAtemporalization5() throws Narsese.NarseseException {
        for (String s : new String[]{"(y &&+- (x==>y))", "((x==>y) &&+- y)"}) {
            Term c = $(s);
            assertTrue(c instanceof Compound);

            assertEquals("((x==>y) &&+- y)",
                    c.root().toString());
        }
    }

    @Test
    public void testAtemporalization6() throws Narsese.NarseseException {
        Compound x = $("((--,(($1&&x) ==>+1 ((--,y) &&+2 $1))) &&+3 (--,y))");

        Term y = x.temporalize(Retemporalize.retemporalizeAllToXTERNAL);
        assertEquals("((--,((x &&+- $1) ==>+- ((--,y) &&+- $1))) &&+- (--,y))", y.toString());
    }

    @Test
    public void testAtemporalizationSharesNonTemporalSubterms() throws Narsese.NarseseException {

        Task a = n.inputAndGet("((x) ==>+10 (y)).");
        Task c = n.inputAndGet("((x) ==>+9 (y)).");
        Task b = n.inputAndGet("((x) <-> (y)).");
        n.cycle();

        @NotNull Term aa = a.term();
        assertNotNull(aa);

        @Nullable Concept na = a.concept(n, true);
        assertNotNull(na);

        @Nullable Concept nc = c.concept(n, true);
        assertNotNull(nc);

        assertSame(na, nc);

        assertSame(na.sub(0), nc.sub(0));

//        System.out.println(b.concept(n));
//        System.out.println(c.concept(n));

        assertEquals(b.concept(n, true).sub(0), c.concept(n, true).sub(0));

    }

    @Test
    public void testHasTemporal() throws Narsese.NarseseException {
        assertTrue($("(?x &&+1 y)").isTemporal());
    }

    @Test
    public void testParseOperationInFunctionalForm2() throws Narsese.NarseseException {
        assertEquals(
                "(((a)&&(b))&|do(that))",
                //"(&|,do(that),(a),(b))",
                $.$("(do(that) &&+0 ((a)&&(b)))").toString());

        Termed nt = $.$("(((that)-->do) &&+0 ((a)&&(b)))");
        assertEquals(
                "(((a)&&(b))&|do(that))",
                //"(&|,do(that),(a),(b))",
                nt.toString());

        //assertNotNull(n.conceptualize(nt, UnitBudget.One));
        assertEquals(
                //"(&&,do(that),(a),(b))",
                "(((a) &&+- (b)) &&+- do(that))",
                n.conceptualize(nt).toString(), ()->nt.toString() + " conceptualized");

        //assertEquals("(&&,do(that),(a),(b))", n.conceptualize(nt, UnitBudget.One).toString()); ????????

    }

    @Test
    public void testAnonymization2() throws Narsese.NarseseException {
        Termed nn = $.$("((do(that) &&+1 (a)) ==>+2 (b))");
        assertEquals("((do(that) &&+1 (a)) ==>+2 (b))", nn.toString());


        assertEquals("((do(that) &&+- (a))==>(b))", n.conceptualize(nn).toString());

        //assertEquals("(&&,do(that),(a),(b))", n.conceptualize(nt, UnitBudget.One).toString()); ??

    }

    @Test
    public void testCommutiveTemporalityConjEquiv() {
//        testParse("((#1-->$2) <=>-20 ({(row,3)}-->$2))", "(({(row,3)}-->$1) <=>+20 (#2-->$1))");
//        testParse("(({(row,3)}-->$2) <=>+20 (#1-->$2))", "(({(row,3)}-->$1) <=>+20 (#2-->$1))");

        testParse("((#1-->$2) &&-20 ({(row,3)}-->$2))", "(({(row,3)}-->$1) &&+20 (#2-->$1))");
    }

    @Test
    public void testCommutiveTemporalityConjEquiv2() {
        testParse("(({(row,3)}-->$2) &&+20 (#1-->$2))", "(({(row,3)}-->$1) &&+20 (#2-->$1))");
    }

    @Test
    public void testCommutiveTemporalityConj2() {
        testParse("(goto(a) &&+5 ((SELF,b)-->at))", "(goto(a) &&+5 at(SELF,b))");
    }


    @Test
    public void testCommutiveConjTemporal() throws Narsese.NarseseException {
        Term x = $("(a &&+1 b)");
        assertEquals("a", x.sub(0).toString());
        assertEquals("b", x.sub(1).toString());
        assertEquals(+1, x.dt());
        assertEquals("(a &&+1 b)", x.toString());

        Term y = $("(a &&-1 b)");
        assertEquals("a", y.sub(0).toString());
        assertEquals("b", y.sub(1).toString());
        assertEquals(-1, y.dt());
        assertEquals("(b &&+1 a)", y.toString());

        Term z = $("(b &&+1 a)");
        assertEquals("a", z.sub(0).toString());
        assertEquals("b", z.sub(1).toString());
        assertEquals(-1, z.dt());
        assertEquals("(b &&+1 a)", z.toString());

        Term w = $("(b &&-1 a)");
        assertEquals("a", w.sub(0).toString());
        assertEquals("b", w.sub(1).toString());
        assertEquals(+1, w.dt());
        assertEquals("(a &&+1 b)", w.toString());

        assertEquals(y, z);
        assertEquals(x, w);

    }

    @Test
    public void testCommutiveTemporality1() {
        testParse("(goto(a)&&((SELF,b)-->at))", "(at(SELF,b)&&goto(a))");
        testParse("(goto(a)&|((SELF,b)-->at))", "(at(SELF,b)&|goto(a))");
        testParse("(at(SELF,b) &&+5 goto(a))", "(at(SELF,b) &&+5 goto(a))");
    }

    @Test
    public void testCommutiveTemporality2() {
        testParse("(at(SELF,b)&&goto(a))");
        testParse("(at(SELF,b)&|goto(a))");
        testParse("(at(SELF,b) &&+5 goto(a))");
        testParse("(goto(a) &&+5 at(SELF,b))");
    }

    @Test
    public void testCommutiveTemporalityDepVar0() throws Narsese.NarseseException {
        Term t0 = $.$("((SELF,#1)-->at)").term();
        Term t1 = $.$("goto(#1)").term();
        assertEquals(
                Op.subterms(Terms.sorted(t0, t1)),
                Op.subterms(Terms.sorted(t1, t0))
        );
    }

    @Test
    public void testCommutiveTemporalityDepVar1() {
        testParse("(goto(#1) &&+5 at(SELF,#1))");
    }

    @Test
    public void testCommutiveTemporalityDepVar2() {
        testParse("(goto(#1) &&+5 at(SELF,#1))", "(goto(#1) &&+5 at(SELF,#1))");
        testParse("(goto(#1) &&-5 at(SELF,#1))", "(at(SELF,#1) &&+5 goto(#1))");
    }

    @Test
    public void testCommutiveEquivAgain1() throws Narsese.NarseseException {
        assertEquals($("((--,(0,0)) <=>+48 (happy))"), $("((happy) <=>-48 (--,(0,0)))"));
    }

    @Test
    public void testCommutiveEquivAgain2() throws Narsese.NarseseException {
        assertEquals($("((--,(0,0)) <=>+48 (happy))"), $("((--,(happy)) <=>-48 (0,0))"));
    }

    @Test
    public void testCommutiveEquivAgain3() throws Narsese.NarseseException {
        assertEquals($("((--,(0,0)) <=>+48 (--,(happy)))"), $("((--,(happy)) <=>-48 (--,(0,0)))"));
    }

    void testParse(String s) {
        testParse(s, null);
    }

    void testParse(String input, String expected) {
        Termed t = null;
        try {
            t = $.$(input);
        } catch (Narsese.NarseseException e) {
            fail(e);
        }
        if (expected == null)
            expected = input;
        assertEquals(expected, t.toString());
    }

    @Test
    public void testCommutiveTemporalityConcepts() throws Narsese.NarseseException {
        NAR n = NARS.shell();

        n.log();

        n.input("(goto(#1) &&+5 ((SELF,#1)-->at)).");
        //n.step();

        n.input("(goto(#1) &&-5 ((SELF,#1)-->at)).");
        //n.step();

        n.input("(goto(#1) &&+0 ((SELF,#1)-->at)).");
        //n.step();
        n.input("(((SELF,#1)-->at) &&-3 goto(#1)).");
        //n.step();
        //n.input("(((SELF,#1)-->at) &&+3 goto(#1)).");
        //n.step();
        //n.input("(((SELF,#1)-->at) &&+0 goto(#1)).");


        n.run(2);

        BaseConcept a = (BaseConcept) n.conceptualize("(((SELF,#1)-->at) && goto(#1)).");
        Concept a0 = n.conceptualize("(goto(#1) && ((SELF,#1)-->at)).");
        assertNotNull(a);
        assertSame(a, a0);


        a.beliefs().print();

        assertTrue(a.beliefs().size() >= 4);
    }

    @Test
    public void testCommutiveTemporalityConcepts2() throws Narsese.NarseseException {
        NAR n = NARS.shell();

        for (String op : new String[]{"&&"}) {
            Concept a = n.conceptualize($("(x " + op + "   y)"));
            Concept b = n.conceptualize($("(x " + op + "+1 y)"));

            assertSame(a, b);

            Concept c = n.conceptualize($("(x " + op + "+2 y)"));

            assertSame(b, c);

            Concept d = n.conceptualize($("(x " + op + "-1 y)"));

            assertSame(c, d);

            Term e0 = $("(x " + op + "+- y)");
            assertEquals("(x " + op + "+- y)", e0.toString());
            Concept e = n.conceptualize(e0);

            assertSame(d, e);

            Term f0 = $("(y " + op + "+- x)");
            assertEquals("(x " + op + "+- y)", f0.toString());
            assertEquals("(x " + op + "+- y)", f0.root().toString());

            Concept f = n.conceptualize(f0);
            assertSame(e, f, e + "==" + f);

            //repeat
            Concept g = n.conceptualize($("(x " + op + "+- x)"));
            assertEquals("(x " + op + "+- x)", g.toString());

            //co-negation
            Concept h = n.conceptualize($("(x " + op + "+- (--,x))"));
            assertEquals("((--,x) " + op + "+- x)", h.toString());


        }

    }

    @Nullable
    final Term A = $.the("a");
    @Nullable
    final Term B = $.the("b");

    @Test
    public void parseTemporalRelation() throws Narsese.NarseseException {
        //TODO move to NarseseTest
        assertEquals("(x ==>+5 y)", $("(x ==>+5 y)").toString());
        assertEquals("(x &&+5 y)", $("(x &&+5 y)").toString());

        assertEquals("(x ==>-5 y)", $("(x ==>-5 y)").toString());

        assertEquals("((before-->x) ==>+5 (after-->x))", $("(x:before ==>+5 x:after)").toString());
    }

    @Test
    public void temporalEqualityAndCompare() throws Narsese.NarseseException {
        assertNotEquals($("(x ==>+5 y)"), $("(x ==>+0 y)"));
        assertNotEquals($("(x ==>+5 y)").hashCode(), $("(x ==>+0 y)").hashCode());
        assertNotEquals($("(x ==> y)"), $("(x ==>+0 y)"));
        assertNotEquals($("(x ==> y)").hashCode(), $("(x ==>+0 y)").hashCode());

        assertEquals($("(x ==>+0 y)"), $("(x ==>-0 y)"));
        assertNotEquals($("(x ==>+5 y)"), $("(y ==>-5 x)"));


        assertEquals(0, $("(x ==>+0 y)").compareTo($("(x ==>+0 y)")));
        assertEquals(-1, $("(x ==>+0 y)").compareTo($("(x ==>+1 y)")));
        assertEquals(+1, $("(x ==>+1 y)").compareTo($("(x ==>+0 y)")));
    }


    @Test
    public void testReversibilityOfCommutive() throws Narsese.NarseseException {
        for (String c : new String[]{"&&"/*, "<=>"*/}) {
            assertEquals("(a " + c + "+5 b)", $("(a " + c + "+5 b)").toString());
            assertEquals("(b " + c + "+5 a)", $("(b " + c + "+5 a)").toString());
            assertEquals("(a " + c + "+5 b)", $("(b " + c + "-5 a)").toString());
            assertEquals("(b " + c + "+5 a)", $("(a " + c + "-5 b)").toString());

            assertEquals($("(b " + c + "-5 a)"), $("(a " + c + "+5 b)"));
            assertEquals($("(b " + c + "+5 a)"), $("(a " + c + "-5 b)"));
            assertEquals($("(a " + c + "-5 b)"), $("(b " + c + "+5 a)"));
            assertEquals($("(a " + c + "+5 b)"), $("(b " + c + "-5 a)"));
        }
    }

    @Test
    public void testCommutiveWithCompoundSubterm() throws Narsese.NarseseException {
        Term a = $("(((--,(b0)) &&+0 (pre_1)) &&+10 (else_0))");
        Term b = $("((else_0) &&-10 ((--,(b0)) &&+0 (pre_1)))");
        assertEquals(a, b);

        Term c = $.seq($("((--,(b0)) &&+0 (pre_1))"), 10, $("(else_0)"));
        Term d = $.seq($("(else_0)"), -10, $("((--,(b0)) &&+0 (pre_1))"));

//        System.out.println(a);
//        System.out.println(b);
//        System.out.println(c);
//        System.out.println(d);

        assertEquals(b, c);
        assertEquals(c, d);
        assertEquals(a, c);
        assertEquals(a, d);
    }

    @Test
    public void testConceptualization() throws Narsese.NarseseException {

        Term t = $("(x==>y)");
        Term x = t.root();
        assertEquals(DTERNAL, x.dt());
        assertEquals("(x==>y)", x.toString());

        n.input("(x ==>+0 y).", "(x ==>+1 y).").run(2);

        BaseConcept xImplY = (BaseConcept) n.conceptualize(t);
        assertNotNull(xImplY);

        assertEquals("(x==>y)", xImplY.toString());

        assertEquals(3, xImplY.beliefs().size());

        int indexSize = n.terms.size();
        n.terms.print(System.out);

        n.input("(x ==>+1 y). :|:"); //present
        n.cycle();

        //d.concept("(x==>y)").print();

        assertEquals(4, xImplY.beliefs().size());

        n.terms.print(System.out);
        assertEquals(indexSize, n.terms.size()); //remains same amount

        n.conceptualize("(x==>y)").print();
    }


    @Test
    public void testSubtermTimeRecursive() throws Narsese.NarseseException {
        Compound c = $("(hold:t2 &&+1 (at:t1 &&+3 ([opened]:t1 &&+5 open(t1))))");
        assertEquals("(((t2-->hold) &&+1 (t1-->at)) &&+3 ((t1-->[opened]) &&+5 open(t1)))", c.toString());
        assertEquals(0, c.subTime($("hold:t2")));
        assertEquals(1, c.subTime($("at:t1")));
        assertEquals(4, c.subTime($("[opened]:t1")));
        assertEquals(9, c.subTime($("open(t1)")));
        assertEquals(9, c.dtRange());
    }

    @Test
    public void testSubtermNonCommutivePosNeg() throws Narsese.NarseseException {
        Term x = $("((d-->c) ==>-3 (a-->b))");
        assertEquals(0, x.subTime($("(d-->c)")));
        assertEquals(-3, x.subTime($("(a-->b)")));
        assertEquals(DTERNAL, x.subtermTimeSafe($("a"))); //a is not an event within x
    }

    @Test
    public void subtermTimeWithConjInImpl() throws Narsese.NarseseException {
        @NotNull Term t = $("((b &&+5 c) ==>-10 (a &&+5 d))");
        assertEquals(0, t.subTime($("(b &&+5 c)")));
        assertEquals(-5, t.subTime($("(a &&+5 d)")));
        assertEquals(-5, t.subTime($("a")));
        assertEquals(0, t.subTime($("b")));
        assertEquals(5, t.subTime($("c")));
        assertEquals(DTERNAL, t.subtermTimeSafe($("x")));
    }

    @Test
    public void testSubtermTimeRecursiveWithNegativeCommutive() throws Narsese.NarseseException {
        Compound b = $("(a &&+5 b)");
        assertEquals(0, b.subTime(A));
        assertEquals(5, b.subTime(B));

        Compound c = $("(a &&-5 b)");
        assertEquals(5, c.subTime(A));
        assertEquals(0, c.subTime(B));

        Compound d = $("(b &&-5 a)");
        assertEquals(0, d.subTime(A));
        assertEquals(5, d.subTime(B));

//        Compound e = $("(a <=>+1 b)");
//        assertEquals(0, e.subtermTime(A));
//        assertEquals(1, e.subtermTime(B));
//
//        Compound f = $("(a <=>-1 b)");
//        assertEquals(1, f.subtermTime(A));
//        assertEquals(0, f.subtermTime(B));
//
//        Compound g = $("(b <=>+1 a)");
//        assertEquals(1, g.subtermTime(A));
//        assertEquals(0, g.subtermTime(B));

    }

    @Test
    public void testSubtermConjInConj() throws Narsese.NarseseException {
        String g0 = "(((x) &&+1 (y)) &&+1 (z))";
        Compound g = $(g0);
        assertEquals(g0, g.toString());
        assertEquals(0, g.subTime($("(x)")));
        assertEquals(1, g.subTime($("(y)")));
        assertEquals(2, g.subTime($("(z)")));

        Compound h = $("((z) &&+1 ((x) &&+1 (y)))");
        assertEquals(0, h.subTime($("(z)")));
        assertEquals(1, h.subTime($("(x)")));
        assertEquals(2, h.subTime($("(y)")));

        Compound i = $("((y) &&+1 ((z) &&+1 (x)))");
        assertEquals(0, i.subTime($("(y)")));
        assertEquals(1, i.subTime($("(z)")));
        assertEquals(2, i.subTime($("(x)")));

        Compound j = $("((x) &&+1 ((z) &&+1 (y)))");
        assertEquals(0, j.subTime($("(x)")));
        assertEquals(1, j.subTime($("(z)")));
        assertEquals(2, j.subTime($("(y)")));
    }

    @Test
    public void testDTRange() throws Narsese.NarseseException {
        assertEquals(1, $("((z) &&+1 (y))").dtRange());
    }

    @Test
    public void testDTRange2() throws Narsese.NarseseException {
        Term t = $("((x) &&+1 ((z) &&+1 (y)))");
        assertEquals(2, t.dtRange());
    }

    @Test
    public void testDTRange3() throws Narsese.NarseseException {
        assertEquals(4, $("((x) &&+1 ((z) &&+1 ((y) &&+2 (w))))").dtRange());
        assertEquals(4, $("(((z) &&+1 ((y) &&+2 (w))) &&+1 (x))").dtRange());
    }


    @Test
    public void testNonCommutivityImplConcept() throws Narsese.NarseseException {
        Param.DEBUG = true;
        NAR n = NARS.shell();
        n.input("((x) ==>+5 (y)).", "((y) ==>-5 (x)).");
        n.run(5);

        TreeSet d = new TreeSet(Comparator.comparing(Object::toString));
        n.conceptsActive().forEach(x -> d.add(x.get()));

        //2 unique impl concepts created
        assertTrue(d.contains($("((x)==>(y))")));
        assertTrue(d.contains($("((y)==>(x))")));
    }

    @Test
    public void testCommutivity() throws Narsese.NarseseException {

        assertTrue($("(b && a)").isCommutative());
        assertTrue($("(b &| a)").isCommutative());
        assertFalse($("(b &&+1 a)").isCommutative());
        assertFalse($("(b &&+- a)").isCommutative());


        Term abc = $("((a &| b) &| c)");
        assertEquals("(&|,a,b,c)", abc.toString());
        assertTrue(abc.isCommutative());

    }

    @Test
    public void testInvalidConjunction() throws Narsese.NarseseException {

        Compound x = $("(&&,(#1-->I),(#1-->{i141}),(#2-->{i141}))");
        assertNotNull(x);
        assertEquals(Op.Null, x.dt(-1));
        assertEquals(Op.Null, x.dt(+1));
        assertNotEquals(Op.Null, x.dt(0));
        assertNotEquals(Op.Null, x.dt(DTERNAL));
        assertNotEquals(Op.Null, x.dt(XTERNAL));
    }

    @Test
    public void testWeirdParse() throws Narsese.NarseseException {
        assertEquals("(&&,a,b,c,-59)", $("( &&-59 ,a, b, c)").toString());

//        assertNotNull(x.dt(0));
//        assertNotNull(x.dt(0).dt(DTERNAL));
//        assertEquals(x, x.dt(0).dt(DTERNAL));
//
//        try {
//            x.dt(-59);
//            assertTrue(x.toString(), false);
//        } catch (InvalidTerm e) {
//            assertTrue(true);
//        }
    }

    @Test
    public void testEqualsAnonymous() throws Narsese.NarseseException {
        //        if (as == bs) {
//            return true;
//        } else if (as instanceof Compound && bs instanceof Compound) {
//            return equalsAnonymous((Compound) as, (Compound) bs);
//        } else {
//            return as.equals(bs);
//        }
        //assertEquals("", $.terms.atemporalize())
        @NotNull Term a = $("(x && y)");

        Term b = $.$("(x &&+1 y)");
        assertEquals("(x &&+- y)", b.root().toString());

        Term c = $.$("(x &&+1 x)");
        assertEquals("(x &&+- x)", c.root().toString());
    }

    @Test
    public void testEqualsAnonymous2() throws Narsese.NarseseException {

        //assertTrue(Terms.equalAtemporally(a, b));
        //        if (as == bs) {
//            return true;
//        } else if (as instanceof Compound && bs instanceof Compound) {
//            return equalsAnonymous((Compound) as, (Compound) bs);
//        } else {
//            return as.equals(bs);
//        }
        //assertTrue(Terms.equalAtemporally(a, $.$("(y &&+1 x)")));
        //        if (as == bs) {
//            return true;
//        } else if (as instanceof Compound && bs instanceof Compound) {
//            return equalsAnonymous((Compound) as, (Compound) bs);
//        } else {
//            return as.equals(bs);
//        }
        //assertFalse(Terms.equalAtemporally(a, $.$("(z &&+1 x)")));

        //        if (as == bs) {
//            return true;
//        } else if (as instanceof Compound && bs instanceof Compound) {
//            return equalsAnonymous((Compound) as, (Compound) bs);
//        } else {
//            return as.equals(bs);
//        }
        //assertTrue(Terms.equalAtemporally($("(x ==> y)"), $.$("(x ==>+1 y)")));
        //        if (as == bs) {
//            return true;
//        } else if (as instanceof Compound && bs instanceof Compound) {
//            return equalsAnonymous((Compound) as, (Compound) bs);
//        } else {
//            return as.equals(bs);
//        }
        Term f = $("(x ==> y)");
        assertEquals("(x==>y)", f.root().toString());
        Term g = $("(y ==>+1 x)");
        assertEquals("(y==>x)", g.root().toString());
    }

    @Disabled
    @Test
    public void testEqualsAnonymous3() throws Narsese.NarseseException {
        //        if (as == bs) {
//            return true;
//        } else if (as instanceof Compound && bs instanceof Compound) {
//            return equalsAnonymous((Compound) as, (Compound) bs);
//        } else {
//            return as.equals(bs);
//        }
        //assertTrue(Terms.equalAtemporally($("(x && (y ==> z))"), $.$("(x &&+1 (y ==> z))")));
        //        if (as == bs) {
//            return true;
//        } else if (as instanceof Compound && bs instanceof Compound) {
//            return equalsAnonymous((Compound) as, (Compound) bs);
//        } else {
//            return as.equals(bs);
//        }

        assertEquals($.<Compound>$("(x && (y ==> z))").temporalize(Retemporalize.retemporalizeAllToXTERNAL),
                $.<Compound>$("(x &&+1 (y ==>+1 z))").temporalize(Retemporalize.retemporalizeAllToXTERNAL));
        //        if (as == bs) {
//            return true;
//        } else if (as instanceof Compound && bs instanceof Compound) {
//            return equalsAnonymous((Compound) as, (Compound) bs);
//        } else {
//            return as.equals(bs);
//        }
        assertEquals("((x &&+1 z) ==>+1 w)",
                $("(x &&+1 (z ==>+1 w))").toString());

        assertEquals($.<Compound>$("((x &&+- z) ==>+- w)").temporalize(Retemporalize.retemporalizeAllToXTERNAL),
                $.<Compound>$("(x &&+1 (z ==>+1 w))").temporalize(Retemporalize.retemporalizeAllToXTERNAL));
    }

//    @Test
//    public void testAtemporalization1() throws Narsese.NarseseException {
//        Term x = $("(((--,(tetris-->(_n,#2))) &&+1 $1) <=>+1 ($1 &&+0 (--,(tetris-->(_n,#2)))))");
//        Term y = x.eternal();
//        assertEquals("(((--,(tetris-->(_n,#1))) &&+- $2) <=>+- ((--,(tetris-->(_n,#1))) &&+- $2))", y.toString());
//    }


    @Test
    public void testEqualsAnonymous4() {
        //temporal terms within non-temporal terms
        //        if (as == bs) {
//            return true;
//        } else if (as instanceof Compound && bs instanceof Compound) {
//            return equalsAnonymous((Compound) as, (Compound) bs);
//        } else {
//            return as.equals(bs);
//        }
        //assertTrue(Terms.equalAtemporally($("(a <-> (y ==> z))"), $.$("(a <-> (y ==>+1 z))")));
        //        if (as == bs) {
//            return true;
//        } else if (as instanceof Compound && bs instanceof Compound) {
//            return equalsAnonymous((Compound) as, (Compound) bs);
//        } else {
//            return as.equals(bs);
//        }
        //assertFalse(Terms.equalAtemporally($("(a <-> (y ==> z))"), $.$("(a <-> (w ==>+1 z))")));

        //        if (as == bs) {
//            return true;
//        } else if (as instanceof Compound && bs instanceof Compound) {
//            return equalsAnonymous((Compound) as, (Compound) bs);
//        } else {
//            return as.equals(bs);
//        }
        //assertTrue(Terms.equalAtemporally($("((a ==> b),(b ==> c))"), $.$("((a ==> b),(b ==>+1 c))")));
        //        if (as == bs) {
//            return true;
//        } else if (as instanceof Compound && bs instanceof Compound) {
//            return equalsAnonymous((Compound) as, (Compound) bs);
//        } else {
//            return as.equals(bs);
//        }
        //assertTrue(Terms.equalAtemporally($("((a ==>+1 b),(b ==> c))"), $.$("((a ==> b),(b ==>+1 c))")));
    }

    @Test
    public void testEqualAtemporally5() {
        //special handling for images
        //        if (as == bs) {
//            return true;
//        } else if (as instanceof Compound && bs instanceof Compound) {
//            return equalsAnonymous((Compound) as, (Compound) bs);
//        } else {
//            return as.equals(bs);
//        }
        //assertTrue(Terms.equalAtemporally($("(/, (a ==> b), c, _)"), $.$("(/, (a ==>+1 b), c, _)")));
        //        if (as == bs) {
//            return true;
//        } else if (as instanceof Compound && bs instanceof Compound) {
//            return equalsAnonymous((Compound) as, (Compound) bs);
//        } else {
//            return as.equals(bs);
//        }
        //assertFalse(Terms.equalAtemporally($("(/, a, b, _)"), $.$("(/, a, _, b)")));
    }

    @Test
    public void testRetermporalization1() throws Narsese.NarseseException {

        String st = "((--,(happy)) && (--,((--,(o))&&(happy))))";
        Compound t = $.$(st);
        assertEquals("((--,((--,(o))&&(happy)))&&(--,(happy)))", t.toString());
        Term xe = t.temporalize(Retemporalize.retemporalizeXTERNALToDTERNAL);
        assertEquals("((--,((--,(o))&&(happy)))&&(--,(happy)))", xe.toString());

        //TODO this will require a refactor allowing arbitrary function mapping matched dt source value to a target dt
//        Term xz = $.terms.retemporalize(t, $.terms.retemporalizeZero);
//        assertEquals("((--,((--,(o))&|(happy)))&|(--,(happy)))", xz.toString());
    }

    @Test
    public void testConjSeqConceptual1() throws Narsese.NarseseException {
        assertConceptual("((--,(nario,zoom)) &&+- happy)", "((--,(nario,zoom)) && happy)");
        assertConceptual("((--,(nario,zoom)) &&+- happy)", "--((--,(nario,zoom)) && happy)");
        assertConceptual("((--,(nario,zoom)) &&+- happy)", "((--,(nario,zoom)) &&+- happy)");
        assertConceptual("(((--,(nario,zoom)) &&+- happy) &&+- (--,(x,(--,x))))", "(((--,(nario,zoom)) &&+- happy) &&+- (--,(x,(--,x))))");

        String c = "((--,(nario,zoom)) &&+- (vx &&+- vy))";
        assertConceptual(
                //WRONG:"((--,(nario,zoom)) &&+- (vx &&+- vy))"
                c, "((vx &&+97 vy) &&+156 (--,(nario,zoom)))");
        assertConceptual(
                c, "((vx &&+97 vy) &&+100 (--,(nario,zoom)))");
    }
    @Test
    public void testConjSeqConceptual2() throws Narsese.NarseseException {
        assertConceptual(
                "((--,(nario,zoom)) &&+- (vx &&+- vy))",
                "((vx &&+97 vy) &&-100 (--,(nario,zoom)))");

    }

    @Test
    public void testConceptual2() throws Narsese.NarseseException {

        Term x = $("((--,(vy &&+- happy)) &&+- (happy &&+- vy))");
        assertTrue(x instanceof Compound);
        Term y = $("((--,(vy &&+84 happy))&&(happy&|vy))");
        assertEquals(
                //"(&|,(--,(vy &&+84 happy)),happy,vy)",
                "((--,(vy &&+84 happy))&&(happy&|vy))",
                y.toString());
        assertEquals(
                //"(&&,(--,(happy &&+- vy)),happy,vy)",
                "((--,(happy &&+- vy)) &&+- (happy &&+- vy))",
                y.conceptual().toString());

    }

//    @Test
//    public void testConceptual2b() throws Narsese.NarseseException {
//        assertConceptual(
//                "(((--,(happy &&+- vy)) &&+- (happy &&+- vy))==>((--,(happy &&+- vy)) &&+- (--,vx)))",
//                "(((--,(vy &&+84 happy))&&(happy&|vy)) ==>+84 ((--,vx) &&+21 (--,(happy &&+146 vy))))");
//    }


    static void assertConceptual(String cexp, String c) throws Narsese.NarseseException {
        Term p = $(c);
        assertEquals(cexp, p.conceptual().toString());
    }

    @Test
    public void testRetermporalization2() throws Narsese.NarseseException {
        String su = "((--,(happy)) &&+- (--,((--,(o))&&+-(happy))))";
        Compound u = $.$(su);
        assertEquals("((--,((--,(o)) &&+- (happy))) &&+- (--,(happy)))", u.toString());

        Term ye = u.temporalize(Retemporalize.retemporalizeXTERNALToDTERNAL);
        assertEquals("((--,((--,(o))&&(happy)))&&(--,(happy)))", ye.toString());

        Term yz = u.temporalize(Retemporalize.retemporalizeXTERNALToZero);
        assertEquals("((--,((--,(o))&|(happy)))&|(--,(happy)))", yz.toString());

    }

//    @Test public void testRelationTaskNormalization() {
//        String a = "pick({t002})";
//        String b = "reachable:(SELF,{t002})";
//
//        String x = "(" + a + " &&+5 " + b + ")";
//        String y = "(" + b + " &&+5 " + a + ")";
//
//        NAR n = new Default();
//        Task xt = n.inputTask(x + ". :|:");
//        Task yt = n.inputTask(y + ". :|:");
//        out.println(xt);
//        out.println(yt);
//        assertEquals(5, xt.term().dt());
//        assertEquals(0, xt.occurrence());
//
//        //should have been shifted to place the earliest component at
//        // the occurrence time expected by the semantics of the input
//        assertEquals(-5, yt.term().dt());
//        assertEquals(5, yt.occurrence());
//
//
//    }

//    @Test
//    public void testAfter() {
//
//        assertTrue("after", Tense.after(1, 4, 1));
//
//        assertFalse("concurrent (equivalent)", Tense.after(4, 4, 1));
//        assertFalse("before", Tense.after(6, 4, 1));
//        assertFalse("concurrent (by duration range)", Tense.after(3, 4, 3));
//
//    }
}
