package nars.io;

import nars.*;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Variable;
import nars.term.anon.Anom;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.Int;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.System.out;
import static nars.$.$;
import static nars.$.$$;
import static nars.Op.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Term serialization
 */
public class TermIOTest {

    final NAR nar = NARS.shell();

    void assertEqualSerialize(@NotNull String orig) throws Narsese.NarseseException, IOException {
        assertEqualSerialize($.$(orig).term());
    }

    void assertEqualTask(@NotNull String orig) throws Narsese.NarseseException, IOException {
        assertEqualSerialize((Object)nar.inputTask(orig));
    }

    static byte[] assertEqualSerialize(@NotNull Object orig) throws IOException {
        


        byte barray[];
        if (orig instanceof Task) {
            Task torig = (Task) orig;
            if (torig.isDeleted())
                throw new RuntimeException("task is deleted already");
            barray = IO.taskToBytes(torig);
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

        
        
        
        
        
        

        

        
        out.println("\t" + (copy == orig ? "same" : "copy") + ": " + copy);

        

        
        assertEquals(orig, copy);
        assertEquals(copy, orig);
        assertEquals(orig.toString(), copy.toString());
        assertEquals(orig.hashCode(), copy.hashCode());
        

        
        return barray;
    }


    
    @Test
    public void testTermSerialization() throws Exception {

        assertEqualSerialize("<a-->b>" /* term, not the concept */);
        assertEqualSerialize("<aa-->b>" /* term, not the concept */);
        assertEqualSerialize("<aa--><b<->c>>" /* term, not the concept */);
        
        assertEqualSerialize("exe(a,b)" /* term, not the concept */);
    }

    @Test
    public void testNegationSerialization() throws Exception {
        assertEqualSerialize("--x");

        
        assertEquals(1,
                IO.termToBytes($$("(x)")).length -
                IO.termToBytes($$("(--,x)")).length);
    }

    @Test
    public void testTemporalSerialization() throws Exception {

        assertEqualSerialize("(a &&+1 b)" /* term, not the concept */);
        assertEqualSerialize("(a &&+1 (a &&+1 a))" /* term, not the concept */);
        assertEqualSerialize("(a ==>+1 b)" /* term, not the concept */);
        assertEqualSerialize("(b ==>+1 b)" /* term, not the concept */);

        assertEqualSerialize("(a ==>+- b)");
        assertEqualSerialize("(a ==>+- a)");
        assertEqualSerialize("(a ==> b)");

    }

    @Test
    public void testImageSerialization() throws Exception {
        assertEqualSerialize("/");
        assertEqualSerialize("\\");
        assertEqualSerialize("(a,/,1)");
        assertEqualSerialize("(a,/,1,/,x)");
        assertEqualSerialize("(x --> (a,/,1))");
        assertEqualSerialize("(a,\\,1)");
        assertEqualSerialize("(a,\\,1,\\,2)");
        assertEqualSerialize("((a,\\,1)--> y)");
    }

    @Test public void testUnnormalizedVariableSerialization() throws Exception {
        assertEqualSerialize("#abc");
        assertEqualSerialize("$abc");
        assertEqualSerialize("?abc");
        assertEqualSerialize("%abc");
    }

    @Test
    public void testAnonSerialization() throws IOException {

        Term[] anons = new Term[] {
                $.v(VAR_DEP, (byte)1),
                $.v(VAR_INDEP, (byte)1),
                $.v(VAR_QUERY, (byte)1),
                $.v(VAR_PATTERN, (byte)1),
                Atomic.the("x"),
                Int.the(1),
                Anom.the(1),
        };

        for (Term a : anons) {
            a.printRecursive();
            byte[] b = assertEqualSerialize(a);

            
            if (a instanceof Anom) {
                assertEquals(2, b.length); 
            } else if (a.op() == INT) {
                assertEquals(5, b.length); 
            } else if (a instanceof Atom) {
                assertEquals(4, b.length); 
            }
        }

        assertEqualSerialize($.p( anons ));

        assertEqualSerialize($.p( Anom.the(1), Anom.the(2) ));
        assertEqualSerialize($.p( Anom.the(1), $.varDep(2) ));
    }

    @Test
    public void testTermSerialization2() throws Exception {
        assertTermEqualSerialize("(a-->(be))");
    }

    @Test
    public void testTermSerialization3() throws Exception {
        assertTermEqualSerialize("(#1 --> b)");
    }

    @Test
    public void testTermSerialization3_2() throws Exception {
        

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

    static void assertTermEqualSerialize(String s) throws Narsese.NarseseException, IOException {
        Termed t = $.$(s);
        assertTrue(t.term().isNormalized());
        assertEqualSerialize(t.term() /* term, not the concept */);
    }

    @Test
    public void testTaskSerialization() throws Exception {
        assertEqualTask("(a-->b).");
        assertEqualTask("(a-->(b,c))!");
        assertEqualTask("(a-->(b==>c))?");
        assertEqualTask("$0.1 (b-->c)! %1.0;0.8%");
        assertEqualTask("$0.1 (b-->c)! :|: %1.0;0.8%");
        assertEqualTask("$0.1 (a ==>+4 (b-->c)). :|: %1.0;0.8%");
        assertEqualTask("$0.1 (1 ==>+4 (2-->3)). :|: %1.0;0.8%");

        assertEqualTask("(x ==>+- y)?");
        assertEqualTask("(x ==>+- y)? :|:");
        assertEqualTask("(x ==>+- x)?");
        assertEqualTask("(x ==>+- x)? :|:");
        assertEqualTask("(x &&+- y)?");
        assertEqualTask("(x &&+- y)? :|:");
        assertEqualTask("(x &&+- x)?");
        assertEqualTask("(x &&+- x)? :|:");

        assertEqualTask("(x &&+- x)@");
        assertEqualTask("(x &&+- x)@ :|:");

        assertEqualTask("cmd(x,y,z);");
        assertEqualTask("(x &&+- x);");

    }

    @Test
    public void testTaskSerialization2() throws Exception {
        assertEqualSerialize((Object)nar.inputTask("$0.3 (a-->(bd))! %1.0;0.8%"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "a:b. b:c.",
        "a:b. b:c. c:d! a@",
        "d(x,c). :|: (x<->c)?",
        "((x &&+1 b) &&+1 c). :|: (c && --b)!"
    })
    public void testNARTaskSaveAndReload(String input) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream(16384);

        final AtomicInteger count = new AtomicInteger();



        Set<Task> written = new HashSet();

        NAR a = NARS.tmp()

                .input(new String[] { input });
        a
                .run(16);
        a
                .synch()
                .outputBinary(baos, (Task t)->{
                    if (!written.add(t)) {
                        System.err.println("duplicate: " + t);
                        fail();
                    }
                    count.incrementAndGet();
                    return true;
                })
                
        ;

        byte[] x = baos.toByteArray();
        out.println(count.get() + " tasks serialized in " + x.length + " bytes");

        NAR b = NARS.shell()
                .inputBinary(new ByteArrayInputStream(x));
                
                
                
        

        

        Set<Task> aHas = new HashSet();

        a.tasks().forEach((Task t) -> aHas.add(t) ); 

        assertEquals(count.get(), aHas.size());

        assertEquals(written, aHas);

        Set<Task> bRead = new HashSet();

        b.tasks().forEach(t -> bRead.add(t));

        assertEquals(aHas, bRead);



        








    }

    @Test
    public void testByteMappingAtom() throws Exception {
        assertEquals("(0,0)=. ", map("x"));
    }


    @Test
    public void testByteMappingInh() throws Exception {
        assertEquals("(0,0)=--> (1,2)=. (1,6)=. ", map("a:b"));
    }

    @Test
    public void testByteMappingCompoundDT() throws Exception {
        assertEquals("(0,0)===> (1,2)=. (1,6)=. ",
                map("(a ==>+1 b)"));
    }

    @Test
    public void testByteMappingCompoundDTExt() throws Exception {
        assertEquals("(0,0)=--> (1,2)===> (2,4)=. (2,8)=. (1,16)=. ",
                map("((a ==>+1 b) --> c)"));
    }

    @Test
    public void testByteMappingCompound() throws Exception {
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
            
            sb.append(msg);
        });
        return sb.toString();
    }










}

