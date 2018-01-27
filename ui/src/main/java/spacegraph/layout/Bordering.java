package spacegraph.layout;

import jcog.TODO;
import spacegraph.Surface;

/* 9-element subdivision


 */
public class Bordering extends Stacking {
    public final static int C = 0;
    public final static int N = 1;
    public final static int S = 2;
    public final static int E = 3;
    public final static int W = 4;
    public final static int NE = 5;
    public final static int NW = 6;
    public final static int SW = 7;
    public final static int SE = 8;

    /** in percent of the half total size of the corresponding dimension */
    protected float borderWest = 0.25f;
    protected float borderEast = 0.25f;
    protected float borderSouth = 0.25f;
    protected float borderNorth = 0.25f;

    public Bordering() {
        super();
    }

    public Bordering(Surface center) {
        this();
        set(center);
    }

    @Override
    public void doLayout(int dtMS) {

        float X = x();
        float Y = y();
        float W = w();
        float H = h();
        float w2, h2;

        boolean aspectEqual = true;
        if (aspectEqual) {
            w2 = h2 = Math.min(W,H)/2;
        } else {
            w2 = W / 2;
            h2 = H / 2;
        }
        Surface[] children = children();
        for (int i = 0, childrenLength = children.length; i < childrenLength; i++) {
            Surface c = children[i];

            if (c instanceof EmptySurface)
                continue;

            float x1, y1, x2, y2;

            switch (i) {
                case C:
                    x1 = borderWest * w2;
                    y1 = borderSouth * h2;
                    x2 = W - borderEast * w2;
                    y2 = H - borderNorth * h2;
                    break;
                case N:
                    x1 = borderWest * w2;
                    y1 = H - borderNorth * h2;
                    x2 = W - borderEast * w2;
                    y2 = H;
                    break;
                case S:
                    x1 = borderWest * w2;
                    y1 = 0;
                    x2 = W - borderEast * w2;
                    y2 = borderSouth * h2;
                    break;
                case Bordering.W:
                    x1 = 0;
                    y1 = borderSouth * h2;
                    x2 = borderWest * w2;
                    y2 = H - borderNorth * h2;
                    break;
                case E:
                    x1 = W - borderEast * w2;
                    y1 = borderSouth * h2;
                    x2 = W;
                    y2 = H - borderNorth * h2;
                    break;
                case NE:
                    x1 = W - borderEast * w2;
                    y1 = H - borderNorth * h2;
                    x2 = W;
                    y2 = H;
                    break;
                case SW:
                    x1 = 0;
                    y1 = 0;
                    x2 = borderWest * w2;
                    y2 = borderSouth * h2;
                    break;
                default:
                    throw new TODO();
            }
            assert (x2 >= x1 && y2 >= y1);
            c.pos(X + x1, Y + y1, X + x2, Y + y2);
        }
    }

    @Override
    public Surface set(int index, Surface next) {
        if (index >= 9)
            throw new ArrayIndexOutOfBoundsException();

        synchronized (this) {
            int empties = index - (childrenCount()-1);
            for (int i = 0; i < empties; i++)
                add(new EmptySurface()); //placeholders

            return super.set(index, next);
        }
    }
}
