package spacegraph.input.finger;

import com.jogamp.newt.opengl.GLWindow;
import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.space2d.widget.windo.Windo;
import spacegraph.util.math.v2;
import spacegraph.video.JoglSpace;

public class FingerResizeWindow extends FingerResize {

    final static int MIN_WIDTH = 32;
    final static int MIN_HEIGHT = MIN_WIDTH;

    private final JoglSpace window;

    public FingerResizeWindow(JoglSpace window, int button, Windo.DragEdit mode) {
        super(button, mode, true);
        this.window = window;
    }


    @Override
    protected v2 pos(Finger finger) {
        
        return finger.posScreen.scale(1f,-1f);
    }


    @Override
    protected RectFloat2D size() {
        JoglSpace w = this.window;
        GLWindow ww = w.window;
        int sx = ww.getX();
        int sy = ww.getY();
        return RectFloat2D.XYXY(sx, sy, sx + ww.getWidth(),sy + ww.getHeight());
    }


    @Override
    protected void resize(float x1, float y1, float x2, float y2) {
        int w = Math.round(x2 - x1);
        if (w < MIN_WIDTH)
            return;
        int h = Math.round(y2 - y1);
        if (w < MIN_HEIGHT)
            return;


        window.setPositionAndSize(Math.round(x1), Math.round(y1), w, h);
    }

}
