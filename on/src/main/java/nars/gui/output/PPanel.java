package nars.gui.output;

import processing.core.PApplet;

import javax.swing.*;
import java.awt.*;

/**
 * Processing.org Panel
 */
public class PPanel extends PApplet {

    public PPanel() {
        super();
    }



    @Override
    public void setup() {
        noLoop();
        
    }

    

    public JPanel newPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.add((Component) this.getSurface().getNative(), BorderLayout.CENTER);
        //init();
        return p;
    }
    

}

