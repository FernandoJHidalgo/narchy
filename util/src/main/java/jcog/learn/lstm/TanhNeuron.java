package jcog.learn.lstm;

public final class TanhNeuron implements Neuron
{
	@Override
	final public double activate(final double x) {
		return Math.tanh(x);
	}

	@Override
	final public double derivate(final double x) {
		double coshx = Math.cosh(x);
		double denom = (Math.cosh(2.0*x) + 1.0);
		return 4.0 * coshx * coshx / (denom * denom);
	}

	
}
