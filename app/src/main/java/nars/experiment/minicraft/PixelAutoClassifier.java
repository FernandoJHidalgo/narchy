package nars.experiment.minicraft;

import nars.*;
import nars.budget.Budgeted;
import nars.task.MutableTask;
import nars.term.Term;
import nars.util.Util;
import nars.util.signal.Autoencoder;
import org.jetbrains.annotations.NotNull;
import spacegraph.Surface;
import spacegraph.obj.MatrixView;

import java.util.List;

import static nars.nal.UtilityFunctions.w2c;
import static spacegraph.obj.GridSurface.col;

/**
 * Created by me on 9/22/16.
 */
public class PixelAutoClassifier extends Autoencoder {

    public static final MetaBits NoMetaBits = (x, y) -> Util.EmptyFloatArray;
    private final NAR nar;
    private final MetaBits metabits;

    @NotNull String TAG = ("ae");

    private final float[][] pixIn;

    private final short[][][] pixOut;
    private final float[][] pixConf;

    public final float[][] pixRecon; //reconstructed input
    private final Term root;
    private final float in[];
    private final int sw, sh;
    private final int nw, nh;
    private final int pw, ph;
    private boolean reconstruct = true;
    public boolean learn = true;


    public Surface newChart() {
        return col(
            new MatrixView(W.length, W[0].length, MatrixView.arrayRenderer(W)),
            new MatrixView(pixRecon.length, pixRecon[0].length, MatrixView.arrayRenderer(pixRecon))
        );
    }


    public interface MetaBits {
        float[] get(int subX, int subY);
    }

    /*
    (subX, subY) -> {
            //context metadata: camera zoom, to give a sense of scale
            return new float[]{subX / ((float) (nx - 1)), subY / ((float) (nx - 1)), cam.Z};
        }
     */

    public PixelAutoClassifier(String root, float[][] pixIn, int sw, int sh, int states, NAgent agent) {
        this(root, pixIn, sw, sh, NoMetaBits, states, agent);
    }

    /**
     * metabits must consistently return an array of the same size, since now the size of this autoencoder is locked to its dimension
     */
    public PixelAutoClassifier(String root, float[][] pixIn, int sw, int sh, MetaBits metabits, int states, NAgent agent) {
        super(sw * sh + metabits.get(0, 0).length, states, agent.nar.random);
        this.metabits = metabits;
        this.nar = agent.nar;
        this.root = $.the(root);
        this.pixIn = pixIn;
        this.sw = sw; //stride width
        this.sh = sh; //stride height
        ph = pixIn[0].length;
        pw = pixIn.length;
        this.nw = (int) Math.ceil(pw / ((float) sw)); //number strides wide
        this.nh = (int) Math.ceil(ph / ((float) sh)); //number strides high
        this.in = new float[xx.length];
        this.pixRecon = new float[pw][ph];

        this.pixOut = new short[nw][nh][];
        this.pixConf = new float[nw][nh];


    }

