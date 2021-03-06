package spacegraph.space2d.widget.button;

import com.jogamp.newt.event.KeyEvent;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.Widget;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import static java.awt.event.KeyEvent.VK_ENTER;
import static java.awt.event.KeyEvent.VK_SPACE;

/**
 * TODO abstract to FocusableWidget
 */
public abstract class AbstractButton extends Widget {


    final static int CLICK_BUTTON = 0;

    private final Predicate<Finger> pressable = Finger.clicked(this, CLICK_BUTTON, (f) -> {
        dz = 0;
        onClick(f);
    }, () -> dz = 0.5f, () -> dz = 0f, () -> dz = 0f);


    private final AtomicBoolean enabled = new AtomicBoolean(true);

    protected AbstractButton(Surface content) {
        super(content);
    }


    @Override
    public Surface finger(Finger finger) {
        Surface f = super.finger(finger);
        if (f == this) {
            if (pressable.test(finger))
                return this;

            if (finger.dragging(CLICK_BUTTON) || finger.dragging(1 /* HACK */) || finger.dragging(0 /* HACK */)) {
                //allow pass-through for drag actions
                return null;
            }

        }
        return this;
    }

    public final boolean enabled() {
        return enabled.getOpaque();
    }

    public final <B extends AbstractButton> B enabled(boolean e) {
        enabled.set(e);
        return (B)this;
    }

    protected abstract void onClick();

    /** when clicked by finger */
    protected void onClick(Finger f) {
        if (enabled())
            onClick();
    }

    /** when clicked by key press */
    protected void onClick(KeyEvent key) {
        if (enabled()) {
            int keyCode = key.getKeyCode();
            if (keyCode == KeyEvent.VK_SPACE || keyCode == KeyEvent.VK_ENTER)
                onClick();
        }
    }



    @Override
    public boolean key(KeyEvent e, boolean pressedOrReleased) {
        if (!super.key(e, pressedOrReleased)) {
            if (pressedOrReleased) {
                short c = e.getKeyCode();
                if (c == VK_ENTER || c == VK_SPACE) {
                    onClick(e);
                    return true;
                }
            }

        }
        return false;
    }

}
