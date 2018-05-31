package nars;

import com.google.common.collect.Iterables;
import jcog.TODO;
import jcog.Util;
import jcog.event.ListTopic;
import jcog.event.On;
import jcog.event.Topic;
import jcog.exe.Loop;
import jcog.list.FasterList;
import jcog.math.*;
import nars.concept.Concept;
import nars.concept.action.ActionConcept;
import nars.concept.scalar.DigitizedScalar;
import nars.concept.scalar.FilteredScalar;
import nars.concept.scalar.Scalar;
import nars.control.Activate;
import nars.control.DurService;
import nars.control.NARService;
import nars.control.channel.CauseChannel;
import nars.task.ITask;
import nars.task.NALTask;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Variable;
import nars.term.atom.Atomic;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.util.TimeAware;
import nars.util.signal.Bitmap2DSensor;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static jcog.Texts.n2;
import static jcog.Util.compose;
import static nars.Op.*;
import static nars.time.Tense.ETERNAL;
import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * an integration of sensor concepts and motor functions
 */
abstract public class NAgent extends NARService implements NSense, NAct, Runnable {


    public static final Logger logger = LoggerFactory.getLogger(NAgent.class);
    

    public final Map<Scalar, CauseChannel<ITask>> sensors = new LinkedHashMap();

    @Deprecated public final Set<DigitizedScalar> senseNums = new LinkedHashSet<>();
    @Deprecated public final Set<Bitmap2DSensor<?>> sensorCam = new LinkedHashSet<>();

    public final Map<ActionConcept, CauseChannel<ITask>> actions = new LinkedHashMap();
    final Topic<NAR> frame = new ListTopic();

    /** list of concepts involved in this agent */
    private final List<Concept> concepts = new FasterList();








    /**
     * lookahead time in durations (multiples of duration)
     */



    /**
     * action exploration rate; analogous to epsilon in QLearning
     */
    public FloatRange curiosity;

    /** dampens the dynamically normalized happiness range toward sadness as a motivation strategy */
    public final FloatRange depress = new FloatRange(0f, 0f, 1f);


    public final AtomicBoolean enabled = new AtomicBoolean(false);

    public FilteredScalar happy;

    public boolean trace;

    public long now = ETERNAL; 


    /**
     * range: -1..+1
     */
    public volatile float reward;


    private CauseChannel<ITask> in = null;



    public final FloatRange motivation = new FloatRange(1f, 0f, 2f);
    protected List<Task> always = $.newArrayList();

    /** non-null if an independent loop process has started */
    private volatile Loop loop = null;
    public int sensorDur;
    private long last;

    protected NAgent(NAR nar) {
        this("", nar);
    }

    protected NAgent(String id, NAR nar) {
        this(id.isEmpty() ? null : Atomic.the(id), nar);
    }

    @Deprecated protected NAgent(Term id, NAR nar) {
        super(id);
        this.nar = nar;

        this.curiosity = new FloatRange(0.10f, 0f, 1f);

        if (nar!=null)
            nar.on(this);
    }

    protected NAgent() {
        this("", null);
    }

    protected NAgent(@Nullable Term id) {
        this(id, null);
    }

    @Deprecated public void alwaysWant(Iterable<Termed> x, float conf) {
        x.forEach(xx -> alwaysWant(xx, conf));
    }

    @Deprecated public Task alwaysWant(Termed x, float conf) {
        Task t = new NALTask(x.term(), GOAL, $.t(1f, conf), now,
                ETERNAL, ETERNAL,
                Stamp.UNSTAMPED
                
        );

        always.add(t);
        return t;
    }

    public Task alwaysQuestion(Termed x) {
        return alwaysQuestion(x, true);
    }
    public Task alwaysQuest(Termed x) {
        return alwaysQuestion(x, false);
    }
    public Task alwaysQuestion(Termed x, boolean questionOrQuest) {
        Task t = new NALTask(x.term(), questionOrQuest ? QUESTION : QUEST, null, now,
                ETERNAL, ETERNAL,
                
                nar().evidence()
        ) {
            @Override
            public boolean isInput() {
                return false;
            }
        };

        always.add(t);
        return t;
    }


    /** creates a new loop to run this */
    public Loop startFPS(float fps) {
        synchronized (this) {
            if (this.loop == null) {
                return this.loop = new Loop(fps) {
                    @Override
                    public boolean next() {
                        NAgent.this.run();
                        return true;
                    }
                };
            } else {
                throw new RuntimeException("already started: " + loop);
            }
        }
    }

    @Override
    public FloatRange curiosity() {
        return curiosity;
    }

