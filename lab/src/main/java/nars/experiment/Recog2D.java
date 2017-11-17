package nars.experiment;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.learn.MLPMap;
import nars.*;
import nars.concept.BaseConcept;
import nars.concept.Concept;
import nars.concept.GoalActionConcept;
import nars.gui.BeliefTableChart;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.time.Tense;
import nars.truth.Truth;
import nars.util.signal.CameraSensor;
import nars.op.video.PixelBag;
import nars.op.video.Scale;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.jetbrains.annotations.NotNull;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.layout.Grid;
import spacegraph.render.Draw;
import spacegraph.widget.meter.Plot2D;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static jcog.Texts.n2;
import static nars.Op.BELIEF;
import static spacegraph.layout.Grid.col;
import static spacegraph.layout.Grid.row;

/**
 * Created by me on 10/8/16.
 */
public class Recog2D extends NAgentX {


    private final Graphics2D g;
    private final int h;
    private final int w;
    private final Outputs outs;

    private final Training train;

    boolean mlpLearn = true, mlpSupport = true;

    BufferedImage canvas;

    public final AtomicBoolean neural = new AtomicBoolean(false);


    int image;
    final int maxImages = 3;

    int imagePeriod = 24;

    //goal -> belief transfer function
    FloatToFloatFunction goalInfluence = (x) ->
            x;
            //x > 0.5f ? 1 : 0; //1f/(maxImages); //how much goal feedback will influence beliefs, <=1

//    float theta;
//    float dTheta = 0.25f;

    static {
        Param.DEBUG = false;
    }

    public Recog2D(NAR n) {
        super("x", n);


        w = 10;
        h = 12;
        canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_BGR);
        g = ((Graphics2D) canvas.getGraphics());

        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        //n.beliefConfidence(0.2f);

//        imgTrainer.out.keySet().forEach(x ->
//                        predictors.addAll(
//                                new TaskBuilder($.seq(x.term(), 1, happy.term()), '?', null).time(now, now),
//                                new TaskBuilder($.impl($.inh($.varQuery("wat"), $.the("cam")), 0, happy.term()), '?', null) {
//                                    @Override
//                                    public boolean onAnswered(Task answer) {
//                                        System.err.println(this + "\n\t" + answer);
//                                        return false;
//                                    }
//                                }.time(now, now)
//                        )
//                predictors.add(new TaskBuilder(x, Symbols.QUESTION, null).present(nar.time()))


        //retina
//        Sensor2D spR = senseCameraRetina($.p(id, $.the("full")).toString(),
//                () -> canvas, w, h, v -> $.t(v, nar.confDefault(BELIEF)));

        //still
        CameraSensor sp = senseCamera(
                id
                //$.p(id,
                //$.the("zoom")
                //)
                ,
                /*new Blink*/(new Scale(() -> canvas, w, h)/*, 0.8f*/));

        //nar.log();

        outs = new Outputs(ii -> $.the("s" + ii), maxImages, this, goalInfluence);
        train = new Training(
                //sensors,
                Lists.newArrayList(
                        sp.src instanceof PixelBag ? Iterables.concat(sensors.keySet(), ((PixelBag) sp.src).actions ) : sensors.keySet()
                ),
                outs, nar);

