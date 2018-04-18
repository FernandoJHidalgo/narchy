package spacegraph.space2d.widget.tab;

import jcog.exe.Loop;
import org.eclipse.collections.api.block.procedure.primitive.ObjectBooleanProcedure;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.MutableContainer;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.button.ToggleButton;
import spacegraph.space2d.widget.sketch.Sketch2DBitmap;
import spacegraph.space2d.widget.text.Label;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by me on 12/2/16.
 */
public class TabPane extends Splitting {


    private static final float CONTENT_VISIBLE_SPLIT = 0.9f;
    private final ButtonSet tabs;
    private final MutableContainer content;


    public TabPane(Map<String, Supplier<Surface>> builder) {
        this(ButtonSet.Mode.Multi, builder);
    }

    public TabPane(ButtonSet.Mode mode, Map<String, Supplier<Surface>> builder) {
        this(mode, builder, CheckBox::new);
    }

    public TabPane(ButtonSet.Mode mode, Map<String, Supplier<Surface>> builder, Function<String, ToggleButton> buttonBuilder) {
        super();

        split(0f);

        content = new Gridding();

        tabs = new ButtonSet<>(mode, builder.entrySet().stream().map(x -> {
            final Surface[] created = {null};
            Supplier<Surface> creator = x.getValue();
            String label = x.getKey();
            ObjectBooleanProcedure<ToggleButton> toggle = (cb, a) -> {
                Loop.invokeLater(() -> {
                    if (a) {
                        Surface cx;
                        try {
                            cx = creator.get();
                        } catch (Throwable t) {
                            String msg = t.getMessage();
                            if (msg == null)
                                msg = t.toString();
                            cx = new Label(msg);
                        }


                        content.add(created[0] = cx);
                        split(CONTENT_VISIBLE_SPLIT); //hide empty content area

                    } else {

                        if (created[0] != null) {
                            content.remove(created[0]);
                            created[0] = null;
                        }
                        if (content.isEmpty()) {
                            split(0f); //hide empty content area
                        }

                    }
                });
            };

            ToggleButton bb = buttonBuilder.apply(label);
            return bb.on(toggle);
        }).toArray(ToggleButton[]::new));

        set(tabs, content);

    }


    public static void main(String[] args) {
        SpaceGraph.window(new TabPane(Map.of(
                "a", () -> new Sketch2DBitmap(40, 40),
                "b", () -> new PushButton("x"))), 800, 800);
    }


}
