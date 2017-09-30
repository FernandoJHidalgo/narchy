package spacegraph.audio.granular;

import jcog.random.XorShift128PlusRandom;
import spacegraph.audio.Audio;
import spacegraph.audio.SoundSource;
import spacegraph.audio.sample.SampleLoader;

public enum GranulizerDemo {
    ;

    @SuppressWarnings("HardcodedFileSeparator")
    public static void main(String[] args) throws InterruptedException {

        Audio audio = new Audio(4);

        Granulize ts =
            new Granulize(SampleLoader.load("/tmp/awake.wav"), 0.25f, 0.9f, new XorShift128PlusRandom(1))
                    .setStretchFactor(0.25f);

        audio.play(ts, SoundSource.center, 1, 1);

        //audio.play(new SamplePlayer(smp, 0.5f), SoundListener.zero, 1, 1);

        audio.thread.join();
    }


}
