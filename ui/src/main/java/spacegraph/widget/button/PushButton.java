package spacegraph.widget.button;

import com.jogamp.opengl.GL2;
import org.jetbrains.annotations.Nullable;
import spacegraph.widget.AbstractButton;
import spacegraph.widget.Label;

import java.util.function.Consumer;

/**
 * Created by me on 11/11/16.
 */
public class PushButton extends AbstractButton {

    private final Label label;

    @Nullable private Consumer<PushButton> onClick;

    public PushButton() {
        this("");
    }

    public PushButton(String s) {
        super();
        label = new Label(s);
    }

    public PushButton(Consumer<PushButton> onClick) {
        this();
        setOnClick(onClick);
    }

    public PushButton(String s, Runnable onClick) {
        this(s, (p) -> onClick.run());
    }

    public PushButton(String s, @Nullable Consumer<PushButton> onClick) {
        this(s);
        setOnClick(onClick);
    }

    public void setOnClick(@Nullable Consumer<PushButton> onClick) {
        this.onClick = onClick;
    }

    public void setLabel(String s) {
        this.label.set(s);
    }

    @Override
    protected void paintContent(GL2 gl, float x, float y, float w, float h) {
        text(gl, label, x, y, w, h);
    }


    @Override
    protected void onClick() {
        if (onClick!=null)
            onClick.accept(this);
    }

}
