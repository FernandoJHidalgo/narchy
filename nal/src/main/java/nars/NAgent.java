package nars;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import nars.budget.Budget;
import nars.concept.ActionConcept;
import nars.concept.Concept;
import nars.concept.SensorConcept;
import nars.nal.UtilityFunctions;
import nars.nar.Default;
import nars.task.GeneratedTask;
import nars.task.MutableTask;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.Truth;
import nars.util.Util;
import nars.util.data.list.FasterList;
import nars.util.math.FirstOrderDifferenceFloat;
import nars.util.math.FloatNormalized;
import nars.util.math.FloatPolarNormalized;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.DoubleSummaryStatistics;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static nars.$.t;
import static nars.nal.UtilityFunctions.w2c;
import static nars.time.Tense.ETERNAL;
import static nars.util.Texts.n2;
import static nars.util.Texts.n4;

/**
 * explicit management of sensor concepts and motor functions
 */
abstract public class NAgent implements NSense, NAction {


    public static final Logger logger = LoggerFactory.getLogger(NAgent.class);

    /** general reward signal for this agent */
    public final SensorConcept happy;

    /** d(happy)/dt = change in happiness over time (ie. first-order difference of happiness signal) */
    public final SensorConcept joy;

    public final FloatNormalized rewardNormalized;

    public NAR nar;

    public final List<SensorConcept> sensors = $.newArrayList();
    public final List<ActionConcept> actions = $.newArrayList();

    public float alpha, gamma, epsilonProbability = 0.25f;
    @Deprecated
    public float gammaEpsilonFactor = 0.5f;

    final int curiosityMonitorDuration = 32; //frames
    final DescriptiveStatistics avgActionDesire = new DescriptiveStatistics(curiosityMonitorDuration);
    final DescriptiveStatistics rewardWindow = new DescriptiveStatistics(curiosityMonitorDuration);


    public float rewardValue;

    float predictorProbability = 0.25f;

    private final FasterList<MutableTask> predictors = $.newArrayList();
    private float predictorPriFactor = 1f;

    public boolean trace = false;


    /** >=0 : additional NAR frames that are computed between each Agent frame */
    final int decisionFrames;

    protected long now;
    private long stopTime;
    private NARLoop loop;


    //private float curiosityAttention;
    private float rewardSum = 0;
    private MutableFloat maxSensorPriority;
    private MutableFloat maxRewardPriority;
    private MutableFloat minSensorPriority;
    private MutableFloat minRewardPriority;

    public NAgent(NAR nar) {
        this(nar, 0);
    }

    public NAgent(NAR nar, int decisionFrames) {

        this.nar = nar;
        alpha = this.nar.confidenceDefault(Symbols.BELIEF);
        gamma = this.nar.confidenceDefault(Symbols.GOAL);
        this.decisionFrames = decisionFrames;

        float rewardConf = alpha;

        rewardNormalized = new FloatPolarNormalized(() -> rewardValue);

        happy = new SensorConcept("(happy)", nar,
                rewardNormalized,
                (x) -> t(x, rewardConf)
        );


        joy = new SensorConcept("(joy)", nar,
                new FloatPolarNormalized(
                        new FirstOrderDifferenceFloat(
                                () -> nar.time(), () -> rewardValue
                        )
                ),
                (x) -> t(x, rewardConf)
        );

    }

    @Override public final Collection<SensorConcept> sensors() {
        return sensors;
    }

    @Override public final Collection<ActionConcept> actions() {
        return actions;
    }

    @Override public final NAR nar() {
        return nar;
    }

    /** should only be invoked before agent has started TODO check for this */
    public void sense(SensorConcept... s) {
        sense(Lists.newArrayList(s));
    }

    /** should only be invoked before agent has started TODO check for this */
    public void sense(Iterable<SensorConcept> s) {
        Iterables.addAll(sensors, s);
    }

    /** should only be invoked before agent has started TODO check for this */
    public void action(ActionConcept... s) {
        action(Lists.newArrayList(s));
    }

    /** should only be invoked before agent has started TODO check for this */
    public void action(Iterable<ActionConcept> s) {
        Iterables.addAll(actions, s);
    }



    /**
     * interpret motor states into env actions
     */
    protected abstract float act();

    protected void frame() {

        now = nar.time();
        if (now % (1+decisionFrames) != 0)
            return;

        updateActionDesire();

        curiosity();

        //System.out.println(nar.conceptPriority(reward) + " " + nar.conceptPriority(dRewardSensor));

        predict();

        now = nar.time();

        float r = rewardValue = act();
        rewardSum += r;
        rewardWindow.addValue(rewardValue);

        if (trace)
            System.out.println(summary());


        if (stopTime > 0 && now >= stopTime) {
            if (loop != null) {
                loop.stop();
                this.loop = null;
            }
        }
    }


