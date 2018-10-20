package spacegraph.space2d.widget.textedit;

import spacegraph.space2d.widget.textedit.keybind.Action;
import spacegraph.space2d.widget.textedit.keybind.NoopAction;
import spacegraph.space2d.widget.textedit.keybind.basic.*;

import java.util.HashMap;
import java.util.Map;

public class TextEditActions {

    private final Map<String, Action> actionMap = new HashMap<>(128);

    public TextEditActions() {
//        add(new ExitNavyAction());

        add(new NoopAction());
        add(new TypeAction());
        add(new ForwardAction());
        add(new BackAction());
        add(new PreviousAction());
        add(new NextAction());
        add(new BufferHeadAction());
        add(new BufferLastAction());

        add(new HeadAction());
        add(new LastAction());
        add(new BackspaceAction());
        add(new DeleteAction());
        add(new ReturnAction());
        add(new MarkAction());
        add(new PasteAction());
        add(new CopyAction());
        add(new CutAction());
        add(new KillRingAction());

        add(new NewBufferAction());
        add(new SaveAction());
        add(new OpenFileAction());
        add(new SaveFileAction());



//        add(new FullScreenAction());
    }

    private void add(Action action) {
        actionMap.put(action.name(), action);
    }

    public void run(TextEditModel editor, String name, String... args) {
        Action r = actionMap.get(name);
        if (r == null)
            throw new RuntimeException("Unregistered action [" + name + ']');
        r.execute(editor, args);
    }
}