        //new Thread(() -> {
        SpaceGraph.window(conceptTraining(outs, nar), 800, 600);
        //}).start();

    }

    public Surface conceptTraining(Outputs tv, NAR nar) {

        LinkedHashMap<BaseConcept, Outputs.Neuron> out = tv.out;

        Plot2D p;

        int history = 256;

        Grid g = col(

                row(beliefTableCharts(nar, out.keySet(), 256)),

                row(p = new Plot2D(history, Plot2D.Line).add("Reward", () ->
                                rewardCurrent
                        //tv.errorSum()
                )),
                //row(s = new Plot2D(history, Plot2D.BarWave).add("Rward", () -> rewardValue)),

                row(out.entrySet().stream().map(ccnn -> new Surface() {
                    @Override
                    protected void paint(GL2 gl) {
                        Concept c = ccnn.getKey();
                        Outputs.Neuron nn = ccnn.getValue();

                        float freq, conf;

                        Truth t = nar.beliefTruth(c, nar.time());
                        if (t != null) {
                            conf = t.conf();
                            freq = t.freq();
                        } else {
                            conf = nar.confMin.floatValue();
                            float defaultFreq =
                                    0.5f; //interpret no-belief as maybe
                            //Float.NaN  //use NaN to force learning of negation as separate from no-belief
                            freq = defaultFreq;
                        }


                        Draw.colorBipolar(gl,
                                2f * (freq - 0.5f)
                                //2f * (freq - 0.5f) * conf  //unipolar (1 color)
                                //2f * (-0.5f + freq) //bipolar (2 colors)
                        );

                        float m = 0.5f * conf;

                        Draw.rect(gl, 0, 0, 1f, 1f);

                        if (tv.verify) {
                            float error = nn.error;
                            if (error != error) {

                                //training phase
                                //Draw.rect(gl, m / 2, m / 2, 1 - m, 1 - m);
                            } else {

                                //verification

                                //draw backgroudn/border
                                //gl.glColor3f(error, 1f - error, 0f);

                                float fontSize = 0.08f;
                                gl.glColor3f(1f, 1f, 1f);
                                Draw.text(gl, c.term().toString(), fontSize, m / 2, 1f - m / 2, 0);
                                Draw.text(gl, "err=" + n2(error), fontSize, m / 2, m / 2, 0);
                            }
                        }


                    }
                }).toArray(Surface[]::new)));

        final int[] frames = {0};
        onFrame(() -> {

            if (frames[0]++ % imagePeriod == 0) {
                nextImage();
            }

            redraw();

            if (neural.get()) {
                //if (nar.time() < trainFrames) {
                outs.expect(image);
            }
            //} else {
            //  outs.expect(-1);
            //  outs.verify();
            //}


            if (neural.get()) {
                train.update(mlpLearn, mlpSupport);
            }

            p.update();
            //s.update();
        });

        return g;
    }

    @Deprecated
    public List<Surface> beliefTableCharts(NAR nar, Collection<? extends Termed> terms, long window) {
        long[] btRange = new long[2];
        onFrame(() -> {
            long now = nar.time();
            btRange[0] = now - window;
            btRange[1] = now + window;
        });
        return terms.stream().map(c -> new BeliefTableChart(nar, c, btRange)).collect(toList());
    }


    @Override
    protected float act() {

        float error = 0;

        for (int i = 0; i < maxImages; i++) {


            long when = nar.time();
            Truth g = nar.beliefTruth(outs.outVector[i], when);

            if (g == null) {
                error += 0.5;
            } else {
                error += Math.abs(g.freq() - ((image == i) ? 1f : 0f)); //smooth
                //error += ((image == i) ? g.freq() > 0.5f : g.freq() < 0.5f) ? 1f : 0f; //discrete
            }
        }

        float sharp = 1;
        return (float) (1f - 2 * Math.pow((error / maxImages), sharp));

//            r = 0.5f - (float) outs.errorSum()
//                    / outs.states;
        //return r;


    }


    protected int nextImage() {

        image = nar.random().nextInt(maxImages);


        return image;
    }

    private void redraw() {
        g.clearRect(0, 0, w, h);
        FontMetrics fontMetrics = g.getFontMetrics();

        String s = String.valueOf((char) ('0' + image));
        //LineMetrics lineMetrics = fontMetrics.getLineMetrics(s, g);
        Rectangle2D sb = fontMetrics.getStringBounds(s, g);

        //System.out.println(s + " : " + sb);

        //g.rotate(nar.random.nextFloat() * dTheta, w/2, h/2);

        g.drawString(s, Math.round(w / 2f - sb.getCenterX()), Math.round(h / 2f - sb.getCenterY()));
    }

    public static void main(String[] arg) {

        NAgentX.runRT((n) -> {

            Recog2D a = new Recog2D(n);
            a.nar.truthResolution.set(0.07f);
            a.nar.termVolumeMax.set(16);
            return a;

        }, 15);
    }

    public static class Training {
        private final List<Concept> ins;
        private final Outputs outs;
        private final MLPMap trainer;
        private final NAR nar;

        private final float learningRate = 0.3f;

        /**
         * Introduction of the momentum rate allows the attenuation of oscillations in the gradient descent. The geometric idea behind this idea can probably best be understood in terms of an eigenspace analysis in the linear case. If the ratio between lowest and largest eigenvalue is large then performing a gradient descent is slow even if the learning rate large due to the conditioning of the matrix. The momentum introduces some balancing in the update between the eigenvectors associated to lower and larger eigenvalues.
         * <p>
         * For more detail I refer to
         * <p>
         * http://page.mi.fu-berlin.de/rojas/neural/chapter/K8.pdf
         */
        private final float momentum = 0.6f;

        public Training(java.util.List<Concept> ins, Outputs outs, NAR nar) {

            this.nar = nar;
            this.ins = ins;
            this.outs = outs;


            this.trainer = new MLPMap(ins.size(), new int[]{(ins.size() + outs.states) / 2, outs.states}, nar.random());
            trainer.layers[1].setIsSigmoid(false);

        }


        float[] in(float[] i, long when) {
            int s = ins.size();

            if (i == null || i.length != s)
                i = new float[s];
            for (int j = 0, insSize = ins.size(); j < insSize; j++) {
                float b = nar.beliefTruth(ins.get(j), when).freq();
                if (b != b) //dont input NaN
                    b = 0.5f;
                i[j] = b;
            }

            return i;
        }

        protected void update(boolean train, boolean apply) {
            float[] i = in(null, nar.time());

            float errSum;
            if (train) {
                float[] err = trainer.put(i, outs.expected(null), learningRate, momentum);
                //System.err.println("error=" + Texts.n2(err));
                errSum = Util.sumAbs(err) / err.length;
                System.err.println("  error sum=" + errSum);
            } else {
                errSum = 0f;
            }

            if (apply/* && errSum < 0.25f*/) {
                float[] o = trainer.get(i);
                for (int j = 0, oLength = o.length; j < oLength; j++) {
                    float y = o[j];
                    //nar.goal(
                    float c = nar.confDefault(BELIEF) * (1f - errSum);
                    if (c > 0) {
                        nar.believe(
                                outs.outVector[j].term(),
                                Tense.Present, y, c);
                    }

                }
                //System.out.println(Arrays.toString(o));
            }
        }
    }


    /**
     * Created by me on 10/15/16.
     */
    public static class Outputs {

        public double errorSum() {
            return out.values().stream().mapToDouble(x -> x.error).map(x -> x == x ? x : 1f).sum();
        }

        static class Neuron {

            public float actual, actualConf;

            public float expected;

            public float error;

            public Neuron() {
                clear();
            }

            public void clear() {
                expected = Float.NaN;
                error = 0;
            }

            public void expect(float expected) {
                this.expected = expected;
                update();
            }

            public void actual(float f, float c) {
                this.actual = f;
                this.actualConf = c;
                update();
            }

            protected void update() {
                float a = this.actual;
                float e = this.expected;
                if (e != e) {
                    this.error = Float.NaN;
                } else if (a != a) {
                    this.error = 1f;
                } else {
                    this.error = (Math.abs(a - e))
                    //* ( this.actualConf )
                    ;
                }
            }
        }

        public float[] expected(float[] output) {
            output = sized(output);
            for (int i = 0; i < outVector.length; i++)
                output[i] = expected(i);
            return output;
        }

        public float[] actual(float[] output) {
            output = sized(output);
            for (int i = 0; i < outVector.length; i++)
                output[i] = actual(i);
            return output;
        }

        float[] sized(float[] output) {
            if (output == null || output.length != states) {
                output = new float[states];
            }
            return output;
        }


        final LinkedHashMap<BaseConcept, Neuron> out;
        BaseConcept[] outVector;

        final int states;

        private final NAR nar;

        boolean verify;


        public Outputs(IntFunction<Term> namer, int maxStates, NAgent a, FloatToFloatFunction transferFunction) {
            this.nar = a.nar;
            this.states = maxStates;
            this.out = new LinkedHashMap<>(maxStates);
            this.outVector = IntStream.range(0, maxStates).mapToObj((int i) -> {
                        Term tt = namer.apply(i);
                        @NotNull GoalActionConcept aa = a.action(tt, (b, d) -> {
    //                        if (train) {
    //                            float ee = expected(i);
    //
    //                            float thresh = 0.1f;
    //                            if (d==null || Math.abs(ee-d.freq())>thresh) {
    //                                //correction
    //                                a.nar.goal(tt, Tense.Present, ee, a.gamma);
    //                                //return null;
    //                            }
    //
                            //return $.t(ee, a.alpha() );
    //                            //return null;
    //                        }

    //                        if (b!=null && d!=null) {
    //                            return d.confMult(0.5f + 0.5f * Math.abs(d.freq()-b.freq()));
    //                        } else {
                            //return d!=null ? d.confWeightMult(0.5f) : null;
                            //}

                            return $.t(transferFunction.valueOf(d!=null ? d.freq() : 0), nar.confDefault(BELIEF));

                            //return d!=null ? new PreciseTruth(d.freq(), d.conf()goalInfluence.d.eviMult(goalInfluence, a.nar.dur()) : null;
                        });
                        //aa.resolution.setValue(1f);
                        return aa;
                    }
    //                        a.sense(namer.apply(i), () -> {
    //                            if (train) {
    //                                return out.get(outVector[i]).expected;// ? 1f : 0.5f - (1f / states);
    //                            } else {
    //                                return Float.NaN; //no prediction
    //                            }
    //                        }, 0.01f, (v) -> $.t(v, a.alpha/2f))
    //                            .pri(0.9f)
    //                            //.timing(0, 1) //synchronous feed

            ).peek(c -> out.put(c, new Neuron()))
                    .toArray(BaseConcept[]::new);


            a.onFrame(() -> {
                long now = nar.time();
                int dur = nar.dur();
                out.forEach((cc, nnn) -> {

                    Truth t =
                            //cc.belief(now, dur);
                            nar.goalTruth(cc, now);

                    float f, c;
                    if (t == null) {
                        f = Float.NaN;
                        c = Float.NaN;
                    } else {
                        f = t.freq();
                        c = t.conf();
                    }
                    nnn.actual(f, c);
                });

            });
        }

        public float expected(int i) {
            return out.get(outVector[i]).expected;
        }


        public float actual(int state) {

            //return actual(outVector[state], when);
            return out.get(outVector[state]).actual;
        }
    //    public float actual(Termed<Compound> state, long when) {
    //        return nar.concept(state).beliefFreq(when);
    //    }

        void expect(IntToFloatFunction stateValue) {
            //long now = nar.time();
            for (int i = 0; i < states; i++)
                out.get(outVector[i]).expect(stateValue.valueOf(i));
        }

        public void expect(int onlyStateToBeOn) {
            float offValue =
                    0f;
            //0.5f - (1f/states)*0.5f;
            //1f/states * 0.5f;
            //0.5f;

            expect(ii -> ii == onlyStateToBeOn ? 1f : offValue);
        }

    //    public void train() {
    //        train = true;
    //        verify = false;
    //    }

    //    public void verify() {
    //        verify = true;
    //        train = false;
    //    }

        public float error(Compound c) {
            return out.get(c).error;
        }
    }
}
