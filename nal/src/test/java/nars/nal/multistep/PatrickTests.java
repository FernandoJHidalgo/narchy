package nars.nal.multistep;

import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.nal.AbstractNALTest;
import nars.nar.Default;
import nars.test.TestNAR;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.function.Supplier;

import static nars.$.$;

/**
 * see Natural_Language_Processing2.md
 */
@RunWith(Parameterized.class)
public class PatrickTests extends AbstractNALTest {

    public PatrickTests(Supplier<NAR> b) {
        super(b);
    }

    @Parameterized.Parameters(name = "{index}:{0}")
    public static Iterable<Supplier<NAR>> configurations() {
        return AbstractNALTest.nars(8);
    }


    @Test
    public void testExample1() {
        /*
        ////Example 1, REPRESENT relation with lifting
        //the whole can sometimes be understood by understanding what the parts represent (lifting)

        <(&&,<$1 --> (/,REPRESENT,_,$3)>,<$2 --> (/,REPRESENT,_,$4)>) ==> <(*,(*,$1,$2),(*,$3,$4)) --> REPRESENT>>.
        //the word fish represents the concept FOOD
        <cat --> (/,REPRESENT,_,ANIMAL)>.
        //the word eats represents the concept EATING
        <eats --> (/,REPRESENT,_,EATING)>.

        //what does cat eats represent?
        <(*,(*,cat,eats),?what) --> REPRESENT>?
        //RESULT: <(*,(*,cat,eats),(*,ANIMAL,EATING)) --> REPRESENT>. %1.00;0.73%
         */
        TestNAR tt = test();
        tt
//.log()
                .believe("(((/,REPRESENT,_,$3):$1 && (/,REPRESENT,_,$4):$2) ==> REPRESENT:(($1,$2),($3,$4)))")
                .believe("(/,REPRESENT,_,ANIMAL):cat")
                .believe("(/,REPRESENT,_,EATING):eats")

//should WORK with either of these two questions:
//.askAt(1250,"REPRESENT:((eats,cat),?what)")
                .askAt(550, "REPRESENT:((cat,eats),(?x, ?y))")

                .mustBelieve(2500, "REPRESENT:((eats,cat),(EATING, ANIMAL))", 1f, 0.32f);
        //.mustBelieve(2500, "REPRESENT:((eats, cat),(EATING,ANIMAL))", 1f, 0.73f);

    }

//    @Test public void testExample1a() {
//        /*
//        ////Example 1, REPRESENT relation with lifting
//        //the whole can sometimes be understood by understanding what the parts represent (lifting)
//
//        <(&&,<$1 --> (/,REPRESENT,_,$3)>,<$2 --> (/,REPRESENT,_,$4)>) ==> <(*,(*,$1,$2),(*,$3,$4)) --> REPRESENT>>.
//        //the word fish represents the concept FOOD
//        <cat --> (/,REPRESENT,_,ANIMAL)>.
//        //the word eats represents the concept EATING
//        <eats --> (/,REPRESENT,_,EATING)>.
//
//        //what does cat eats represent?
//        <(*,(*,cat,eats),?what) --> REPRESENT>?
//        //RESULT: <(*,(*,cat,eats),(*,ANIMAL,EATING)) --> REPRESENT>. %1.00;0.73%
//         */
//        TestNAR t = test();
//        //t.nar.DEFAULT_JUDGMENT_PRIORITY = 0.1f;
//        //t.nar.DEFAULT_QUESTION_PRIORITY = 0.1f;
//        t
//.log()
//.believe("(($1:(/,REPR,_,$3) && $2:(/,REPR,_,$4)) ==> REPR:(($1,$3)<->($2,$4)))")
//.believe("cat:(/,REPR,_,ANIMATING)")
//.believe("eats:(/,REPR,_,EATING)")
//
////should WORK with either of these two questions:
////.askAt(100,"REPR:((cat,ANIMATING)<->?what)")
//.askAt(10000,"REPR:((cat,ANIMATING)<->(?x, ?y))")
//
//.mustBelieve(11250, "REPR:((eats,EATING)<->(cat,ANIMATING))", 1f, 0.73f);
//
//
//    }


