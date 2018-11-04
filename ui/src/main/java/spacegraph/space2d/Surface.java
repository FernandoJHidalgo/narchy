package spacegraph.space2d;

import com.jogamp.opengl.GL2;
import jcog.Texts;
import jcog.Util;
import jcog.WTF;
import jcog.tree.rtree.Spatialization;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.container.collection.AbstractMutableContainer;
import spacegraph.space2d.container.unit.AspectAlign;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Predicate;

/**
 * planar subspace.
 * (fractal) 2D Surface embedded relative to a parent 2D surface or 3D space
 */
abstract public class Surface implements SurfaceBase, spacegraph.input.finger.Fingered {

    private static final AtomicReferenceFieldUpdater<Surface, RectFloat> BOUNDS = AtomicReferenceFieldUpdater.newUpdater(Surface.class, RectFloat.class, "bounds");
    private final static AtomicReferenceFieldUpdater<Surface,SurfaceBase> PARENT = AtomicReferenceFieldUpdater.newUpdater(Surface.class, SurfaceBase.class, "parent");



    public static final Surface[] EmptySurfaceArray = new Surface[0];

    private final static AtomicInteger serial = new AtomicInteger();
    protected boolean clipBounds = true;
    /**
     * serial id unique to each instanced surface
     */
    public final int id = serial.incrementAndGet();

    /**
     * scale can remain the unit 1 vector, normally
     */

    public volatile RectFloat bounds = RectFloat.Zero;
    public volatile SurfaceBase parent;
    protected volatile boolean visible = true, showing = false;

    public Surface() {

    }


    public float x() {
        return bounds.x;
    }

    public float y() {
        return bounds.y;
    }

    public float left() {
        return bounds.left();
    }

    public float top() {
        return bounds.top();
    }

    public float right() {
        return bounds.right();
    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public final int hashCode() {
        return id;
    }

    public float bottom() {
        return bounds.bottom();
    }

    public float cx() {
        return bounds.x + 0.5f * bounds.w;
    }

    public float cy() {
        return bounds.y + 0.5f * bounds.h;
    }

    abstract protected void paint(GL2 gl, SurfaceRender surfaceRender);

    public <S extends Surface> S pos(RectFloat next) {
        BOUNDS.set(this, next);
//        if (bounds.area() < ScalarValue.EPSILON)
//            throw new WTF();
        return (S) this;
    }

    protected final boolean posChanged(RectFloat next) {
        RectFloat last = BOUNDS.getAndSet(this, next);
//        if (bounds.area() < ScalarValue.EPSILON)
//            throw new WTF();
        return !last.equals(next, Spatialization.EPSILONf);
    }

    public final Surface pos(float x1, float y1, float x2, float y2) {
        return pos(RectFloat.XYXY(x1, y1, x2, y2));
    }
    public final Surface posXYWH(float x, float y, float w, float h) {
        return pos(RectFloat.XYWH(x, y, w, h));
    }

    public AspectAlign align(AspectAlign.Align align) {
        return new AspectAlign(this, 1.0f, align, 1.0f);
    }

    public SurfaceRoot root() {
        return rootParent();
    }

    /** default root() impl */
    public final SurfaceRoot rootParent() {
        SurfaceBase parent = this.parent;
        return parent == null ? null : parent.root();
    }

    /**
     * finds the most immediate parent matching the class
     */
    public <S extends Surface> S parent(Class<S> s) {
        return (S) parent(s::isInstance);
    }

    /**
     * finds the most immediate parent matching the predicate
     */
    private SurfaceBase parent(Predicate<SurfaceBase> test) {

        SurfaceBase p = this.parent;

        if (p instanceof Surface) {
            if (test.test(p))
                return p;
            else
                return ((Surface) p).parent(test);
        }

        return null;
    }

    public boolean start(SurfaceBase parent) {
        assert(parent!=null);
        SurfaceBase p = PARENT.getAndSet(this, parent);

            if (p != parent) {
                if (p != null)
                    throw new WTF();

                //synchronized (this) {
                starting();
                //}
                return true;
            }

        return false;
    }


    protected void starting() {
        //for implementing in subclasses
    }

    protected void stopping() {
        //for implementing in subclasses
    }

    public boolean stop() {
        if (PARENT.getAndSet(this, null) != null) {
            //synchronized (this) {
                showing = false;
                stopping();
            //}
            return true;
        }
        return false;
    }

    public void layout() {
        
    }

    public float w() {
        return bounds.w;
    }

    public float h() {
        return bounds.h;
    }

    public Surface pos(float x, float y) {
        pos(bounds.pos(x, y, Spatialization.EPSILONf));
        return this;
    }
    public Surface move(float dx, float dy) {
        pos(bounds.move(dx, dy, Spatialization.EPSILONf));
        return this;
    }

    public void print(PrintStream out, int indent) {
        out.print(Texts.repeat("  ", indent));
        out.println(this);
    }

    @Deprecated public final void render(GL2 gl, float pw, float ph, int dtMS) {
        render(gl, new SurfaceRender(1, 1, dtMS).set(pw, ph, pw/2, ph/2));
    }

    public final void render(GL2 gl, SurfaceRender r) {

        if (showing = (visible() && (!clipBounds || r.visible(bounds)))) {
            paint(gl, r);
        }
        //else System.out.println(this + " invisible because: visible=" + visible() + (clipBounds ? " clip=" + clipBounds : ""));

    }






    public Surface hide() {
        visible = false;
        showing = false;
        return this;
    }

    public Surface show() {
        visible = true;
        return this;
    }

    public Surface visible(boolean b) {
        return b ? show() : hide();
    }

    public boolean visible() {
        return parent!=null && visible;
    }

    public float radius() {
        return bounds.radius();
    }


    public Surface size(float w, float h) {
        return pos(bounds.size(w, h));
    }

    public boolean showing() {
        return showing;
    }

    public void posxyWH(float x, float y, float w, float h) {
        pos(RectFloat.X0Y0WH(x,y,w,h));
    }

    /** keeps this rectangle within the given bounds */
    public void fence(RectFloat bounds) {
        //if (bounds.contains(this.bounds)) {
        if (this.bounds == bounds)
            return;
        float x = left();
        float y = top();
        float L = bounds.left();
        float T = bounds.top();
        pos(Util.clamp(x, L, Math.max(L, bounds.right() - w())),
                    Util.clamp(y, T, Math.max(T, bounds.bottom() - h())));
        //}
    }

    /** detach from parent, if possible */
    public boolean remove() {
        SurfaceBase p = this.parent;
        if (p instanceof AbstractMutableContainer) {
            return ((AbstractMutableContainer) p).removeChild(this);
        }
        return false;
    }

}
