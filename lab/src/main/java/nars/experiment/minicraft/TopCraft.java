package nars.experiment.minicraft;

import jcog.signal.wave2d.MonoBufImgBitmap2D;
import nars.$;
import nars.NAR;
import nars.NAgentX;
import nars.Narsese;
import nars.experiment.minicraft.top.InputHandler;
import nars.experiment.minicraft.top.TopDownMinicraft;
import nars.sensor.Bitmap2DSensor;
import nars.sensor.PixelBag;
import nars.video.AutoclassifiedBitmap;
import spacegraph.SpaceGraph;


/**
 * Created by me on 9/19/16.
 */
public class TopCraft extends NAgentX {

    private final TopDownMinicraft craft;
    private final AutoclassifiedBitmap camAE;
    private float prevHealth;
    float prevScore;
    private Bitmap2DSensor<PixelBag> pixels;

    public TopCraft(NAR nar) throws Narsese.NarseseException {
        super("cra", nar);

        this.craft = new TopDownMinicraft();
        TopDownMinicraft.start(craft);
        //craft.changeLevel(1);

        PixelBag p = new PixelBag(new MonoBufImgBitmap2D(() -> craft.image), 64, 64).addActions(id, this);
        int nx = 8;
        camAE = new AutoclassifiedBitmap("cae", p.pixels, nx, nx, (subX, subY) -> {
            return new float[1]; //return new float[]{p.X, p.Y, p.Z};
        }, 8, this);
        camAE.alpha.set(0.04f);
        camAE.noise.set(0.02f);
        SpaceGraph.window(camAE.newChart(), 500, 500);
        onFrame(()->{ p.update(); });


        senseSwitch($.func("dir", id), () -> craft.player.dir, 0, 4);
        sense($.func("stamina", id), () -> (craft.player.stamina) / ((float) craft.player.maxStamina));
        sense($.func("health", id), () -> (craft.player.health) / ((float) craft.player.maxHealth));

        int tileMax = 13;
        senseSwitch("tile:here", () -> craft.player.tile().id, 0, tileMax);
        senseSwitch("tile:up", () -> craft.player.tile(0, 1).id, 0, tileMax);
        senseSwitch("tile:down", () -> craft.player.tile(0, -1).id, 0, tileMax);
        senseSwitch("tile:right", () -> craft.player.tile(1, 0).id, 0, tileMax);
        senseSwitch("tile:left", () -> craft.player.tile(-1, 0).id, 0, tileMax);

        InputHandler input = craft.input;
        actionPushButton($.func("fire", id), input.attack::pressed/*, 16*/);
        actionPushButtonMutex($.func("l", id), $.func("r", id),
                input.left::pressed, input.right::pressed);
        actionPushButtonMutex($.func("u", id), $.func("d", id),
                input.up::pressed, input.down::pressed);
        actionPushButton($.func("next", id), (i) -> {
            if (craft.menu != null) {
                input.up.pressed(false);
                input.down.pressIfUnpressed();
            }
        });
        actionToggle($.func("menu", id), input.menu::pressIfUnpressed);

        rewardNormalized("score", -1, +1, () -> {
            float nextScore = craft.frameImmediate();
            float ds = nextScore - prevScore;
            if (ds == 0)
                return Float.NaN;
            this.prevScore = nextScore;
            System.out.println("score delta:" + ds);
            return ds;
        });
        rewardNormalized("health", -1, +1, ()-> {
            float nextHealth = craft.player.health;
            float dh = nextHealth - prevHealth;
            if (dh == 0)
                return Float.NaN;
            this.prevHealth = nextHealth;
            System.out.println("health delta: " + dh);
            return dh;
            //return (craft.player.health / ((float) craft.player.maxHealth));
        });
    }

    public static void main(String[] args) {
        runRT(n -> {
            try {
                TopCraft tc = new TopCraft(n);


                return tc;
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
                return null;
            }
        }, 20);
    }


}
