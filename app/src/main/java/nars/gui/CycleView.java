package nars.gui;

import nars.NAR;
import org.jetbrains.annotations.NotNull;
import spacegraph.widget.Label;
import spacegraph.widget.button.ToggleButton;
import spacegraph.widget.meta.WindowButton;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * for use with ReflectionSurface
 */
public class CycleView implements Runnable {

    public final NAR nar;

    public final AtomicBoolean run;

    public final Runnable step;

    public final Label time;

    public final ToggleButton details;

    public CycleView(@NotNull NAR nar) {

        this.nar = nar;


        run = new AtomicBoolean(false);

        step = ()->nar.run(1);

        time = new Label("");

        details = new WindowButton("Details", nar);

        nar.onCycle(this);

    }

    @Override
    public void run() {
        time.set("@: " + Long.toString(nar.time()));
    }

}
