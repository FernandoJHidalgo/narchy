package nars.nario;


import nars.nario.sprites.Mario;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.util.Random;


public class MarioComponent extends JComponent implements Runnable, KeyListener, FocusListener {
    private static final long serialVersionUID = 739318775993206607L;
    public int TICKS_PER_SECOND = 24;

    private boolean running = false;
    GraphicsConfiguration graphicsConfiguration;
    protected Scene scene;

    private boolean focused = false;
    protected MapScene mapScene;
    private BufferedImage image;
    private int tick;
    private int renderedFrames;
    boolean antialias = false;
    private final boolean soundEnabled = false;
    private double time;
    private double now;
    private double averagePassedTime;
    private boolean naiveTiming;
    private Graphics og;
    private float alpha;
    protected BufferedImage imageBuffer;
    private final float bufferScale = 0.5f;


    public MarioComponent() {
        setFocusable(true);
        setEnabled(true);



        setFocusable(true);
    }


    protected void toggleKey(int keyCode, boolean isPressed) {
        if (keyCode == KeyEvent.VK_LEFT) {
            scene.toggleKey(Mario.KEY_LEFT, isPressed);
        }
        if (keyCode == KeyEvent.VK_RIGHT) {
            scene.toggleKey(Mario.KEY_RIGHT, isPressed);
        }
        if (keyCode == KeyEvent.VK_DOWN) {
            scene.toggleKey(Mario.KEY_DOWN, isPressed);
        }
        if (keyCode == KeyEvent.VK_UP) {
            scene.toggleKey(Mario.KEY_UP, isPressed);
        }
        if (keyCode == KeyEvent.VK_A) {
            scene.toggleKey(Mario.KEY_SPEED, isPressed);
        }
        if (keyCode == KeyEvent.VK_S) {
            scene.toggleKey(Mario.KEY_JUMP, isPressed);
        }
    }

    public BufferedImage getImage() {
        return image;
    }

    @Override
    public void paint(Graphics g) {

            /*          drawString(og, "FPS: " + fps, 5, 5, 0);
             drawString(og, "FPS: " + fps, 4, 4, 7);*/


        if (antialias) {
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        }

        if (image != null) {
            int bufferWidth = (int) (image.getWidth() * bufferScale);
            int bufferHeight = (int) (image.getHeight() * bufferScale);
            if ((imageBuffer == null) || (imageBuffer.getWidth() != bufferWidth) || (imageBuffer.getHeight() != bufferHeight)) {
                //finalized copy for image processing
                imageBuffer = new BufferedImage(bufferWidth, bufferHeight, image.getType());
            }

            if (imageBuffer != null)
                imageBuffer.getGraphics().drawImage(image, 0, 0, bufferWidth, bufferHeight, null);

            g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
        }

        renderedFrames++;


    }

    @Override
    public void update(Graphics g) {
    }

    public void start() {
        if (!running) {
            running = true;
            new Thread(this, "Game Thread").start();
        }
    }

    public void stop() {
        Art.stopMusic();
        running = false;
    }

    private boolean updateDoubleBuffer(int w, int h) {
        if ((w == 0) || (h == 0))
            return false;
        
            /*
             * if image is already compatible and optimized for current system 
             * settings, simply return it
             */
        if ((image != null) /*&& (image.getColorModel().equals(gfx_config.getColorModel()))*/ && (image.getWidth() == w) && (image.getHeight() == h)) {
            //use existing image
        } else {
            // obtain the current system graphical settings
            GraphicsConfiguration gfx_config = GraphicsEnvironment.
                    getLocalGraphicsEnvironment().getDefaultScreenDevice().
                    getDefaultConfiguration();

            image = gfx_config.createCompatibleImage(w, h);
        }

        return true;
    }

