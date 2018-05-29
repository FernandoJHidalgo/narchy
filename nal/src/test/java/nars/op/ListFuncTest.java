package nars.op;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.term.Evaluation;
import nars.term.Term;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static nars.$.$$;
import static nars.Op.False;
import static nars.Op.Null;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ListFuncTest {
    
    final NAR n = NARS.shell();

    @Test
    public void testAppendTransform() {
        

        assertEquals(
                Set.of($$("(x,y)")),
                Evaluation.solveAll($$("append((x),(y))"), n));
        assertEquals(
                Set.of($$("append(#x,(y))")),
                Evaluation.solveAll($$("append(#x,(y))"), n));

    }

    @Test
    public void testAppendResult() {
        


        //solve result
        assertEquals(
                Set.of($$("append((x),(y),(x,y))")),
                Evaluation.solveAll($$("append((x),(y),#what)"), n));

        //solve result in multiple instances
        assertEquals(
                Set.of($$("(append((x),(y),(x,y)) && ((x,y)<->solution))")),
                Evaluation.solveAll($$("(append((x),(y),#what) && (#what<->solution))"), n));

    }


    @Test
    public void testTestResult() {
        


        assertEquals(
                Set.of($$("append((x),(y),(x,y))")),
                Evaluation.solveAll($$("append((x),(y),(x,y))"), n));

        assertEquals(
                Set.of($$("append(x,y,(x,y))")),
                Evaluation.solveAll($$("append(x,y,(x,y))"), n));

        assertEquals(
                Set.of($$("append((x),(y),(x,y,z))").neg()),
                Evaluation.solveAll($$("append((x),(y),(x,y,z))"), n));

    }

    @Test
    public void testAppendTail() {
        


        //solve tail
        assertEquals(
                Set.of($$("append((x),(y),(x,y))")),
                Evaluation.solveAll($$("append((x),#what,(x,y))"), n));

        //solve tail with non-list prefix that still matches
        assertEquals(
                Set.of($$("append(x,(y),(x,y))")),
                Evaluation.solveAll($$("append(x,#what,(x,y))"), n));

        //solve tail but fail
        assertEquals(
                Set.of(Null),
                Evaluation.solveAll($$("append((z),#what,(x,y))"), n));

        //solve result in multiple instances
        assertEquals(
                Set.of($$("(append((x),(),(x)) && (()<->solution))")),
                Evaluation.solveAll($$("(append((x),#what,(x)) && (#what<->solution))"), n));

    }

    @Test
    public void testAppendHeadAndTail() {
        


        assertEquals(
                Set.of(
                        $$("append((x,y,z),(),(x,y,z))"),
                        $$("append((x,y),(z),(x,y,z))"),
                        $$("append((x),(y,z),(x,y,z))"),
                        $$("append((),(x,y,z),(x,y,z))")
                ),
                Evaluation.solveAll($$("append(#x,#y,(x,y,z))"), n));
    }
    @Test
    public void testAppendHeadAndTailMulti() {
        


        assertEquals(
            Set.of(
                    $$("(append((),(x,y),(x,y)),append((a),(b),(a,b)))"),
                    $$("(append((x),(y),(x,y)),append((),(a,b),(a,b)))"),
                    $$("(append((),(x,y),(x,y)),append((a,b),(),(a,b)))"),
                    $$("(append((x),(y),(x,y)),append((a,b),(),(a,b)))"),
                    $$("(append((x,y),(),(x,y)),append((a,b),(),(a,b)))"),
                    $$("(append((),(x,y),(x,y)),append((),(a,b),(a,b)))"),
                    $$("(append((x),(y),(x,y)),append((a),(b),(a,b)))"),
                    $$("(append((x,y),(),(x,y)),append((a),(b),(a,b)))"),
                    $$("(append((x,y),(),(x,y)),append((),(a,b),(a,b)))")
            ),
            Evaluation.solveAll($$("(append(#x,#y,(x,y)), append(#a,#b,(a,b)))"), n));

        assertEquals(
                Set.of(
                        $$("(append((),(x,y),(x,y)),append((),(x,b),(x,b)))"),
                        $$("(append((x),(y),(x,y)),append((x),(b),(x,b)))")
                ),
                Evaluation.solveAll($$("(append(#x,#y,(x,y)), append(#x,#b,(x,b)))"), n));

        assertEquals(
                Set.of(
                        (Term)False,
                        $$("(append((),(x,y),(x,y)) && append((),(x,b),(x,b)))"),
                        $$("(append((x),(y),(x,y)) && append((x),(b),(x,b)))")
                ),
                Evaluation.solveAll($$("(&&,append(#x,#y,(x,y)),append(#a,#b,(x,b)),equal(#x,#a))"), n));

    }

    @Test
    public void testAppendHead() {
        
        

        //solve head
        assertEquals(
                Set.of($$("append((x),(y),(x,y))")),
                Evaluation.solveAll($$("append(#what,(y),(x,y))"), n));

        assertEquals(
                Set.of($$("append((),(x,y),(x,y))")),
                Evaluation.solveAll($$("append(#what,(x,y),(x,y))"), n));

    }

    @Test
    public void testHanoi() throws Narsese.NarseseException {
        /*
        http://book.simply-logical.space/part_ii.html#reasoning_with_structured_knowledge
        An analytic solution to the Towers of Hanoi

        In the case of the Towers of Hanoi, there is a simple analytic solution based on the following observation: suppose we are able to solve the problem for n –1 disks, then we can solve it for n disks also: move the upper n –1 disks from the left to the middle peg [12] , move the remaining disk on the left peg to the right peg, and move the n –1 disks from the middle peg to the right peg. Since we are able to solve the problem for 0 disks, it follows by complete induction that we can solve the problem for any number of disks. The inductive nature of this argument is nicely reflected in the following recursive program:

          :-op(900,xfx,to).

          % hanoi(N,A,B,C,Moves) <-Moves is the list of moves to
          %                        move N disks from peg A to peg C,
          %                        using peg B as intermediary peg

          hanoi(0,A,B,C,[]).

          hanoi(N,A,B,C,Moves):-
            N1 is N-1,
            hanoi(N1,A,C,B,Moves1),
            hanoi(N1,B,A,C,Moves2),
            append(Moves1,[A to C|Moves2],Moves).

        For instance, the query ?-hanoi(3,left,middle,right,M) yields the answer

          M = [left to right, left to middle, right to middle,
        left to right,
        middle to left, middle to right, left to right ]

        The first three moves move the upper two disks from the left to the middle peg, then the largest disk is moved to the right peg, and again three moves are needed to move the two disks on the middle peg to the right peg.
         */

        NAR n = NARS.tmp();
        n.termVolumeMax.set(64);

        int levels =
                2;
                //3;

        n.log();
        n.input("hanoi(0,#A,#B,#C,()).");
        n.input(
                "((&&" +
                ",hanoi(add($N,-1),$A,$C,$B,#Moves1)" +
                ",hanoi(add($N,-1),$B,$A,$C,#Moves2)" +
                ",append(#Moves1,append(to($A,$C),#Moves2),$Moves)) ==> hanoi($N,$A,$B,$C,$Moves)).");

        for ( ; levels > 0; levels--)
            n.input("hanoi(" + levels + ",left,middle,right,#M)?");

        n.run(2000);

        /*


        hanoi(N,A,B,C,Moves):-
                N1 is N-1,
                hanoi(N1,A,C,B,Moves1),
                hanoi(N1,B,A,C,Moves2),
                append(Moves1,[A to C|Moves2],Moves).
        */
    }
//    @Test
//    public void test1() {
//        NAR n = NARS.tmp(3);
//        Deriver listDeriver = new Deriver(n, "list.nal");
//
////                "motivation.nal"
////                //, "goal_analogy.nal"
////        ).apply(n).deriver, n) {
//        TestNAR t = new TestNAR(n);
//    }
}
