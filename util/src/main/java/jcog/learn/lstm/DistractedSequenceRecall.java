package jcog.learn.lstm;

import java.util.Random;
import java.util.function.Consumer;

public class DistractedSequenceRecall extends AbstractTraining {
	final int length;

	public DistractedSequenceRecall(Random r, int inputs, int outputs, int length, int batches) {
		super(r, inputs, outputs);

		this.length = length;
		this.batches = batches;
	}

	@Override
	protected void interact(Consumer<Interaction> experience) {


		for (int i = 0; i < this.batches; i++) {


			int[] seq = new int[length];

			int target1 = random.nextInt(outputs);
			int target2 = random.nextInt(outputs);
			for (int t = 0; t < length; t++) {
				seq[t] = random.nextInt(outputs) + outputs;//+4 so as not to overlap with target symbols
			}
			int loc1 = random.nextInt(length);
			int loc2 = random.nextInt(length);
			while (loc1 == loc2)
				loc2 = random.nextInt(length);
			if (loc1 > loc2) {
				int temp = loc1;
				loc1 = loc2;
				loc2 = temp;
			}
			seq[loc1] = target1;
			seq[loc2] = target2;

			for (int t = 0; t < seq.length; t++) {
				double[] input = new double[inputs];
				input[seq[t]] = 1.0;

				Interaction inter = new Interaction();
//				if (t == 0)
//					inter.forget = 1f;
				inter.actual = input;
				experience.accept(inter);

			}

			//final 2 steps
			double[] input1 = new double[inputs];
			input1[8] = 1.0;
			double[] target_output1 = new double[outputs];
			target_output1[target1] = 1.0;
			experience.accept( Interaction.the(input1, target_output1) );

			double[] input2 = new double[inputs];
			input2[9] = 1.0;
			double[] target_output2 = new double[outputs];
			target_output2[target2] = 1.0;
			experience.accept( Interaction.the(input2, target_output2) );

		}

	}
}