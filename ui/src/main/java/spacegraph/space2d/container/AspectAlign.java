package spacegraph.space2d.container;

import spacegraph.space2d.Surface;

import static spacegraph.space2d.container.AspectAlign.Align.Center;

public class AspectAlign extends UnitContainer {

    /**
     * not used unless aspect ratio is set to non-NaN value
     */
    protected Align align = Align.Center;

    /**
     * height/width target aspect ratio; if aspect is NaN, no adjustment applied
     */
    protected float aspect;

    /**
     * relative size adjustment uniformly applied to x,y
     * after the 100% aspect size has been calculated
     */
    protected float scaleX, scaleY;

    protected AspectAlign() {
        this(1f);
    }

    protected AspectAlign(float scale) {
        this(null, 1f, Center, scale);
    }

    public AspectAlign(Surface the) {
        this(the, 1f, Center, 1f);
    }


    public AspectAlign(Surface the, Align a, float w, float h) {
        this(the, h / w, a, 1f);
    }

    public AspectAlign(Surface the, float aspect) {
        this(the, aspect, Align.Center);
    }

    public AspectAlign(Surface the, float aspect, Align a) {
        this(the, aspect, a, 1);
    }

    public AspectAlign(Surface the, float aspect, Align a, float scale) {
        this(the, aspect, a, scale, scale);
    }

    public AspectAlign(Surface the, float aspect, Align a, float scaleX, float scaleY) {
        super(the);
        this.aspect = aspect;
        this.align = a;
        scale(scaleX, scaleY);
    }

    public AspectAlign scale(float sx, float sy) {
        this.scaleX = sx;
        this.scaleY = sy;
        layout(); 
        return this;
    }

    @Override
    protected void doLayout(int dtMS) {

        



        
        final float w = w();
        final float h = h();

        
        float tw = w * scaleX;
        float th = h * scaleY;
        final float otw = tw, oth = th;

        float aspect = this.aspect;
        if (aspect == aspect /* not NaN */) {

            if (th > tw/aspect) {

                    
                    
                    th = tw * aspect;



                    
                
            }
            if (tw * aspect < th) {

                    
                    tw = th / aspect;
                    




            }
            if (tw/aspect > th) {
                
                tw = th * aspect;
            }
            








            if (tw > otw) {
                
                th = otw/(tw/th);
                tw = otw;
            }
            if (th > oth) {
                
                tw = oth/(th/tw);
                th = oth;
            }










        }

        float tx, ty;
        switch (align) {

            

            case Center:
                
                tx = bounds.left() + (w - tw) / 2f;
                ty = bounds.top() + (h - th) / 2f;
                break;
            case LeftCenter:
                tx = bounds.left();
                ty = bounds.top() + (h - th) / 2f;
                break;

            case RightTop:
                tx = bounds.right() - tw;
                ty = bounds.bottom() - th;
                break;

            case RightTopOut:
                tx = bounds.right();
                ty = bounds.bottom();
                break;
            case LeftTopOut:
                tx = bounds.left();
                ty = bounds.bottom();
                break;

            case LeftTop:
                tx = bounds.left();
                ty = bounds.bottom() - th;
                break;

            case None:
            default:
                tx = bounds.left();
                ty = bounds.top();
                break;

        }

        doLayout(tx, ty, tw, th);
    }

    protected void doLayout(float tx, float ty, float tw, float th) {
        the.pos(tx, ty, tx+tw, ty+th);
    }

    @Override
    public AspectAlign align(Align align) {
        this.align = align;
        return this;
    }







    public AspectAlign scale(float s) {
        return scale(s, s);
    }

    public enum Align {


        None,

        /**
         * 1:1, centered
         */
        Center,

        /**
         * 1:1, x=left, y=center
         */
        LeftCenter,

        /**
         * 1:1, x=right, y=center
         */
        RightTop, LeftTop,

        

        RightTopOut, LeftTopOut
    }
}
