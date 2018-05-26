package jcog.learn;

import jcog.math.Quantiler;

public class QuantileDiscretize1D implements Discretize1D {

    Quantiler q = null;

    private boolean updated = false;
    float[] thresh = null;
    private boolean difference;

    @Override
    public void reset(int levels, double min, double max) {
        assert(levels>1);
        q = new Quantiler(8);
        updated = false;
        this.thresh = new float[levels];
    }

    @Override
    public void put(double value) {
        q.add((float)value);
        updated = false;
    }

    protected void ensureUpdated() {
        if (!updated) {
            //TODO calculate by nearest median (sort by distance)
            boolean difference = false;
            for (int i = 0; i < thresh.length; i++) {
                float t = q.quantile((i +0.5f) / thresh.length);
                if (i > 0 && Math.abs(t - thresh[i-1]) > Float.MIN_NORMAL) {
                    difference = true;
                }
                thresh[i] = t;
            }
            this.updated = true;
            this.difference = difference;
        }

    }

    @Override
    public int index(double value) {
        ensureUpdated();

        if (!difference)
            return 0; //default to zero if no difference known

        int nearest = -1;
        float nearestDist = Float.POSITIVE_INFINITY;
        for (int i = 0; i < thresh.length; i++) {
            float d = (float) Math.abs(thresh[i] - value);
            if (d < nearestDist) {
                nearest = i;
                nearestDist = d;
            }
        }
        return nearest;
    }

    @Override
    public double value(int v) {
        return thresh[v];
    }
}
