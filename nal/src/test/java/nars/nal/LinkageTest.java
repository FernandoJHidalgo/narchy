package nars.nal;


import com.google.common.graph.MutableValueGraph;
import jcog.pri.PriReference;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.concept.Concept;
import nars.term.Term;
import nars.term.Termed;
import nars.term.var.Variable;
import nars.test.TestNAR;
import nars.util.AbstractNALTest;
import nars.util.graph.TermGraph;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Ignore;
import org.junit.Test;

import static nars.Op.*;
import static org.junit.Assert.*;

//don't touch this file - patham9

public class LinkageTest extends AbstractNALTest {

    int runCycles = 10;


//    @Override
//    public TestNAR test(NAR n) {
//        return new TestNAR(n) {
//            @Override protected boolean requireConditions() {
//                return false;
//            }
//        };
//    }


    public void ProperlyLinkedTest(@NotNull String premise1, @NotNull String premise2) throws Exception {

        test.requireConditions = false;
        TestNAR tester = test;
        tester.believe(premise1); //.en("If robin is a type of bird then robin can fly.");
        tester.believe(premise2); //.en("Robin is a type of bird.");

        tester.run(runCycles, false);

        Concept ret = tester.nar.conceptualize(premise1);
        assertTrue(ret + " termlinks contains " + premise2, isPassed2(premise2, ret));

        Concept ret2 = tester.nar.conceptualize(premise2);
        assertTrue(ret2 + " termlinks contains " + premise1, isPassed2(premise1, ret2));


//        if(passed && passed2) { //dummy to pass the test:
//            tester.believe("<a --> b>");
//        } else {
//            throw new Exception("failed");
//        }
//        tester.mustBelieve(10,"<a --> b>",0.9f);
    }

