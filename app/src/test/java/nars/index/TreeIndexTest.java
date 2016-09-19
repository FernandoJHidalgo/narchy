package nars.index;

import com.google.common.collect.Sets;
import nars.concept.AtomConcept;
import nars.concept.Concept;
import nars.nar.Terminal;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static nars.$.$;
import static org.junit.Assert.*;

/**
 * Created by me on 2/25/16.
 */
public class TreeIndexTest {


    @Test
    public void testAtomInsertion() {

        TermTree tree = new TermTree();

        Function<Term, Concept> cb = (t)->new AtomConcept((Atomic)t, null, null);

        tree.computeIfAbsent("concept", cb);
        tree.computeIfAbsent("term", cb);
        tree.computeIfAbsent("termutator", cb);
        tree.print(System.out);

        assertNotNull(tree.get("term"));
        assertNull(tree.get("xerm"));
        assertNull(tree.get("te")); //partial

        assertNotNull(tree.computeIfAbsent("term", cb));
        assertEquals(3, tree.size());

        assertNotNull(tree.computeIfAbsent("termunator", cb));

        tree.print(System.out);

        assertEquals(4, tree.size());


//        String stringWithUnicode = "unicode\u00easomething";
//        assertNull( tree.resolveOrAdd(stringWithUnicode)); //unicode not supported yet

    }


    @Test
    public void testCompoundInsertion() {

        Terminal nar = new Terminal();
        TreeIndex index = new TreeIndex(nar.concepts.conceptBuilder(), 1000);

        String[] terms = {
            "x",
            "(x)", "(xx)", "(xxx)",
            "(x,y)", "(x,z)",
            "(x --> z)", "(x <-> z)",
            "(x&&z)","(/,x,_)","(/,_,x)"
        };
        HashSet<Term> input = new HashSet();
        for (String s : terms) {
            @NotNull Term ts = $(s);
            input.add(ts);
            index.get(ts, true);
        }

        assertEquals(terms.length, index.size());


        Set<Termed> stored = StreamSupport.stream(index.concepts.spliterator(), false).collect(Collectors.toSet());

        assertEquals(Sets.symmetricDifference(input, stored) + " = difference", input, stored);

        index.concepts.print();
        index.print(System.out);

//        String stringWithUnicode = "unicode\u00easomething";
//        assertNull( tree.resolveOrAdd(stringWithUnicode)); //unicode not supported yet

    }

}