package spacegraph.space2d.widget.textedit.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spacegraph.space2d.widget.textedit.buffer.Buffer;
import spacegraph.space2d.widget.textedit.buffer.BufferListener;


public class TextEditViewTest {

  private Buffer buffer;

  @BeforeEach
  public void setup() {
    buffer = new Buffer("testBuffer", "あいうえお");

    BufferListener listener = new TextEditView(buffer);
    buffer.addListener(listener);
  }

  @Test
  public void test() {
    buffer.back();
    buffer.backspace();
    buffer.delete();
    buffer.insertEnter();
  }

}
