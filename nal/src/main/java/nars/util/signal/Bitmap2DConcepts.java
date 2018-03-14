package nars.util.signal;

import jcog.Util;
import jcog.math.FloatRange;
import jcog.math.FloatSupplier;
import jcog.signal.Bitmap2D;
import jcog.util.Array2DIterable;
import jcog.util.Int2Function;
import nars.NAR;
import nars.Task;
import nars.concept.scalar.Scalar;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static nars.Op.BELIEF;

/**
 * rectangular region of pixels
 */
public class Bitmap2DConcepts<P extends Bitmap2D> implements Iterable<Scalar> {

    public final Scalar[][] matrix;
    public final int width, height, area;
    public final P src;

    public final Array2DIterable<Scalar> iter;

    /** each pixel's belief task priority for next input */
    public final FloatRange pixelPri = new FloatRange(0, 0, 1f);

    protected Bitmap2DConcepts(P src, @Nullable Int2Function<Term> pixelTerm, NAR n) {

        this.width = src.width();
        this.height = src.height();
        this.area = width * height;
        assert(area > 0);

        this.src = src;
        this.pixelPri.set( n.priDefault(BELIEF) );

        this.matrix = new Scalar[width][height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                int xx = x, yy = y;

                FloatSupplier f = () -> Util.unitize(src.brightness(xx, yy));

                Scalar sss = new Scalar(pixelTerm.get(x, y), f, n)
                        .pri(pixelPri);

                matrix[x][y] = sss;
            }
        }

        this.iter = new Array2DIterable<>(matrix);
    }

    /** iterate columns (x) first, then rows (y) */
    @Override final public Iterator<Scalar> iterator() {
        return iter.iterator();
    }

    public void update() {
        src.update();
    }

    public Scalar get(int i) {
        return iter.order.get(i);
    }

    /** crude ASCII text representation of the current pixel state */
    public void print(PrintStream out) {
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                float b = matrix[i][j].asFloat();
                out.print(b >= 0.5f ? '*' : ' ');
            }
            out.println();
        }
    }

    public Bitmap2DConcepts resolution(float resolution) {
        forEach(p -> p.resolution.set(resolution));
        return this;
    }

    /** streams (potentially) all pixels */
    public final Stream<Task> stream(FloatFloatToObjectFunction<Truth> truther, NAR nar) {
        return stream(truther, 0, area, nar);
    }

    /** stream of tasks containing changes in all updated pixels */
    public Stream<Task> stream(FloatFloatToObjectFunction<Truth> truther, int start, int end, NAR nar) {

        long now = nar.time();
        int dur = nar.dur();

        return IntStream.range(start, end)
                .mapToObj(i -> get(i).update(truther, now, dur, nar))
                //.filter(Objects::nonNull)
        ;
    }

    public Scalar getSafe(int i, int j) {
        return matrix[i][j];
    }
    @Nullable public Scalar get(int i, int j) {
        if ((i < 0) || (j < 0) || (i >= width || j >= height))
                return null;
        return getSafe(i, j);
    }

}
