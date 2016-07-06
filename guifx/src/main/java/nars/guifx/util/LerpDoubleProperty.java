package nars.guifx.util;

import javafx.beans.property.SimpleDoubleProperty;

/**
 * Created by me on 8/13/15.
 */
public class LerpDoubleProperty extends SimpleDoubleProperty {

    final double epsilon = 0.01;

    final double rate = 0.25;

    public double target;
    boolean stable;

    public LerpDoubleProperty(double v) {
        super(v);
        target = v;
        stable = true;
    }

    public double getTarget() {
        return target;
    }

    @Override
    public void setValue(Number v) {
        set(v.doubleValue());
    }

    @Override
    public void set(double v) {
        target = v;
        updateStability();
    }

    /**
     * call each animation frame
     */
    public void update() {
        if (stable) return;

        double v = getValue() * (1.0d - rate) + target * rate;

        super.set(v);

        updateStability();
    }


    private void updateStability() {
        stable = (Math.abs(target - get()) <= epsilon);
    }

    public void setTargetPlus(double d) {
        set(getTarget() + d);
    }
}
