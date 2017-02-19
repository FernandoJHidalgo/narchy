package nars.nlp;

import com.google.common.base.Joiner;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.nar.Default;
import nars.op.Command;
import nars.op.Operator;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.obj.IntTerm;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static nars.term.Terms.compoundOrNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 2/18/17.
 */
public class NLPOperatorTest {

    @Test
    public void testProductSlice() {
        //https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/slice
        //_.slice(array, [start=0], [end=array.length])

        Param.DEBUG = true;

        NAR n = new Default();
        n.log();
        n.input("(slice((a,b,c),2)).");
        n.input("assertEquals(c, slice((a,b,c),add(1,1)));");
        n.input("assertEquals((a,b), slice((a,b,c),(0,2)));");

        //TODO add invalid slice conditions

        n.input("(quote(x)).");
        n.input("log(quote(x));");
        n.input("assertEquals(c, c);");
        n.input("assertEquals(x, quote(x));");
        n.input("assertEquals(c, slice((a,b,c),2));");
        n.input("assertEquals(quote(slice((a,b,c),#x)), slice((a,b,c),#x));");
        n.run(5);


    }

    @Test
    public void testNLP1() {
        NAR n = new Default();
        n.log();
        n.input(

            "( ( INT:{$start} && INT:{$end} ) ==> RANGE($start,$end) )",

            "(( RANGE($start,$end) && SENTENCE:$x) ==> FRAG:slice($x, ($start,$end))).",

            "((VERB:$V && FRAG($N1,$V,$N2)) ==> (((/,MEANS,$N1,_),(/,MEANS,$N2,_)) --> (/,MEANS,$V,_))).",

            "INT:{0,1,2,3,4}.",
            "VERB:is.",

            "SENTENCE(yes,is,no)."
        );

        n.run(250);
    }

}
