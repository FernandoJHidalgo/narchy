package kashiki.keybind.basic;

import kashiki.Editor;
import kashiki.buffer.Buffer;
import kashiki.keybind.Action;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

public class CutAction implements Action {

  @Override
  public String name() {
    return "cut";
  }

  @Override
  public void execute(Editor editor, String... args) {
    Buffer currentBuffer = editor.buffer();
    setClipboardString(currentBuffer.copy());
    currentBuffer.cut();
  }

  private static void setClipboardString(String value) {
    StringSelection selection = new StringSelection(value);

    Toolkit toolKit = Toolkit.getDefaultToolkit();
    Clipboard clipboard = toolKit.getSystemClipboard();
    clipboard.setContents(selection, selection);
  }
}