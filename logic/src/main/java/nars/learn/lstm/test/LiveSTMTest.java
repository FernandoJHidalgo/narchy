package nars.learn.lstm.test;

import jcog.Texts;
import nars.learn.lstm.Interaction;

import java.util.Arrays;

/**
 * Tests an LSTM in continuous active mode
 */
public class LiveSTMTest {

    public static void main(String[] args) throws Exception {

        final int seqPeriod = 12;

        int inputs = 4;
        int outputs = 8;
        int cells = 8;

        Interaction i = Interaction.the(inputs, outputs);
        double[] expect = i.expected;
        i.zero();


        LiveSTM l = new LiveSTM(inputs, outputs, cells) {

            int t;


            @Override
            protected Interaction observe() {


                i.expected = expect;
                int tt = t % seqPeriod;


                i.forget =
                        //(tt == 0) ? 0.9f : 0.5f;
                        0.1f;

                Arrays.fill(i.actual, 0);
                i.actual[(tt/3)%4] = 1;
                Arrays.fill(expect, 0);
                expect[(int)Math.round(  ((Math.sin(tt)+1f)/2f)*7f ) ] = 1f;

                //for (int x = 0; x < inputs; x++) {
                    //i.actual[x] = ((t + x * 37) ^ 5)%2 * 0.9f + random.nextDouble()*0.1f; //trippy tri-tone xor line chart
                //}

                //for (int x = 0; x < outputs; x++) {
                //    expect[x] = ((t * 3 + x) ^ 13) % 2;
                //}

                if ((t/20)%2 == 0) {
                    //train
                    validation_mode = false;
                } /*else {
                    validation_mode = true;
                    //predict
                    //i.expected = null;
                }*/

                t++;

                return i;
            }
        };

        for (int c = 0; c < 400000; c++) {
            System.out.println(c + "\t\t" + Texts.n4( l.next()) + "\t\t" + i);
        }
    }

}
