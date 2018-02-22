package org.jbox2d.gui;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.EdgeShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.*;
import org.jbox2d.dynamics.joints.DistanceJoint;
import org.jbox2d.dynamics.joints.Joint;
import org.jbox2d.dynamics.joints.MouseJoint;
import org.jbox2d.dynamics.joints.MouseJointDef;
import org.jbox2d.fracture.Material;
import org.jbox2d.fracture.PolygonFixture;
import org.jbox2d.fracture.util.MyList;
import org.jbox2d.gui.fracture.*;
import org.jbox2d.gui.jbox2d.BlobTest4;
import org.jbox2d.gui.jbox2d.ChainTest;
import org.jbox2d.gui.jbox2d.TheoJansen;
import org.jbox2d.gui.jbox2d.VerletTest;
import spacegraph.math.Tuple2f;
import spacegraph.math.v2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

/**
 * GUI pre testovacie scenare. Ovladanie:
 * s - start
 * r - reset
 * koliecko mysi - priblizovanie/vzdalovanie
 * prave tlacitko mysi - posuvanie sceny
 * lave tlacitko mysi - hybanie objektami
 *
 * @author Marek Benovic
 */
public class Box2DTests extends JComponent implements Runnable {
    private final Dimension screenSize = new Dimension(1024, 540);
    private final Tuple2f center = new v2();
    private float zoom = 1;
    private volatile Dynamics2D w;

    private final Tuple2f startCenter = new v2();
    private volatile Point clickedPoint = null;
    private volatile Graphics2D g;
    private volatile boolean running = false;
    private volatile ICase testCase;

    private volatile Body2D ground;


    /**
     * Pole testovacich scenarov
     */
    private static final ICase[] cases = new ICase[]{

            new MainScene(),

            new Cube(),
            new Circle(),

            new RotatedBody(),
            new StaticBody(),
            new Fluid(),
            new Materials(Material.UNIFORM),
            new Materials(Material.DIFFUSION),
            new Materials(Material.GLASS),
            new ChainTest(),
            new BlobTest4(),
            new TheoJansen(),
            new VerletTest()
    };


    private Box2DTests() {
        initWorld();

        initMouse();

        zoom = 10;
        center.set(0, 7);
    }

    private volatile MouseJointDef mjdef;
    private volatile MouseJoint mj;
    private volatile boolean destroyMj = false;
    private volatile Tuple2f mousePosition = new Vec2();

