package nars.guifx.util;

import javafx.animation.AnimationTimer;

import java.util.function.Consumer;


public final class Animate extends AnimationTimer {

    private final Consumer<Animate> run;
    private final long periodMS;
    private long last;

    public Animate(long periodMS, Consumer<Animate> r) {
        this.periodMS = periodMS;
        run = r;
    }

    @Override
    public void handle(long nowNS) {
        long now = nowNS/1000000L; //ns -> ms
        if (now - last > periodMS) {
            last = now;
            run.accept(this);
        }
    }

    public long getPeriod() {
        return periodMS;
    }
}