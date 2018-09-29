package spacegraph.space2d.widget.chip;

import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import spacegraph.audio.Audio;
import spacegraph.audio.Sound;
import spacegraph.audio.SoundProducer;
import spacegraph.space2d.container.Gridding;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.button.IconToggleButton;
import spacegraph.space2d.widget.port.Port;
import spacegraph.space2d.widget.text.LabeledPane;

public class AudioOutPort extends Gridding implements SoundProducer {

    private final Port in, passThru;
    private final IconToggleButton enableButton;
    private Sound<AudioOutPort> playback;

    public AudioOutPort() {
        super();

        enableButton = CheckBox.awesome("play");
        enableButton.set(true);

        set(new LabeledPane("in", in = new Port()),
            enableButton,
            new LabeledPane("passthru", passThru = new Port()));
    }

    @Override
    protected void starting() {
        super.starting();
        playback = Audio.the().play(this);
    }


    @Override
    public void read(float[] buf, int readRate) {
        if (enableButton.get() && in.active()) {

            ObjectIntPair<float[]> nextBuffer = PrimitiveTuples.pair(buf /* TODO buffer mix command object */, readRate);

            //input fill
            in.out(nextBuffer);

            //passthru WARNING the downstream access to pass through can modify the buffer unless it is cloned
            if (passThru.active())
                passThru.out(nextBuffer);
        }
    }

    @Override
    public void skip(int samplesToSkip, int readRate) {
        //TODO
        //buffer.skip(..);
        //System.out.println("skip " + samplesToSkip);
    }

    @Override
    public boolean isLive() {
        return parent != null;
    }


    //playing.volume(0); //initially off

}
