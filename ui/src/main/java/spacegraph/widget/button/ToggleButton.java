package spacegraph.widget.button;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by me on 11/12/16.
 */
public abstract class ToggleButton extends AbstractButton {

    final AtomicBoolean on = new AtomicBoolean(false);

    @FunctionalInterface  public interface ToggleAction {
        void onChange(ToggleButton t, boolean enabled);
    }

    @Nullable ToggleAction action;

    protected ToggleButton() {
        this(false);
    }
    protected ToggleButton(boolean startingValue) {
        on.set(startingValue);
    }


    protected ToggleButton(ToggleAction a) {
        this();
        on(a);
    }

    public ToggleButton set(boolean on) {
        if (this.on.compareAndSet(!on, on)) {
            if (action != null)
                action.onChange(this, on);
        }
        return this;
    }

    public ToggleButton on(ToggleAction a) {
        this.action = a;
        return this;
    }

    public boolean on() {
        return on.get();
    }

    @Override
    protected void onClick() {
        set(!on.get());
    }
}
