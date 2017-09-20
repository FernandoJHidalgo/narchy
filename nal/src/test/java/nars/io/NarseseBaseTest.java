package nars.io;

import nars.$;
import nars.Narsese;
import nars.Op;
import nars.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.*;

public class NarseseBaseTest extends NarseseTest {

        @Test
    public void testParseCompleteEternalTask() throws Narsese.NarseseException {
        Task t = task("$0.99 <a --> b>! %0.93;0.95%");

        assertNotNull(t);
        assertEquals('!', t.punc());
        assertEquals(0.99f, t.pri(), 0.001);
        assertEquals(0.93f, t.freq(), 0.001);
        assertEquals(0.95f, t.conf(), 0.001);
    }

    @Test public void testTruth() throws Narsese.NarseseException {
        testTruth("%1;0.9%", 1f, 0.9f);
        testTruth("%1.0;0.90%", 1f, 0.9f);
        testTruth("%1.00;0.90%", 1f, 0.9f);
        testTruth("%1;0.90%", 1f, 0.9f);
        testTruth("%1;.9%", 1f, 0.9f);
        testTruth("%0;0.90%", 0f, 0.9f);
    }
//    @Test public void testTruthFreqOnly() {
//        testTruth("%0.0%", 0f, 0.9f);
//        testTruth("%1.0%", 1f, 0.9f);
//    }

    @Test
    public void testIncompleteTask() throws Narsese.NarseseException {
        Task t = task("<a --> b>.");
        assertNotNull(t);
        assertEquals(Op.INH, t.op());
        Term i = t.term();
        assertEquals("a", i.sub(0).toString());
        assertEquals("b", i.sub(1).toString());
        assertEquals('.', t.punc());
        //assertEquals(Global.DEFAULT_JUDGMENT_PRIORITY, t.getPriority(), 0.001);
        //assertEquals(Global.DEFAULT_JUDGMENT_DURABILITY, t.getDurability(), 0.001);
        assertEquals(1.0f, t.truth().freq(), 0.001);
        //assertEquals(Global.DEFAULT_JUDGMENT_CONFIDENCE, t.getTruth().getConfidence(), 0.001);
    }

    @Test
    public void testPropertyInstance() throws Narsese.NarseseException {

        taskParses("<a -{- b>.");
        taskParses("<a -]- b>.");
        taskParses("<a {-] b>.");
    }

    @Test
    public void testBudget() throws Narsese.NarseseException {
        Task t = task("$0.70 <a ==> b>. %0.00;0.93");
        assertEquals(0.7f, t.pri(), 0.01f);

        Task u = task("$0.9 <a ==> b>. %0.00;0.93");
        assertEquals(0.9f, u.pri(), 0.01f);
    }

    @Test
    public void testNoBudget() throws Narsese.NarseseException {
        Task t = task("<a ==> b>. %0.00;0.93");
        assertNotNull(t);
        assertEquals(Op.IMPL, t.op());

        assertEquals('.', t.punc());
        //assertEquals(Global.DEFAULT_JUDGMENT_PRIORITY, t.getPriority(), 0.001);
        //assertEquals(Global.DEFAULT_JUDGMENT_DURABILITY, t.getDurability(), 0.001);
        assertEquals(0.0f, t.freq(), 0.001);
        assertEquals(0.93f, t.conf(), 0.001);
    }

    @Test
    public void testMultiCompound() throws Narsese.NarseseException {
        String tt = "((a==>b)-->(c==>d))";
        Task t = task(tt + '?');
        assertNotNull(t);
        assertEquals(Op.INH, t.op());
        assertEquals(tt, t.term().toString());
        assertEquals('?', t.punc());
        assertNull(t.truth());
        assertEquals(7, t.term().complexity());
    }




    @Test
    public void testFailureOfMultipleDistinctInfixOperators() {
        assertInvalidTerms("(a * b & c)");
    }

    @Test
    public void testQuest() throws Narsese.NarseseException {
        String tt = "(a,b,c)";
        Task t = task(tt + '@');
        assertNotNull(t);
        assertEquals(Op.PROD, t.op());
        assertEquals(tt, t.term().toString());
        assertEquals('@', t.punc());
        assertNull(t.truth());

    }

    @Test
    public void testStatementTerms() throws Narsese.NarseseException {

        assertNotNull(term("< a --> b >"));
        assertNotNull(term("(a-->b)"));

    }

    @Test
    public void testProduct() throws Narsese.NarseseException {

        Compound pt = term("(a, b, c)");

        assertNotNull(pt);
        assertEquals(Op.PROD, pt.op());

        testProductABC(pt);

        testProductABC(term("(*,a,b,c)")); //with optional prefix
        testProductABC(term("(a,b,c)")); //without spaces
        testProductABC(term("(a, b, c)")); //additional spaces
        testProductABC(term("(a , b, c)")); //additional spaces
        testProductABC(term("(a , b , c)")); //additional spaces
        testProductABC(term("(a ,\tb, c)")); //tab
        //testProductABC(term("(a b c)")); //without commas
        //testProductABC(term("(a *  b * c)")); //with multiple (redundant) infix
    }