    @NotNull
    @Override
    public final Map<Scalar, CauseChannel<ITask>> sensors() {
        return sensors;
    }

    @NotNull
    @Override
    public final Map<ActionConcept, CauseChannel<ITask>> actions() {
        return actions;
    }

    @Override
    public final NAR nar() {
        return nar;
    }


    /**
     * interpret motor states into env actions
     */
    protected abstract float act();


    public Random random() {
        TimeAware timeAware = this.nar;
        return timeAware !=null ? timeAware.random() : ThreadLocalRandom.current();
    }

    @NotNull
    public String summary() {

        

        return id + " rwrd=" + n2(reward) +
                " dex=" + /*n4*/(dexterity(now, now)) +
                
                /*" var=" + n4(varPct(nar)) + */ "\t" + nar.concepts.summary() + " " +
                nar.emotion.summary();
    }

    /**
     * registers sensor, action, and reward concepts with the NAR
     * TODO call this in the constructor
     */
    @Override
    protected void starting(NAR nar) {
        synchronized (this) {

            Term id = (this.id == null) ? nar.self() : this.id;








































            FloatSupplier happyValue = new FloatCached(
                    () -> reward - depress.floatValue(),
                    nar::time
            );





            this.happy =
                    
                    new FilteredScalar(
                            happyValue,
                            nar,

                            
                            pair(id, ///$.inh(id, "happy"),
                                
                                new FloatNormalizer().relax(Param.HAPPINESS_RE_SENSITIZATION_RATE)),

                            
                            pair($.func("happy", id, $.the("chronic")), compose(
                                new FloatNormalizer().relax(Param.HAPPINESS_RE_SENSITIZATION_RATE),
                                new FloatExpMovingAverage(0.02f)
                            )),

                            
                            pair($.func("happy", id, $.the("acute")), compose(
                                new FloatExpMovingAverage(0.1f, false),
                                new FloatPolarNormalizer().relax(Param.HAPPINESS_RE_SENSITIZATION_RATE_FAST)
                            ))
                    );

            happy.pri(()->motivation.floatValue()*nar.priDefault(BELIEF));

            alwaysWant(happy.filter[0].term, nar.confDefault(GOAL));
            alwaysWant(happy.filter[1].term, nar.confDefault(GOAL)/2);
            alwaysWant(happy.filter[2].term, nar.confDefault(GOAL)/2);

            actions.keySet().forEach(a -> {
                alwaysQuest(a);
                
                
                alwaysQuestion(Op.CONJ.the(happy.term, a.term));
                alwaysQuestion(Op.CONJ.the(happy.term, a.term.neg()));
            });

            this.in = nar.newChannel(this);
            this.now = nar.time() - nar.dur(); 



            
            concepts.addAll(actions.keySet());
            concepts.addAll(sensors.keySet());
            always.forEach(t -> concepts.add(t.concept(nar,true)));
            Iterables.addAll(concepts, happy);


            
            enabled.set(true);
        }
    }

    @Override
    protected void stopping(NAR nar) {
        
        
        throw new TODO();
    }


    protected void always(float activation) {
        int n = always.size();
        if (n == 0)
            return;

        for (int i = 0; i < n; i++) {
            Task x = always.get(i);
            x.pri(
                activation * nar.priDefault(x.punc())
            );
        }

        in.input(always);
    }


    /** runs a frame */
    @Override public void run() {
        if (!enabled.get())
            return;

        long last = this.now;
        this.last = last;
        long now = nar.time();
        if (now <= last)
            return;
        this.now = now;

        this.sensorDur = Math.max(nar.dur(), (int)(now - last)); 

        reward = act();

        frame.emit(nar);

        happy.update(last, now, sensorDur, nar);

        FloatFloatToObjectFunction<Truth> truther = (prev, next) -> $.t(Util.unitize(next), nar.confDefault(BELIEF));
        sensors.forEach((key, value) -> value.input(key.update(last, now, truther, sensorDur, nar)));

        
        always( motivation.floatValue() );

        
        Map.Entry<ActionConcept, CauseChannel<ITask>>[] aa = actions.entrySet().toArray(new Map.Entry[actions.size()]);
        ArrayUtils.shuffle(aa, random()); 
        for (Map.Entry<ActionConcept, CauseChannel<ITask>> ac : aa) {
            Stream<ITask> s = ac.getKey().update(last, now, sensorDur, NAgent.this.nar);
            if (s != null)
                ac.getValue().input(s);
        }

        Truth happynowT = nar.beliefTruth(happy, last, now);
        float happynow = happynowT != null ? (happynowT.freq() - 0.5f) * 2f : 0;
        nar.emotion.happy(motivation.floatValue() * dexterity(last, now) * happynow /* /nar.confDefault(GOAL) */);

        if (trace)
            logger.info(summary());
    }



