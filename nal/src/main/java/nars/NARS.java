package nars;

import jcog.Services;
import jcog.list.FasterList;
import jcog.math.random.XoRoShiRo128PlusRandom;
import nars.concept.builder.ConceptBuilder;
import nars.concept.builder.DefaultConceptBuilder;
import nars.control.Causable;
import nars.control.Deriver;
import nars.exe.Exec;
import nars.exe.UniExec;
import nars.index.term.TermIndex;
import nars.index.term.map.CaffeineIndex;
import nars.index.term.map.MapTermIndex;
import nars.op.stm.STMLinkage;
import nars.time.CycleTime;
import nars.time.RealTime;
import nars.time.Time;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * NAR builder
 */
public class NARS {

    public NAR get() {
        NAR n = new NAR(index.get(), exe.get(), time, rng.get(), concepts.get());
        init(n);
        derivers.forEach(d -> d.apply(n));
        n.time.synch(n);
        postInit.forEach(x -> x.accept(n));
        n.time.synch(n);
        return n;
    }

    /**
     * subclasses may override this to configure newly constructed NAR's
     */
    protected void init(NAR n) {

    }

    protected Supplier<TermIndex> index;

    protected Time time;

    protected Supplier<Exec> exe;

    protected Supplier<Random> rng;

    protected Supplier<ConceptBuilder> concepts;

    protected List<Function<NAR, Deriver>> derivers;

    /**
     * applied in sequence as final step before returning the NAR
     */
    protected final List<Consumer<NAR>> postInit = new FasterList(0);


    public NARS index(@NotNull TermIndex concepts) {
        this.index = () -> concepts;
        return this;
    }

    public NARS time(@NotNull Time time) {
        this.time = time;
        return this;
    }

    public NARS exe(Exec exe) {
        this.exe = () -> exe;
        return this;
    }

    public NARS concepts(ConceptBuilder cb) {
        this.concepts = () -> cb;
        return this;
    }


    /**
     * adds a deriver with the standard rules for the given range (inclusive) of NAL levels
     */
    public NARS deriverAdd(int minLevel, int maxLevel) {
        derivers.add(
                Deriver.deriver(minLevel, maxLevel)
        );
        return this;
    }

    /**
     * adds a deriver with the provided rulesets
     */
    public NARS deriverAdd(String... rules) {
        deriverAdd(
                Deriver.deriver(1, 9, rules)
        );
        return this;
    }


    public NARS deriverAdd(Function<NAR, Deriver> dBuilder) {
        this.derivers.add(dBuilder);
        return this;
    }


    /**
     * defaults
     */
    public NARS() {

        index = () ->
                //new CaffeineIndex(new DefaultConceptBuilder(), 8*1024, 16*1024, null)
                new MapTermIndex(new /*Linked*/HashMap(64, 0.9f));

        time = new CycleTime();

        exe = () -> new UniExec(64);

        rng = () ->
                new XoRoShiRo128PlusRandom(1);
        //new XorShift128PlusRandom(1);

        concepts = DefaultConceptBuilder::new;

        derivers = new FasterList();
    }

    /**
     * temporary, disposable NAR. safe for single-thread access only.
     * full NAL8 with STM Linkage
     */
    public static NAR tmp() {
        return tmp(8);
    }


    /**
     * temporary, disposable NAR. useful for unit tests or embedded components
     * safe for single-thread access only.
     *
     * @param nal adjustable NAL level. level >= 7 include STM (short-term-memory) Linkage plugin
     */
    public static NAR tmp(int nal) {
        return new DefaultNAR(nal, false).get();
    }

    /**
     * single-thread, limited to NAL6 so it should be more compact than .tmp()
     */
    public static NAR tmpEternal() {
        return new DefaultNAR(6, false).get();
    }

    /**
     * single thread but for multithread usage:
     * unbounded soft reference index
     */
    public static NAR threadSafe() {
        return new DefaultNAR(8, true).get();
    }

    public NARS nal(int nal) {
        postInit.add((x) -> x.nal(nal));
        return this;
    }

    public NARS threadable() {
        index = () -> new CaffeineIndex(64 * 1024 /*HACK */);
        return this;
    }


    public static NARS realtime(float durFPS) {
        return new DefaultNAR(8, true).time(new RealTime.CS().durFPS(durFPS));
    }

    /**
     * provides only low level functionality.
     * an empty deriver, but allows any kind of term
     */
    public static NAR shell() {
        return tmp(0).nal(8);
    }

    public NARS memory(String s) {
        return then(n -> {
            File f = new File(s);

            try {
                n.inputBinary(f);
            } catch (FileNotFoundException ignored) {
                //ignore
            } catch (IOException e) {
                NAR.logger.error("input: {} {}", s, e);
            }

            Runnable save = () -> {
                try {
                    n.outputBinary(f, false);
                } catch (IOException e) {
                    NAR.logger.error("output: {} {}", s, e);
                }
            };
            Runtime.getRuntime().addShutdownHook(new Thread(save));
        });
    }

    /**
     * adds a post-processing step before ready NAR is returned
     */
    public NARS then(Consumer<NAR> n) {
        postInit.add(n);
        return this;
    }

    public @NotNull NARLoop startFPS(float framesPerSecond) {
        return get().startFPS(framesPerSecond);
    }


    /**
     * generic defaults
     */
    @Deprecated
    public static class DefaultNAR extends NARS {

        final int nal;

        public DefaultNAR(int nal, boolean threadSafe) {

            this.nal = nal;

            if (nal > 0)
                deriverAdd(1, nal);

            if (threadSafe)
                index = () -> new CaffeineIndex(64 * 1024);

            postInit.add((n) -> {
                //HACK dummy Focus - useful only for single threaded testing.  to be moved to a special Testing executor
                List<Causable> can = n.services().map(x -> x instanceof Causable ? ((Causable)x) : null).filter(Objects::nonNull).collect(Collectors.toList());
                n.serviceAddOrRemove.on((xb)->{
                    Services.Service<NAR> s = xb.getOne();
                    if (s instanceof Causable) {
                        if (xb.getTwo())
                            can.add((Causable) s);
                        else
                            can.remove(s);
                    }
                });
                n.onCycle(() -> {
                    int WORK_PER_CYCLE = 2;
                    for (int i = 0, canSize = can.size(); i < canSize; i++) {
                        can.get(i).run(n, WORK_PER_CYCLE);
                    }
                });
            });
        }

        @Override
        protected void init(NAR nar) {

            nar.nal(nal);

            nar.termVolumeMax.set(38);
            //nar.confMin.setValue(0.05f);

            nar.DEFAULT_BELIEF_PRIORITY = 0.5f;
            nar.DEFAULT_GOAL_PRIORITY = 0.5f;
            nar.DEFAULT_QUEST_PRIORITY = nar.DEFAULT_QUESTION_PRIORITY = 0.5f;

            if (nal >= 7)
                new STMLinkage(nar, 1, false);

            nar.defaultWants();


        }
    }

}
