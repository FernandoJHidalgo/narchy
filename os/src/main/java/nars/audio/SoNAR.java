package nars.audio;

import jcog.exe.Loop;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.concept.Concept;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import spacegraph.audio.Audio;
import spacegraph.audio.Sound;
import spacegraph.audio.SoundProducer;
import spacegraph.audio.sample.SampleLoader;
import spacegraph.audio.sample.SoundSample;
import spacegraph.audio.synth.SineWave;
import spacegraph.audio.synth.granular.Granulize;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static nars.$.$;

/**
 * NAR sonification
 */
public class SoNAR extends TimerTask {

    private final NAR nar;
    public final Audio audio;

    private long now;

    public static class SampleDirectory {
        final Map<String, SoundSample> samples = new ConcurrentHashMap<>();

//        final Random random = new XorShift128PlusRandom(System.currentTimeMillis());

        public SoundSample sample(String file) {
            return samples.computeIfAbsent(file, SampleLoader::load);
        }

        public void samples(String dirPath) {
            for (File f : new File(dirPath).listFiles()) {
                String path = f.getAbsolutePath();
                samples.computeIfAbsent(path, SampleLoader::load);
            }
        }

        /**
         * gets a random sample from what is loaded
         */
        public SoundSample sample(int hash) {
            List<SoundSample> l = samples.values().stream().collect(Collectors.toList());
            if (l != null && !l.isEmpty()) {
                SoundSample s;
                do {
                    s = l.get(Math.abs(hash) % l.size());
                } while (s == null);
                return s;
            } else
                return null;
        }

        public SoundProducer byHash(Object x) {






            return new SineWave((float) (Math.random() * 1000 + 200));
        }
    }

    public SoNAR(NAR n) {
        this(n, new Audio(16));
    }


    public SoNAR(NAR n, Audio audio) {
        this(n, audio, 10);
    }

    public SoNAR(NAR n, Audio audio, int updatePeriodMS) {

        this.nar = n;
        this.audio = audio;


        new Loop(updatePeriodMS) {

            @Override
            public boolean next() {
                SoNAR.this.run();
                return true;
            }
        };












    }


    /**
     * updated each cycle
     */
    final Map<Concept, Sound> termSounds = new ConcurrentHashMap();

    /**
     * vol of each individual sound
     */
    float soundVolume = 0.25f;

    public void listen(Concept k, Function<? super Concept, ? extends SoundProducer> p) {
        termSounds.computeIfAbsent(k, kk -> {
            SoundProducer ss = p.apply(kk);
            return audio.play(ss, soundVolume,
                    0.5f, /* priority */
                    (float) (Math.random() - 0.5f) /* balance */
            );
        });
    }












    @Override
    public void run() {
        now = nar.time();
        termSounds.forEach(this::update);
    }

    private boolean update(@NotNull Concept c, Sound s) {







        Truth b = nar.goalTruth(c, now);



        float thresh = 0.55f;
        if (b != null && b.freq() > thresh) {

            if (s.producer instanceof Granulize) {
                float stretchFactor = (b.freq() - 0.5f) * 2f;
                if (stretchFactor > 0 && stretchFactor < 0.05f) stretchFactor = 0.05f;
                else if (stretchFactor < 0 && stretchFactor > -0.05f) stretchFactor = -0.05f;
                ((Granulize) s.producer).setStretchFactor(stretchFactor);
            }
//            if (s.producer instanceof SoundProducer.Amplifiable) {
//
//                ((SoundProducer.Amplifiable) s.producer).setAmplitude(2f * (b.freq() - 0.5f));
//            }


            return true;
        } else {
//            if (s.producer instanceof SoundProducer.Amplifiable) {
//                ((SoundProducer.Amplifiable) s.producer).setAmplitude(0f);
//            }
            return false;


        }









    }

    public void join() throws InterruptedException {
        audio.thread.join();
    }

    public static void main(String[] args) throws InterruptedException, Narsese.NarseseException {
        NAR n = new NARS().get();


        n.input("a:b. :|: (--,b:c). c:d. d:e. (--,e:f). f:g. b:f. a:g?");
        n.startPeriodMS(16);
        SoNAR s = new SoNAR(n);
        SampleDirectory d = new SampleDirectory();
        d.samples("/home/me/wav/legoweltkord");
        s.listen(n.conceptualize($("a")), d::byHash);
        s.listen(n.conceptualize($("b")), d::byHash);
        s.listen(n.conceptualize($("c")), d::byHash);
        s.listen(n.conceptualize($("d")), d::byHash);
        s.listen(n.conceptualize($("e")), d::byHash);
        s.listen(n.conceptualize($("f")), d::byHash);
        s.listen(n.conceptualize($("g")), d::byHash);
        s.listen(n.conceptualize($("a:b")), d::byHash);
        s.listen(n.conceptualize($("b:c")), d::byHash);
        s.listen(n.conceptualize($("c:d")), d::byHash);
        s.listen(n.conceptualize($("d:e")), d::byHash);
        s.listen(n.conceptualize($("e:f")), d::byHash);
        s.listen(n.conceptualize($("f:g")), d::byHash);
        s.listen(n.conceptualize($("a:g")), d::byHash);
        try {
            s.audio.record("/tmp/test.raw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        s.join();
    }
}
