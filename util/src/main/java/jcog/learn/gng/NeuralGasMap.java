package jcog.learn.gng;

import jcog.Util;
import jcog.learn.Autoencoder;
import jcog.learn.gng.impl.Centroid;
import jcog.random.XorShift128PlusRandom;
import org.jetbrains.annotations.NotNull;

import static java.lang.System.arraycopy;

/**
 * dimension reduction applied to a neural gasnet
 */
public class NeuralGasMap extends NeuralGasNet<NeuralGasMap.AECentroid> {

    private final Autoencoder enc;
    private final int outs;

    /** call this before retrieving values */
    public void update() {

        enc.forget(0.01f);
        forEachNode(n -> {

            if (n.center==null)
                n.center = new float[outs];

            float[] x1 = Util.doubleToFloatArray(n.getDataRef());
            if (x1[0] == x1[0]) { //avoid NaN contaminating the matrices
                enc.put(x1, 0.01f, 0.001f, 0.0f, false, false, false);
                arraycopy(enc.output(), 0, n.center, 0, outs);
//                if (n.isInfinite()) {
//                    clear();
//                    Arrays.fill(n.center, 0);
//                }
                //System.out.println(n4(n.center));
            }
        });
    }

    @Override
    public void clear() {
        super.clear();
        if (enc!=null)
            enc.randomize();
    }

    public static class AECentroid extends Centroid {

        public float[] center;

        public AECentroid(int id, int dimensions) {
            super(id, dimensions);
            randomizeUniform(-1, 1);
        }

        @Override
        public double learn(double[] x) {
            double e = super.learn(x);
            return e;
        }

        public float[] center() {
            return center;
        }

    }

    //final StreamingNormalizer s;

    public NeuralGasMap(int in, int maxNodes, int out) {
        super(in, maxNodes);
        this.outs = out;
        this.enc = new Autoencoder(in, out, new XorShift128PlusRandom(1));
        //this.s = new StreamingNormalizer(in);
        //s.normalize(x, x);
    }


    @Override
    public AECentroid put(double[] x) {
        return super.put(x);
    }

    @NotNull
    @Override
    public NeuralGasMap.AECentroid newNode(int i, int dims) {
        return new AECentroid(i, dims);
    }
}