    @Test
    public void testInfix2() throws Narsese.NarseseException {
        Compound t = term("(x & y)");
        assertEquals(Op.SECTe, t.op());
        assertEquals(2, t.subs());
        assertEquals("x", t.sub(0).toString());
        assertEquals("y", t.sub(1).toString());

        Compound a = term("(x | y)");
        assertEquals(Op.SECTi, a.op());
        assertEquals(2, a.subs());

        Compound b = term("(x * y)");
        assertEquals(Op.PROD, b.op());
        assertEquals(2, b.subs());

        Compound c = term("(<a -->b> && y)");
        assertEquals(Op.CONJ, c.op());
        assertEquals(2, c.subs());
        assertEquals(5, c.complexity());
        assertEquals(Op.INH, c.sub(0).op()); //heavier term on the left
    }


    @Test
    public void testShortFloat() throws Narsese.NarseseException {

        taskParses("<{a} --> [b]>. %0;0.9%");
        taskParses("<a --> b>. %0.95;0.9%");
        taskParses("<a --> b>. %0.9;0.9%");
        taskParses("<a --> b>. %1;0.9%");
        taskParses("<a --> b>. %1.0;0.9%");
    }

    @Test
    public void testNegation() throws Narsese.NarseseException {
        taskParses("(--,(negated)).");
        taskParses("(--, (negated)).");

        assertEquals("(--,(negated))", term("(--, (negated))").toString());

    }



    private void testOperationStructure(@NotNull Compound t) {
        //Term[] aa = Operator.argArray(t);
        Term[] aa = ((Compound)t.sub(0)).toArray();
        assertEquals(2, aa.length);
        assertEquals("believe", t.sub(1).toString());
        //assertEquals("^believe", Operator.operator(t).toString());
        assertEquals("a", aa[0].toString());
        assertEquals("b", aa[1].toString());
    }

    @Test
    public void testOperationNoArgs() throws Narsese.NarseseException {
        Term t = term("op()");
        assertNotNull(t);
        assertEquals(t.toString(), Op.INH, t.op());
        //assertEquals(0, Operator.opArgs((Compound)t).size());

        taskParses("op()!");
        taskParses("op( )!");
    }



    @Test
    public void testOperation2() throws Narsese.NarseseException {
        testOperationStructure(term("believe(a,b)"));
        testOperationStructure(term("believe(a, b)"));
    }
    @Test
    public void testImplIsNotOperation() throws Narsese.NarseseException {
        assertEquals( "((b)==>a)", $.impl($.$("(b)"), Atomic.the("a")).toString() );
        assertEquals( "((b) ==>+1 a)", $.impl($.$("(b)"), 1, Atomic.the("a")).toString() );
    }
    @Test
    public void testOperationEquivalence() throws Narsese.NarseseException {
        Term a, b;
        a = term("a(b,c)");
        b = term("((b,c) --> a)");
        assertEquals(a.op(), b.op());
        assertEquals(a.getClass(), b.getClass());
        assertEquals(a, b);
    }
    @Test
    public void testOperationEquivalenceWithOper() throws Narsese.NarseseException {
        Term a;
        a = term("a(b,c)");
        Compound b = term("((b,c) --> a)");

        assertEquals(a, b);

        assertEquals(a.op(), b.op());
        assertEquals(a.getClass(), b.getClass());

        assertEquals(Op.ATOM, b.sub(1).op());

    }

    @Test
    public void testOperationTask() throws Narsese.NarseseException {
        taskParses("break({t001},SELF)! %1.00;0.95%");
    }



    @Test
    public void testCompoundTermOpenerCloserStatements() throws Narsese.NarseseException {
        Term a = term("<a --> b>");
        Term x = term("(a --> b)");
        Term y = term("(a-->b)");
        assertEquals(Op.INH, x.op());
        assertEquals(x, a);
        assertEquals(x, y);

        assertNotNull(term("((a,b)-->c)")); //intermediate
        assertNotNull(term("((a,b) --> c)")); //intermediate
        assertNotNull(term("<(a,b) --> c>")); //intermediate
        assertNotNull(term("<a --> (c,d)>")); //intermediate
        assertNotNull(term("<a-->(c,d)>")); //intermediate
        assertNotNull(term("(a-->(c,d))")); //intermediate
        assertNotNull(term("(a --> (c,d))")); //intermediate

        Term abcd = term("((a,b) --> (c,d))");
        Term ABCD = term("<(*,a,b) --> (*,c,d)>");
        assertEquals(Op.INH, x.op());
        assertEquals(abcd + " != " + ABCD, abcd, ABCD);
    }

