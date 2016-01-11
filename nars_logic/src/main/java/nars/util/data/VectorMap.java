package nars.util.data;

import nars.NAR;

/**
 * 
 * @author me
 */
public abstract class VectorMap {

	public final UniformVector input;
	public final UniformVector output;

	protected VectorMap(NAR n, String prefix, int numInputs,
			float inputPriority, int numOutputs, float outputPriority) {
		input = new UniformVector(n, prefix + "_i", new double[numInputs])
				.setPriority(inputPriority);
		output = new UniformVector(n, prefix + "_o", new double[numOutputs])
				.setPriority(outputPriority);

	}

	public void update() {
		map(input.data, output.data);
		input.update();
		output.update();
	}

	protected abstract void map(double[] in, double[] out);

}
