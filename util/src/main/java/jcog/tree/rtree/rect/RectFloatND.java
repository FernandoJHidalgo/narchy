package jcog.tree.rtree.rect;

/*
 * #%L
 * Conversant RTree
 * ~~
 * Conversantmedia.com © 2016, Conversant, Inc. Conversant® is a trademark of Conversant, Inc.
 * ~~
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import jcog.tree.rtree.HyperRect;
import jcog.tree.rtree.point.FloatND;

import java.io.Serializable;
import java.util.function.Function;

import static java.lang.Float.NEGATIVE_INFINITY;
import static java.lang.Float.POSITIVE_INFINITY;

/**
 * Created by jcovert on 6/15/15.
 */

public class RectFloatND implements HyperRect<FloatND>, Serializable {

    public static final HyperRect ALL_1 = RectFloatND.all(1);
    public static final HyperRect ALL_2 = RectFloatND.all(2);
    public static final HyperRect ALL_3 = RectFloatND.all(3);
    public static final HyperRect ALL_4 = RectFloatND.all(4);


    public final FloatND min;


    public final FloatND max;


    public static final FloatND unbounded = new FloatND() {
        @Override
        public String toString() {
            return "*";
        }
    };

    public static HyperRect all(int i) {
        return new RectFloatND(FloatND.fill(i, NEGATIVE_INFINITY), FloatND.fill(i, POSITIVE_INFINITY));
    }

    public RectFloatND() {
        min = unbounded;
        max = unbounded;
    }

    public RectFloatND(final FloatND p) {
        min = p;
        max = p;
    }


    public RectFloatND(float[] a, float[] b) {
        this(new FloatND(a), new FloatND(b));
    }

    public RectFloatND(final FloatND a, final FloatND b) {
        int dim = a.dim();

        float[] min = new float[dim];
        float[] max = new float[dim];

        float[] ad = a.coord;
        float[] bd = b.coord;
        for (int i = 0; i < dim; i++) {
            float ai = ad[i];
            float bi = bd[i];
            min[i] = Math.min(ai, bi);
            max[i] = Math.max(ai, bi);
        }
        this.min = new FloatND(min);
        this.max = new FloatND(max);
    }


    @Override
    public boolean contains(final HyperRect _inner) {
        final RectFloatND inner = (RectFloatND) _inner;

        int dim = dim();
        for (int i = 0; i < dim; i++) {
            if (!(min.coord[i] <= inner.min.coord[i] && max.coord[i] >= inner.max.coord[i]))
            //if (min.coord[i] > inner.min.coord[i] || max.coord[i] < inner.max.coord[i])
                return false;
        }
        return true;
    }

    @Override
    public boolean intersects(final HyperRect r) {
        final RectFloatND x = (RectFloatND) r;

        int dim = dim();
        for (int i = 0; i < dim; i++) {
            /*return !((min.x > r2.max.x) || (r2.min.x > max.x) ||
                    (min.y > r2.max.y) || (r2.min.y > max.y));*/

            if (min.coord[i] > x.max.coord[i] || x.min.coord[i] > max.coord[i])
                return false;
        }
        return true;
    }

    @Override
    public double cost() {
        float sigma = 1f;
        int dim = dim();
        for (int i = 0; i < dim; i++) {
            sigma *= getRangeFinite(i, 1 /* an infinite dimension can not be compared, so just ignore it */);
        }
        return sigma;
    }

    @Override
    public HyperRect mbr(final HyperRect r) {
        final RectFloatND x = (RectFloatND) r;

        int dim = dim();
        float[] newMin = new float[dim];
        float[] newMax = new float[dim];
        for (int i = 0; i < dim; i++) {
            newMin[i] = Math.min(min.coord[i], x.min.coord[i]);
            newMax[i] = Math.max(max.coord[i], x.max.coord[i]);
        }
        return new RectFloatND(newMin, newMax);
    }


    @Override public double center(int dim) {
        return centerF(dim);
    }

    public float centerF(int dim) {
        float min = this.min.coord[dim];
        float max = this.max.coord[dim];
        if ((min == NEGATIVE_INFINITY) && (max == Float.POSITIVE_INFINITY))
            return 0;
        if (min == NEGATIVE_INFINITY)
            return max;
        if (max == Float.POSITIVE_INFINITY)
            return min;

        return (max + min)/2f;
    }

    @Override
    public FloatND center() {
        int dim = dim();
        float[] c = new float[dim];
        for (int i = 0; i < dim; i++) {
            c[i] = centerF(i);
        }
        return new FloatND(c);
    }


    @Override
    public int dim() {
        return min.dim();
    }

    @Override
    public FloatND min() {
        return min;
    }

    @Override
    public FloatND max() {
        return max;
    }

    @Override public double getRange(final int i) {
        float min = this.min.coord[i];
        float max = this.max.coord[i];
        if (min == max)
            return 0;
        if ((min == NEGATIVE_INFINITY) || (max == Float.POSITIVE_INFINITY))
            return Float.POSITIVE_INFINITY;
        return (max - min);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null /*|| getClass() != o.getClass()*/) return false;

        RectFloatND r = (RectFloatND) o;
        return min.equals(r.min) && max.equals(r.max);
    }

    @Override
    public int hashCode() {
        int result = min.hashCode();
        result = 31 * result + max.hashCode();
        return result;
    }

    public String toString() {
        if (min.equals(max)) {
            return min.toString();
        } else {
            final StringBuilder sb = new StringBuilder();
            sb.append('(');
            sb.append(min);
            sb.append(',');
            sb.append(max);
            sb.append(')');
            return sb.toString();
        }
    }



    public final static class Builder<X extends RectFloatND> implements Function<X, HyperRect> {

        @Override
        public X apply(final X rect2D) {
            return rect2D;
        }

    }


}