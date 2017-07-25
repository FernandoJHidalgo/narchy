package nars.io;

import com.google.common.collect.Sets;
import nars.*;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static java.lang.System.out;
import static nars.$.$;
import static org.junit.Assert.*;

/**
 * Term serialization
 */
public class TermIOTest {

    final NAR nar = NARS.shell();

    void assertEqualSerialize(@NotNull String orig) throws Narsese.NarseseException {
        assertEqualSerialize(nar.term(orig).term());
    }

    void assertEqualSerialize(@NotNull Object orig) {
        //final IO.DefaultCodec codec = new IO.DefaultCodec(nar.index);


        byte barray[];
        if (orig instanceof Task) {
            Task torig = (Task) orig;
            if (torig.isDeleted())
                throw new RuntimeException("task is deleted already");
            barray = IO.asBytes(torig);
        } else if (orig instanceof Term)
            barray = IO.termToBytes((Term) orig);
        else
            throw new RuntimeException("");

        out.println(orig + "\n\tserialized: " + barray.length + " bytes " + Arrays.toString(barray));


        Object copy;
        if (orig instanceof Task)
            copy = IO.taskFromBytes(barray);
        else if (orig instanceof Term)
            copy = IO.termFromBytes(barray);
        else
            throw new RuntimeException("");

        //if (copy instanceof Task) {
        //((MutableTask)copy).invalidate();
        //((Task)copy).normalize(nar);
        //out.println("\t\t" +((Task)orig).explanation());
        //out.println("\t\t" +((Task)copy).explanation());
        //}

        //Terms.printRecursive(System.out, (Task)orig, 10);

        //System.out.println("\tbytes: " + Arrays.toString(barray));
        out.println("\tcopy: " + copy);

        //Terms.printRecursive(System.out, (Term)copy, 10);

        //assertTrue(copy != orig);
        assertEquals(orig, copy);
        assertEquals(orig.hashCode(), copy.hashCode());

        //assertEquals(copy.getClass(), orig.getClass());
    }


    //    /* https://github.com/RuedigerMoeller/fast-serialization/wiki/Serialization*/
    @Test
    public void testTermSerialization() throws Narsese.NarseseException {

        assertEqualSerialize("<a-->b>" /* term, not the concept */);
        assertEqualSerialize("<aa-->b>" /* term, not the concept */);
        assertEqualSerialize("<aa--><b<->c>>" /* term, not the concept */);
        //assertEqualSerialize(("(/, x, _, y)") /* term, not the concept */);
        assertEqualSerialize(("exe(a,b)") /* term, not the concept */);
    }

    @Test
    public void testTemporalSerialization() throws Narsese.NarseseException {

        assertEqualSerialize(("(a &&+1 b)") /* term, not the concept */);
        assertEqualSerialize("(a &&+1 (a &&+1 a))" /* term, not the concept */);
        assertEqualSerialize("(a ==>+1 b)" /* term, not the concept */);
        assertEqualSerialize("(a <=>+1 b)" /* term, not the concept */);
        assertEqualSerialize("(a <=>+1 a)" /* term, not the concept */);
        assertEqualSerialize(("(b ==>+1 b)") /* term, not the concept */);


    }


    @Test
    public void testTermSerialization2() throws Narsese.NarseseException {
        assertTermEqualSerialize("<a-->(be)>");
    }

    @Test
    public void testTermSerialization3() throws Narsese.NarseseException {
        assertTermEqualSerialize("(#1 --> b)");
    }

    @Test
    public void testTermSerialization3_2() throws Narsese.NarseseException {
        //multiple variables

        Variable q = $.varQuery(1);
        Compound twoB = $.inh($.varDep(2), Atomic.the("b"));
        assertNotEquals(
                q.compareTo(twoB),
                twoB.compareTo(q));

        assertTermEqualSerialize("((#a --> b) <-> ?c)");

        Term a = $("(#2-->b)");
        Term b = $("?1");
        int x = a.compareTo(b);
        int y = b.compareTo(a);
        assertNotEquals((int) Math.signum(x), (int) Math.signum(y));

    }

    void assertTermEqualSerialize(@NotNull String s) throws Narsese.NarseseException {
        Termed t = nar.term(s);
        assertTrue(t.isNormalized());
        assertTrue(t.term().isNormalized());
        assertEqualSerialize(t.term() /* term, not the concept */);
    }

