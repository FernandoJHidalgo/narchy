package spacegraph.util.animate;

import jcog.Util;
import org.apache.commons.lang3.mutable.MutableFloat;
import spacegraph.util.math.Tuple2f;
import spacegraph.util.math.v2;

/**
 * Created by me on 12/3/16.
 */
public class AnimVector2f extends v2 implements Animated {

    public final v2 target = new v2();
    final MutableFloat speed;
    private boolean running = true;

    public AnimVector2f(float speed) {
        this(Float.NaN, Float.NaN, speed);
    }

    public AnimVector2f(float x, float y, float speed) {
        super(x, y);
        target.set(this);
        this.speed = new MutableFloat(speed);

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
            
            super.set(target);
        } else {
            
            interpLERP(dt);
        }

        return running;
    }

    public float targetX() {
        return target.x;
    }
    public float targetY() {
        return target.y;
    }

    public void interpLERP(float dt) {
        float rate = Util.unitize(speed.floatValue() * dt);
        super.set(
                Util.lerp(rate, x, target.x),
                Util.lerp(rate, y, target.y)
        );
        
    }


    public AnimVector2f scaled(float s) {
        set(this.x * s, this.y * s);
        return this;
    }

    @Override
    public Tuple2f set(float x, float y) {
        float px = this.x;
        if (px != px || x!=x)
            super.set(x, y); 

        target.set(x, y); 
        return this;
    }

    @Override
    public void add(float x, float y) {
        set(target.x + x, target.y + y);
    }


}
