package spacegraph.util;

import jcog.TODO;
import jcog.Util;
import jcog.math.v2;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.container.graph.Graph2D;

/**
 * similar to RectFloat2D with additional
 * except the x,y components are mutable
 */
public class MutableFloatRect<X> {

    public float cx, cy;
    public float cxPrev, cyPrev;
    public float w, h;

    public Graph2D.NodeVis<X> node;
    private transient float rad;

    public MutableFloatRect() {
        clear();
    }


    public final MutableFloatRect set(float x, float y, float w, float h) {
        this.cxPrev = this.cx = x + w/2;
        this.cyPrev = this.cy = y + h/2;
        return size(w, h);

    }

    public final void set(RectFloat r) {
        set(r.x, r.y, r.w, r.h);
    }

    public float radius() {
        return rad;
    }

    public MutableFloatRect pos(float x, float y) {
        this.cx = x;
        this.cy = y;
        return this;
    }

    public MutableFloatRect move(float dx, float dy) {
        this.cx += dx;
        this.cy += dy;
        return this;
    }


    public float cx() {
        return cx;
    }

    public float cy() {
        return cy;
    }

    public void commit(float speedLimit) {
        v2 delta = new v2(cx, cy);
        float lenSq = delta.lengthSquared();
        if (lenSq > speedLimit*speedLimit) {

            delta.subbed(cxPrev, cyPrev);

            float len = (float) Math.sqrt(lenSq);
            delta.scaled(speedLimit/len);
            //x = Util.lerp(momentum, x0 + delta.x, x0);
            //y = Util.lerp(momentum, y0 + delta.y, y0);
            cx = cxPrev + delta.x;
            cy = cyPrev + delta.y;
        }

    }

    public void move(double dx, double dy) {
        move((float)dx, (float)dy);
    }

    public void moveTo(float x, float y, float rate) {
        this.cx = Util.lerp(rate, this.cx, x);
        this.cy = Util.lerp(rate, this.cy, y);
    }

    public float area() {
        return w * h;
    }

    public float aspectRatio() {
        return h/w;
    }

    public void set(Graph2D.NodeVis<X> v) {
        set((this.node = v).bounds);
        v.mover = this;
    }

    public void clear() {
        if (node!=null) {
            node.mover = null;
            node = null;
        }
        this.cxPrev = this.cyPrev = 0;
        set(RectFloat.Unit);
    }

    /** keeps this rectangle within the given bounds */
    public void fence(RectFloat bounds) {
        if ((cx != cx) || (cy != cy)) randomize(bounds);
        cx = Util.clamp(cx, bounds.left()+w/2, bounds.right()-w/2);
        cy = Util.clamp(cy, bounds.top()+h/2, bounds.bottom()-h/2);
    }

    public void randomize(RectFloat bounds) {
        throw new TODO();
    }

    public MutableFloatRect size(float w, float h) {
        this.w = w;
        this.h = h;
        this.rad = (float) Math.sqrt((w * w) + (h * h));
        return this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "cx=" + cx +
                ", cy=" + cy +
                ", w=" + w +
                ", h=" + h +
                '}';
    }

    public float aspectExtreme() {
        return Math.max(w/h, h/w);
    }

    public float aspect() {
        return h/w;
    }

    public RectFloat immutable() {
        return RectFloat.XYWH(cx, cy, w, h);
    }

    public MutableFloatRect<X> mbr(float px, float py) {

        float x1 = left(), x2 = right();
        if (x1 > px) {  w = right() - px; cx = px + w/2; }
        else if (x2 < px) { w = px - left(); cx = px - w/2;}

        float y1 = top(), y2 = bottom();
        if (y1 > py) {  h = bottom() - py; cy = py + h/2; }
        else if (y2 < py) { h = py - top(); cy = py - h/2; }

        return this;
    }

    public final float left() {
        return cx - w/2;
    }
    public final float right() {
        return cx + w/2;
    }
    public final float top() {
        return cy - h/2;
    }
    public final float bottom() {
        return cy + h/2;
    }
}
