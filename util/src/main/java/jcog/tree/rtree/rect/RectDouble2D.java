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

import jcog.tree.rtree.HyperRegion;
import jcog.tree.rtree.RTree;
import jcog.tree.rtree.point.Double2D;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Created by jcovert on 6/15/15.
 */
public class RectDouble2D implements HyperRegion<Double2D>, Comparable<RectDouble2D> {
    public final Double2D min;
    public final Double2D max;

    public RectDouble2D(final Double2D p) {
        min = p;
        max = p;
    }

    public RectDouble2D(double x1, double y1, double x2, double y2) {
        if (x2 < x1) {
            double t = x2;
            x2 = x1;
            x1 = t;
        }
        if (y2 < y1) {
            double t = y2;
            y2 = y1;
            y1 = t;
        }

        min = new Double2D(x1, y1);
        max = new Double2D(x2, y2);
    }

    public RectDouble2D(final Double2D p1, final Double2D p2) {
        final double minX, maxX;

        if (p1.x < p2.x) {
            minX = p1.x;
            maxX = p2.x;
        } else {
            minX = p2.x;
            maxX = p2.x;
        }

        final double minY;
        final double maxY;
        if (p1.y < p2.y) {
            minY = p1.y;
            maxY = p2.y;
        } else {
            minY = p2.y;
            maxY = p2.y;
        }

        min = new Double2D(minX, minY);
        max = new Double2D(maxX, maxY);
    }


    @Override
    public RectDouble2D mbr(final HyperRegion<Double2D> r) {
        final RectDouble2D r2 = (RectDouble2D) r;
        final double minX = Math.min(min.x, r2.min.x);
        final double minY = Math.min(min.y, r2.min.y);
        final double maxX = Math.max(max.x, r2.max.x);
        final double maxY = Math.max(max.y, r2.max.y);

        return new RectDouble2D(minX, minY, maxX, maxY);

    }

    @Override
    public int dim() {
        return 2;
    }

    @Override
    public Double2D center() {
        final double dx = center(0);
        final double dy = center(1);

        return new Double2D(dx, dy);
    }

    @Override
    public double center(int d) {
        if (d == 0) {
            return min.x + (max.x - min.x) / 2.0;
        } else {
            assert (d == 1);
            return min.y + (max.y - min.y) / 2.0;
        }
    }


    @Override
    public double coord(boolean maxOrMin, int dimension) {
        Double2D e = (maxOrMin ? max : min);
        assert(dimension==0 || dimension==1);
        return dimension==0 ? e.x : e.y;
    }


    @Override
    public double getRange(final int d) {
        if (d == 0) {
            return max.x - min.x;
        } else if (d == 1) {
            return max.y - min.y;
        } else {
            throw new IllegalArgumentException("Invalid dimension");
        }
    }

    @Override
    public boolean contains(@NotNull final HyperRegion r) {
        final RectDouble2D r2 = (RectDouble2D) r;

        return min.x <= r2.min.x &&
                max.x >= r2.max.x &&
                min.y <= r2.min.y &&
                max.y >= r2.max.y;
    }

    @Override
    public boolean intersects(final HyperRegion r) {
        final RectDouble2D r2 = (RectDouble2D) r;

        return !((min.x > r2.max.x) || (r2.min.x > max.x) ||
                (min.y > r2.max.y) || (r2.min.y > max.y));
    }

    @Override
    public double cost() {
        final double dx = max.x - min.x;
        final double dy = max.y - min.y;
        return Math.abs(dx * dy);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RectDouble2D rect2D = (RectDouble2D) o;

        return RTree.equals(min.x, rect2D.min.x) &&
                RTree.equals(max.x, rect2D.max.x) &&
                RTree.equals(min.y, rect2D.min.y) &&
                RTree.equals(max.y, rect2D.max.y);
    }

    @Override
    public int hashCode() {
        int result = min.hashCode();
        result = 31 * result + max.hashCode();
        return result;
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append('(');
        sb.append(Double.toString(min.x));
        sb.append(',');
        sb.append(Double.toString(min.y));
        sb.append(')');
        sb.append(' ');
        sb.append('(');
        sb.append(Double.toString(max.x));
        sb.append(',');
        sb.append(Double.toString(max.y));
        sb.append(')');

        return sb.toString();
    }

    @Override
    public int compareTo(@NotNull RectDouble2D o) {
        int a = min.compareTo(o.min);
        if (a != 0) return a;
        int b = max.compareTo(o.max);
        return b;
    }

    public final static class Builder implements Function<RectDouble2D, HyperRegion> {

        @Override
        public HyperRegion apply(final RectDouble2D rect2D) {
            return rect2D;
        }

    }

}