    public String summary() {

        @NotNull Emotion emotion = nar.emotion;

        //long now = nar.time();


        return
//                 + "rwrd=[" +
//                     n4( sad.beliefs().truth(now).motivation() )
//                             + "," +
//                     n4( happy.beliefs().truth(now).motivation() )
//                 + "] "
                "rwrd=" + n2(rewardValue) + "\t"
                        + "motv=" + n4(desireConf()) + " "
                        + "hapy=" + n4(emotion.happy() - emotion.sad()) + " "
                        + "busy=" + n4(emotion.busy.getSum()) + " "
                        + "lern=" + n4(emotion.learning()) + " "
                        + "strs=" + n4(emotion.stress.getSum()) + " "
                        + "alrt=" + n4(emotion.alert.getSum()) + " "
                        + " var=" + n4(varPct(nar)) + " "
                        + "\t" + nar.concepts.summary()

//                + "," + dRewardPos.belief(nar.time()) +
//                "," + dRewardNeg.belief(nar.time());
                ;

    }


    protected void init() {

        int dt = 1 + decisionFrames;
        //this.curiosityAttention = reinforcementAttention / actions.size();


        /** set the sensor budget policy */
        int numSensors = sensors.size();

//        /** represents the approx equivalent number of sensors which can be fully budgeted at any time */
//        float activeSensors =
//                //(float) Math.sqrt(numSensors); //HEURISTIC
//                numSensors / 2f;

        minSensorPriority = new MutableFloat(Param.BUDGET_EPSILON * 10 /* to be safe */);
        maxSensorPriority = new MutableFloat(Util.unitize(nar.priorityDefault(Symbols.BELIEF) / numSensors + minSensorPriority.floatValue())); //measured per-each sensor
        SensorConcept.attentionGroup(sensors, minSensorPriority, maxSensorPriority, nar);

        minRewardPriority = new MutableFloat(Param.BUDGET_EPSILON * 20 /* estimate */);
        maxRewardPriority = new MutableFloat(Util.unitize(nar.priorityDefault(Symbols.BELIEF) / numSensors + minRewardPriority.floatValue()));
        SensorConcept.attentionGroup(newArrayList(happy, joy), minRewardPriority, maxRewardPriority, nar);


        //@NotNull Term what = $.$("?w"); //#w
        @NotNull Term what = $.$("#s"); //#w

        @NotNull Compound happiness = happy.term();

        {
            float rewardGamma =
                    1.0f - Param.TRUTH_EPSILON
                    //1.0f
                    //gamma
                    ;


//            float happinssDurability =
//                    nar.durabilityDefault(Symbols.GOAL);

            predictors.add(
                    new MutableTask(happy, '!', 1f, rewardGamma)
                            .eternal() //.present(nar.time()+dt)
            );
//                    happy.desire($.t(1f, rewardGamma),
//                            nar.priorityDefault(Symbols.GOAL),
//                            happinssDurability));
        }

//        predictors.addAll(
//                //what will imply reward
//                new MutableTask($.equi(what, dt, happiness), '?', null).time(now, now),
//                //new MutableTask($.equi(sth, dt, happiness), '.', null).time(now,now),
//
//                //what will imply non-reward
//                //new MutableTask($.equi(what, dt, $.neg(happiness)), '?', null).time(now, now),
//                //new MutableTask($.equi(sth, dt, $.neg(happiness)), '.', null).time(now,now),
//
//                //what co-occurs with reward
//                new MutableTask($.parallel(what, happiness), '?', null).time(now, now)
//
//                //what co-occurs with non-reward
//                //new MutableTask($.parallel(what, $.neg(happiness)), '?', null).time(now, now)
//        );

//        predictors.add(
//                nar.ask($.seq(what, dt, happy.term()), '?', now)
//        );
//        predictors.add( //+2 cycles ahead
//                nar.ask($.seq(what, dt*2, happy.term()), '?', now)
//        );


        for (Concept a : actions) {
            Term action = a.term();

            int lookahead = 1;
            for (int i = 0; i < lookahead; i++) {
                long then = now + (i+1)*dt;
                predictors.addAll(
                    new MutableTask($.seq(action, dt, happiness), '?', null).time(now, then),
                    //new MutableTask($.impl(action, dt, happiness), '?', null).time(now, then),
                    new MutableTask(action, '@', null).time(now, then)
                );
            }
        }

        System.out.println(Joiner.on('\n').join(predictors));
    }