    @Test
    public void testToothbrush() {
        /*
        <(*,toothbrush,plastic) --> made_of>.
        <(&/,<(*,$1,plastic) --> made_of>,<({SELF},$1) --> op_lighter>) =/> <$1 --> [heated]>>.
        <<$1 --> [heated]> =/> <$1 --> [melted]>>.
        <<$1 --> [melted]> <|> <$1 --> [pliable]>>.
        <(&/,<$1 --> [pliable]>,<({SELF},$1) --> op_reshape>) =/> <$1 --> [hardened]>>.
        <<$1 --> [hardened]> =|> <$1 --> [unscrewing]>>.
        <toothbrush --> object>.
        (&&,<#1 --> object>,<#1 --> [unscrewing]>)!

            >> lighter({SELF},$1) instead of <({SELF},$1) --> op_lighter>

        */


        TestNAR tt = test();
        tt.nar.time.dur(25);
        tt.nar.termVolumeMax.setValue(32);

        tt.log();
        tt.input(
                "made_of(toothbrush,plastic).",
                "( ( made_of($1, plastic) &&+0 lighter(I, $1) ) ==>+10 <$1 --> [heated]>).",
                "(<$1 --> [heated]> ==>+10 <$1 --> [melted]>).",
                "(<$1 --> [melted]> <=>+0 <$1 --> [pliable]>).",
                "(( <$1 --> [pliable]> &&+0 reshape(I,$1)) ==>+10 <$1 --> [hardened]>).",
                "(<$1 --> [hardened]> ==>+0 <$1 --> [unscrews]>).",
                "<toothbrush --> here>. :|:", //there is a toothbrush here NOW
                "( <#1 --> here> &&+0 <#1 --> [unscrews]>)! :|:" //make something that is here a screwdriver
        );
        //tt.log();

        tt.mustDesire(1000, "lighter(I, toothbrush)", 1f,
                0.39f,
/*@*/ 0L);  //is this correct time? might be off by +/-10 , will check


    }

    @Ignore
    @Test
    public void testConditioningWithoutAnticipation() throws Narsese.NarseseException {
        /*
        <a --> A>. :|: <b --> B>. :|: %0% <c --> C>. %0%
        8
        <b --> B>. :|: <a --> A>. :|: %0% <c --> C>. %0%
        8
        <c --> C>. :|: <a --> a>. :|: %0% <b --> B>. %0%
        8
        <a --> A>. :|: <b --> B>. :|: %0% <c --> C>. %0%
        100
        <b --> B>. :|: <a --> A>. :|: %0% <c --> C>. %0%
        100
        <?1 =/> <c --> C>>? //this line needs to be translated to NARchy syntax

        Expected result: (also in OpenNARS syntax)
        For appropriate Interval term "time", "time2",
        <(&/,<a --> A>,time) =/> <c --> C>>.
        and
        <(&/,<b --> B>,time) =/> <c --> C>>.
        needs to be reduced in frequency, making
        <(&/,<a --> A>,time,<b --> B>,time2) =/> <c --> C>>.
        the strongest hypothesis based on the last two inputs where neither a nor b "leaded to" c.
         */

        Default n = new Default(1024, 3);
        n.DEFAULT_BELIEF_PRIORITY = 0.01f;
        n.termVolumeMax.setValue(16);


        //n.log();
        n.inputAt(0, "  A:a. :|:    --B:b. :|:    --C:c. :|:");
        n.inputAt(8, "  B:b. :|:    --A:a. :|:    --C:c. :|:");
        n.inputAt(16, "  C:c. :|:    --A:a. :|:    --B:b. :|:");
        n.inputAt(24, "  A:a. :|:    --B:b. :|:    --C:c. :|:");
        n.inputAt(124, "  B:b. :|:    --A:a. :|:    --C:c. :|:");

        n.run(224);
        n.clear();

        n.input("       $0.9;0.9$ (?x ==>   C:c)?");
        //n.input("       $0.9;0.9$ (?x ==>+8 C:c)?");
        //n.input("       $0.9;0.9$ ((A:a && B:b) ==> C:c)?");
        //n.input("       $0.9;0.9$ ((A:a && B:b) ==> C:c)? :|:");
        n.run(2000);

        /*
        Expected result: (also in OpenNARS syntax)
        For appropriate Interval term "time", "time2",
        <(&/,<a --> A>,time) =/> <c --> C>>.
        and
        <(&/,<b --> B>,time) =/> <c --> C>>.
        needs to be reduced in frequency, making
        <(&/,<a --> A>,time,<b --> B>,time2) =/> <c --> C>>.
        the strongest hypothesis based on the last two inputs where neither a nor b "leaded to" c.
         */

    }

