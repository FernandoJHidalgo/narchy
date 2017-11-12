package spacegraph.layout;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL2;
import jcog.Texts;
import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.Surface;
import spacegraph.input.Finger;
import spacegraph.math.v2;

import java.io.PrintStream;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by me on 7/20/16.
 */
abstract public class Layout extends Surface {

    //final AtomicBoolean mustLayout = new AtomicBoolean(true);
    boolean mustLayout = true;

    protected boolean clipTouchBounds = true;


    @Override
    public final void layout() {
        mustLayout = true;
    }

    abstract protected void doLayout();

    @Override
    public void print(PrintStream out, int indent) {
        super.print(out, indent);

        forEach(c -> {
            out.print(Texts.repeat("  ", indent + 1));
            c.print(out, indent + 1);
        });
    }



    @Override
    public void pos(RectFloat2D r) {
        RectFloat2D b = this.bounds;
        super.pos(r);
        if (bounds!=b) //if different
            layout();
    }

    @Override
    protected void paint(GL2 gl) {

        //TODO maybe in a separate update thread
        if (mustLayout) {
            doLayout();
            mustLayout = false;
        }

        forEach(c -> c.render(gl));
    }


    @Override
    public Surface onTouch(Finger finger, v2 hitPoint, short[] buttons) {
        Surface x = super.onTouch(finger, hitPoint, buttons);
        if (x != null)
            return x;

        //2. test children reaction


        // Draw forward, propagate touch events backwards
        if (hitPoint == null) {
            forEach(c -> c.onTouch(finger, null, null));
            return null;
        } else {

            //HACK
            final Surface[] found = {null};
        float fx = finger.hit.x;
        float fy = finger.hit.y;
            forEach(c -> {

                if (found[0]!=null) //TODO use whileEach() with a predicate for fast terminate
                    return;

                //TODO factor in the scale if different from 1

//                if (/*csx != csx || */csx <= 0 || /*csy != csy ||*/ csy <= 0)
//                    return;

                //project to child's space

                //subHit.sub(tx, ty);

//                float hx = relativeHit.x, hy = relativeHit.y;

                if (!clipTouchBounds || (
                        fx >= c.bounds.min.x && fx <= c.bounds.max.x && fy >= c.bounds.min.y && fy <= c.bounds.max.y)) {

                    v2 relativeHit = new v2(finger.hit);
                    relativeHit.sub(c.x(), c.y());
                    float csx = c.w();
                    float csy = c.h();
                    relativeHit.scale(1f / csx, 1f / csy);

                    Surface s = c.onTouch(finger, relativeHit, buttons);
                    if (s != null) {
                        if (found[0]==null || found[0].bounds.cost() > s.bounds.cost())
                            found[0] = s; //FIFO
                    }
                }

            });

            if ((found[0])!=null)
                return found[0];
        }

        return tangible() ? this : null;
    }

    public boolean tangible() {
        return false;
    }

    @Override
    public boolean onKey(KeyEvent e, boolean pressed) {
        if (!super.onKey(e, pressed)) {
            forEach(c -> c.onKey(e, pressed));
        }
        return false;
    }

    @Override
    public boolean onKey(v2 hitPoint, char charCode, boolean pressed) {
        if (!super.onKey(hitPoint, charCode, pressed)) {
            forEach(c -> onKey(hitPoint, charCode, pressed));
        }
        return false;
    }

    abstract public void forEach(Consumer<Surface> o);

    /**
     * identity compare
     */
    static boolean equals(List x, Object[] y) {
        int s = x.size();
        if (s != y.length) return false;
        for (int i = 0; i < s; i++) {
            if (x.get(i) != y[i])
                return false;
        }
        return true;
    }

    /**
     * identity compare
     */
    static boolean equals(List x, List y) {
        int s = x.size();
        if (s != y.size()) return false;
        for (int i = 0; i < s; i++) {
            if (x.get(i) != y.get(i))
                return false;
        }
        return true;
    }

}
