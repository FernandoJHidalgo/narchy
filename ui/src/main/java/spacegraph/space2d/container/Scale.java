package spacegraph.space2d.container;

import spacegraph.space2d.Surface;

public class Scale extends UnitContainer {

    protected float scale;

    public Scale(Surface the, float s) {
        super(the);
        scale(s);
    }

    public Scale scale(float scale) {
        this.scale = scale;
        return this;
    }

    public float scale() {
        return scale;
    }


    @Override
    protected void doLayout(int dtMS) {

        



        float w = w();
        float vw = w * scale; 
        float h = h();
        float vh = h * scale;
        float marginAmt = (1f - scale) / 2;
        float tx = x() + w * marginAmt, ty = y() + h * marginAmt;

        the.pos(tx, ty, tx+vw, ty+vh);
    }










}
