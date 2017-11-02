package jcog.exe;

import jcog.Texts;
import jcog.constraint.continuous.DoubleVar;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.concurrent.atomic.AtomicInteger;

import static jcog.Texts.n2;
import static jcog.Texts.n4;

/** potentially executable procedure of some value N >=1 iterations per invocation */
public class Can {

    final static AtomicInteger ids = new AtomicInteger();

    final static int WINDOW = 8;

    final DescriptiveStatistics iterationTime = new DescriptiveStatistics(WINDOW);
    public final DescriptiveStatistics supply = new DescriptiveStatistics(WINDOW);
    protected final DescriptiveStatistics value = new DescriptiveStatistics(WINDOW);

    /**
     * next iterations, to be solved
     */
    public final DoubleVar iterations;

    public Can() {
        this(String.valueOf(ids.incrementAndGet()));
    }

    public Can(String id) {
        iterations = new DoubleVar(id);
    }

    /**
     * in seconds
     */
    public float iterationTimeMean() {
        double mean = iterationTime.getMean();
        if (mean!=mean) return 1.0f;
        return (float) mean;
    }

    /**
     * max iterations that can/should be requested
     */
    public double supply() {
        double mean = supply.getMax();
        if (mean != mean) return 0;
        return mean;
    }

    /**
     * relative value of an iteration; ie. past value estimate divided by the actual supplied unit count
     * between 0..1.0
     */
    public float value() {
        double mean = value.getMean();
        if (mean!=mean) mean = 0.5f;
        return (float) mean;
    }

    /**
     * totalTime in sec
     */
    public void update(int supplied, double totalValue, double totalTimeSec) {
        supply.addValue(supplied);
        if (supplied > 0) {
            value.addValue(totalValue / supplied);
            iterationTime.addValue(totalTimeSec / supplied);
        }
    }

    /** called after the iteration variable has been set */
    public void commit() {

    }

    @Override
    public String toString() {
        return iterations.name + "{" +
                "iterations=" + iterations() +
                ", value=" + n4(value()) +
                ", iterationTime=" + Texts.strNS(Math.round(iterationTime.getMean()*1.0E9)) +
                ", supply=" + n2(supply.getMax()) +
                '}';
    }

    /** estimated iterations this should be run next, given the value and historically estimated cost */
    public int iterations() {
        return (int) Math.ceil(iterations.value());
    }
    public double iterationsRaw() {
        return iterations.value();
    }



}
