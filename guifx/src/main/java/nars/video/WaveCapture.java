package nars.video;

import com.gs.collections.impl.list.mutable.primitive.FloatArrayList;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import nars.guifx.NARfx;
import nars.guifx.chart.Plot2D;
import nars.util.event.DefaultTopic;
import nars.util.event.On;
import nars.util.event.Topic;
import nars.util.signal.OneDHaar;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by me on 10/28/15.
 */
public class WaveCapture implements Runnable {


    private final Plot2D.Series rawWave, wavelet1d;

    private int bufferSamples;

    ScheduledThreadPoolExecutor exec;
    private float[] samples;

    private WaveSource source;

    /**
     * called when next sample buffer is ready
     */
    final Topic<WaveCapture> nextReady = new DefaultTopic();

    synchronized void start(float FRAME_RATE) {
        if (exec != null) {
            exec.shutdownNow();
            exec = null;
        }

        if (FRAME_RATE > 0) {
            exec = new ScheduledThreadPoolExecutor(1);
            exec.scheduleAtFixedRate(this, 0, (long) (1000.0f / FRAME_RATE),
                    TimeUnit.MILLISECONDS);

        }
    }

    public void stop() {
        start(0);
    }

    public VBox newMonitorPane() {


        Plot2D audioPlot = new Plot2D(Plot2D.Line, bufferSamples, 450, 60);
        audioPlot.add(rawWave);
        Plot2D audioPlot2 = new Plot2D(Plot2D.Line, bufferSamples, 450, 60);
        audioPlot2.add(wavelet1d);


        Consumer<WaveCapture> u = (a) -> {
            audioPlot.update();
            audioPlot2.update();
        };

        VBox v = new VBox(
                audioPlot,
                audioPlot2);

        v.maxWidth(Double.MAX_VALUE);
        v.maxHeight(Double.MAX_VALUE);

        //noinspection OverlyComplexAnonymousInnerClass
        ChangeListener onParentChange = new ChangeListener() {

            public On observe;

            @Override
            public void changed(ObservableValue observableValue, Object o, Object t1) {

                if (t1 == null) {
                    if (observe != null) {
                        //System.out.println("stopping view");
                        this.observe.off();
                        this.observe = null;
                    }
                } else {
                    if (observe == null) {
                        //System.out.println("starting view");
                        observe = nextReady.on(u);
                    }
                }
            }
        };
        //runLater(() -> {
        onParentChange.changed(null, null, null);
        v.sceneProperty().addListener(onParentChange);
        //});
        return v;
    }

    public WaveCapture(WaveSource source, float updateFrameRate) {

        setSource(source);


        //                double nextDouble[] = new double[1];
        //                DoubleSupplier waveSupplier = () -> {
        //                    return nextDouble[0];
        //                };


        rawWave = new Plot2D.Series("Audio", 1) {

            @Override
            public void update() {
                history.clear();

                float[] samples = WaveCapture.this.samples;
                if (samples == null) return;
                //samples[0] = null;

                history.addAll(samples);
//                final FloatArrayList history = this.history;
//
//                for (int i = 0; i < nSamplesRead; i++) {
//                    history.add((float) samples[i]);
//                }
//
//                while (history.size() > maxHistory)
//                    history.removeAtIndex(0);

                minValue = -1.0f; //Short.MIN_VALUE;
                maxValue = 1.0f;  //Short.MAX_VALUE;
//                                        minValue = Float.POSITIVE_INFINITY;
//                                        maxValue = Float.NEGATIVE_INFINITY;
//
//                                        history.forEach(v -> {
//                                            if (Double.isFinite(v)) {
//                                                if (v < minValue) minValue = v;
//                                                if (v > maxValue) maxValue = v;
//                                            }
//                                            //mean += v;
//                                        });
            }

        };
        wavelet1d = new Plot2D.Series("Wavelet", 1) {

            @Override
            public void update() {
                float[] ss = samples;
                if (ss == null) return;
                //samples[0] = null;

                FloatArrayList history = this.history;

                //                        for (short s : ss) {
                //                            history.add((float)s);
                //                        }
                //
                //
                //                        while (history.size() > maxHistory)
                //                            history.removeAtIndex(0);
                //
                //                        while (history.size() < maxHistory)
                //                            history.add(0);


                //1d haar wavelet transform
                //OneDHaar.displayOrderedFreqsFromInPlaceHaar(x);
                OneDHaar.inPlaceFastHaarWaveletTransform(samples);
                //OneDHaar.displayOrderedFreqsFromInPlaceHaar(samples, System.out);

//                //apache commons math - discrete cosine transform
//                {
//                    double[] dsamples = new double[samples.length + 1];
//                    for (int i = 0; i < samples.length; i++)
//                        dsamples[i] = samples[i];
//                    dsamples = new FastCosineTransformer(DctNormalization.STANDARD_DCT_I).transform(dsamples, TransformType.FORWARD);
//                    for (int i = 0; i < samples.length; i++)
//                        samples[i] = (float) dsamples[i];
//                }

                history.clear();
                history.addAll(samples);

//                minValue = Short.MIN_VALUE;
//                maxValue = Short.MAX_VALUE;

                minValue = Float.POSITIVE_INFINITY;
                maxValue = Float.NEGATIVE_INFINITY;

                history.forEach(v -> {
                    //if (Float.isFinite(v)) {
                    if (v < minValue) minValue = v;
                    if (v > maxValue) maxValue = v;
                    //}
                    //mean += v;
                });

                //System.out.println(maxHistory + " " + start + " " + end + ": " + minValue + " " + maxValue);

            }

        };

        start(updateFrameRate);

    }

    public final synchronized void setSource(WaveSource source) {
        if (this.source != null) {
            this.source.stop();
            this.source = null;
        }

        this.source = source;

        if (this.source != null) {
            int audioBufferSize = this.source.start();

            bufferSamples = audioBufferSize;

            //System.out.println("bufferSamples=" + bufferSamples + ", sampleRate=" + sampleRate + ", numChannels=" + numChannels);

            if (samples == null || samples.length!=audioBufferSize)
                samples = new float[audioBufferSize];
        }
    }

    @Override
    public void run() {
        try {

            source.next(samples);

            nextReady.emit(this);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        WaveCapture au = new WaveCapture(
                new AudioSource(0, 30),
                //new SineSource(128),
                30);

        NARfx.run((a, b) -> {
            b.setScene(new Scene(au.newMonitorPane(), 500, 400));
            b.show();
        });
    }

}