    private void initMouse() {
        addMouseWheelListener((MouseWheelEvent e) -> {
            if (e.getWheelRotation() < 0) {
                zoom *= 1.25f * -e.getWheelRotation();
            } else {
                zoom /= 1.25f * e.getWheelRotation();
            }

            zoom = Math.min(zoom, 100);
            zoom = Math.max(zoom, 0.1f);
            repaint();
        });

        addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point p = e.getPoint();
                mousePosition = getPoint(p);
                if (clickedPoint != null) {
                    p.x -= clickedPoint.x;
                    p.y -= clickedPoint.y;
                    center.x = startCenter.x - p.x / zoom;
                    center.y = startCenter.y + p.y / zoom;
                } else {
                    if (mj != null) {
                        mj.setTarget(mousePosition);
                    }
                }
                if (!running) {
                    repaint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                Point p = e.getPoint();
                mousePosition = getPoint(p);
                if (!running) {
                    repaint();
                }
            }
        });

        addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();
                Point p = new Point(x, y);
                switch (e.getButton()) {
                    case 3:
                        startCenter.set(center);
                        clickedPoint = p;
                        break;
                    case 1:
                        Tuple2f v = getPoint(p);
                        /*synchronized(Tests.this)*/
                    {
                        bodyFor:
                        for (Body2D b = w.bodies(); b != null; b = b.next) {
                            for (Fixture f = b.fixtures(); f != null; f = f.next) {
                                if (f.testPoint(v)) {
                                    MouseJointDef def = new MouseJointDef();

                                    def.bodyA = ground;
                                    def.bodyB = b;
                                    def.collideConnected = true;

                                    def.target.set(v);

                                    def.maxForce = 500f * b.getMass();
                                    def.dampingRatio = 0;

                                    mjdef = def;
                                    break bodyFor;
                                }
                            }
                        }
                    }

                    break;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                //synchronized (Tests.this) {
                switch (e.getButton()) {
                    case 3:
                        clickedPoint = null;
                        break;
                    case 1:
                        if (mj != null) {
                            destroyMj = true;
                        }
                        break;
                }
                //}
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
    }

    private void initWorld() {
        w = new Dynamics2D(new v2(0, -9.81f));
        w.setParticleRadius(0.2f);
        w.setParticleDensity(1.0f);
        w.setContinuousPhysics(true);

        setBox();

        mj = null;
        destroyMj = false;
        mjdef = null;
    }

    private void setBox() {
        ground = w.addBody(new BodyDef(BodyType.STATIC),
                new FixtureDef(PolygonShape.box(40, 5),
                        0, 0));
        ground.setTransform(new v2(0, -5.0f), 0);

        Body2D wallRight = w.addBody(new BodyDef(BodyType.STATIC),
                new FixtureDef(PolygonShape.box(2, 40), 0, 0));
        wallRight.setTransform(new v2(-41, 30.0f), 0);

        Body2D wallLeft = w.addBody(new BodyDef(BodyType.STATIC),
                new FixtureDef(PolygonShape.box(2, 40), 0, 0));
        wallLeft.setTransform(new v2(41, 30.0f), 0);
    }

    private Point getPoint(Tuple2f point) {
        float x = (point.x - center.x) * zoom + (getWidth() >> 1);
        float y = (getHeight() >> 1) - (point.y - center.y) * zoom;
        return new Point((int) x, (int) y);
    }

    private Tuple2f getPoint(Point point) {
        float x = (point.x - (getWidth() >> 1)) / zoom + center.x;
        float y = ((getHeight() >> 1) - point.y) / zoom + center.y;
        return new v2(x, y);
    }

    private MyThread t = new MyThread();

    @Override
    public void run() {
        t.start();
    }

    private final int stepsInSecond = 50;
    private final int iterations = 8;
    private final int velocity = 8;
    private final int slowmotion = 1;
    private final int plynuleSlowMo = 1;

    private MyThread createThread() {
        return new MyThread();
    }

    private void setCase(ICase testcase) {
        w.invoke(() -> {
            this.testCase = testcase;
            initWorld();
            testCase.init(w);
            repaint();
        });
    }

    private class MyThread extends Thread {
        @Override
        public void run() {
            for (; ; ) {
                long l1 = System.nanoTime();
                //synchronized(Tests.this) {
                if (running) {
                    w.step(1.0f / stepsInSecond / plynuleSlowMo, velocity, iterations);
                }
                if (destroyMj) {
                    if (mj.getBodyA().fixtureCount > 0 && mj.getBodyB().fixtureCount > 0) {
                        w.removeJoint(mj);
                    }
                    mj = null;
                    destroyMj = false;
                }
                if (mjdef != null && mj == null) {
                    mj = (MouseJoint) w.addJoint(mjdef);
                    mjdef.bodyA.setAwake(true);
                    mjdef = null;
                }
                //}

                repaint();

                long l3 = System.nanoTime();
                int fulltime = (int) ((double) (l3 - l1) / 1000000);

                try {
                    int interval = (int) (1000.0f / stepsInSecond * slowmotion);
                    interval -= fulltime;
                    interval = Math.max(interval, 0);
                    Thread.sleep(interval);
                } catch (InterruptedException ex) {
                    break;
                }
            }
        }
    }

    private void drawJoint(Joint joint) {
        g.setColor(Color.GREEN);
        Tuple2f v1 = new Vec2();
        Tuple2f v2 = new Vec2();
        switch (joint.getType()) {
            case DISTANCE:
                DistanceJoint dj = (DistanceJoint) joint;
                v1 = joint.getBodyA().getWorldPoint(dj.getLocalAnchorA());
                v2 = joint.getBodyB().getWorldPoint(dj.getLocalAnchorB());
                break;
            case MOUSE:
                MouseJoint localMj = (MouseJoint) joint;
                localMj.getAnchorA(v1);
                localMj.getAnchorB(v2);
                break;
        }
        Point p1 = getPoint(v1);
        Point p2 = getPoint(v2);
        g.drawLine(p1.x, p1.y, p2.x, p2.y);
    }

    private void drawParticles() {
        Tuple2f[] vec = w.getParticlePositionBuffer();
        if (vec == null) {
            return;
        }
        g.setColor(Color.MAGENTA);
        float radius = w.getParticleRadius();
        int size = w.getParticleCount();
        for (int i = 0; i < size; i++) {
            Tuple2f vx = vec[i];
            Point pp = getPoint(vx);
            float r = radius * zoom;

            if (r < 0.5f) {
                g.drawLine(pp.x, pp.y, pp.x, pp.y); //ak je zoom priliz maly, tak by kvapalinu nezobrazilo
            } else {
                //int radInt = Math.round(r * 2);
                g.fillOval(pp.x - (int) r, pp.y - (int) r, (int) (r * 2), (int) (r * 2));
            }
        }
    }

    final int MAX_POLY_EDGES = 32;
    private final int x[] = new int[MAX_POLY_EDGES];
    private final int y[] = new int[MAX_POLY_EDGES];

    private void drawBody(Body2D body) {
        if (body.getType() == BodyType.DYNAMIC) {
            g.setColor(Color.LIGHT_GRAY);
        } else {
            g.setColor(Color.GRAY);
        }
        Tuple2f v = new Vec2();
        MyList<PolygonFixture> generalPolygons = new MyList<>();
        for (Fixture f = body.fixtures; f != null; f = f.next) {
            PolygonFixture pg = f.polygon;
            if (pg != null) {
                if (!generalPolygons.contains(pg)) {
                    generalPolygons.add(pg);
                }
            } else {
                Shape shape = f.shape();
                switch (shape.m_type) {
                    case POLYGON:
                        PolygonShape poly = (PolygonShape) shape;
                        for (int i = 0; i < poly.vertices; ++i) {
                            body.getWorldPointToOut(poly.vertex[i], v);
                            Point p = getPoint(v);
                            x[i] = p.x;
                            y[i] = p.y;
                        }
                        g.fillPolygon(x, y, poly.vertices);
                        break;
                    case CIRCLE:
                        CircleShape circle = (CircleShape) shape;
                        float r = circle.radius;
                        body.getWorldPointToOut(circle.center, v);
                        Point p = getPoint(v);
                        int wr = (int) (r * zoom);
                        g.fillOval(p.x - wr, p.y - wr, wr * 2, wr * 2);
                        break;
                    case EDGE:
                        EdgeShape edge = (EdgeShape) shape;
                        Tuple2f v1 = edge.m_vertex1;
                        Tuple2f v2 = edge.m_vertex2;
                        Point p1 = getPoint(v1);
                        Point p2 = getPoint(v2);
                        g.drawLine(p1.x, p1.y, p2.x, p2.y);
                        break;
                }
            }
        }

        if (generalPolygons.size() != 0) {
            PolygonFixture[] polygonArray = generalPolygons.toArray(new PolygonFixture[generalPolygons.size()]);
            for (PolygonFixture poly : polygonArray) {
                int n = poly.size();
                int x[] = new int[n];
                int y[] = new int[n];
                for (int i = 0; i < n; ++i) {
                    body.getWorldPointToOut(poly.get(i), v);
                    Point p = getPoint(v);
                    x[i] = p.x;
                    y[i] = p.y;
                }
                g.fillPolygon(x, y, n);
            }
        }
    }

    final static Color clearColor = new Color(0, 0, 0);

    @Override
    public void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        if (w == null) {
            return;
        }

        BufferedImage bi = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        g = (Graphics2D) bi.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
