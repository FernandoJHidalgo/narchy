package nars.util.event;

import nars.NAR;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**  call at the end of a frame (a batch of cycles) */
public abstract class FrameReaction implements Consumer<NAR> {

    @Nullable
    private On reg;

    protected FrameReaction(@NotNull NAR n) {
        reg = n.onFrame(this);
    }

    public void off() {
        if (reg!=null) {
            reg.off();
            reg = null;
        }
    }

    @Override
    public void accept(NAR nar) {
        onFrame();
    }


    @Deprecated
    public abstract void onFrame();
}