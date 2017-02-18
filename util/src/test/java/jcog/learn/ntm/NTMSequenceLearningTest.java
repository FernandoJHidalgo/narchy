package jcog.learn.ntm;

import jcog.learn.ntm.run.SequenceGenerator;
import jcog.learn.ntm.run.SequenceLearner;
import jcog.learn.ntm.run.TrainingSequence;

import static org.junit.Assert.*;

/**
 * Created by me on 2/17/17.
 */
public class NTMSequenceLearningTest {

    public static class RunSequenceLearner extends SequenceLearner {

        /** print every frame in all sequences, in the order they are trained */
        boolean printSequences;

        public static void main(String[] args) throws Exception {
            SequenceLearner s = new RunSequenceLearner(16);
            while (true) {
                s.run();
            }
        }

        public RunSequenceLearner(int vectorSize) {
            super(vectorSize, 8, 32, 1, 64);
        }

        @Override
        protected TrainingSequence nextTrainingSequence() {
            return SequenceGenerator.generateSequenceXOR(rand.nextInt(10) + 1, vectorSize);
            //return SequenceGenerator.generateSequenceSawtooth(rand.nextInt(10) + 1, vectorSize);
            //return SequenceGenerator.generateSequenceWTF(rand.nextInt(20) + 1, vectorSize);
        }

        @Override
        public void onTrained(int sequenceNum, TrainingSequence sequence, NTM[] output, long trainTimeNS, double avgError) {

            double[][] ideal = sequence.ideal;
            int slen = ideal.length;

            if (printSequences) {
                for (int t = 0; t < slen; t++) {
                    double[] actual = output[t].getOutput();
                    System.out.println("\t" + sequenceNum + "#" + t + ":\t" + toNiceString(ideal[t]) + " =?= " + toNiceString(actual));
                }
            }

            if ((sequenceNum+1) % statisticsWindow == 0) {
                System.out.format("@ %d :       avgErr: %f       time(s): %f", i,
                        mean(errors), mean(times)/1.0e9);
                System.out.println();
            }

        }



    }

}