//        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

        //predpripravi scenu

        int fixtures = 0;

        /*synchronized(this)*/
        {
            g.setColor(clearColor);
            //g.fillRect(0, 0, getWidth(), getHeight());
            g.clearRect(0, 0, getWidth(), getHeight());

            //vykresli particles
            drawParticles();

            //vykresli tuhe telesa
            for (Body2D b = w.bodies(); b != null; b = b.next()) {
                drawBody(b);
                fixtures += b.fixtureCount;
            }

            //vykresli joiny
            for (Joint j = w.joints(); j != null; j = j.next) {
                drawJoint(j);
            }
        }

        //text
        {
//            g.setColor(Color.DARK_GRAY);
            g.setFont(new Font("Courier New", Font.BOLD, 16));
            g.setColor(Color.ORANGE);
            g.drawString("s - start/stop", 20, 20);
            g.drawString("r - reset", 20, 40);
            g.setColor(Color.ORANGE);
            g.drawString("Mouse position:  [" + mousePosition.x + ", " + mousePosition.y + ']', 20, 60);
            g.drawString("Screen position: [" + center.x + ", " + center.y + ']', 20, 80);
            g.drawString("Zoom:      " + zoom, 20, 100);
            g.drawString("Bodies:    " + w.getBodyCount(), 20, 120);
            g.drawString("Fixtures:  " + fixtures, 20, 140);
            g.drawString("Contacts:  " + w.getContactCount(), 20, 160);
            g.drawString("Particles: " + w.getParticleCount(), 20, 180);
        }

