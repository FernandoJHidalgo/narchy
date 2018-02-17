package nars.op;

import jcog.learn.Autoencoder;
import nars.NAR;
import nars.concept.Concept;
import nars.control.DurService;
import nars.table.BeliefTable;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.List;
import java.util.Set;

import static nars.Op.*;

/**
 * decompiles a continuously trained autoencoding of an input concept vector
 */
public class AutoConceptualizer {
    public final Autoencoder ae;
    public final List<? extends Concept> in;
    private final DurService on;
    private final boolean beliefOrGoal;
    private final float[] x;
    float learningRate = 0.01f;
    float noiseLevel = 0.001f;

    public AutoConceptualizer(List<? extends Concept> in, boolean beliefOrGoal, int features, NAR n) {
        this.in = in;
        this.beliefOrGoal = beliefOrGoal;
        this.ae = new Autoencoder(in.size(), features, n.random());
        this.x = new float[in.size()];
        this.on = DurService.on(n, this::update);
    }

    protected void update(NAR n) {
        long now = n.time();
        float[] x = this.x;
        int inputs = in.size();
        for (int i = 0, inSize = inputs; i < inSize; i++) {
            Concept xx = in.get(i);
            Truth t = ((BeliefTable) xx.table(beliefOrGoal ? BELIEF : GOAL)).truth(now, n);
            float f;
            if (t == null) {
                f = 0.5f;
                //0.5f + (learningRate * 2) * n.random().nextFloat() - learningRate;
                //n.random()
            } else {
                f = t.freq();
            }
            x[i] = f;
        }
        //System.out.println(n4(x));
        float err = ae.put(x, learningRate, noiseLevel, 0, true);
        //System.out.println("err=" + n4(err/inputs) + ": \t" + n4(ae.y));

        //decompile/unfactor the outputs
        //if err < thresh
        int outputs = ae.outputs();
        float[] b = new float[outputs];

        float thresh = n.freqResolution.floatValue();

        int[] order = new int[inputs];
        for (int i = 0; i < outputs; i++) {
            b[i] = 1; //basis vector for each output

            float[] a = ae.decode(b, true);
            //System.out.println("\tfeature " + i + "=" + n4(a));
            Term feature = conj(order, a /* threshold, etc */, 5 /*a.length/2*/,
                    thresh);
            if (feature != null) {
                //System.out.println("\t  " + feature);
                onFeature(feature);
            }
            b[i] = 0; //clear
        }
    }

    protected void onFeature(Term feature) {

    }

    private Term conj(int[] order, float[] a, int maxArity, float threshold) {

        //sort by absolute polarity (divergence from 0.5), collecting the top N components
        int n = a.length;
//        float mean = 0;
        for (int i = 0; i < n; i++) {
            order[i] = i;
//            mean += a[i];
        }
//        mean/=n;

        float finalMean = 0.5f; //mean;
        jcog.data.array.Arrays.sort(order, (i) -> Math.abs(finalMean - a[i]));

        Set<Term> x = new UnifiedSet(maxArity);
        for (int i = 0; i < maxArity; i++) {
            int oi = order[i];
            float aa = a[oi];
            if (Math.abs(aa - 0.5f) < threshold)
                break; //done

            x.add(in.get(oi).term().negIf(aa < finalMean));
        }

        if (x.isEmpty())
            return null;
        return CONJ.the(0, x);
    }

}
