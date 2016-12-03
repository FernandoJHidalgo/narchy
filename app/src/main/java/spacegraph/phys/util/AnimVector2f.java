package spacegraph.phys.util;

import nars.util.Util;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.Nullable;
import spacegraph.math.v2;
import spacegraph.math.v3;
import spacegraph.phys.Dynamics;

/**
 * Created by me on 12/3/16.
 */
public class AnimVector2f extends v2 implements Animated {

    public final v2 target = new v2();
    final MutableFloat speed;
    private boolean running = true;

    public AnimVector2f(float speed) {
        this(Float.NaN, Float.NaN, null, speed);
    }

    public AnimVector2f(v3 current, Dynamics w, float speed) {
        this(current.x, current.y, w, speed);
    }

    public AnimVector2f(float x, float y, float speed) {
        this(x, y, null, speed);
    }

    public AnimVector2f(float x, float y, @Nullable Dynamics w, float speed) {
        super(x, y);
        target.set(this);
        this.speed = new MutableFloat(speed);
        if (w!=null)
            w.addAnimation(this);
    }

    public void stop() {
        running = false;
    }

    public void invalidate() {
        super.set(Float.NaN, Float.NaN);
    }

    @Override
    public boolean animate(float dt) {

        if (x!=x) {
            //invalidated
            super.set(target);
        } else {
            //interpLinear(dt);
            interpLERP(dt);
        }

        return running;
    }

    public void interpLERP(float dt) {
        float rate = Util.unitize(speed.floatValue() * dt);
        super.set(
                Util.lerp(target.x, x, rate),
                Util.lerp(target.y, y, rate)
        );
    }




    @Override
    public void set(float x, float y) {
        float px = this.x;
        if (px != px || x!=x)
            super.set(x, y); //initialization: if invalidated, use the target value immediately

        target.set(x, y); //interpolation
    }

    @Override
    public void add(float x, float y) {
        set(target.x + x, target.y + y);
    }


}