//        g.setFont(new Font("Courier New", Font.BOLD, 16));
//        g.drawString("Marek Beňovič © 2015", 20, getHeight() - 20);

        graphics.drawImage(bi, 0, 0, null);
    }

    @Override
    public Dimension getPreferredSize() {
        return screenSize;
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    /**
     * Primarny kod spustajuci framework pre testovacie scenare.
     *
     * @param args
     */
    public static void main(String args[]) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Tests");
            frame.setBackground(Color.BLACK);
            frame.getContentPane().setBackground(Color.BLACK);
            frame.setIgnoreRepaint(true);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            Container pane = frame.getContentPane();

            pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));

            String[] caseNames = new String[cases.length];
            for (int i = 0; i < cases.length; ++i) {
                caseNames[i] = (i + 1) + ". " + cases[i];
            }

            JComboBox petList = new JComboBox(caseNames);
            pane.add(petList);

            Dimension dimMax = petList.getMaximumSize();
            petList.setMaximumSize(new Dimension(dimMax.width, 30));

            Box2DTests canvas = new Box2DTests();

            petList.addActionListener(e -> {

                JComboBox cb = (JComboBox) e.getSource();
                int index = cb.getSelectedIndex();
                canvas.setCase(cases[index]);
                pane.requestFocusInWindow();
            });

            canvas.setAlignmentX(Component.CENTER_ALIGNMENT);
            canvas.setIgnoreRepaint(true);
            pane.add(canvas);

            canvas.setCase(cases[0]);

            pane.setFocusable(true);
            pane.requestFocusInWindow();

            pane.addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent e) {
                }

                @Override
                public void keyPressed(KeyEvent e) {
                    switch (e.getKeyChar()) {
                        case 's':
                            canvas.running = !canvas.running;
                            break;
                        case 'r':
                            try { //pockam, kym vlakno dobehne (robilo to nejake problemy s logami)
                                canvas.t.interrupt();
                                canvas.t.join();
                            } catch (InterruptedException ex) {
                            }
                            canvas.t = canvas.createThread();
                            canvas.initWorld();
                            canvas.testCase.init(canvas.w);
                            canvas.t.start();
                            break;
                    }

                }

                @Override
                public void keyReleased(KeyEvent e) {
                }
            });

            canvas.run();

            frame.pack();
            frame.setVisible(true);
        });
    }
}