package jcog.learn;

import jcog.Util;
import jcog.learn.lstm.Interaction;
import jcog.learn.lstm.test.LiveSTM;
import jcog.math.random.XoRoShiRo128PlusRandom;
import org.eclipse.collections.api.block.function.primitive.LongToFloatFunction;

import java.util.Random;

import static jcog.Util.toDouble;

/**
 * NOT TESTED YET
 * http:
 * https:
 */
public class LivePredictor {



    public interface Framer {
        /** computes training vector from current observations */
        double[] inputs(long now);

        double[] outputs();


        double[] shift();
        
    }

    /*public static class Autoregression implements Framer {

    }*/

    public static class DenseShiftFramer implements Framer {
        /** time -> value function, (ex: one per concept) */
        private final LongToFloatFunction[] ins;
        private final LongToFloatFunction[] outs;

        private int past; 
        private int dur; 

        /**
         * temporary buffers, re-used
         */
        private double[] pastVector, present;

        public DenseShiftFramer(LongToFloatFunction[] ins, int past, int sampleDur, LongToFloatFunction[] outs) {
            this.ins = ins;
            this.outs = outs;
            this.past = past;
            this.dur = sampleDur;
        }

        @Override
        public double[] inputs(long now) {
            if (pastVector == null || pastVector.length!=(past * ins.length)) {
                pastVector = new double[past * ins.length];
                present = new double[1 * outs.length];
            }

            int i = 0;
            for (int t = past-1; t >=0; t--) { 
                for (LongToFloatFunction c : ins) {
                    pastVector[i++] = c.valueOf(now - (t+1) * dur);
                }
            }
            int k = 0;
            for (LongToFloatFunction c : outs) {
                present[k++] = c.valueOf(now);
            }

            return pastVector;
        }

        @Override
        public double[] outputs() {
           return present;
        }

        @Override
        public double[] shift() {
            
            int stride = ins.length;
            int all = pastVector.length;
            System.arraycopy(pastVector, stride, pastVector, 0, all - stride);
            System.arraycopy(present, 0, pastVector, all - stride,  stride);

            return pastVector;
        }
    }

    public interface Predictor {

        void learn(double[] ins, double[] outs);

        double[] predict();
    }

    public static class LSTMPredictor implements Predictor {
        private final int memoryScale;
        float learningRate;
        private LiveSTM net;
        private double[] nextPredictions;

        public LSTMPredictor(float learningRate, int memoryScale) {
            this.learningRate = learningRate;
            this.memoryScale = memoryScale;
        }

        @Override
        public String toString() {
            return super.toString() + "[" + net + "]";
        }

        @Override
        public void learn(double[] ins, double[] outs) {
            synchronized (this) {
                if (net == null || net.inputs != ins.length || net.outputs != outs.length) {
                    net = new LiveSTM(ins.length, outs.length,
                            Math.max(ins.length,outs.length) * memoryScale) {
                        @Deprecated
                        @Override
                        protected Interaction observe() {
                            throw new UnsupportedOperationException();
                        }
                    };
                }
                nextPredictions = net.agent.learn(ins, outs, learningRate);
            }
        }

        @Override
        public double[] predict() {
            return nextPredictions;
        }

    }

    public static class MLPPredictor implements Predictor {

        private final Random rng;
        float learningRate;
        float momentum = 0f;
        MLPMap mlp;
        private float[] next;


        public MLPPredictor(float learningRate) {
            this(learningRate, new XoRoShiRo128PlusRandom(1));
        }

        public MLPPredictor(float learningRate, Random rng) {
            this.learningRate = learningRate;
            this.rng = rng;
        }

        @Override
        public void learn(double[] ins, double[] outs) {
            if (mlp == null /*|| mlp.inputs()!=ins.length ...*/) {
                 mlp = new MLPMap(ins.length,
                         new int[] {
                                 2 * (ins.length + outs.length),
                                 ins.length + outs.length,
                                 outs.length},
                         rng, true);
                 Util.last(mlp.layers).setIsSigmoid(false);
            }
            float[] fIns = Util.toFloat(ins);
            mlp.put(fIns, Util.toFloat(outs), learningRate, momentum);
            next = mlp.get(fIns);
        }

        @Override
        public double[] predict() {
            return toDouble(next);
        }
    }

    /* TODO public static class NTMPredictor implements Predictor {

    } */


    public final Predictor model;
    public final Framer framer;

    public LivePredictor(Predictor model, Framer framer) {
        this.model = model;
        this.framer = framer;
    }



























    public synchronized double[] next(long when) {
        model.learn(framer.inputs(when), framer.outputs());
        framer.shift();

        return model.predict();
    }

    /** applies the vector as new hypothetical present inputs,
     * after shifting the existing data (destructively)
     * down one time slot.
     * then prediction can proceed again
     */
    public synchronized double[] project(double[] prevOut) {
        model.learn(((DenseShiftFramer)framer).pastVector, prevOut);
        double[] nextIn = framer.shift();

        return model.predict(); 
    }


}









