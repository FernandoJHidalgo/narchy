package nars.perf;

import org.openjdk.jmh.profile.HotspotMemoryProfiler;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.*;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * Created by me on 12/11/15.
 */
public enum JmhBenchmark {
	;

	public static void perf(Class c, Consumer<ChainedOptionsBuilder> config) throws RunnerException {
		perf(c.getSimpleName(), config);
	}

	public static void perf(String include, Consumer<ChainedOptionsBuilder> config) throws RunnerException {
		ChainedOptionsBuilder opt = new OptionsBuilder()
				.include(include)
				.shouldDoGC(true)
				.warmupIterations(1)
//				.measurementIterations(iterations)
//				.measurementBatchSize(batchSize)
				//.threads(1)
				//.forks(1)
				.resultFormat(ResultFormatType.TEXT)
				//.verbosity(VerboseMode.EXTRA) //VERBOSE OUTPUT

			    //.addProfiler(StackProfiler2.class)

//			    .addProfiler(PausesProfiler.class, "period=10" /*uS */)
//        		.addProfiler(SafepointsProfiler.class)

//				.addProfiler(StackProfiler.class,
//			 "lines=10;top=10;period=3;detailLine=true;excludePackages=true" +
//					";excludePackageNames=java., jdk., javax., sun., " +
//					 "sunw., com.sun., org.openjdk.jmh."
//				)
				 //.addProfiler(GCProfiler.class)

				//.addProfiler(HotspotRuntimeProfiler.class)
				//.addProfiler(HotspotMemoryProfiler.class)
				//.addProfiler(HotspotThreadProfiler.class)

				//.addProfiler(HotspotCompilationProfiler.class)
				// .addProfiler(HotspotClassloadingProfiler.class)

				// sudo sysctl kernel.perf_event_paranoid=0
//				.addProfiler(LinuxPerfProfiler.class)
//			    .addProfiler(LinuxPerfAsmProfiler.class)
//				.addProfiler(LinuxPerfNormProfiler.class)

				//.addProfiler(CompilerProfiler.class)


				.timeout(TimeValue.seconds(500))
		;

		config.accept(opt);

		Collection<RunResult> result = new Runner(opt.build()).run();
		result.forEach(r -> {
			r.getSecondaryResults().forEach((k,v)->{
				if (v instanceof StackProfiler2.StackResult) {

				}
//				if (v instanceof StackProfiler.StackResult) {
//					StackProfiler.StackResult s = (StackProfiler.StackResult)v;
//					s.getStack(Thread.State.RUNNABLE)
//				}
			});
			System.out.println(r.getPrimaryResult().getClass());
//			System.out.println(r.getBenchmarkResults().forEach );
//			System.out.println(r.getAggregatedResult());
		});


	}

}