    @NotNull
    protected Variable testVar(char prefix) throws Narsese.NarseseException {
        Term x = term(prefix + "x");
        assertNotNull(x);
        assertTrue(x instanceof Variable);
        Variable i = (Variable) x;
        assertEquals(prefix + "x", i.toString());
        return i;
    }

    @Test
    public void testVariables() throws Narsese.NarseseException {
        Variable v;
        v = testVar(Op.VAR_DEP.ch);
        assertTrue(v.hasVarDep());

        v = testVar(Op.VAR_INDEP.ch);
        assertTrue(v.hasVarIndep());

        v = testVar(Op.VAR_QUERY.ch);
        assertTrue(v.hasVarQuery());
    }

    @Test public void testQueryVariableTask() throws Narsese.NarseseException {
        String term = "hear(Time,(the,?x))";
        assertEquals("hear(Time,(the,?x))", term( term ).toString());
        assertEquals("$.25 hear(Time,(the,?1))?", task(term + "?").toString());
    }
    @Test public void testQueryVariableTaskQuotes() throws Narsese.NarseseException {
        String term = "hear(\"Time\",(\"the\",?x))";
        assertEquals("hear(\"Time\",(\"the\",?x))", term( term ).toString());
        assertEquals("$.25 hear(\"Time\",(\"the\",?1))?", task(term + "?").toString());
    }

    @Test
    public void testSet() throws Narsese.NarseseException {
        Compound xInt = term("[x]");
        assertEquals(Op.SETi, xInt.op());
        assertEquals(1, xInt.subs());
        assertEquals("x", xInt.sub(0).toString());

        Compound xExt = term("{x}");
        assertEquals(Op.SETe, xExt.op());
        assertEquals(1, xExt.subs());
        assertEquals("x", xExt.sub(0).toString());

        Compound abInt = term("[a,b]");
        assertEquals(2, abInt.subs());
        assertEquals("a", abInt.sub(0).toString());
        assertEquals("b", abInt.sub(1).toString());

        assertEquals(abInt, term("[ a,b]"));
        assertEquals(abInt, term("[a,b ]"));
        assertEquals(abInt, term("[ a , b ]"));


    }

    @Test
    public void testTenses() throws Narsese.NarseseException {
        Task now = task("<a --> b>. :|:");
        Task f = task("<a --> b>. :/:");
        Task p = task("<a --> b>. :\\:");
        assertTrue(now.start() > p.start());
        assertTrue(now.start() < f.start());
    }

    @Test
    public void testQuoteEscape() throws Narsese.NarseseException {
        assertEquals("\"ab c\"", term("\"ab c\"").toString());
        for (String x : new String[] { "a", "a b" }) {
            taskParses("<a --> \"" + x + "\">.");
            assertTrue(task("<a --> \"" + x + "\">.").toString().contains("(a-->\"" + x + "\")."));
        }
    }
    @Test
    public void testQuoteEscapeBackslash() {
        //TODO
        //assertEquals("")
    }

    @Test
    public void testFuzzyKeywords() {
        //definately=certainly, uncertain, doubtful, dubious, maybe, likely, unlikely, never, always, yes, no, sometimes, usually, rarely, etc...
        //ex: %maybe never%, % doubtful always %, %certainly never%
    }

    @Test
    public void testEmbeddedJavascript() {

    }

    @Test
    public void testEmbeddedPrologRules() {

    }

    /**
     * test ability to report meaningful parsing errors
     */
    @Test
    public void testError() {

    }

    @Test
    public void testSimpleTask() throws Narsese.NarseseException {
        taskParses("(-,mammal,swimmer). %0.00;0.90%");

    }

    @Test
    public void testCompleteTask() throws Narsese.NarseseException {
        taskParses("$0.80 <<lock1 --> (/,open,$1,_)> ==> <$1 --> key>>. %1.00;0.90%");
    }

    @Test public void testNonNegativeIntegerAtoms() throws Narsese.NarseseException {
        //TODO test parsing to numeric atom types
        Term a = term("1");
        assertEquals("1", a.toString());
    }

    @Test public void testNegativeIntegerAtoms() throws Narsese.NarseseException {
        //TODO test parsing to numeric atom types
        Term a = term("-1");
        assertNotNull(a);
        assertEquals("-1", a.toString());
    }
    @Test public void testFloatAtom() throws Narsese.NarseseException {
        //TODO test parsing to numeric atom types
        float f = 1.24f;
        String ff = Float.toString(f);
        Atomic a = term(ff);
        assertNotNull(a);
        assertEquals('"' + ff + '"', a.toString());
    }


}
