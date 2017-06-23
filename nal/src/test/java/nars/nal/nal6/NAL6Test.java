package nars.nal.nal6;

import nars.NAR;
import nars.nal.AbstractNALTest;
import nars.test.TestNAR;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.function.Supplier;

import static nars.Op.BELIEF;
import static nars.time.Tense.ETERNAL;

@RunWith(Parameterized.class)
public class NAL6Test extends AbstractNALTest {


    final int cycles = 1480;

    public NAL6Test(Supplier<NAR> b) {
        super(b);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable configurations() {
        return AbstractNALTest.nars(6);
    }

    @Test
    public void variable_unification1()  {
        TestNAR tester = test();
        tester.mustBelieve(cycles, "<<$1 --> bird> ==> <$1 --> flyer>>", 0.79f, 0.92f); //en("If something is a bird, then usually, it is a flyer.");
        tester.believe("<<$x --> bird> ==> <$x --> flyer>>"); //en("If something is a bird, then it is a flyer.");
        tester.believe("<<$y --> bird> ==> <$y --> flyer>>", 0.00f, 0.70f); //en("If something is a bird, then it is not a flyer.");

    }



    @Test
    public void variable_unification2()  {
        TestNAR tester = test();
        tester.believe("<<$x --> bird> ==> <$x --> animal>>"); //en("If something is a bird, then it is a animal.");
        tester.believe("<<$y --> robin> ==> <$y --> bird>>"); //en("If something is a robin, then it is a bird.");
        tester.mustBelieve(cycles, "<<$1 --> robin> ==> <$1 --> animal>>", 1.00f, 0.81f); //en("If something is a robin, then it is a animal.");
        tester.mustBelieve(cycles, "<<$1 --> animal> ==> <$1 --> robin>>", 1.00f, 0.45f); //en(" I guess that if something is a animal, then it is a robin.");

    }


    @Test
    public void variable_unification3()  {
        TestNAR tester = test();
        //tester.log();
        tester.believe("<<$x --> swan> ==> <$x --> bird>>", 1.00f, 0.80f); //en("If something is a swan, then it is a bird.");
        tester.believe("<<$y --> swan> ==> <$y --> swimmer>>", 0.80f, 0.9f); //en("If something is a swan, then it is a swimmer.");
        tester.mustBelieve(cycles, "<<$1 --> swan> ==> (&&,<$1 --> bird>,<$1 --> swimmer>)>", 0.80f, 0.72f); //en("I believe that if something is a swan, then usually, it is both a bird and a swimmer.");
        tester.mustBelieve(cycles, "<<$1 --> swimmer> ==> <$1 --> bird>>", 1.00f, 0.37f); //en("I guess if something is a swimmer, then it is a bird.");
        tester.mustBelieve(cycles, "<<$1 --> bird> ==> <$1 --> swimmer>>", 0.80f, 0.42f); //en("I guess if something is a bird, then it is a swimmer.");
        tester.mustBelieve(cycles, "<<$1 --> bird> <=> <$1 --> swimmer>>", 0.80f, 0.42f); //en("I guess something is a bird, if and only if it is a swimmer.");
        //tester.mustBelieve(cycles, "<<$1 --> swan> ==> (&&,(--,<$1 --> bird>),(--,<$1 --> swimmer>))>", 0.00f, 0.72f); //en("I believe that if something is a swan, then it is a bird or a swimmer.");
            //technicallt this is the same as:
            //  tester.mustBelieve(cycles, "<<$1 --> swan> ==> (||,<$1 --> bird>,<$1 --> swimmer>)>", 1.00f, 0.72f); //en("I believe that if something is a swan, then it is a bird or a swimmer.");

    }


    @Test
    public void variable_unification4()  {
        TestNAR tester = test();
        tester.believe("<<bird --> $x> ==> <robin --> $x>>"); //en("What can be said about bird can also be said about robin.");
        tester.believe("<<swimmer --> $y> ==> <robin --> $y>>", 0.70f, 0.90f); //en("What can be said about swimmer usually can also be said about robin.");
        tester.mustBelieve(cycles, "<(&&,<bird --> $1>,<swimmer --> $1>) ==> <robin --> $1>>", 0.7f /*1f? */, 0.81f); //en("What can be said about bird and swimmer can also be said about robin.");
        //tester.mustBelieve(cycles, "<(||,<bird --> $1>,<swimmer --> $1>) ==> <robin --> $1>>", 0.70f, 0.81f); //en("What can be said about bird or swimmer can also be said about robin.");
        tester.mustBelieve(cycles, "<<bird --> $1> ==> <swimmer --> $1>>", 1f, 0.36F); //en("I guess what can be said about bird can also be said about swimmer.");
        tester.mustBelieve(cycles, "<<swimmer --> $1> ==> <bird --> $1>>", 0.7f, 0.45f); //en("I guess what can be said about swimmer can also be said about bird.");
        tester.mustBelieve(cycles, "<<bird --> $1> <=> <swimmer --> $1>>", 0.7f, 0.45f); //en("I guess bird and swimmer share most properties.");

    }


    @Test
    public void variable_unification5()  {
        TestNAR tester = test();
        //tester.log();
        tester.believe("<(&&,<$x --> flyer>,<$x --> [chirping]>) ==> <$x --> bird>>"); //en("If something can fly and chirp, then it is a bird.");
        tester.believe("<<$y --> [withWings]> ==> <$y --> flyer>>"); //en("If something has wings, then it can fly.");
        tester.mustBelieve(cycles, "<(&&,<$1 --> [chirping]>,<$1 --> [withWings]>) ==> <$1 --> bird>>", 1.00f, 0.81f); //en("If something can chirp and has wings, then it is a bird.");

    }


    @Test
    public void variable_unification6()  {
        TestNAR tester = test();
        tester.believe("<(&&,<$x --> flyer>,<$x --> [chirping]>, <($x, worms) --> food>) ==> <$x --> bird>>"); //en("If something can fly, chirp, and eats worms, then it is a bird.");
        tester.believe("<(&&,<$y --> [chirping]>,<$y --> [withWings]>) ==> <$y --> bird>>"); //en("If something can chirp and has wings, then it is a bird.");
        tester.mustBelieve(cycles, "<(&&,<$1 --> flyer>,<($1,worms) --> food>) ==> <$1 --> [withWings]>>", 1.00f, 0.45f); //en("If something can fly and eats worms, then I guess it has wings.");
        tester.mustBelieve(cycles, "<<$1 --> [withWings]> ==> (&&,<$1 --> flyer>,<($1,worms) --> food>)>", 1.00f, 0.45f); //en("I guess if something has wings, then it can fly and eats worms.");


        /*
        <patham9> 
        first result:
            (&&,<$1 --> flyer>,<($1,worms) --> food>) ==> <$1 --> [withWings]>>
        it comes from the rule
            ((&&,C,A_1..n) ==> Z), ((&&,C,B_1..m) ==> Z) |- ((&&,A_1..n) ==> (&&,B_1..m)), (Truth:Induction)
        which basically says: if two different precondition conjunctions, with a common element lead to the same conclusion,
        it might be that these different preconditions in the specific conjunctions imply each other
        (each other because the premises can be swapped for this rule and it is still valid)

        second result:
            <<$1 --> [withWings]> ==> (&&,<$1 --> flyer>,<($1,worms) --> food>)>
        by the same rule:
            ((&&,C,A_1..n) ==> Z), ((&&,C,B_1..m) ==> Z) |- ((&&,B_1..m) ==> (&&,A_1..n)), (Truth:Induction)
        where this time the diffierent preconditions of the second conjunction imply the different preconditions of the first
        no, no additionally info needed
        now I will show you what I think went wrong in your system:
        you got:
            ((&&,(($1,worms)-->food),($1-->flyer),($1-->[chirping]))==>(($1-->[withWings])&&($1-->[chirping]))).
        abduction in progress ^^
        your result is coming from the induction rule
            (P ==> M), (S ==> M), not_equal(S,P) |- (S ==> P), (Truth:Induction, Derive:AllowBackward)
        there are two possibilities, either restrict not_equal further to no_common_subter,
        or make the constructor of ==> make sure that the elements which occur in predicate and subject as well are removed
        its less fatal than in the inheritance composition, the derivation isnt wrong fundamentally, but if you can do so efficiently, let it avoid it
        additionally make sure that the two
            ((&&,C,A_1..n) ==> Z), ((&&,C,B_1..m) ==> Z) |- ((&&,A_1..n) ==> (&&,B_1..m)), (Truth:Induction)
        rules work, they are demanded by this reasoning about preconditions
        *hypothetical reasoning about preconditions to be exact
         */
    }


    @Test
    public void variable_unification7()  {
        TestNAR tester = test();
        tester.believe("<(&&,<$x --> flyer>,<($x,worms) --> food>) ==> <$x --> bird>>"); //en("If something can fly and eats worms, then it is a bird.");
        tester.believe("<<$y --> flyer> ==> <$y --> [withWings]>>"); //en("If something can fly, then it has wings.");
        tester.mustBelieve(cycles, "<(&&,<$1 --> [withWings]>,<($1,worms) --> food>) ==> <$1 --> bird>>", 1.00f, 0.45f); //en("If something has wings and eats worms, then I guess it is a bird.");

    }


    @Test
    public void variable_elimination()  {
        TestNAR tester = test();
        tester.believe("<<$x --> bird> ==> <$x --> animal>>"); //en("If something is a bird, then it is an animal.");
        tester.believe("<robin --> bird>"); //en("A robin is a bird.");
        tester.mustBelieve(cycles, "<robin --> animal>", 1.00f, 0.81f); //en("A robin is an animal.");

    }


    @Test
    public void variable_elimination2()  {
        TestNAR tester = test();
        tester.believe("<<$x --> bird> ==> <$x --> animal>>"); //en("If something is a bird, then it is an animal.");
        tester.believe("<tiger --> animal>"); //en("A tiger is an animal.");
        tester.mustBelieve(cycles, "<tiger --> bird>", 1.00f, 0.45f); //en("I guess that a tiger is a bird.");

    }


    @Test
    public void variable_elimination3()  {
        TestNAR tester = test();
        tester.believe("<<$x --> animal> <=> <$x --> bird>>"); //en("Something is a animal if and only if it is a bird.");
        tester.believe("<robin --> bird>"); //en("A robin is a bird.");
        tester.mustBelieve(cycles, "<robin --> animal>", 1.00f, 0.81f); //en("A robin is a animal.");

    }


    @Test
    public void variable_elimination4()  {
        TestNAR tester = test();
        tester.log();
        tester.believe("(&&,<#x --> bird>,<#x --> swimmer>)"); //en("Some bird can swim.");
        tester.believe("<swan --> bird>", 0.90f, 0.9f); //en("Swan is a type of bird.");
        tester.mustBelieve(cycles, "<swan --> swimmer>", 0.90f, //en("I guess swan can swim.");
                0.43f);
    }
   @Test
    public void variable_elimination4b()  {
        TestNAR tester = test();
        //tester.log();
        tester.believe("(<#x --> bird> <-> <#x --> swimmer>)"); //en("Some bird can swim.");
        tester.believe("<swan --> bird>", 0.90f, 0.9f); //en("Swan is a type of bird.");
        tester.mustBelieve(cycles, "<swan --> swimmer>", 0.90f, //en("I guess swan can swim.");
                0.81f);
                //0.43f);
    }

    @Test
    public void variable_elimination5()  {
        TestNAR tester = test();
        tester.believe("<{Tweety} --> [withWings]>"); //en("Tweety has wings.");
        tester.believe("<(&&,<$x --> [chirping]>,<$x --> [withWings]>) ==> <$x --> bird>>"); //en("If something can chirp and has wings, then it is a bird.");
        tester.mustBelieve(cycles, "<<{Tweety} --> [chirping]> ==> <{Tweety} --> bird>>", 1.00f, 0.81f /*0.73f*/); //en("If Tweety can chirp, then it is a bird.");

    }


    @Test
    public void variable_elimination6()  {
        TestNAR tester = test();
        tester.believe("<(&&,<$x --> flyer>,<$x --> [chirping]>, <($x, worms) --> food>) ==> <$x --> bird>>"); //en("If something can fly, chirp, and eats worms, then it is a bird.");
        tester.believe("<{Tweety} --> flyer>"); //en("Tweety can fly.");
        tester.mustBelieve(cycles*6, "<(&&,<{Tweety} --> [chirping]>,<({Tweety},worms) --> food>) ==> <{Tweety} --> bird>>", 1.00f, 0.81f); //en("If Tweety can chirp and eats worms, then it is a bird.");
    }


    @Test
    public void multiple_variable_elimination()  {
        TestNAR tester = test();
        tester.believe("<(&&,<$x --> key>,<$y --> lock>) ==> <$y --> (/,open,$x,_)>>"); //en("Every lock can be opened by every key.");
        tester.believe("<{lock1} --> lock>"); //en("Lock-1 is a lock.");
        tester.mustBelieve(cycles*2, "<<$1 --> key> ==> <{lock1} --> (/,open,$1,_)>>", 1.00f, 0.81f); //en("Lock-1 can be opened by every key.");

    }


    @Test
    public void multiple_variable_elimination2()  {
        TestNAR tester = test();
        tester.believe("<<$x --> lock> ==> (&&,<#y --> key>,<$x --> (/,open,#y,_)>)>"); //en("Every lock can be opened by some key.");
        tester.believe("<{lock1} --> lock>"); //en("Lock-1 is a lock.");
        tester.mustBelieve(cycles, "(&&,<#1 --> key>,<{lock1} --> (/,open,#1,_)>)", 1.00f, 0.81f); //en("Some key can open Lock-1.");

    }


    @Test
    public void multiple_variable_elimination3()  {
        TestNAR tester = test();
        tester.believe("(&&,<#x --> lock>,<<$y --> key> ==> <#x --> (/,open,$y,_)>>)"); //en("There is a lock that can be opened by every key.");
        tester.believe("<{lock1} --> lock>"); //en("Lock-1 is a lock.");
        tester.mustBelieve(cycles, "<<$1 --> key> ==> <{lock1} --> (/,open,$1,_)>>", 1.00f,
                0.81f);
                //0.43f); //en("I guess Lock-1 can be opened by every key.");

    }


    @Test
    public void multiple_variable_elimination4()  {
        TestNAR tester = test();
        tester.believe("(&&,<#x --> (/,open,#y,_)>,<#x --> lock>,<#y --> key>)"); //en("There is a key that can open some lock.");
        tester.believe("<{lock1} --> lock>"); //en("Lock-1 is a lock.");
        tester.mustBelieve(cycles*6, "(&&,<#1 --> key>,<{lock1} --> (/,open,#1,_)>)",
                1.00f,
                0.43f
        ); //en("I guess there is a key that can open Lock-1.");

    }

    @Test
    public void testBeliefToEquivalence() {
        TestNAR tester = test();
        tester.believe("<<$1 --> bird> ==> <$1 --> swimmer>>", 1f, 0.9f); //en("I guess a bird is usually a swimmer.");
        tester.believe("<<$1 --> swimmer> ==> <$1 --> bird>>", 1f, 0.9f); //en("I guess a bird is usually a swimmer.");
        tester.mustBelieve(cycles, "<<$1 --> swimmer> <=> <$1 --> bird>>", 1.00f, 0.81f); //en("I guess a swimmer is a bird.");
    }

    @Test
    public void variable_introduction()  {
        TestNAR tester = test();
        //tester.log();
        tester.believe("<swan --> bird>"); //en("A swan is a bird.");
        tester.believe("<swan --> swimmer>", 0.80f, 0.9f); //en("A swan is usually a swimmer.");
        tester.mustBelieve(cycles, "<<$1 --> bird> ==> <$1 --> swimmer>>", 0.80f, 0.45f); //en("I guess a bird is usually a swimmer.");
        tester.mustBelieve(cycles, "<<$1 --> swimmer> ==> <$1 --> bird>>", 1.00f, 0.39f); //en("I guess a swimmer is a bird.");
        tester.mustBelieve(cycles, "<<$1 --> swimmer> <=> <$1 --> bird>>", 0.80f, 0.45f); //en("I guess a bird is usually a swimmer, and the other way around.");
        tester.mustBelieve(cycles, "(&&, <#1 --> swimmer>, <#1 --> bird>)", 0.80f, 0.81f); //en("Some bird can swim.");

    }
    @Test
    public void variable_introduction_with_existing_vars()  {
        //test that an introduced variable doesn't interfere with an existing variable of same name ($1)
        TestNAR tester = test();
        tester.believe("<swan --> <#1 --> birdlike>>"); //en("A swan is a bird.");
        tester.believe("<swan --> swimmer>", 0.80f, 0.9f); //en("A swan is usually a swimmer.");
        tester.mustBelieve(cycles, "<<$1 --> <#2 --> birdlike>> ==> <$1 --> swimmer>>", 0.80f, 0.45f); //en("I guess a bird is usually a swimmer.");
    }


    @Test
    public void variable_introduction2()  {
        TestNAR tester = test();
        tester.believe("<gull --> swimmer>"); //en("A gull is a swimmer.");
        tester.believe("<swan --> swimmer>", 0.80f, 0.9f); //en("Usually, a swan is a swimmer.");
        tester.mustBelieve(cycles, "<<gull --> $1> ==> <swan --> $1>>", 0.80f, 0.45f); //en("I guess what can be said about gull usually can also be said about swan.");
        tester.mustBelieve(cycles, "<<swan --> $1> ==> <gull --> $1>>", 1.00f, 0.39f); //en("I guess what can be said about swan can also be said about gull.");
        tester.mustBelieve(cycles, "<<gull --> $1> <=> <swan --> $1>>", 0.80f, 0.45f); //en("I guess gull and swan share most properties.");
        tester.mustBelieve(cycles, "(&&,<gull --> #1>,<swan --> #1>)", 0.80f, 0.81f); //en("Gull and swan have some common property.");
    }

    @Test
    @Ignore
    public void variable_introduction3()  {
        TestNAR tester = test();
        tester.believe("<gull --> swimmer>", 1f, 0.9f); //en("A gull is a swimmer.");
        tester.believe("<swan --> swimmer>", 0f, 0.9f); //en("A swan is never a swimmer.");
        tester.mustBelieve(cycles, "(&&,<gull --> #1>,<swan --> #1>)", 0.0f, 0.81f); //en("Gull and swan have no commonality.");
        tester.mustBelieve(cycles, "(&&,<gull --> #1>,(--,<swan --> #1>))", 1.0f, 0.81f); //en("Gull and non-swans have commonality.");

//        tester.mustBelieve(cycles, "<<gull --> $1> ==> <swan --> $1>>", 0.80f, 0.45f); //en("I guess what can be said about gull usually can also be said about swan.");
//        tester.mustBelieve(cycles, "<<swan --> $1> ==> <gull --> $1>>", 1.00f, 0.39f); //en("I guess what can be said about swan can also be said about gull.");
//        tester.mustBelieve(cycles, "<<gull --> $1> <=> <swan --> $1>>", 0.80f, 0.45f); //en("I guess gull and swan share most properties.");
    }

    @Test
    public void variable_introduction_with_existing_vars2()  {
        //test that an introduced variable doesn't interfere with an existing variable of same name ($1)
        TestNAR tester = test();
        //tester.log();
        tester.believe("<#1 --> swimmer>"); //en("A gull is a swimmer.");
        tester.believe("<swan --> swimmer>", 0.80f, 0.9f); //en("Usually, a swan is a swimmer.");
        tester.mustBelieve(cycles, "<<#1 --> $2> ==> <swan --> $2>>", 0.80f, 0.45f); //en("I guess what can be said about gull usually can also be said about swan.");
        tester.mustBelieve(cycles, "<<swan --> $1> ==> <#2 --> $1>>", 1.00f, 0.39f); //en("I guess what can be said about swan can also be said about gull.");
        tester.mustBelieve(cycles, "<<#1 --> $2> <=> <swan --> $2>>", 0.80f, 0.45f); //en("I guess gull and swan share most properties.");
        tester.mustBelieve(cycles, "(&&,<#1 --> #2>,<swan --> #2>)", 0.80f, 0.81f); //en("Gull and swan have some common property.");
    }

    @Test
    public void variables_introduction()  {
        TestNAR tester = test();
        tester.believe("<{key1} --> (/,open,_,{lock1})>"); //en("Key-1 opens Lock-1.");
        tester.believe("<{key1} --> key>"); //en("Key-1 is a key.");
        tester.mustBelieve(cycles, "<<$1 --> key> ==> <$1 --> (/,open,_,{lock1})>>", 1.00f, 0.45f); //en("I guess every key can open Lock-1.");
        tester.mustBelieve(cycles, "(&&,<#1 --> (/,open,_,{lock1})>,<#1 --> key>)", 1.00f, 0.81f); //en("Some key can open Lock-1.");

    }


    @Test
    public void multiple_variables_introduction()  {
        TestNAR tester = test();
        //tester.log();
        tester.believe("<<$x --> key> ==> open($x,{lock1})>"); //en("Lock-1 can be opened by every key.");
        tester.believe("<{lock1} --> lock>"); //en("Lock-1 is a lock.");
        //tester.mustBelieve(cycles, "(&&,<#1 --> lock>,<<$2 --> key> ==> <#1 --> (/,open,$2,_)>>)", 1.00f, 0.81f); //en("There is a lock that can be opened by every key.");
        tester.mustBelieve(cycles, "(&&,<#1 --> lock>,<<$2 --> key> ==> open($2,#1)>)", 1.00f, 0.81f); //en("There is a lock that can be opened by every key.");
        //tester.mustBelieve(cycles, "<(&&,<$1 --> key>,<$2 --> lock>) ==> <$2 --> (/,open,$1,_)>>", 1.00f, 0.45f); //en("I guess every lock can be opened by every key.");
        //tester.mustBelieve(cycles, "<(&&,<$1 --> key>,<$2 --> lock>) ==> open($1,$2)>", 1.00f, 0.45f); //en("I guess every lock can be opened by every key.");
    }


    @Test
    public void multiple_variables_introduction2()  {
        TestNAR tester = test();
        tester.believe("(<#x --> key> && open(#x,{lock1}))"); //en("Lock-1 can be opened by some key.");
        tester.believe("<{lock1} --> lock>"); //en("Lock-1 is a lock.");

        tester.mustBelieve(cycles, "(&&, <#1 --> key>, <#2 --> lock>, open(#1,#2))", 1.00f, 0.81f); //en("There is a key that can open some lock.");
        //tester.mustBelieve(cycles, "(&&, <#1 --> lock>, <#1 --> (/, open, #2, _)>, <#2 --> key>)", 1.00f, 0.81f); //en("There is a key that can open some lock.");

        tester.mustBelieve(cycles, "(<$1 --> lock> ==> (<#2 --> key> && open(#2,$1)))", 1.00f, 0.45f); //en("I guess every lock can be opened by some key.");

    }




    @Test
    public void second_level_variable_unificationNoImgAndAsPrecondition()  {
        TestNAR tester = test();
        tester.believe("((<#1 --> lock>&&<$2 --> key>) ==> open(#1,$2))", 1.00f, 0.90f);
            //en("there is a lock which is opened by all keys");
            //in other words:
            //      there is a lock which is opened by any/all keys

        tester.believe("<{key1} --> key>", 1.00f, 0.90f); //en("key1 is a key");
        tester.mustBelieve(cycles*2, "((#1-->lock)==>open(#1,{key1}))", 1.00f, 0.81f); //en("there is a lock which is opened by key1");
    }

    @Test
    public void second_level_variable_unification()  {
        TestNAR tester = test();
        tester.believe("(&&,<#1 --> lock>,<<$2 --> key> ==> <#1 --> (/,open,$2,_)>>)", 1.00f, 0.90f); //en("there is a lock which is opened by all keys");
        tester.believe("<{key1} --> key>", 1.00f, 0.90f); //en("key1 is a key");
        tester.mustBelieve(cycles*2, "(&&,<#1 --> lock>,<#1 --> (/,open,{key1},_)>)", 1.00f, 0.81f); //en("there is a lock which is opened by key1");
    }
    @Test
    public void second_level_variable_unification_neg()  {
        TestNAR tester = test();
        tester.believe("(&&,<#1 --> lock>,<--($2 --> key) ==> <#1 --> (/,open,$2,_)>>)");
        tester.believe("--({key1} --> key)");
        tester.mustBelieve(cycles, "(&&,<#1 --> lock>,<#1 --> (/,open,{key1},_)>)", 1.00f, 0.81f);
    }



    @Test
    public void second_level_variable_unification2()  {
        TestNAR tester = test();
        tester.believe("<<$1 --> lock> ==> (&&,<#2 --> key>,<$1 --> (/,open,#2,_)>)>", 1.00f, 0.90f); //en("all locks are opened by some key");
        tester.believe("<{key1} --> key>", 1.00f, 0.90f); //en("key1 is a key");
        tester.mustBelieve(cycles, "<<$1 --> lock> ==> <$1 --> (/,open,{key1},_)>>", 1.00f,
                0.81f
                /*0.43f*/); //en("maybe all locks are opened by key1");

    }

    @Test public void testSimpleIndepUnification() {
        TestNAR t = test();
        t.input("(<$x --> y> ==> <$x --> z>).");
        t.input("(x --> y).");
        t.mustBelieve(cycles, "(x --> z)", 1.0f, 0.81f);
    }

//    @Test
//    public void second_level_variable_unification2_clean()  {
//        TestNAR tester = test();
//        tester.believe("<<$1 --> x> ==> (&&,<#2 --> y>,<$1 --> (/,open,#2,_)>)>", 1.00f, 0.90f); //en("all xs are opened by some y");
//        tester.believe("<{z} --> y>", 1.00f, 0.90f); //en("z is a y");
//        tester.mustBelieve(cycles, "<<$1 --> x> ==> <$1 --> (/,open,{z},_)>>", 1.00f, 0.42f); //en("maybe all xs are opened by z");
//
//    }

    @Test
    public void second_variable_introduction_induction()  {

        TestNAR tester = test();
        //tester.log();
        tester.believe("(open($1,lock1) ==> key:$1)"); //en("if something opens lock1, it is a key");
        tester.believe("open(lock,lock1)");
        tester.mustBelieve(cycles,
                "((open(lock,#1) && open($2,#1)) ==> key:$2)",
                1.00f, 0.81f); //en("there is a lock with the property that when opened by something, this something is a key (induction)");
        //tester.mustBelieve(cycles, "<(<$1 --> lock> && open($2,$1)) ==> <$2 --> key>>", 1.00f, 0.45f); //en("there is a lock with the property that when opened by something, this something is a key (induction)");

    }


    @Test
    public void variable_elimination_deduction()  {
        test()
            .believe("((&&,(#1 --> lock),open($2,#1)) ==> ($2 --> key))", 1.00f, 0.90f) //en("there is a lock with the property that when opened by something, this something is a key");
            .believe("(lock1 --> lock)", 1.00f, 0.90f) //en("lock1 is a lock");
            .mustBelieve(cycles, "(open($1,lock1) ==> ($1 --> key))", 1.00f, 0.81f); //en("whatever opens lock1 is a key");
    }
    @Test
    public void variable_elimination_deduction_neg()  {
        test()
            .believe("((&&, --(#1 --> lock), open($2,#1)) ==> ($2 --> key))") //en("there is not a lock with the property that when opened by something, this something is a key");
            .believe("--(lock1 --> lock)") //en("lock1 is not a lock");
            .mustBelieve(cycles, "(open($1,lock1) ==> ($1 --> key))", 1.00f, 0.81f); //en("whatever opens lock1 is a key");
    }



    @Test
    public void abduction_with_variable_elimination()  {
        test()
            .believe("(open($1,lock1) ==> ($1 --> key))", 1.00f, 0.90f) //en("whatever opens lock1 is a key");
                ///tester.believe("<<lock1 --> (/,open,$1,_)> ==> <$1 --> key>>", 1.00f, 0.90f); //en("whatever opens lock1 is a key");
            .believe("(((#1 --> lock) && open($2,#1)) ==> ($2 --> key))", 1.00f, 0.90f) //en("there is a lock with the property that when opened by something, this something is a key");
            .mustBelieve(cycles*4, "lock:lock1", 1.00f, 0.45f) //en("lock1 is a lock");
        ;
    }

    @Test /** TODO verify */
    public void abduction_with_variable_elimination_negated()  {
        test()
                //.log()
                .believe("(open($1,lock1) ==> ($1 --> key))", 1.00f, 0.90f) //en("whatever opens lock1 is a key");
                ///tester.believe("<<lock1 --> (/,open,$1,_)> ==> <$1 --> key>>", 1.00f, 0.90f); //en("whatever opens lock1 is a key");
                .believe("(((--,(#1 --> lock)) && open($2,#1)) ==> ($2 --> key))", 1.00f, 0.90f) //en("there is NOT a lock with the property that when opened by something, this something is a key");
                .mustBelieve(cycles*2, "lock:lock1", 0.00f, 0.45f) //en("lock1 is NOT a lock")
                .mustNotOutput(cycles*2, "lock:lock1", BELIEF, 0.5f,1f,0,1f,ETERNAL)
        ;
    }

    @Test //see discussion on https://groups.google.com/forum/#!topic/open-nars/1TmvmQx2hMk
    public void strong_unification_simple()  {
        TestNAR tester = test();
        tester.believe("<<($a,$b) --> pair> ==> <$a --> $b>>", 1.00f, 0.90f);
        tester.believe("<(x,y) --> pair>", 1.00f, 0.90f);
        tester.mustBelieve(cycles, "<x --> y>", 1.00f, 0.81f); //en("there is a lock which is opened by key1");
    }

    @Test public void strong_unification_simple2()  {
        TestNAR tester = test();
        tester.believe("<<($a,$b) --> pair> ==> {$a,$b}>", 1.00f, 0.90f);
        tester.believe("<(x,y) --> pair>", 1.00f, 0.90f);
        tester.mustBelieve(cycles, "{x,y}", 1.00f, 0.81f); //en("there is a lock which is opened by key1");
    }


    @Test //see discussion on https://groups.google.com/forum/#!topic/open-nars/1TmvmQx2hMk
    public void strong_unification()  {
        TestNAR tester = test();
        tester.believe("<<($a,is,$b) --> sentence> ==> <$a --> $b>>", 1.00f, 0.90f);
        tester.believe("<(bmw,is,car) --> sentence>", 1.00f, 0.90f);
        tester.mustBelieve(cycles, "<bmw --> car>", 1.00f, 0.81f); //en("there is a lock which is opened by key1");

    }

    @Test //see discussion on https://groups.google.com/forum/#!topic/open-nars/1TmvmQx2hMk
    public void strong_elimination()  {
        TestNAR tester = test();
        //tester.log();
        tester.believe("<(&&,<($a,is,cat) --> test>,<($a,is,$b) --> sentence>) ==> <$a --> $b>>");
        tester.believe("<(tim,is,cat) --> test>");
        tester.mustBelieve(cycles*2, "<<(tim,is,$1) --> sentence> ==> <tim --> $1>>", 1.00f, 0.81f); //en("there is a lock which is opened by key1");

    }

    @Test //see discussion on https://groups.google.com/forum/#!topic/open-nars/1TmvmQx2hMk
    public void impliesUnbelievedYet()  {
        TestNAR tester = test();
        tester.believe("(x:a ==> c:d)."); //x:a, x:#1, x:$1
        tester.believe("x:a.");
        tester.mustBelieve(cycles, "c:d", 1.00f, 0.81f); //en("there is a lock which is opened by key1");
    }

    @Test
    public void equivVariableSubst()  {
        TestNAR tester = test();
        tester.believe("(x,y).");
        tester.believe("((x,$y)<=>($y,x)).");
        tester.mustBelieve(cycles, "(y,x)", 1.00f, 0.81f); //en("there is a lock which is opened by key1");
    }


    @Test public void recursionSmall2() throws nars.Narsese.NarseseException {

        test()
        .believe("<0 --> n>", 1.0f, 0.9f)
        .believe("<<$1 --> n> ==> <(/,next,$1,_) --> n>>", 1.0f, 0.9f)
        .ask("<(/,next,(/,next,0,_),_) --> n>")
        .mustBelieve(cycles*2, "<(/,next,0,_) --> n>", 1.0f, 1.0f, 0.73f, 1.0f)
        .mustBelieve(cycles*2, "<(/,next,(/,next,0,_),_) --> n>", 1.0f, 1.0f, 0.73f, 1.0f) //should work
        //.mustBelieve(time, "((/,next,(/,next,(/,next,0,_),_),_)-->n).", 1.0f, 1.0f, finalConf, 1.0f)
        ;
    }

    @Test public void testDecomposeImplPred() {
        test()
            .believe("( (a,#b) ==> (&&, (x,#b), y, z ) )")
            .mustBelieve(cycles, "( (a,#b) ==> (x,#b) )", 1f, 0.73f)
            .mustBelieve(cycles, "( (a,#b) ==> y )", 1f, 0.73f)
            .mustBelieve(cycles, "( (a,#b) ==> z )", 1f, 0.73f)
        ;
    }

    @Test public void testEquivSpecificPP() {
        test().believe("(y)").believe("(($x) <=> ($x,y))")
            .mustBelieve(cycles, "(y,y)", 1f, 0.81f) ; //B, (A <=> C), ...
    }
    @Test public void testEquivSpecificNP() {
        test().believe("--(y)").believe("(($x) <=> ($x,y))")
            .mustBelieve(cycles, "(y,y)", 0f, 0.81f) ; //B, (A <=> C), ...
    }
    @Test public void testEquivSpecificPN() {
        test().log().believe("(y)").believe("--(($x) <=> ($x,y))")
            .mustBelieve(cycles, "(y,y)", 0f, 0.81f) ; //B, (A <=> C), ...
    }
    @Test public void testEquivSpecificNN() {
        test().believe("--(y)").believe("--(($x) <=> ($x,y))")
            .mustBelieve(cycles, "(y,y)", 1f, 0.81f) ; //B, (A <=> C), ...
    }

    @Test public void testImplSpecificNeg() {
        // B, (C ==> A), belief(negative), time(decomposeBeliefLate) |- (--,subIfUnifiesAny(C,A,B)), (Belief:AbductionPN, Goal:DeductionPN)
        test()
            //.log()
            .believe("--(($x) ==> ($x,y))")
            .believe("(y)")
            .mustBelieve(cycles, "(y,y)", 0f, 0.81f)
        ;
    }


    @Test
    public void recursionSmall() throws nars.Narsese.NarseseException {

        //<patham9> this is the only rule which is needed in this example
        //B (A ==> C) |- C :post (:t/deduction :order-for-all-same) :pre ((:substitute-if-unifies "$" A B) (:shift-occurrence-forward ==>))


        test()
        .believe("num:0", 1.0f, 0.9f)
        .believe("( num:$1 ==> num($1) )", 1.0f, 0.9f)
        .ask("num(((0)))")
        .mustBelieve(cycles, "num(0)", 1.0f, 1.0f, 0.4f /*0.81f*/, 1.0f)
        //.mustBelieve(cycles, "num((0))", 1.0f, 1.0f, 0.32f, 1.0f)
        .mustBelieve(cycles, "num(((0)))", 1.0f, 1.0f, 0.28f, 1.0f)
        //.mustBelieve(time, "num:((((0))))", 1.0f, 1.0f, 0.81f, 1.0f)
        // ''outputMustContain('<(((0))) --> num>. %1.00;0.26%')
        ;
    }
    @Test
    public void recursionSmall1() throws nars.Narsese.NarseseException {

        //<patham9> this is the only rule which is needed in this example
        //B (A ==> C) |- C :post (:t/deduction :order-for-all-same) :pre ((:substitute-if-unifies "$" A B) (:shift-occurrence-forward ==>))


        test()
                .believe("num(0)", 1.0f, 0.9f)
                .believe("( num($1) ==> num(($1)) )", 1.0f, 0.9f)
                .ask("num(((0)))")
                .mustBelieve(cycles, "num((0))", 1.0f, 1.0f, 0.66f, 1.0f)
                .mustBelieve(cycles, "num(((0)))", 1.0f, 1.0f, 0.21f /*0.66f*/, 1.0f)
        //.mustBelieve(time, "num:(((0)))", 1.0f, 1.0f, 0.66f, 1.0f)
        //.mustBelieve(time, "num:((((0))))", 1.0f, 1.0f, 0.81f, 1.0f)
        // ''outputMustContain('<(((0))) --> num>. %1.00;0.26%')
        ;
    }
//    @Test public void missingEdgeCase1() {
//        //((<%1 --> %2>, <(&&, %3, <%1 --> $4>) ==> %5>, substitute($4, %2)), (<%3 ==> %5>, (<Deduction --> Truth>, <ForAllSame --> Order>)))
//        //  ((<p1 --> p2>, <(&&, p3, <p1 --> $4>) ==> p5>, substitute($4, p2)), (<p3 ==> p5>, (<Deduction --> Truth>, <ForAllSame --> Order>)))
//        new RuleTest("<p1 --> p2>.","<(&&, p3, <p1 --> $4>) ==> p5>.",
//                "<p3 ==> p5>.", 0, 1, 0, 1).run();
//
//    }

}
