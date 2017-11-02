package nars.perf;

import nars.nal.nal1.NAL1Test;
import nars.term.compound.FastCompoundNAL1Test;
import org.junit.jupiter.api.Disabled;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.LoggingListener;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.RunnerException;

import static nars.perf.JmhBenchmark.perf;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Created by me on 4/24/17.
 */
@State(Scope.Benchmark)
@Disabled
public class NARTestBenchmark {

//    @Setup public void prepare() {
//        System.out.println(DefaultDeriver.the); //warm
//    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public void testExample() {

//        Builder.Compound.the = new BiFunction<Op, Term[], Term>() {
//            @Override public Term apply(Op op, Term[] terms) {
//                return FastCompound.get(new GenericCompound(op, Op.subterms(terms)));
//            }
//        };

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(
                        //selectPackage("com.example.mytests"),
                        selectClass(NAL1Test.class)
                        //selectClass(FastCompoundNAL1Test.class)
                )
                // .filters( includeClassNamePatterns(".*Tests")  )
                .build();


        Launcher launcher = LauncherFactory.create();


        //SummaryGeneratingListener listener = new SummaryGeneratingListener();
        LoggingListener listener = LoggingListener.forJavaUtilLogging();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request, listener);

        //listener.getSummary().printTo(new PrintWriter(System.out));


    }

    public static void main(String[] args) throws RunnerException {
        perf(NARTestBenchmark.class, (x)->{
            x.measurementIterations(5);
            x.warmupIterations(2);
            x.forks(1);
            x.threads(1);
            x.addProfiler(StackProfiler2.class);
        });
    }
}

//public class TestBenchmark1 {
//
////    static String eval(String script) {
////        // We don't actually need the context object here, but we need it to have
////        // been initialized since the
////        // constructor for Ctx sets static state in the Clojure runtime.
////
////        Object result = Compiler.eval(RT.readString(script));
////
////        return RT.printString(result) + " (" +result.getClass() + ")";
////    }
////    @Benchmark
////    @BenchmarkMode(value = Mode.SingleShotTime)
////    public void eval1() {
////
////        new Dynajure().eval("(+ 1 1)");
////    }
////
////    @Benchmark
////    @BenchmarkMode(value = Mode.SingleShotTime)
////    public void eval2() {
////        new Dynajure().eval("(* (+ 1 1) 8)");
////        //out.println(eval("'(inh a b)") );
////        //out.println(eval("'[inh a b]") );
////    }
//
//    @Benchmark
//    @BenchmarkMode(Mode.SingleShotTime)
//    public void testExecution() throws Narsese.NarseseException {
//        NAR n = new NARS().get();
//        //n.log();
//        n.input("a:b!");
//        n.input("<(rand 5)==>a:b>.");
//
//        n.run(6);
//    }
//
////    public static void main(String[] args) throws RunnerException {
////        perf(TestBenchmark1.class, 6, 10);
////
////    }
//}