    public boolean isPassed2(String premise1Str, @Nullable Concept ret2) {
        Term premise1 = null;
        try {
            premise1 = $.$(premise1Str).conceptual();
        } catch (Narsese.NarseseException e) {
            return false;
        }
        if (ret2 != null) {// && ret2.getTermLinks()!=null) {
            for (PriReference<Term> entry : ret2.termlinks()) {
                Term w = entry.get().term();
                if (w.equals(premise1)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void ProperlyLinkedIndirectlyTest(@NotNull String spremise1, @NotNull String spremise2) throws Exception {
        ProperlyLinkedIndirectlyTest(spremise1, BELIEF, spremise2);
    }

    //interlinked with an intermediate concept, this is needed in order to select one as task and the other as belief
    public void ProperlyLinkedIndirectlyTest(@NotNull String spremise1, byte punc, @NotNull String spremise2) throws Exception {


        test.requireConditions = false;
        NAR nar = test.nar;

        //nar.log();

        Termed premise1 = nar.term(spremise1);
        assertEquals("reparsed", $.$(spremise1), premise1);
        assertNotNull(premise1);
        assertEquals(nar.term(spremise1), premise1);

        Termed premise2 = nar.term(spremise2);
        assertEquals("reparsed", $.$(spremise2), premise2);
        assertNotNull(premise2);
        assertEquals(nar.term(spremise2), premise2);

        String t1 = getTask(punc, premise1);
        String t2 = getTask(punc, premise2);

        nar.input(t1, t2).run(runCycles);

        //List<String> fails = new ArrayList();


        @Nullable Concept p1 = nar.concept(premise1);
        assertNotNull(p1.state());


        //p1.print(); System.out.println("------------------------");

        Concept p2 = nar.concept(premise2);
        assertNotNull(p2);
        assertNotNull(p2.state());
        //c2.print(); System.out.println("------------------------");

        MutableValueGraph<Term, Float> g = TermGraph.termlink(nar);
        System.out.println("\tEdges:" + g.edges());

        boolean p12 = linksIndirectly(p1, p2, nar);
        assertTrue(premise1 + " no link to " + premise2, p12);

        boolean p21 = linksIndirectly(p2, p1, nar);
        assertTrue(premise1 + " no link to " + p2, p21);


        //System.err.println(premise1 + " not linked with " + premise2);

        int numNodes = g.nodes().size();
        assertTrue(numNodes > 0);
        assertTrue(g.toString(), !g.edges().isEmpty());

//        for (Term x : g.nodes()) {
//            assertEquals(x + " not reachable by all other nodes", numNodes, Graphs.reachableNodes(g.asGraph(), x).size());
//        }

//        //g.print(System.out);
//        //System.out.println(g.isConnected() + " " + g.vertexSet().size() + " " + g.edgeSet().size());
//        if (!g.isConnected()) {
////        if (!g.isStronglyConnected()) {
////            StrongConnectivityInspector ci =
//            ConnectivityInspector ci = new ConnectivityInspector(g);
////                    new StrongConnectivityInspector(g);
//
//            System.out.println("premises: " + premise1 + " and " + premise2 + " termlink subgraph connectivity:");
//
//            ci
//                .connectedSets()
//                //.stronglyConnectedSubgraphs()
//                .forEach( s -> System.out.println("\t" + s));
//
//            nar.forEachConceptActive(x -> x.get().print());
//
//        }
//        assertTrue(g.isConnected());


    }

    @NotNull
    public String getTask(byte punc, @NotNull Termed premise1) {
        if (punc == QUESTION) {
            return premise1.toString() + (char) (QUESTION);
        } else {
            return premise1.toString() + (char) (punc) + " %1.0;0.9%";
        }
    }

    public boolean linksIndirectly(@NotNull Concept src, @NotNull Concept target, @NotNull NAR nar) {


        for (PriReference<Term> entry : src.termlinks()) {

            //test 1st level link
            Term w = entry.get();
            if (target.equals(w))
                return true;

            Concept ww = nar.concept(w);

            if (ww != null) {
                if (target.equals(ww)) {
                    return true;
                }

                //test 2nd level link
                for (PriReference<Term> entry2 : ww.termlinks()) {
                    Term e = entry2.get();
                    if (target.equals(e))
                        return true;

                    Concept ee = nar.concept(e);
                    if (ee != null && target.equals(ee))
                        return true;

                }
            }

        }
        return false;
    }


    //interlinked with an intermediate concept, this is needed in order to select one as task and the other as belief
    public void ProperlyLinkedIndirectlyLayer2Test(@NotNull String premise1, @NotNull String premise2) throws Exception {

        TestNAR tester = test;
        tester.believe(premise1); //.en("If robin is a type of bird then robin can fly.");
        tester.believe(premise2); //.en("Robin is a type of bird.");
        tester.nar.run(1);

        boolean passed = links(premise1, premise2, tester);
        boolean passed2 = links(premise2, premise1, tester);
        assertTrue(passed);
        assertTrue(passed2);


        //dummy
        tester.believe("<a --> b>");
        tester.mustBelieve(1, "<a --> b>", 0.9f);
    }

    public boolean links(@NotNull String premise1, String premise2, @NotNull TestNAR tester) throws Narsese.NarseseException {
        Concept ret = tester.nar.conceptualize(premise1);
        boolean passed = false;
        if (ret != null) {
            for (PriReference<Term> entry : ret.termlinks()) {
                Term et1 = entry.get().term();
                if (et1.toString().equals(premise2)) {
                    passed = true;
                    break;
                }

                if (!(et1 instanceof Variable)) {
                    Concept Wc = tester.nar.concept(et1);
                    if (Wc != null) {
                        for (PriReference<Term> entry2 : Wc.termlinks()) {
                            Term et2 = entry2.get().term();
                            if (et2.toString().equals(premise2)) {
                                passed = true;
                                break;
                            }
                            Concept Wc2 = tester.nar.concept(et2);
                            if (Wc2 != null) {
                                for (PriReference<Term> entry3 : Wc2.termlinks()) {
                                    Term et3 = entry3.get().term();
                                    if (et3.toString().equals(premise2)) {
                                        passed = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                /*if (w.toString().equals(premise2)) {
                    passed = true;
                }*/
            }
        }
        return passed;
    }


    @Test
    public void Linkage_NAL5_abduction() throws Exception {
        ProperlyLinkedTest("((robin-->bird)==>(robin-->animal))", "(robin-->animal)");
    }


    @Test
    public void Linkage_NAL5_detachment() throws Exception {
        ProperlyLinkedTest("((robin-->bird)==>(robin-->animal))", "(robin-->bird)");
    }

    @Test
    public void Linkage_NAL6_variable_elimination2() throws Exception {
        ProperlyLinkedIndirectlyTest("<<$1-->bird>==><$1-->animal>>", "(tiger-->animal)");
    }

    //here the problem is: they should be interlinked by lock
    @Test
    public void Part_Indirect_Linkage_NAL6_multiple_variable_elimination4() throws Exception {
        ProperlyLinkedIndirectlyTest("<#1 --> lock>", "<{lock1} --> lock>");
    }

//    @Test
//    public void Indirect_Linkage_NAL6_multiple_variable_elimination4() throws Exception {
//        ProperlyLinkedIndirectlyTest(
//                "(&&, <#1 --> (/, open, #2, _)>, <#1 --> lock>, <#2 --> key>)",
//                "<{lock1} --> lock>");
//    }
//
//    @Test
//    public void Indirect_Linkage_NAL6_abduction_with_variable_elimination_abduction() throws Exception {
//        ProperlyLinkedIndirectlyTest(
//                "<<lock1 --> (/, open, $1, _)> ==> <$1 --> key>>",
//                "<(&&, <#1 --> (/, open, $2, _)>, <#1 --> lock>) ==> <$2 --> key>>"
//        );
//    }
//
//    @Test
//    public void Indirect_Linkage_NAL6_second_variable_introduction_induction() throws Exception {
//        ProperlyLinkedIndirectlyTest("<<lock1 --> (/, open, $1, _)> ==> <$1 --> key>>", "<lock1 --> lock>");
//    }

    @Test
    public void Indirect_Linkage_NAL6_multiple_variable_elimination() throws Exception {
        ProperlyLinkedIndirectlyTest("<(&&, <$1 --> lock>, <$2 --> key>) ==> open($2, $1)>",
                "<{lock1} --> lock>");
    }

//    @Test
//    public void Indirect_Linkage_NAL6_second_level_variable_unification2() throws Exception {
//        ProperlyLinkedIndirectlyTest(
//                "<<$1 --> lock> ==> (&&, <$1 --> (/, open, #2, _)>, <#2 --> key>)>",
//                "<{key1} --> key>");
//    }

    @Test
    public void Indirect_Linkage_NAL6_variable_elimination_deduction() throws Exception {
        ProperlyLinkedIndirectlyTest(
                "<lock1 --> lock>",
                "<(&&, open($2, #1), <#1 --> lock>) ==> <$2 --> key>>");
    }

    @Test
    public void Indirect_Linkage_NAL6_variable_unification7() throws Exception {
        ProperlyLinkedIndirectlyTest(
                "<(&&, <$1 --> flyer>, <($1, worms) --> food>) ==> <$1 --> bird>>",
                "<<$1 --> flyer> ==> <$1 --> [withWings]>>");
    }

    @Test
    public void Indirect_Linkage_NAL6_variable_unification6() throws Exception {
        ProperlyLinkedIndirectlyTest(
                "<(&&, <$1 --> flyer>, <$1 --> [chirping]>, <($1, worms) --> food>) ==> <$1 --> bird>>",
                "<(&&, <$1 --> [chirping]>, <$1 --> [withWings]>) ==> <$1 --> bird>>");
    }

    @Test
    public void Indirect_Linkage_NAL6_second_level_variable_unification() throws Exception {
        //ProperlyLinkedIndirectlyTest("(&&, <#1 --> lock>, <<$2 --> key> ==> <#1 --> (/, open, $2, _)>>)", "<{key1} --> key>");
        ProperlyLinkedIndirectlyTest("(&&, <#1 --> lock>, <<$2 --> key> ==> ($2, #1):open>)", "<{key1} --> key>");
    }

    @Test
    public void Indirect_Linkage_NAL6_second_level_variable_unification_alt() throws Exception {
        ProperlyLinkedIndirectlyTest("(&&, <#1 --> lock>, <<$2 --> key> ==> open($2, #1)>)", "<{key1} --> key>");
    }

    @Test
    public void Indirect_Linkage_Basic() throws Exception {
        ProperlyLinkedIndirectlyTest("<a --> b>", "<b --> c>");
    }

    @Test
    public void Indirect_Linkage_Layer2_Basic() throws Exception {
        ProperlyLinkedIndirectlyTest("<a --> <b --> <k --> x>>>", "<k --> x>");
    }

    @Test
    public void Indirect_Linkage_Layer2_Basic_WithVar() throws Exception {
        ProperlyLinkedIndirectlyTest("<#1 --> <b --> <k --> x>>>", "<k --> x>");
    }

    @Test
    @Ignore /* requires inheritance to have termlink templates to level 2, but this doesnt seem critical otherwise */
    public void Indirect_Linkage_Layer2_Basic_WithVar2() throws Exception {
        ProperlyLinkedIndirectlyTest("<a --> <b --> <#1 --> x>>>", BELIEF, "<k --> x>");
    }

    @Test
    @Ignore /* requires inheritance to have termlink templates to level 2, but this doesnt seem critical otherwise */
    public void Indirect_Linkage_Layer2_Basic_WithVar2_Goal() throws Exception {
        ProperlyLinkedIndirectlyTest("<a --> <b --> <#1 --> x>>>", GOAL, "<k --> x>");
    }

    @Test
    @Ignore /* requires inheritance to have termlink templates to level 2, but this doesnt seem critical otherwise */
    public void Indirect_Linkage_Layer2_Basic_WithVar2_Question() throws Exception {
        ProperlyLinkedIndirectlyTest("<a --> <b --> <#1 --> x>>>", QUESTION, "<k --> x>");
    }

    public void testConceptFormed(@NotNull String s) throws Exception {

        test.requireConditions = false;
        TestNAR tester = test;
        tester.believe(s, 1.0f, 0.9f);
        tester.nar.run(10);
        Concept ret = tester.nar.conceptualize(s);

        assertNotNull("Failed to create a concept for " + s, ret);
    }

    @Test
    public void Basic_Concept_Formation_Test() throws Exception {
        testConceptFormed("<a --> b>");
    }

    @Test
    public void Advanced_Concept_Formation_Test() throws Exception {
        testConceptFormed("<#1 --> b>");
    }

    @Test
    public void Advanced_Concept_Formation_Test2() throws Exception {
        testConceptFormed("<<$1 --> a> ==> <$1 --> b>>");
    }

    @Test
    public void Advanced_Concept_Formation_Test2_2() throws Exception {
        testConceptFormed("<<$1 --> bird> ==> <$1 --> animal>>");
    }

    @Test
    public void Advanced_Concept_Formation_Test3() throws Exception {
        testConceptFormed("(&&,<#1 --> lock>,<<$2 --> key> ==> open($2, #1)>)");
    }

//    @Test
//    public void Advanced_Concept_Formation_Test4() throws Exception {
//        testConceptFormed("(&&,<#1 --> (/,open,#2,_)>,<#1 --> lock>,<#2 --> key>)");
//    }


    @Test
    public void Variable_Normalization_1() throws Exception {
        //this.activeTasks = activeTasks;
        NAR tester = new NARS().get();
        test.requireConditions = false;

        String nonsense = "<(&&,<#1 --> M>,<#2 --> M>) ==> <#1 --> nonsense>>";
        tester.believe(nonsense); //.en("If robin is a type of bird then robin can fly.");
        tester.run(1);
        Concept c = tester.conceptualize(nonsense);
        assertNotNull(c);
    }

}
