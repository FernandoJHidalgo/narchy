package spacegraph.space2d.widget.meta;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.SimpleTheme;
import org.jetbrains.annotations.Nullable;
import org.slf4j.event.Level;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.container.Gridding;
import spacegraph.space2d.widget.console.ConsoleGUI;
import spacegraph.space2d.widget.console.TextEdit;
import spacegraph.util.SpaceLogger;

import java.util.function.Supplier;

public class SpaceLogConsole extends Gridding implements SpaceLogger {

    final int MAX_LINES = 5;
    public final TextEdit text = new TextEdit(40, MAX_LINES);
    public ConsoleGUI textGUI;

    public SpaceLogConsole() {
        super();
        text.textBox.setReadOnly(true);
        //TextBox.DefaultTextBoxRenderer r = new TextBox.DefaultTextBoxRenderer();
        //r.setHideScrollBars(true);
        SimpleTheme theme = new SimpleTheme(TextColor.ANSI.WHITE, TextColor.ANSI.BLACK);
        //text.textBox.setRenderer(r);
        text.textBox.setTheme(theme);


    }

    @Override
    public boolean start(SurfaceBase parent) {
        if (super.start(parent)) {

            textGUI = text.surface();

            set(textGUI);
            return true;
        }
        return false;
    }

    @Override
    public void log(@Nullable Object key, float duration, Level level, Supplier<String> message) {

        synchronized(text) {
            int lines = text.getBufferLineCount();

            text.limitLines(MAX_LINES);

            text.setCursorPosition(0,lines);
            text.addLine(key + " " + message.get());
        }
    }
}
