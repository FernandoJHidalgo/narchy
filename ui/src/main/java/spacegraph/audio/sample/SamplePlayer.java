package spacegraph.audio.sample;

import org.jetbrains.annotations.NotNull;
import spacegraph.audio.SoundProducer;


public class SamplePlayer implements SoundProducer {
    @NotNull
    private final SoundSample sample;
    private float pos;
    public boolean alive = true;
    private final float rate;


    public SamplePlayer(@NotNull SoundSample sample, float rate) {
        this.rate = rate;
        this.sample = sample;
    }

    @Override
    public float read(float[] buf, int readRate) {
        float step = (sample.rate * rate) / readRate;

        float[] sb = sample.buf;

        for (int i = 0; i < buf.length; i++) {
            float next;
            if (pos >= sb.length) {
                next = 0;
                alive = false;
            } else {
                next = sb[(int) (pos)];
            }
            buf[i] = next;
            pos += step;
        }


        return 1;
    }

    @Override
    public void skip(int samplesToSkip, int readRate) {
        float step = sample.rate / readRate;
        pos += step * samplesToSkip;

        if (alive && pos >= sample.buf.length) {
            alive = false;
        }
    }

    @Override
    public boolean isLive() {
        return alive;
    }

    @Override
    public void stop() {
        alive = false;
    }
}