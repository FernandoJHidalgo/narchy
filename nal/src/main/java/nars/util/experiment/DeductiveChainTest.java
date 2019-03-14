package nars.util.experiment;

import nars.$;
import nars.NAR;
import nars.Param;
import nars.nar.Default;
import nars.term.Compound;
import nars.term.atom.Atom;
import nars.util.signal.TestNAR;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class DeductiveChainTest  {

    @NotNull
    public final Compound q;
    @NotNull
    public final Compound[] beliefs;

    @FunctionalInterface
    public interface IndexedStatementBuilder {
        @NotNull
        Compound apply(int x, int y);
    }

    @Nullable
    public static final IndexedStatementBuilder inh = (int x, int y) ->
            (Compound)$.inh(a(x), a(y));
    @Nullable
    public static final IndexedStatementBuilder sim = (int x, int y) ->
            (Compound)$.sim(a(x), a(y));
    @Nullable
    public static final IndexedStatementBuilder impl = (int x, int y) ->
            (Compound)$.impl(a(x), a(y));
    @Nullable
    public static final IndexedStatementBuilder equiv = (int x, int y) ->
            (Compound)$.equi(a(x), a(y));

    public DeductiveChainTest(@NotNull NAR n, int length, int timeLimit, @NotNull IndexedStatementBuilder b) {
        this(new TestNAR(n), length, timeLimit, b);
    }

    public DeductiveChainTest(@NotNull TestNAR n, int length, int timeLimit, @NotNull IndexedStatementBuilder b) {

        beliefs = new Compound[length];
        for (int x = 0; x < length; x++) {
            beliefs[x] = b.apply(x, x+1);
        }

        q = b.apply(0, length);

        for (Compound belief : beliefs) {
            n.nar.believe(belief);
        }
        n.nar.ask( q );

        n.mustBelieve(timeLimit, q.toString(), 1f, 1f, 0.01f, 1f);

    }


    @NotNull
    public static Atom a(int i) {
        return $.the((byte)('a' + i));
    }


    public static void main(String[] args) {

        Param.DEBUG = false;

        for (int length = 3; length < 10; length++) {
            Default n = new Default(1024, 1, 1, 3);
            n.nal(6);
            test(n, length, 1000*length, inh);
        }
    }

    static void test(@NotNull NAR n, int chainLen, int cycles, @NotNull IndexedStatementBuilder statementType) {


        TestNAR testnar = new TestNAR(n);
        DeductiveChainTest test = new DeductiveChainTest(testnar, chainLen, cycles, statementType) {
//            @Override
//            public TestNAR mustBelieve(long withinCycles, String term, float confidence, float x, float y, float z) throws InvalidInputException {
//                return this;
//            }
        };

        System.out.print(DeductiveChainTest.class.getSimpleName() + " test: "
                + test.q + "?\t");

        final long start = System.currentTimeMillis();

//        new AnswerReaction(n) {
//
//            @Override
//            public void onSolution(Task belief) {
//                if (belief.getTerm().equals(test.q)) {
//                    System.out.println(belief + " " + timestamp(start) + " " +
//                            n.concepts().size() + " concepts");
//                    System.out.println(belief.getExplanation());
//                    System.out.println();
//                }
//            }
//        };


        testnar.run(false);


        //n.stdout();
        //n.frame(5000);

        int nc = ((Default) n).core.concepts.size();
        String ts = timestamp(start);
        long time = n.time();

        //n.stdout();
        //n.frame(55); //to print the ending

        //while (true) {

//        Report report = new Report(test, test.error);
//
//
//
//        test.requires.forEach(report::add);
//
//
//        Report r = report;
//
//        System.out.println(
//                (r.isSuccess() ? "OK" : "ERR") +
//                "\t@" + time + " (" + ts + "ms) " +
//                nc + 'C');


        //TextOutput.out(n).setOutputPriorityMin(0.85f);

//        while (true) {
//
//            n.run(500);
//            //sleep(20);
//
//            if (n.time() % printEvery == 0) {
//                System.out.println(n.time() + " " + timestamp(start) + " " +
//                        n.memory().size());
//            }
//        }


    }

    @NotNull
    private static String timestamp(long start) {
        return (System.currentTimeMillis() - start) + " ms";
    }
}
