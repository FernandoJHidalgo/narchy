package nars.test;

import nars.$;
import nars.NAR;
import nars.Param;
import nars.nar.Default;
import nars.term.Compound;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static nars.time.Tense.ETERNAL;


public class DeductiveMeshTest {

    @NotNull
    public final Compound q;
    @NotNull
    public final List<Compound> coords;





    public DeductiveMeshTest(@NotNull NAR n, @NotNull int... dims) {
        this(n, dims, -1);
    }

    public DeductiveMeshTest(@NotNull NAR n, @NotNull int[] dims, int timeLimit) {
        this(new TestNAR(n), dims, timeLimit);
    }

    public DeductiveMeshTest(@NotNull TestNAR n, @NotNull int[] dims, int timeLimit) {

        if (dims.length!=2)
            throw new UnsupportedOperationException("2-D only implemented");

        Set<Compound> edges = new HashSet();

        coords = $.newArrayList();
        for (int x = 0; x < dims[0]; x++) {
            for (int y = 0; y < dims[1]; y++) {
                //Compound c = c(x, y);

                if (x > y) {
                    if (x > 0)
                        edges.add(link(x, y, x - 1, y));
                    if (y > 0)
                        edges.add(link(x, y, x, y - 1));
                    if (x < dims[0] - 1)
                        edges.add(link(x, y, x + 1, y));
                    if (y < dims[1] - 1)
                        edges.add(link(x, y, x, y + 1));
                }
            }
        }

        edges.forEach(n.nar::believe);

        Compound term = q = link(0, 0, dims[0] - 1, dims[1] - 1);
        ask(n, term);

        if (timeLimit>0)
            n.mustBelieve(timeLimit, q.toString(), 1f, 1f, 0.01f, 1f);

    }

    public void ask(@NotNull TestNAR n, Compound term) {
        n.nar.question(term, ETERNAL, (q, a) -> {
            System.out.println(a.proof());
        });
    }

    @Nullable
    private Compound link(int x1, int y1, int x2, int y2) {
        //return $.prop($.p(a, b), $.the("X"));
        return $.sim( $.p($.the(x1), $.the(y1)), $.p($.the(x2), $.the(y2)) );
    }

    public @NotNull Compound c(int x, int y) {
        return $.p($.the(x), $.the(y));
    }


    @NotNull
    static Atomic a(int i) {
        return $.the((byte)('a' + i));
    }


    public static void main(String[] args) {

        Param.DEBUG = false;



        Default n = new Default();
        //n.nal(5);
        n.logBudgetMin(System.out, 0.1f);


//        n.onCycle(x -> {
//            if (n.time() == 1000) {
//                System.out.println(Arrays.toString(n.core.active.priHistogram(new double[10])));
//            }
//        });

        test(n, new int[] { 3, 3 }, 2500);



    }

    static void test(@NotNull NAR n, @NotNull int[] dims, int cycles) {


        TestNAR testnar = new TestNAR(n);
        DeductiveMeshTest test = new DeductiveMeshTest(testnar, dims, cycles);

        System.out.print(DeductiveMeshTest.class.getSimpleName() + " test: "
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