    /** creates an activator specific to this agent context */
    public Consumer<Predicate<Activate>> fire() {

        return p -> {
            Activate a;

            final int numConcepts = concepts.size();
            int remainMissing = numConcepts;
            if (remainMissing == 0) return;

            
            Random rng = nar.random();
            do {
                Concept cc = nar.conceptualize(concepts.get(rng.nextInt(numConcepts)));
                if (cc!=null) {
                    a = new Activate(cc, 0);
                    a.delete(); 
                } else {
                    a = null;
                    if (remainMissing-- <= 0) 
                        break;
                    else
                        continue;
                }
            } while (a==null || p.test(a));
        };
    }


    /** default rate = 1 dur/ 1 frame */
    public void runSynch(int frames) {
        DurService d = DurService.on(nar, this);
        nar.run(frames * nar.dur() + 1);
        d.off();
    }


    /**
     * experimental nagging question feed about how to make an agent happy
     */
    public static class AgentPredictions extends DurService {


        /**
         * prediction templates
         */
        public final List<Supplier<Task>> predictors = $.newArrayList();
        private final CauseChannel<ITask> predict;

        public AgentPredictions(NAgent a) {
            super(a.nar);

            this.predict = nar.newChannel(a.id + " predict");


            


































            
            
            
            
            


            
            

            Variable what = $.varQuery(1);

            predictors.add(question($.impl(what, a.happy.term())));
            

            for (Concept c : a.actions.keySet()) {
                Term action = c.term();

                Term notAction = action.neg();

                ((FasterList) predictors).addingAll(

                        question($.impl(CONJ.the(what, a.happy.term()), action)),
                        question($.impl(CONJ.the(what, a.happy.term().neg()), action))
                        




                        






                        
                        

                        
                        

                        
                        

                        

                        

                        

                        


                        
                        

                        
                        

                        
                        
                        
                        
                        
                        

                        

                        
                        

                        
                        


                        
                        
                        
                        


                        
                        


                        
                        
                        
                        
                        
                        

                        
                        
                        
                        
                        
                        


                        
                        

                        
                        
                );

            }

            
            
            
            
            
            
            
            
            


            
            
            
            
            
            
            
            
            
            
            
            
            
            
            

            
            
            
            
            
            


            

        }

        public Supplier<Task> question(@NotNull Term term) {
            return prediction(term, QUESTION, null);
        }

        public Supplier<Task> quest(@NotNull Term term) {
            return prediction(term, QUEST, null);
        }

        Supplier<Task> prediction(@NotNull Term _term, byte punct, Truth truth) {
            Term term = _term.normalize();

            long now = nar.time();




            long start = ETERNAL, end = ETERNAL;

            Task t = new NALTask(term, punct, truth, now,
                    start, end,
                    nar.evidence());

            return () -> {

                Task u;
                if (t.isEternal()) {
                    u = t;
                } else {
                    long nownow = nar.time();
                    
                    u = new NALTask(t.term(), t.punc(), t.truth(), nownow, nownow, nownow, new long[]{nar.time.nextStamp()});
                }

                u.pri(nar.priDefault(u.punc()));

                return u;
            };
        }

        @Override
        protected void run(NAR n, long dt) {
            predict.input(predictions(nar.time(), 1));
        }

        protected Stream<ITask> predictions(long now, float prob) {
            return predictors.stream().filter((x) -> nar.random().nextFloat() <= prob).map(x -> x.get().budget(nar));
        }

    }


    public float dexterity() {
        return dexterity(nar.time());
    }

    public float dexterity(long when) {
        return dexterity(when, when);
    }

    /**
     * average confidence of actions
     * see: http:
     */
    public float dexterity(long start, long end) {
        int n = actions.size();
        if (n == 0)
            return 0;

        final float[] m = {0};
        actions.keySet().forEach(a -> {
            Truth g = nar.goalTruth(a, start, end);
            float c;
            if (g != null) {
                
                c = g.conf();
            } else {
                c = 0;
            }
            m[0] += c;
        });
        
        return m[0] > 0 ? m[0] / n /* avg */ : 0;
    }













































    @Override
    public final float alpha() {
        return nar.confDefault(BELIEF);
    }


    @Override
    public On onFrame(Consumer/*<NAR>*/ each) {
        if (each instanceof DigitizedScalar) {
            senseNums.add((DigitizedScalar)each);
        }
        return frame.on(each);
    }

    public DurService onFrame(Runnable each) {
        return DurService.on(nar, ()->{ if (enabled.get()) each.run(); });
    }




























}
