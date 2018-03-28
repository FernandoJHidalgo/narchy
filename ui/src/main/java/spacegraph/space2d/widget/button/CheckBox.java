package spacegraph.space2d.widget.button;

import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectBooleanProcedure;
import spacegraph.space2d.widget.text.Label;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by me on 11/12/16.
 */
public class CheckBox extends ToggleButton {

    public String text;
    protected final Label label;

    public CheckBox(String text) {
        this.text = text;

        content((label = new Label("")));

        set(false);
    }

    public CheckBox(String text, Runnable r) {
        this(text, (b) -> { if (b) r.run(); } );
    }

    public CheckBox(String text, BooleanProcedure b) {
        this(text);
        on((a, e) -> b.value(e));
    }

    public CheckBox(String text, ObjectBooleanProcedure<ToggleButton> on) {
        this(text);
        on(on);
    }

    public CheckBox(String text, AtomicBoolean b) {
        this(text);
        set(b.get());
        on((button, value) -> b.set(value));
    }

//    public CheckBox(String text, MutableBoolean b) {
//        this(text);
//        set(b.booleanValue());
//        on((button, value) -> b.setValue(value));
//    }


    @Override
    public ToggleButton set(boolean on) {
        label.text(label(text, on));
        super.set(on);
        return this;
    }


    protected String label(String text, boolean on) {
        return (on ? "[X] " : "[ ] ") + text;
    }

    public void setText(String s) {
        this.text = s;
    }

}
