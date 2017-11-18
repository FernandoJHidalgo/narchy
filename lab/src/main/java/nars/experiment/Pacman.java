package nars.experiment;

import jcog.Util;
import nars.$;
import nars.NAR;
import nars.NAgentX;
import nars.Narsese;
import nars.experiment.pacman.PacmanGame;
import nars.op.video.BufferedImageBitmap2D;
import nars.op.video.Scale;
import nars.op.video.SwingBitmap2D;
import nars.term.atom.Atomic;


public class Pacman extends NAgentX {

    private final PacmanGame g;

    public Pacman(NAR nar)  {
        super("G", nar);

        this.g = new PacmanGame();


//        try {
//            senseCamera("G", g.view, 64, 64)
//                    .resolution(0.1f);
//        } catch (Narsese.NarseseException e) {
//            e.printStackTrace();
//        }
         Scale camScale = new Scale( new SwingBitmap2D(g.view), 28, 28);
            for (BufferedImageBitmap2D.ColorMode cm : new BufferedImageBitmap2D.ColorMode[] {
                    BufferedImageBitmap2D.ColorMode.R,
                    BufferedImageBitmap2D.ColorMode.G,
                    BufferedImageBitmap2D.ColorMode.B
            }) {
                try {
                    senseCamera("(G,c" + cm.name() + ")",
                            camScale.filter(cm)//.blur()
                    )
                            .resolution(0.1f);

                } catch (Narsese.NarseseException e) {
                    e.printStackTrace();
                }
            }

        actionTriState($.p(id, Atomic.the("x")), (dh) -> {
            switch (dh) {
                case +1:
                    g.keys[1] = true;
                    g.keys[0] = false;
                    break;
                case -1:
                    g.keys[0] = true;
                    g.keys[1] = false;
                    break;
                case 0:
                    g.keys[0] = g.keys[1] = false;
                    break;
            }
        });

       actionTriState($.p(id, Atomic.the("y")), (dh) -> {
            switch (dh) {
                case +1:
                    g.keys[2] = true;
                    g.keys[3] = false;
                    break;
                case -1:
                    g.keys[3] = true;
                    g.keys[2] = false;
                    break;
                case 0:
                    g.keys[2] = g.keys[3] = false;
                    break;
            }
        });


    }


    int lastScore;

    @Override
    protected float act() {

        g.update();

        int nextScore = g.score;

        float r = (nextScore - lastScore);
//        if (r > 0)
//            System.out.println(r);
        lastScore = nextScore;


            return 2f * (Util.sigmoid(r) - 0.5f);
    }

    public static void main(String[] args) {
        NAgentX.runRT((n) -> {

            Pacman a = new Pacman(n);
            return a;

        }, 1000f/PacmanGame.periodMS);
    }

}
