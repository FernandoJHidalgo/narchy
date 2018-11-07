package spacegraph.input.finger;

import com.jogamp.newt.event.*;
import org.jetbrains.annotations.Nullable;
import spacegraph.video.JoglSpace;
import spacegraph.video.JoglWindow;

public class NewtMouse extends Finger implements MouseListener, WindowListener {

    private final JoglWindow win;

    public NewtMouse(JoglSpace w) {
        this.win = w.io;
        w.onReady(()->{
            if (win.window.hasFocus())
                focused.set(true);

            win.addMouseListenerPre(this);
            win.addWindowListener(this);
            //this.win.onUpdate((Runnable) this::update);
        });
    }

    private boolean update(boolean moved, MouseEvent e) {
        return update(moved, e, null);
    }

    private boolean update(boolean moved, MouseEvent e, short[] buttonsDown) {

        if (moved) {
            int pmx = e.getX(), pmy = win.getHeight() - e.getY();

            posPixel.set(pmx, pmy);
            posScreen.set(win.getX() + pmx, win.getScreenY() - (e.getY() + win.getY()));
        }

        if (buttonsDown != null) {
            update(buttonsDown);
        }

        return e != null;
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mouseEntered(@Nullable MouseEvent e) {
        if (focused.compareAndSet(false, true)) {
            enter();
            if (e != null)
                update(false, e);
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (focused.compareAndSet(true, false)) {
            update(false, null);
            exit();
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {


        if (update(false, e, e.getButtonsDown())) {
            if (touching() != null)
                e.setConsumed(true);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {


        short[] bd = e.getButtonsDown();


        for (int i = 0, bdLength = bd.length; i < bdLength; i++)
            bd[i] = (short) -bd[i];

        update(false, e, bd);

        if (touching() != null)
            e.setConsumed(true);

    }


    @Override
    public void mouseDragged(MouseEvent e) {


        if (update(true, e))
            if (touching() != null)
                e.setConsumed(true);
    }

    @Override
    public void mouseMoved(MouseEvent e) {

        update(true, e);


    }

    @Override
    public void windowResized(WindowEvent e) {

    }

    @Override
    public void windowMoved(WindowEvent e) {

    }

    @Override
    public void windowDestroyNotify(WindowEvent e) {

    }

    @Override
    public void windowDestroyed(WindowEvent e) {

    }

    @Override
    public void windowGainedFocus(WindowEvent e) {
        if (focused.compareAndSet(false, true)) {
            enter();
            update(false, null);
        }
    }

    @Override
    public void windowLostFocus(WindowEvent e) {
        if (focused.compareAndSet(true, false)) {
            update(false, null);
            exit();
        }
    }

    @Override
    public void windowRepaint(WindowUpdateEvent e) {

    }

    @Override
    public void mouseWheelMoved(MouseEvent e) {
        if (e.isConsumed())
            return;
        rotationAdd(e.getRotation());
    }
}