    public NARLoop run(final int cycles) {
        return run(cycles, 0);
    }


    public NAgent runSync(final int cycles) {
        //run(cycles, 0).join();
        start();
        nar.run(cycles);
        return this;
    }

    public NARLoop run(final int cycles, int frameDelayMS) {

        start();

        this.stopTime = nar.time() + cycles;

        this.loop = new NARLoop(nar, frameDelayMS);


        return loop;

//        nar.next(); //step one for any mission() results to process first
//
//        for (int t = 0; t < cycles; t++) {
//            next();
//
//            if (frameDelayMS > 0)
//                Util.pause(frameDelayMS);
//        }
    }

    private void start() {
        if (this.loop != null)
            throw new UnsupportedOperationException();

        nar.runLater(() -> {

            init();


            nar.onFrame(nn -> frame());
        });
    }


    private void curiosity() {
        Budget curiosityBudget = Budget.One.clone().multiplied(minSensorPriority.floatValue(), 0.5f, 0.9f);

        float motorEpsilonProbability = epsilonProbability / actions.size() * (1f - (desireConf() / gamma));
        for (ActionConcept c : actions) {
            if (nar.random.nextFloat() < motorEpsilonProbability) {
                nar.inputLater(
                    new GeneratedTask(c, Symbols.GOAL,
                            $.t(nar.random.nextFloat()
                                //Math.random() > 0.5f ? 1f : 0f
                                , Math.max(nar.truthResolution.floatValue(), nar.random.nextFloat() * gamma * gammaEpsilonFactor)))
                                .time(now, now).budget(curiosityBudget).log("Curiosity"));

                                //in order to auto-destruct corectly, the task needs to remove itself from the taskindex too
                /* {
                    @Override
                    public boolean onConcept(@NotNull Concept c) {
                        if (super.onConcept(c)) {
                            //self-destruct later
                            nar.runLater(()->{
                                delete();
                            });
                            return true;
                        }
                        return false;
                    }
                }*/

            }

            //boost(c);
        }
    }

    private void updateActionDesire() {
        float m = 0;
        int a = actions.size();
        for (ActionConcept c : actions) {
            Truth d = c.desire(now);
            if (d != null)
                m += d.confWeight();
        }
        m/=a;
        avgActionDesire.addValue(w2c(m));
    }

    protected void predict() {

        float pri =
                UtilityFunctions.aveAri(nar.priorityDefault('.'), nar.priorityDefault('!'))
                        / (predictors.size()/predictorProbability) * predictorPriFactor;

        Budget boostBudget = Budget.One.clone().multiplied(pri, 0.5f, 0.99f);

        for (int i = 0, predictorsSize = predictors.size(); i < predictorsSize; i++) {
            predictors.set(i, boost(predictors.get(i), boostBudget));
        }
    }

    public float desireConf() {
        return Math.min(1f, ((float) avgActionDesire.getMean()));
    }

//    @Nullable
//    protected Concept boost(Concept c) {
//
//        new Activation(boostBudget, c, nar, 1) {
//
//            @Override
//            public void commit(float scale) {
//                linkTermLinks(c, scale); //activate all termlinks of this concept
//                super.commit(scale);
//            }
//        };
//
//        return c;
//    }


    private MutableTask boost(@NotNull MutableTask t, Budget budget) {

        if (nar.random.nextFloat() > predictorProbability)
            return t;

        MutableTask s;
        if (t.occurrence() != ETERNAL) {
            s = new GeneratedTask(t.term(), t.punc(), t.truth())
                            .time(now, now + (t.occurrence() - t.creation()));
        } else {
            s = new GeneratedTask(t.term(), t.punc(), t.truth())
                            .time(now, ETERNAL)
                            ;
        }

        s.evidence(t)
          .budget(budget)
          .log("Agent Predictor");

        nar.inputLater(s);
        return s;
    }

    public float rewardSum() {
        return rewardSum;
    }

    public static float varPct(NAR nar) {
        if (nar instanceof Default) {
            DoubleSummaryStatistics is = new DoubleSummaryStatistics();
            nar.forEachActiveConcept(xx -> {

                if (xx != null) {
                    Term tt = xx.term();
                    float v = tt.volume();
                    int c = tt.complexity();
                    is.accept((v - c) / v);
                }

            });

            return (float) is.getAverage();
        }
        return Float.NaN;
    }


}