    @Test
    public void testTaskSerialization() throws Narsese.NarseseException {
        assertEqualSerialize(nar.inputAndGet("<a-->b>."));
        assertEqualSerialize(nar.inputAndGet("<a-->(b==>c)>!"));
        assertEqualSerialize(nar.inputAndGet("<a-->(b==>c)>?"));
        assertEqualSerialize(nar.inputAndGet("$0.1 (b-->c)! %1.0;0.8%"));
    }

    @Test
    public void testTaskSerialization2() throws Narsese.NarseseException {
        assertEqualSerialize(nar.inputAndGet("$0.3 (a-->(bd))! %1.0;0.8%"));
    }

    @Test
    public void testNARTaskDump() throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream(16384);

        NAR a = NARS.tmp()
                .input("a:b.", "b:c.", "c:d!")
                .run(32)
                .output(baos);

        byte[] x = baos.toByteArray();
        out.println("NAR tasks serialized: " + x.length + " bytes");

        NAR b = NARS.tmp()
                .inputBinary(new ByteArrayInputStream(x)).run(1)
                //.next()
                //.forEachConceptTask(true,true,true,true, out::println)
                //.forEachConcept(System.out::println)
                ;

        //dump all tasks to a set of sorted strings and compare their equality:
        Set<String> ab = new HashSet();
        a.tasks().forEach(t -> ab.add(t.toStringWithoutBudget()));

        Set<String> bb = new HashSet();
        b.tasks().forEach(t -> bb.add(t.toStringWithoutBudget()));

        assertEquals("difference: " + Sets.symmetricDifference(ab, bb), ab, bb);

//        //measure with budgets but allow only a certain one budget difference, due to rounding issues
//        Set<String> abB = new HashSet();
//        Set<String> bbB = new HashSet();
//        a.forEachConceptTask(t->abB.add(t.toString()), true,true,true,true);
//        b.forEachConceptTask(t->bbB.add(t.toString()), true,true,true,true);
//        Sets.SetView<String> diff = Sets.symmetricDifference(abB, bbB);
//        assertTrue("diff: " + diff.toString() + "\n\t" + abB + "\n\t" + bbB, 2 >= diff.size());
    }

    @Test
    public void testByteMappingAtom() throws IOException, Narsese.NarseseException {
        assertEquals("(0,0)=. ", map("x"));
    }


    @Test
    public void testByteMappingInh() throws IOException, Narsese.NarseseException {
        assertEquals("(0,0)=--> (1,2)=. (1,6)=. ", map("a:b"));
    }

    @Test
    public void testByteMappingCompoundDT() throws IOException, Narsese.NarseseException {
        assertEquals("(0,0)===> (1,2)=. (1,6)=. ",
                map("(a ==>+1 b)"));
    }

    @Test
    public void testByteMappingCompoundDTExt() throws IOException, Narsese.NarseseException {
        assertEquals("(0,0)=--> (1,2)===> (2,4)=. (2,8)=. (1,16)=. ",
                map("((a ==>+1 b) --> c)"));
    }

    @Test
    public void testByteMappingCompound() throws IOException, Narsese.NarseseException {
        assertEquals("(0,0)===> (1,2)=--> (2,4)=* (3,6)=. (3,10)=. (2,16)=. (1,20)=. ",
                map("(a(b,\"c\") ==>+1 d)"));
    }

    public String map(String x) throws IOException, Narsese.NarseseException {
        return map($.$(x));
    }

    public String map(Term x) throws IOException {
        byte[] xb = IO.termToBytes(x);
        StringBuilder sb = new StringBuilder();
        IO.mapSubTerms(xb, (o, depth, i) -> {
            String msg = "(" + depth + "," + i + ")=" + o + " ";
            //System.out.println(msg);
            sb.append(msg);
        });
        return sb.toString();
    }


//    @Test public void testJacksonCompound() throws Narsese.NarseseException, IOException {
//        Compound c = $.$("(a-->b)");
//        byte[] b = Util.toBytes(c);
//        System.out.println(b.length + " " + Arrays.toString(b));
//        Compound c2 = Util.fromBytes(b, c.getClass() /*Compound.class*/);
//        System.out.println(c2);
//        assertEquals(c, c2);
//    }
}

