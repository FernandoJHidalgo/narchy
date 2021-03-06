package spacegraph.space2d;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.impl.NewtKeyboard;
import spacegraph.input.finger.impl.NewtMouseFinger;
import spacegraph.space2d.container.collection.MutableListContainer;
import spacegraph.space2d.hud.ZoomOrtho;
import spacegraph.video.JoglSpace;

public class SpaceGraphFlat extends JoglSpace {


    private ZoomOrtho zoom;
//    private final Ortho<MutableListContainer> hud;

    private final Finger finger;
    private final NewtKeyboard keyboard;

    public SpaceGraphFlat(Surface content) {
        super();

        finger = new NewtMouseFinger(this);

        keyboard = new NewtKeyboard(/*TODO this */);

        onReady(() -> {

            zoom = new ZoomOrtho(this, content, finger, keyboard) {
                @Override
                protected void starting() {
                    super.starting();
                    io.window.setPointerVisible(false); //HACK
                }
            };
            add(zoom);
            add(finger.zoomBoundsSurface(zoom.cam));
            add(finger.cursorSurface());
            //addOverlay(this.keyboard.keyFocusSurface(cam));

            MutableListContainer hud = new MutableListContainer() {

                @Override
                protected void paintIt(GL2 gl, SurfaceRender r) {
                    gl.glPushMatrix();

                    gl.glLoadIdentity();
                }


                @Override
                protected void compileAbove(SurfaceRender r) {
                    r.on(GLMatrixFunc::glPopMatrix);
                }

            };
            add(hud);
//        hud.content().addAt(new PushButton("x").pos(0, 0, 100f, 100f));


        });

    }

}
