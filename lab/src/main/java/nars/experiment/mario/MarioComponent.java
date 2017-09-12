package nars.experiment.mario;

import nars.experiment.mario.level.LevelGenerator;
import nars.experiment.mario.sprites.Mario;
import spacegraph.audio.Audio;
import spacegraph.audio.FakeSoundEngine;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.util.Random;


public class MarioComponent extends JComponent implements Runnable, KeyListener, FocusListener {

    int fps = 24;

    public void startGame() {
//        scene = mapScene;
//        mapScene.startMusic();
//        mapScene.init();

        startLevel((int) (Math.random() * 50000), 1,
                Math.random() > 0.5f ? LevelGenerator.TYPE_UNDERGROUND
                        :
                        LevelGenerator.TYPE_OVERGROUND
        );
    }

    private static final long serialVersionUID = 739318775993206607L;

    private boolean running;
    private final int width;
    private final int height;
    private GraphicsConfiguration graphicsConfiguration;
    public Scene scene;
    private Audio sound;
    @SuppressWarnings("unused")
    private boolean focused;
    private MapScene mapScene;
    int delay;
    public BufferedImage image;

    public MarioComponent(int width, int height) {
        this.setFocusable(true);
        this.setEnabled(true);
        this.width = width;
        this.height = height;

        Dimension size = new Dimension(width, height);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);

//        try
//        {
//            sound = new Audio(64);
//        }
//        catch (LineUnavailableException e)
//        {
//            e.printStackTrace();
        try {
            sound = new FakeSoundEngine(64);
        } catch (LineUnavailableException e1) {
            e1.printStackTrace();
        }
//        }

        setFocusable(true);
    }

    private void toggleKey(int keyCode, boolean isPressed) {
        if (keyCode == KeyEvent.VK_LEFT) {
            scene.key(Mario.KEY_LEFT, isPressed);
        }
        if (keyCode == KeyEvent.VK_RIGHT) {
            scene.key(Mario.KEY_RIGHT, isPressed);
        }
        if (keyCode == KeyEvent.VK_DOWN) {
            scene.key(Mario.KEY_DOWN, isPressed);
        }
        if (keyCode == KeyEvent.VK_UP) {
            scene.key(Mario.KEY_UP, isPressed);
        }
        if (keyCode == KeyEvent.VK_A) {
            scene.key(Mario.KEY_SPEED, isPressed);
        }
        if (keyCode == KeyEvent.VK_S) {
            scene.key(Mario.KEY_JUMP, isPressed);
        }
        if (isPressed && keyCode == KeyEvent.VK_ESCAPE) {
//        	if (scene instanceof TitleScene)
//        	{
//        		System.exit(0);
//        	}
//        	else if (scene instanceof MapScene)
//        	{
            toTitle();
//        	}
//        	else
//        	{
//        		scene = mapScene;
//                mapScene.startMusic();
//        	}
//
        }
    }

    @Override
    public void paint(Graphics g) {
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

    @Override
    public void run() {
        graphicsConfiguration = getGraphicsConfiguration();

        mapScene = new MapScene(graphicsConfiguration, this, new Random().nextLong());
        scene = mapScene;
        scene.setSound(sound);

        Art.init(graphicsConfiguration);

        image = new BufferedImage(320, 240, BufferedImage.TYPE_4BYTE_ABGR); //createVolatileImage(320, 240);
        image.setAccelerationPriority(1f);
        Graphics g = getGraphics();
        Graphics og = image.getGraphics();

        double time = System.nanoTime() / 1000000000.0;
        double now = time;
        long tm = System.currentTimeMillis();
        long lTick = tm;

        addKeyListener(this);
        addFocusListener(this);

        toTitle();
        adjustFPS();

        while (running) {
            lTick = tm;
            scene.tick();

            float alpha = (float) (System.currentTimeMillis() - lTick);
            sound.clientTick(alpha);

            @SuppressWarnings("unused")
            int x = (int) (Math.sin(now) * 16 + 160);
            @SuppressWarnings("unused")
            int y = (int) (Math.cos(now) * 16 + 120);

            og.fillRect(0, 0, 320, 240);

            alpha = 0;
            scene.render(og, alpha);

            if (lTick / 4 % 2 == 0 && (scene instanceof TitleScene)) {
                String msg = "INSERT COIN";

                //drawString(og, msg, 160 - msg.length() * 4 + 1, 110 + 1, 0);
                //drawString(og, msg, 160 - msg.length() * 4, 110, 7);
            }
            og.setColor(Color.BLACK);

            if (width != 320 || height != 240) {
                g.drawImage(image, 0, 0, width, height, null);
            } else {
                g.drawImage(image, 0, 0, null);
            }

            if (delay > 0)
                try {
                    tm += delay;
                    Thread.sleep(Math.max(0, tm - System.currentTimeMillis()));
                } catch (InterruptedException e) {
                    break;
                }
        }

        Art.stopMusic();
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

    public void startLevel(long seed, int difficulty, int type) {
        scene = new LevelScene(graphicsConfiguration, this, seed, difficulty, type);
        scene.setSound(sound);
        scene.init();
    }

    public void levelFailed() {
        startGame();

//        scene = mapScene;
//        mapScene.startMusic();
//        Mario.lives--;
//        if (Mario.lives == 0)
//        {
//            lose();
//        }
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
        scene.setSound(sound);
        scene.init();
    }

    public void toTitle() {
        Mario.resetStatic();
//        scene = new TitleScene(this, graphicsConfiguration);
//        scene.setSound(sound);
//        scene.init();
        startGame();
    }

    public void lose() {
        scene = new LoseScene(this);
        scene.setSound(sound);
        scene.init();
    }


    public void adjustFPS() {

        delay = (fps > 0) ? (fps >= 100) ? 0 : (1000 / fps) : 100;
//        System.out.println("Delay: " + delay);
    }
}