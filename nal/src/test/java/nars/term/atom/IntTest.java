package nars.term.atom;

import nars.*;
import nars.term.Term;
import org.junit.Ignore;
import org.junit.Test;

import static nars.term.atom.Int.range;
import static nars.term.atom.Int.the;
import static org.junit.Assert.assertEquals;

public class IntTest {

    @Ignore
    @Test
    public void testVariableIntroduction() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.log();
        n.input(" ((3,x) ==>+1 (4,y)).");
        // ((3,x) ==>+1 (add(3,1),y)).

        n.run(10);
    }

    @Test
    public void testIntRange1() throws Narsese.NarseseException {
        Atomic ii = range(0, 2);
        assertEquals("0..2", ii.toString());

        NAR n = NARS.tmp();
        n.log();
        n.believe("(f(1) <-> 5)");
        n.believe($.sim($.func("f", new Term[]{ii}), $.varDep(1)));
        n.run(10);
    }

    @Test
    public void testIntIntersectionReduction() throws Narsese.NarseseException {
        //(P --> M), (S --> M), task("."), notSet(S), notSet(P), neqRCom(S,P) |- ((S | P) --> M), (Belief:Intersection)
        //(P --> M), (S --> M), task("."), notSet(S), notSet(P), neqRCom(S,P) |- ((S & P) --> M), (Belief:Union)

        //simple scalar agglomeration
        assertEquals(
                range(0, 1),
                Op.SECTi.the(the(0), the(1))
        );
        NAR n = NARS.tmp();
        n.log();
        n.believe($.inh($.the(0), $.the("x")));
        n.believe($.inh($.the(1), $.the("x")));
        n.run(10);
    }

    @Test
    public void testIntInProductIntersectionReduction() throws Narsese.NarseseException {

        //simple scalar agglomeration
        assertEquals(
                //$.p(range(0,1), range(0, 2)),
                //"(|,(0,0..2),(0..1,1),(0..1,0..2))",
                "(0..1,0..2)",
                Op.SECTi.the($.p(0, 1), $.p(the(1), range(0,2))).toString()
        );

        NAR n = NARS.tmp();
        n.log();
        n.believe($.inh($.p(0, 1), $.the("x")));
        n.believe($.inh($.p(the(1), range(0,2)), $.the("x")));
        n.run(10);
    }

}
///**
// * Created by me on 8/5/16.
// */
//@Ignore
//public class ArithmeticInductionTest {
//
//    @Test
//    public void testIntRangeCompression() {
//
//
//        assertEquals( //simple range of raw ints
//                "`1<=?<=2`",
//                conj(the(1), the(2)).toString()
//        );
//        assertEquals( //simple range of raw ints
//                "`1<=?<=3`",
//                conj(the(1), the(2), the(3)).toString()
//        );
//
//        assertEquals( //range can not be formed
//                "(1&&3)",
//                conj(the(1), the(3)).toString()
//        );
//
//        assertEquals( //simple range of embedded ints
//                "(x,`1<=?<=2`)",
//                conj(p(Atomic.the("x"), the(1)), p(Atomic.the("x"), the(2))).toString()
//        );
//    }
//
//    @Test
//    public void testIntRangeCompressionInvalid1() {
//        assertEquals(
//                "((x,1)&&(y,2))",
//                conj(p(Atomic.the("x"), the(1)), p(Atomic.the("y"), the(2))).toString()
//        );
//    }
//    @Test
//    public void testIntRangeCompressionInvalid2() {
//        assertEquals(
//                "((x,`1<=?<=2`)&&(y,2))",
//                conj(p(Atomic.the("x"), the(1)), p(Atomic.the("x"), the(2)), p(Atomic.the("y"), the(2))).toString()
//        );
//    }
//    @Test public void testInvalidDueToDT() {
//        assertEquals(
//                "((x,1) &&+1 (x,2))",
//                seq(p(Atomic.the("x"), the(1)), 1, p(Atomic.the("x"), the(2))).toString()
//        );
//    }
//
//    @Test public void testNegationNotCombined() {
//        assertEquals(
//                "((--,(x,2))&&(x,1))",
//                conj(p(Atomic.the("x"), the(1)), $.neg(p(Atomic.the("x"), the(2)))).toString()
//        );
//    }
//
//    @Test
//    public void testIntRangeCompressionPartial() {
//        assertEquals( //partially covered range of embedded ints
//                "((x,4)&&(x,`1<=?<=2`))",
//                conj(p(Atomic.the("x"), the(1)), p(Atomic.the("x"), the(2)), p(Atomic.the("x"), the(4))).toString()
//        );
//    }
//
//    @Test
//    public void testIntRangeCombination() {
//        assertEquals( //partially covered range of embedded ints
//                "(x,`1<=?<=4`)",
//                conj(
//                    p(Atomic.the("x"), new Termject.IntInterval(1,2)),
//                    p(Atomic.the("x"), new Termject.IntInterval(2,4)))
//                .toString()
//        );
//    }
//
//    @Test public void testIntRangeDual() {
//        //This requires the recursive arithmetic induction
//
//        //two separate intervals
//        assertEquals(
//                "((x,`1<=?<=2`)&&(`2<=?<=3`,y))",
//                conj(
//                        p(Atomic.the("x"), the(1)),
//                        p(Atomic.the("x"), the(2)),
//                        p(the(2), Atomic.the("y")),
//                        p(the(3), Atomic.the("y"))
//                ).toString()
//        );
//    }
//
//    @Test public void testIntRangeMultiple() {
//        //This requires the recursive arithmetic induction
//        //two separate intervals
//        assertEquals(
//                "(&&,(x,8),(x,`1<=?<=2`),(x,`4<=?<=6`))",
//                conj(
//                        p(Atomic.the("x"), the(1)),
//                        p(Atomic.the("x"), the(2)),
//                        p(Atomic.the("x"), the(4)),
//                        p(Atomic.the("x"), the(5)),
//                        p(Atomic.the("x"), the(6)),
//                        p(Atomic.the("x"), the(8))
//                )
//                        .toString()
//        );
//    }
//
//    @Test public void testIntRangeMultipleNegation() {
//        //two separate intervals
//        assertEquals(
//                "(&&,(--,(x,`4<=?<=6`)),(x,8),(x,`1<=?<=2`))",
//                conj(
//                        p(Atomic.the("x"), the(1)),
//                        p(Atomic.the("x"), the(2)),
//                        $.neg(p(Atomic.the("x"), the(4))),
//                        $.neg(p(Atomic.the("x"), the(5))),
//                        $.neg(p(Atomic.the("x"), the(6))),
//                        p(Atomic.the("x"), the(8))
//                )
//                        .toString()
//        );
//    }
//    @Test public void testIntRangeDual2() {
//        //two separate intervals
//        assertEquals(
//                "(`2<=?<=3`,`1<=?<=2`)",
//                conj(
//                        p(the(3), the(1)),
//                        p(the(2), the(2))
//                ).toString()
//        );
//    }
//
//    @Test public void testGroupOfThree() {
//        //(`11`,`9`), (`17`,`2`), (`18`,`2`)
//        assertEquals(
//                "((11,9)&&(`17<=?<=18`,2))",
//                conj(
//                        p(the(11), the(9)),
//                        p(the(18), the(2)),
//                        p(the(17), the(2))
//                ).toString()
//        );
//
//    }
//
////        System.out.println(
////                //n.normalize((Compound)
////                    parallel(p(p(0,1), the(1)), p(p(0,1), the(3)), p(p(0,1), the(5)))
////
////        );
//        //n.next();
//
//
//}