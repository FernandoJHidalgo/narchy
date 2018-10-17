package kashiki.keybind.basic;

import kashiki.Editor;
import kashiki.keybind.Action;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class PasteAction implements Action {

  @Override
  public String name() {
    return "paste";
  }

  @Override
  public void execute(Editor editor, String... args) {
    editor.buffer().insertString(getClipboardString());
  }

  private static String getClipboardString() {
    Toolkit toolKit = Toolkit.getDefaultToolkit();
    Clipboard clipboard = toolKit.getSystemClipboard();
    try {
      return clipboard.getData(DataFlavor.stringFlavor).toString();
    } catch (UnsupportedFlavorException | IOException e1) {
      e1.printStackTrace();
    }
    return "";
  }
}
