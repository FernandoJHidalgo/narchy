package jcog.signal;

import jcog.util.IntIntToFloatFunction;

import java.lang.reflect.Array;

public class ArrayBitmap2D implements Bitmap2D {

    private final float[][] b;

    public ArrayBitmap2D(int w, int h) {
        this((float[][]) Array.newInstance(float.class, h, w));
    }

    public ArrayBitmap2D(float[][] x) {
        this.b = x;
    }

    @Override
    public int width() {
        return b[0].length;
    }

    @Override
    public int height() {
        return b.length;
    }

    @Override
    public float brightness(int xx, int yy) {
        return b[yy][xx];
    }

    public void set(int x, int y, float v) {
        this.b[y][x] = v;
    }

    public void set(IntIntToFloatFunction set) {
        int W = width();
        int H = height();
        for (int x = 0; x < W; x++)
            for (int y = 0; y < H; y++)
                this.b[y][x] = set.value(x,y);
    }
}