    public void frame() {
        //int q = 0;

        float minConf = nar.confMin.floatValue();
        float baseConf = nar.confidenceDefault(Symbols.BELIEF);
        float basePri = nar.priorityDefault(Symbols.BELIEF);
        float baseDur = nar.durabilityDefault(Symbols.BELIEF);

        float alpha = 0.1f; //this represents the overall rate; the sub-block rate will be a fraction of this
        float corruption = 0.3f;
        int regionPixels = sw * sh;
        float sumErr = 0;

        int states = y.length;
        float outputThresh = 1f - (1f / (states - 1));
        int maxStatesPerRegion = 2; //states / 4; //limit before considered ambiguous

        //forget(alpha*alpha);

        List<Task> tasks = $.newArrayList();

        for (int i = 0; i < nw; ) {
            for (int j = 0; j < nh; ) {

                int p = 0;
                int oi = i * sw;
                int oj = j * sh;
                for (int si = 0; si < sw; si++) {
                    int d = si + oi;
                    if (d >= pw)
                        break;

                    float[] col = pixIn[d];
                    for (int sj = 0; sj < sh; sj++) {

                        int c = sj + oj;

                        in[p++] = c < ph ? col[c] : 0;

                    }
                }

                float[] metabits = this.metabits.get(i, j);
                for (float m : metabits) {
                    in[p++] = m;
                }

                short[] po = null;
                if (learn) {
                    float regionError = train(in, alpha, 0f, corruption, true, false, true);
                    sumErr += regionError;

                    // must have sufficient error, and
                    // the indecision degree between selected features must not reduce the evidence
                    // below that which would cross the provided min conf thresh
                    float evi;
                    if ((evi = 1f - (regionError / regionPixels)) > 0) {
                        short[] features = max(outputThresh);
                        if (features != null) {
                            if (features.length < maxStatesPerRegion) {
                                evi /= features.length;
                                if ((pixConf[i][j] = (baseConf * w2c(evi))) >= minConf) {
                                    po = features;
                                }
                            }
                        }
                    }
                    //System.out.println(n2(y) + ", +-" + n4(regionError / y.length));
                } else {
                    //System.out.println(n2(y));
                    recode(in, true, false, true);
                }
                pixOut[i][j] = po;


                if (reconstruct) {

                    float mult = (pixOut[i][j] == null) ? -1f  /* invert */ : +1f /* normal */;

                    float z[] = this.z;
                    p = 0;
                    for (int si = 0; si < sw; si++) {
                        int d = si + oi;
                        if (d >= pw)
                            break;

                        float[] col = pixRecon[d];
                        for (int sj = 0; sj < sh; sj++) {

                            int c = sj + oj;

                            if (c >= ph)
                                break;

                            col[c] = z[p++] * mult;
                        }
                    }
                }

                j++;
            }

            i++;
        }

        if (learn) {
            //float meanErrPerPixel = sumErr / (pw * ph);
            //System.out.println(Arrays.toString(pixOut) + ", +-" + n4(meanErrPerPixel));


            for (int i = 0; i < pixOut.length; i++) {
                short[][] po = pixOut[i];
                float[] pc = pixConf[i];
                for (int j = 0; j < po.length; j++) {
                    short[] v = po[j];
                    if (v == null)
                        continue;

                    float conf = pc[j];

                    Term[] vt = new Term[v.length];
                    int vtt = 0;
                    for (short x : v) {
                        vt[vtt++] = $.the(TAG.toString() + x);
                    }

                    Term Y = $.inh($.p(root, $.the(i), $.the(j)), $.seti(vt));
                    tasks.add(
                        new MutableTask(Y, Symbols.BELIEF, 1f, conf)
                            .budget(conf * basePri, baseDur)
                            .present(nar) //.log(TAG))
                    );

                }
            }


            if (!tasks.isEmpty()) {
                Budgeted.normalizePriSum(tasks, 1f);
                nar.inputLater(tasks);
            }

        }
    }


//        /**
//         * Autoencodes a vector of inputs and attempts to classify the current values to
//         * an item. these are input representing summary beliefs. the semantics of the
//         * autoencoding can also be input at some interval, since this can change, the
//         * assocaitions will need some continous remapping in proportion.
//         * these can be done through tensed similarity beliefs.
//         */
//        public class AutoClassifier extends Autoencoder  {
//
//            private static final Logger logger = LoggerFactory.getLogger(AutoClassifier.class);
//
//
//
//
//            //private final Compound aeBase;
//
//            //private int metaInterval = 100;
//
//            public AutoClassifier(int input, int output, Random rng) {
//                super(input, output, rng);
//            }
//
//            //    protected void input(int stride, Term which, float conf) {
//
//        GeneratedTask t = new GeneratedTask(
//                input(stride, which),
//
//
//                '.', $.t(1f, conf));
//        t.time(nar.time(), nar.time()).budget(nar.priorityDefault(Symbols.BELIEF), nar.durabilityDefault(Symbols.BELIEF));
//        nar.inputLater( t );
//
//
//    }
//
//    @NotNull
//    private static Term state(int which) {
//        //even though the state can be identified by an integer,
//        //it does not have the same meaning as integers used
//        //elsewhere. however once the autoencoder stabilizes
//        //these can be relied on as semantically secure in their context
//        //return $.p(aeBase, new Termject.IntTerm(which));
//        return $.the("X" + which);
//    }
//
//    @NotNull
//    private Compound input(int stride, Term state) {
//        Compound c = $.prop(stride(stride), state);
//        if (c == null)  {
//            $.prop(stride(stride), state);
//            throw new NullPointerException();
//        }
//        return c;
//        //return $.image(2, false, base, new Termject.IntTerm(stride), state);
//    }
//
//    private Term stride(int stride) {
//        return $.p(base, new IntTerm(stride));
//    }
//
//    /** input the 'metadata' of the autoencoder that connects the virtual concepts to their semantic inputs */
//    protected void meta() {
//        int k = 0;
//        int n = input.size();
//        //final Term unknown = $.varDep(2);
//        for (int i = 0; i < strides; i++) {
//            List<? extends SensorConcept> l = input.subList(k, Math.min(n, k + stride));
//            //TODO re-use the same eternal belief to reactivate itself
//            Compound x = $.inh(
//                    $.sete(
//                        l.stream().map(CompoundConcept::term).toArray(Term[]::new)
//                    ),
//                    //input(i, unknown).term(0) //the image internal
//                    stride(i)
//            );
//            nar.believe(x);
//            k+= stride;
//        }
//    }
//        }


}