    @Ignore
    @Test
    public void testPixelImage() throws Narsese.NarseseException {

        Default n = new Default(1024, 3);
        //n.log();
        //n.truthResolution.setValue(0.05f);
        n.termVolumeMax.setValue(60);
        n.DEFAULT_BELIEF_PRIORITY = 0.05f;
        n.DEFAULT_QUESTION_PRIORITY = 0.9f;
        n.DEFAULT_QUESTION_QUALITY = 0.9f;

        n.input("<#x --> P>. %0.0;0.25%"); //assume that unless pixel isnt specified then it's black

// to what extent was
//    |          |
//    |    ██    |
//    |  ██████  |
//    |    ██    |
//    |          |
//observed in experience?

//imperfectly observed pattern
//    |      ░░  |
//    |    ▓▓    |
//    |░░▓▓██    |
//    |    ▒▒  ░░|
//    |      ░░  |
        String image1 =
                "<p_1_1 --> P>. :|: %0.5;0.9%\n" +
                        "<p_1_2 --> P>. :|: %0.5;0.9%\n" +
                        "<p_1_3 --> P>. :|: %0.6;0.9%\n" +
                        "<p_1_4 --> P>. :|: %0.6;0.9%\n" +
                        "<p_1_5 --> P>. :|: %0.5;0.9%\n" +
                        "<p_2_1 --> P>. :|: %0.5;0.9%\n" +
                        "<p_2_2 --> P>. :|: %0.5;0.9%\n" +
                        "<p_2_3 --> P>. :|: %0.8;0.9%\n" +
                        "<p_2_4 --> P>. :|: %0.5;0.9%\n" +
                        "<p_2_5 --> P>. :|: %0.5;0.9%\n" +
                        "<p_3_1 --> P>. :|: %0.6;0.9%\n" +
                        "<p_3_2 --> P>. :|: %0.8;0.9%\n" +
                        "<p_3_3 --> P>. :|: %0.9;0.9%\n" +
                        "<p_3_4 --> P>. :|: %0.5;0.9%\n" +
                        "<p_3_5 --> P>. :|: %0.5;0.9%\n" +
                        "<p_4_1 --> P>. :|: %0.5;0.9%\n" +
                        "<p_4_2 --> P>. :|: %0.5;0.9%\n" +
                        "<p_4_3 --> P>. :|: %0.7;0.9%\n" +
                        "<p_5_4 --> P>. :|: %0.6;0.9%\n" +
                        "<p_4_4 --> P>. :|: %0.5;0.9%\n" +
                        "<p_4_5 --> P>. :|: %0.6;0.9%\n" +
                        "<p_5_1 --> P>. :|: %0.5;0.9%\n" +
                        "<p_5_2 --> P>. :|: %0.5;0.9%\n" +
                        "<p_5_3 --> P>. :|: %0.5;0.9%\n" +
                        "<p_5_5 --> P>. :|: %0.5;0.9%\n" +
                        "<example1 --> name>. :|:";


        n.input(image1.split("\n"));


        //(&|,<p_2_3 --> pixel>,<p_3_2 --> pixel>,<p_3_3 --> pixel>,<p_3_4 --> pixel>,<p_4_3 --> pixel>,<example1 --> name>)?\n" +

        //for (int i = 0; i < 2; i++) {
        n.question($.parallel($("P:p_2_3"), $("P:p_3_2"), $("P:p_3_4"), $("P:p_4_3"), $("name:example1")));
        //}

        //Answer (&|,<example1 --> name>,<p_2_3 --> pixel>,<p_3_2 --> pixel>,<p_3_3 --> pixel>,<p_3_4 --> pixel>,<p_4_3 --> pixel>). :-1: %0.80;0.16%
        //ex: (&&,(example1-->name),(p_2_3-->pixel),(p_3_2-->pixel),(p_3_4-->pixel),(p_4_3-->pixel)). %.61;.06%"".


        n.run(6000);

        n.clear();

//imperfectly observed pattern
//    |      ░░  |
//    |    ▓▓    |
//    |░░    ▓▓  |
//    |    ▒▒  ░░|
//    |      ░░  |
        String image2 =
                "<p_1_1 --> pixel>. :|: %0.5;0.9%\n" +
                        "<p_1_2 --> pixel>. :|: %0.5;0.9%\n" +
                        "<p_1_3 --> pixel>. :|: %0.6;0.9%\n" +
                        "<p_1_4 --> pixel>. :|: %0.6;0.9%\n" +
                        "<p_1_5 --> pixel>. :|: %0.5;0.9%\n" +
                        "<p_2_1 --> pixel>. :|: %0.5;0.9%\n" +
                        "<p_2_2 --> pixel>. :|: %0.5;0.9%\n" +
                        "<p_2_3 --> pixel>. :|: %0.8;0.9%\n" +
                        "<p_2_4 --> pixel>. :|: %0.5;0.9%\n" +
                        "<p_2_5 --> pixel>. :|: %0.5;0.9%\n" +
                        "<p_3_1 --> pixel>. :|: %0.6;0.9%\n" +
                        "<p_3_2 --> pixel>. :|: %0.5;0.9%\n" +
                        "<p_3_3 --> pixel>. :|: %0.5;0.9%\n" +
                        "<p_3_4 --> pixel>. :|: %0.8;0.9%\n" +
                        "<p_3_5 --> pixel>. :|: %0.5;0.9%\n" +
                        "<p_4_1 --> pixel>. :|: %0.5;0.9%\n" +
                        "<p_4_2 --> pixel>. :|: %0.5;0.9%\n" +
                        "<p_4_3 --> pixel>. :|: %0.7;0.9%\n" +
                        "<p_5_4 --> pixel>. :|: %0.6;0.9%\n" +
                        "<p_4_4 --> pixel>. :|: %0.5;0.9%\n" +
                        "<p_4_5 --> pixel>. :|: %0.6;0.9%\n" +
                        "<p_5_1 --> pixel>. :|: %0.5;0.9%\n" +
                        "<p_5_2 --> pixel>. :|: %0.5;0.9%\n" +
                        "<p_5_3 --> pixel>. :|: %0.5;0.9%\n" +
                        "<p_5_5 --> pixel>. :|: %0.5;0.9%\n" +
                        "<example2 --> name>. :|:";

        n.input(image2.split("\n"));


        //(&|,<p_2_3 --> pixel>,<p_3_2 --> pixel>,<p_3_3 --> pixel>,<p_3_4 --> pixel>,<p_4_3 --> pixel>,<example2 --> name>)?

        //for (int i = 0; i < 8; i++) {
        n.question($.parallel($("P:p_2_3"), $("P:p_3_2"), $("P:p_3_3"), $("P:p_3_4"), $("P:p_4_3"), $("name:example2")));
        n.run(6000);
        //}

        //Answer (&|,<example2 --> name>,<p_2_3 --> pixel>,<p_3_2 --> pixel>,<p_3_3 --> pixel>,<p_3_4 --> pixel>,<p_4_3 --> pixel>). %0.50;0.40%


    }
}
