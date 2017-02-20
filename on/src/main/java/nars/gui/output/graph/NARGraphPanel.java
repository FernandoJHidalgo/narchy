/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package nars.gui.output.graph;

import nars.NAR;
import nars.gui.WrapLayout;
import nars.gui.util.swing.NPanel;
import nars.gui.util.swing.NSlider;
import nars.gui.util.swing.PCanvas;

import javax.swing.*;
import java.awt.*;

/**
 *
 * @author me
 */
public class NARGraphPanel extends NPanel {
    private final NARGraphVis vis;
    private final PCanvas canvas;
    private final JPanel visControl, layoutControl, canvasControl;
    private final JComponent menu;
    private final JPanel graphControl;

    
    public NARGraphPanel(NAR n) {
        super(new BorderLayout());
        
    
        
        vis = new NARGraphVis(n) {
            @Override public void setMode(NARGraphVis.GraphMode g) {
                super.setMode(g);
                doLayout();
                updateUI();
            }            
        };
        canvas = new PCanvas(vis);

        visControl = vis.newStylePanel();
        canvasControl = newCanvasPanel();
        layoutControl = vis.newLayoutPanel();
        graphControl = vis.newGraphPanel();
        
        
        menu = new JPanel(new WrapLayout(FlowLayout.LEFT));
        menu.setOpaque(false);
        menu.add(graphControl);
        menu.add(visControl);
        menu.add(canvasControl);
        menu.add(layoutControl);

        Component cmp = canvas.component();
        cmp.setPreferredSize(new Dimension(500, 500));
        cmp.setSize(new Dimension(500, 500));
        cmp.setVisible(true);

        add(cmp, BorderLayout.CENTER);
        add(menu, BorderLayout.NORTH);
        
    }

    
    @Override  protected void onShowing(boolean showing) {
        
    }
    
    
    protected JPanel newCanvasPanel() {
        JPanel m = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        NSlider blur = new NSlider(0, 0, 1.0f) {
            @Override
            public void onChange(float v) {
                canvas.setMotionBlur(v);
            }
        };
        blur.setPrefix("Blur: ");
        blur.setPreferredSize(new Dimension(60, 25));
        m.add(blur);

        return m;
    }
}
