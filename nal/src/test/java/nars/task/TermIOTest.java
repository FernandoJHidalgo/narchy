package nars.task;

import com.google.common.collect.Sets;
import nars.$;
import nars.IO;
import nars.NAR;
import nars.Task;
import nars.nar.Default;
import nars.nar.Terminal;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

    final NAR nar = new Terminal();

    void assertEqualSerialize(@NotNull Object orig) {
        //final IO.DefaultCodec codec = new IO.DefaultCodec(nar.index);


        byte barray[];
        if (orig instanceof Task) {
            Task torig = (Task) orig;
            if (torig.isDeleted())
                throw new RuntimeException("task is deleted already");
            barray = IO.asBytes(torig);
        }
        else if (orig instanceof Term)
            barray = IO.asBytes((Term)orig);
        else
            throw new RuntimeException("");

        out.println(orig + "\n\tserialized: " + barray.length + " bytes " + Arrays.toString(barray));


        Object copy;
        if (orig instanceof Task)
            copy = IO.taskFromBytes(barray, nar.concepts);
        else if (orig instanceof Term)
            copy = IO.termFromBytes(barray, nar.concepts);
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
        assertEquals(copy, orig);
        assertEquals(copy.hashCode(), orig.hashCode());
        //assertEquals(copy.getClass(), orig.getClass());
    }


    //    /* https://github.com/RuedigerMoeller/fast-serialization/wiki/Serialization*/
    @Test
    public void testTermSerialization() {

        assertEqualSerialize(nar.term("<a-->b>").term() /* term, not the concept */);
        assertEqualSerialize(nar.term("<aa-->b>").term() /* term, not the concept */);
        assertEqualSerialize(nar.term("<aa--><b<->c>>").term() /* term, not the concept */);
        assertEqualSerialize(nar.term("(a &&+1 b)").term() /* term, not the concept */);
        assertEqualSerialize(nar.term("(/, x, _, y)").term() /* term, not the concept */);
        assertEqualSerialize(nar.term("exe(a,b)").term() /* term, not the concept */);
    }
    @Test
    public void testTermSerialization2() {
        assertTermEqualSerialize("<a-->(be)>");
    }
    @Test
    public void testTermSerialization3() {
        assertTermEqualSerialize("(#a --> b)");
    }
    @Test
    public void testTermSerialization3_2() {
        //multiple variables

        Variable q = $.varQuery(1);
        Compound twoB = $.inh( $.varDep(2), $.the("b"));
        assertNotEquals(
                q.compareTo(twoB),
                twoB.compareTo(q) );

        assertTermEqualSerialize("((#a --> b) <-> ?c)");

        Term a = $("(#2-->b)");
        Term b = $("?1");
        int x = a.compareTo(b);
        int y = b.compareTo(a);
        assertNotEquals((int) Math.signum(x), (int) Math.signum(y));

    }

    void assertTermEqualSerialize(@NotNull String s) {
        Termed t = nar.term(s);
        assertTrue(t.isNormalized());
        assertTrue(t.term().isNormalized());
        assertEqualSerialize(t.term() /* term, not the concept */);
    }

    @Test
    public void testTaskSerialization() {
        assertEqualSerialize(nar.inputTask("<a-->b>."));
        assertEqualSerialize(nar.inputTask("<a-->(b==>c)>!"));
        assertEqualSerialize(nar.inputTask("<a-->(b==>c)>?"));
        assertEqualSerialize(nar.inputTask("$0.1;0.2;0.4$ (b-->c)! %1.0;0.8%"));
    }

    @Test public void testTaskSerialization2() {
        assertEqualSerialize(nar.inputTask("$0.3;0.2;0.1$ (a-->(bd))! %1.0;0.8%"));
    }

    @Test public void testNARTaskDump() throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream(16384);

        NAR a = new Default()
                        .input("a:b.", "b:c.", "c:d!")
                        .run(32)
                        .output(baos);

        byte[] x = baos.toByteArray();
        out.println("NAR tasks serialized: " + x.length + " bytes");

        NAR b = new Default()
                        .input(new ByteArrayInputStream(x))
                        .next()
                        //.forEachConceptTask(true,true,true,true, out::println)
                        //.forEachConcept(System.out::println)
                        ;

        //dump all tasks to a set of sorted strings and compare their equality:
        Set<String> ab = new HashSet();
        Set<String> bb = new HashSet();
        a.forEachConceptTask(t->ab.add(t.toStringWithoutBudget()), true,true,true,true);
        b.forEachConceptTask(t->bb.add(t.toStringWithoutBudget()), true,true,true,true);
        assertEquals("difference: " + Sets.symmetricDifference(ab, bb), ab, bb);

//        //measure with budgets but allow only a certain one budget difference, due to rounding issues
//        Set<String> abB = new HashSet();
//        Set<String> bbB = new HashSet();
//        a.forEachConceptTask(t->abB.add(t.toString()), true,true,true,true);
//        b.forEachConceptTask(t->bbB.add(t.toString()), true,true,true,true);
//        Sets.SetView<String> diff = Sets.symmetricDifference(abB, bbB);
//        assertTrue("diff: " + diff.toString() + "\n\t" + abB + "\n\t" + bbB, 2 >= diff.size());
    }

}

