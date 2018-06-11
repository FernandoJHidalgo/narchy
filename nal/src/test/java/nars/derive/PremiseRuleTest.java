package nars.derive;

import nars.$;
import nars.NARS;
import nars.Narsese;
import nars.derive.premise.PremiseDeriver;
import nars.derive.premise.PremiseDeriverCompiler;
import nars.derive.premise.PremiseDeriverRuleSet;
import nars.derive.premise.PremiseDeriverSource;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Created by me on 7/7/15.
 */
public class PremiseRuleTest {


    @Test
    public void testNoNormalization() throws Exception {

        String a = "<x --> #1>";
        String b = "<y --> #1>";
        Term p = $.p(
                Narsese.term(a),
                Narsese.term(b)
        );
        String expect = "((x-->#1),(y-->#1))";
        assertEquals(expect, p.toString());






    }


    @Test
    public void testParser() throws Narsese.NarseseException {

        

        assertNotNull(Narsese.term("<A --> b>"), "metaparser can is a superset of narsese");

        

        assertEquals(0, Narsese.term("#A").complexity());
        assertEquals(1, Narsese.term("#A").volume());
        assertEquals(0, Narsese.term("%A").complexity());
        assertEquals(1, Narsese.term("%A").volume());

        assertEquals(3, Narsese.term("<A --> B>").complexity());
        assertEquals(1, Narsese.term("<%A --> %B>").complexity());

        {
            



            PremiseDeriverSource x = PremiseDeriverSource.parse("A, A |- A, (Belief:Revision, Goal:Weak)");
            assertNotNull(x);
            
            
        }


        int vv = 19;
        {
            



            PremiseDeriverSource x = PremiseDeriverSource.parse("<A --> B>, <B --> A> |- <A <-> B>, (Belief:Revision, Goal:Weak)");
            
            assertEquals(vv, x.ref.volume());
            

        }
        {
            



            PremiseDeriverSource x = PremiseDeriverSource.parse("<A --> B>, <B --> A> |- <A <-> nonvar>, (Belief:Revision, Goal:Weak)");
            
            assertEquals(vv, x.ref.volume()); 
            
        }
        {
            



            PremiseDeriverSource x = PremiseDeriverSource.parse(" <A --> B>, <B --> A> |- <A <-> B>,  (Belief:Conversion, Punctuation:Belief)");
            
            assertEquals(vv, x.ref.volume());
            
        }








        
        



        PremiseDeriverSource x = PremiseDeriverSource.parse("(S --> M), (P --> M) |- (P <-> S), (Belief:Comparison,Goal:Strong)");
        
        
        assertEquals(vv, x.ref.volume());

    }















    @Test
    public void testMinSubsRulePredicate() {
        

        PremiseDeriver d = PremiseDeriverCompiler.the(new PremiseDeriverRuleSet(NARS.shell(), "(A-->B),B,is(B,\"[\"),subsMin(B,2) |- (A-->dropAnySet(B)), (Belief:StructuralDeduction)"), null);
        d.printRecursive();
        assertNotNull(d);
    }

    @Test
    public void testDoubleOnlyTruthAddsRequiresDoubleBelief() {

        PremiseDeriver d = PremiseDeriverCompiler.the(new PremiseDeriverRuleSet(NARS.shell(),
                "X,Y |- (X&&Y), (Belief:Intersection)"), null);

        d.printRecursive();
        assertEquals("((\".\"-->task),DoublePremise(\".\",(),()),can({0}))", d.what.toString());
    }
    @Test
    public void testDoubleOnlyTruthAddsRequiresDoubleGoal() {

        PremiseDeriver d = PremiseDeriverCompiler.the(new PremiseDeriverRuleSet(NARS.shell(),
                "X,Y |- (X&&Y), (Goal:Intersection)"), null);

        d.printRecursive();
        assertEquals("((\"!\"-->task),DoublePremise((),\"!\",()),can({0}))", d.what.toString());
    }
    @Test
    public void testDoubleOnlyTruthAddsRequiresDoubleBeliefOrGoal() {

        PremiseDeriver d = PremiseDeriverCompiler.the(new PremiseDeriverRuleSet(NARS.shell(),
                "X,Y |- (X&&Y), (Belief:Intersection,Goal:Intersection)"), null);

        d.printRecursive();
        assertEquals("((\".!\"-->task),DoublePremise(\".\",(),()),DoublePremise((),\"!\",()),can({0}))", d.what.toString());
    }
    @Test
    public void testDoubleOnlyTruthAddsRequiresDoubleQuestionOverride() {

        PremiseDeriver d = PremiseDeriverCompiler.the(new PremiseDeriverRuleSet(NARS.shell(),
                "X,Y,task(\"?\") |- (X&&Y), (Punctuation:Belief,Belief:Intersection)"), null);

        d.printRecursive();
        assertEquals("(DoublePremise((),(),\"?@\"),(\"?\"-->task),can({0}))", d.what.toString());
    }
    @Test
    public void testInferQuestionPunctuationFromTaskRequirement() {

        PremiseDeriver d = PremiseDeriverCompiler.the(new PremiseDeriverRuleSet(NARS.shell(),
            "Y, Y, task(\"?\") |- (?1 &| Y), (Punctuation:Question)"
                ));
        d.printRecursive();
        assertEquals("((\"?\"-->task),can({0}))", d.what.toString());
    }


    @Test
    public void testTryFork() {

        PremiseDeriver d = PremiseDeriverCompiler.the(new PremiseDeriverRuleSet(NARS.shell(), "X,Y |- (X&&Y), (Belief:Intersection)", "X,Y |- (||,X,Y), (Belief:Union)"), null);
/*
TODO - share unification state for different truth/conclusions
    TruthFork {
      (Union,_):
      (Intersection,_):
         (unify...
         (
      and {
        truth(Union,_)
        unify(task,%1)
        unify(belief,%2) {
          and {
            derive((||,%1,%2))
            taskify(3)
          }
        }
      }
      and {
        truth(Intersection,_)
        unify(task,%1)
        unify(belief,%2) {
 */
        d.printRecursive();

    }

    @Test public void testConjWithEllipsisIsXternal() {

        PremiseDeriver d = PremiseDeriverCompiler.the(new PremiseDeriverRuleSet(NARS.shell(), "X,Y |- (&&,X,%A..+), (Belief:Analogy)", "X,Y |- (&&,%A..+), (Belief:Analogy)"), null);
            d.printRecursive();
    }
    @Test
    public void printTermRecursive() throws Narsese.NarseseException {
        



        Compound y = (Compound) PremiseDeriverSource.parse("(S --> P), --%S |- (P --> S), (Belief:Conversion, Info:SeldomUseful)").ref;
        Terms.printRecursive(System.out, y);
    }













































































}