    final Runnable repaintRun = new Runnable() {

        @Override
        public void run() {
            try {
                scene.render(og, alpha);
                update();
                repaint();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    };

    public void ready() {
        toTitle();
    }

    int lastTick = -1;
    int fps = 0;

    @Override
    public void run() {
        graphicsConfiguration = getGraphicsConfiguration();

        //      scene = new LevelScene(graphicsConfiguration);
        mapScene = new MapScene(graphicsConfiguration, this, new Random().nextLong());
        scene = mapScene;

        Art.init(graphicsConfiguration);

        updateDoubleBuffer(320, 240);
        og = image.getGraphics();

        //        double lastNow = 0;
        renderedFrames = 0;

        //        double now = 0;
        //        double startTime = System.nanoTime() / 1000000000.0; 
        //        double timePerFrame = 0; 
        time = System.nanoTime() / 1000000000.0;
        now = time;
        averagePassedTime = 0;
        naiveTiming = true;

        addKeyListener(this);
        addFocusListener(this);


        ready();

//        while (running)
//        {
//            cycle();
//        }
//
//        Art.stopMusic();

    }

    public void cycle(double passedTime) {
        if (passedTime == 0) {
            //calculate on realtime
            double lastTime = time;
            time = System.nanoTime() / 1000000000.0;
            passedTime = time - lastTime;
            if (passedTime < 0)
                naiveTiming = false; // Stop relying on nanotime if it starts skipping around in time (ie running backwards at least once). This sometimes happens on dual core amds.
            averagePassedTime = averagePassedTime * 0.9 + passedTime * 0.1;

            if (naiveTiming) {
                now = time;
            } else {
                now += averagePassedTime;
            }

        } else {
            now += passedTime;
        }


        tick = (int) (now * TICKS_PER_SECOND);
        if (lastTick == -1) lastTick = tick;
        while (lastTick < tick) {
            scene.tick();
            lastTick++;

            if (lastTick % TICKS_PER_SECOND == 0) {
                fps = renderedFrames;
                renderedFrames = 0;
            }
        }

        alpha = (float) (now * TICKS_PER_SECOND - tick);
        //sound.clientTick(alpha);

        //int x = (int) (Math.sin(now) * 16 + 160);
        //int y = (int) (Math.cos(now) * 16 + 120);

        //og.setColor(Color.WHITE);
        //og.fillRect(0, 0, 320, 240);

            
            /*if (!this.hasFocus() && tick/4%2==0)             {
                String msg = "CLICK TO PLAY";

                drawString(og, msg, 160 - msg.length() * 4 + 1, 110 + 1, 0);
                drawString(og, msg, 160 - msg.length() * 4, 110, 7);
            }*/

        SwingUtilities.invokeLater(repaintRun);


    }

    protected void update() {
    }


    private void drawString(Graphics g, String text, int x, int y, int c) {
        char[] ch = text.toCharArray();
        for (int i = 0; i < ch.length; i++) {
            g.drawImage(Art.font[ch[i] - 32][c], x + i * 8, y, null);
        }
    }

    @Override
    public void keyPressed(KeyEvent arg0) {
        toggleKey(arg0.getKeyCode(), true);
    }

    @Override
    public void keyReleased(KeyEvent arg0) {
        toggleKey(arg0.getKeyCode(), false);
    }

    public LevelScene startLevel(long seed, int difficulty, int type) {
        scene = newLevel(seed, difficulty, type);
        scene.init();
        return (LevelScene) scene;
    }

    protected Scene newLevel(long seed, int difficulty, int type) {
        return new LevelScene(graphicsConfiguration, this, seed, difficulty, type);
    }

    public void levelFailed() {
        scene = mapScene;
        mapScene.startMusic();
        Mario.lives--;
        if (Mario.lives == 0) {
            lose();

        }
    }

    @Override
    public void keyTyped(KeyEvent arg0) {
    }

    @Override
    public void focusGained(FocusEvent arg0) {
        focused = true;
    }

    @Override
    public void focusLost(FocusEvent arg0) {
        focused = false;
    }

    public void levelWon() {
        scene = mapScene;
        mapScene.startMusic();
        mapScene.levelWon();
    }

    public void win() {
        scene = new WinScene(this);
        scene.init();
    }

    public void toTitle() {
        Mario.resetStatic();
        scene = new TitleScene(this, graphicsConfiguration);
        scene.init();
    }

    public void lose() {
        scene = new LoseScene(this);
        scene.init();
    }

    public void startGame() {
        scene = mapScene;
        mapScene.startMusic();
        mapScene.init();
    }
}