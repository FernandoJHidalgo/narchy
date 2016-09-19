package nars.experiment.asteroids.gui.menus;

import nars.experiment.asteroids.gui.MenuScreen;
import nars.experiment.asteroids.gui.Menu;
import nars.experiment.asteroids.gui.Viewer;
import nars.experiment.asteroids.Universe;
import nars.experiment.asteroids.Hypernova;
import java.awt.image.BufferedImage;

public class Pause extends MenuScreen
{
  private BufferedImage img;

  public Pause() {
    img = Hypernova.getViewer().getImage();
  }

  public void back() {
    Universe.get().togglePause(false);
    Menu.setInMenu(false);
  }

  public void loadMenu() {
    addItem(Alignment.CENTER, "Continue", null, 0);
    addItem(Alignment.CENTER, "Save", null, 1);
    addItem(Alignment.CENTER, "Load", null, 2);
    addItem(Alignment.CENTER, "Exit", null, 3);
  }
 
  public void functions(int func, String value) {
    switch(func)
    {
      case 0:
        back();
        break;
      case 1:
        newScreen(new Save());       
        break;
      case 2:
        newScreen(new Load());       
        break;
      case 3:
        newScreen(new YesNoQuit());       
        break;
    }
  }
}
