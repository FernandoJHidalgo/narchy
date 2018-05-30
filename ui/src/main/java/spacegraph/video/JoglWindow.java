package spacegraph.video;

import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.AnimatorBase;
import jcog.Util;
import jcog.data.map.ConcurrentFastIteratingHashSet;
import jcog.event.ListTopic;
import jcog.event.On;
import jcog.event.Topic;
import jcog.exe.Exe;
import jcog.exe.InstrumentedLoop;
import jcog.exe.Loop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.util.animate.Animated;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;


public abstract class JoglWindow implements GLEventListener, WindowListener {


    /**
     * JOGL default is 10ms; we dont need/want it that often
     */
    private static final long EDT_POLL_PERIOD_MS = 20;


    private static final Collection<JoglWindow> windows = new ConcurrentFastIteratingHashSet<>(new JoglWindow[0]);
    public final Topic<JoglWindow> onUpdate = new ListTopic<>();
    public final Logger logger;
    final AtomicBoolean rendering = new AtomicBoolean(false);

    /**
     * update loop
     */
    final InstrumentedLoop updater;
    final ConcurrentLinkedQueue<Consumer<JoglWindow>> preRenderTasks = new ConcurrentLinkedQueue();
    public float renderFPS = 30f;
    public volatile GLWindow window;
    public GL2 gl;
    /**
     * update time since last cycle (S)
     */
    public float dtS = 0;
    protected float updateFPS = 30f;
    /**
     * render loop
     */
    protected GameAnimatorControl renderer;
    /**
     * update time since last cycle (ms)
     */
    protected long dtMS = 0;
    private long lastRenderMS = System.currentTimeMillis();
    private volatile int nx, ny, nw, nh;
    private final Consumer<JoglWindow> windowUpdater = (s) -> {
        GLWindow w = window;
        if (nx != w.getX() || ny != w.getY())
            w.setPosition(nx, ny);
        int nw = this.nw;
        int nh = this.nh;

        if (nw == 0 || nh == 0) {
            if (w.isVisible()) {
                w.setVisible(false);
                return;
            }
        } else {
            if (!w.isVisible())
                w.setVisible(true);
        }

        if (nw != w.getSurfaceWidth() || nh != w.getSurfaceHeight()) {
            w.setSize(nw, nh);
        }
    };

    protected JoglWindow() {
        logger = LoggerFactory.getLogger(toString());

        renderer = new GameAnimatorControl();
        updater = new InstrumentedLoop() {
            @Override
            public boolean next() {
                return JoglWindow.this.next();
            }
        };
    }

    static GLWindow window() {
        return window(config());
    }






    static GLWindow window(GLCapabilitiesImmutable config) {
        GLWindow w = GLWindow.create(config);

        

        return w;
    }

    static GLCapabilitiesImmutable config() {


        GLCapabilities config = new GLCapabilities(

                
                GLProfile.get(GLProfile.GL2)
                

        );












        config.setStencilBits(1);





        return config;
    }

    public void off() {
        GLWindow w = this.window;
        if (w != null)
            Exe.invokeLater(w::destroy);
    }

    public final void pre(Consumer<JoglWindow> beforeNextRender) {
        preRenderTasks.add(beforeNextRender);
    }

    abstract protected void init(GL2 gl);

    public void printHardware() {
        
        
        System.err.print("GL:");
        System.err.println(gl);
        System.err.print("GL_VERSION=");
        System.err.println(gl.glGetString(GL.GL_VERSION));
        System.err.print("GL_EXTENSIONS: ");
        System.err.println(gl.glGetString(GL.GL_EXTENSIONS));
    }

    public final int getWidth() {
        return nw;
    }

    public final int getHeight() {
        return nh;
    }

    public final int getX() {
        return nx;
    }

    public final int getY() {
        return ny;
    }

    @Override
    public void dispose(GLAutoDrawable arg0) {
    }

    @Override
    public void windowResized(WindowEvent windowEvent) {
        GLWindow w = this.window;
        nw = w.getSurfaceWidth();
        nh = w.getSurfaceHeight();
    }

    @Override
    public void windowMoved(WindowEvent windowEvent) {
        
        
        
    }

    @Override
    public void windowDestroyNotify(WindowEvent windowEvent) {
        renderer.stop();
        updater.stop();
    }

    @Override
    public void windowDestroyed(WindowEvent windowEvent) {
        windows.remove(this);
        
    }

    @Override
    public void windowGainedFocus(WindowEvent windowEvent) {

    }

    @Override
    public void windowLostFocus(WindowEvent windowEvent) {

    }

    @Override
    public void windowRepaint(WindowUpdateEvent windowUpdateEvent) {

    }

    /**
     * dtMS - time transpired since last call (millisecons)
     *
     * @param dtMS
     */
    abstract protected void render(int dtMS);

    public boolean next() {
        if (window.isVisible()) {
            long cycleTimeNS = updater.cycleTimeNS;
            this.dtMS = cycleTimeNS / 1_000_000;
            this.dtS = cycleTimeNS / 1E9f;
            onUpdate.emit(this);
        }
        return true;
    }

    /**
     * dt in milliseconds since last update
     */
    public long dtMS() {
        return dtMS;
    }

    @Override
    public final void display(GLAutoDrawable drawable) {
        if (gl == null)
            gl = drawable.getGL().getGL2(); 

        rendering.set(true);
        try {
            long nowMS = System.currentTimeMillis(), renderDTMS = nowMS - lastRenderMS;
            if (renderDTMS > Integer.MAX_VALUE) renderDTMS = Integer.MAX_VALUE;
            this.lastRenderMS = nowMS;

            render((int) renderDTMS);
        } finally {
            rendering.set(false);
        }
    }

    public void show(int w, int h, boolean async) {
        show("", w, h, async);
    }

    public void show(String title, int w, int h, int x, int y, boolean async) {

        Exe.invokeLater(() -> {

            if (window != null) {
                
                return;
            }

            GLWindow W = window();
            this.window = W;

            window.getScreen().getDisplay().getEDTUtil().setPollPeriod(EDT_POLL_PERIOD_MS);


            windows.add(this);

            window.addGLEventListener(this);
            window.addWindowListener(this);


            W.setTitle(title);
            if (x != Integer.MIN_VALUE) {
                setPositionAndSize(x, y, w, h);
            } else {
                setSize(w, h);
            }
        });

        if (!async) {
            Thread.yield();

            
            while (gl == null) {
                Util.sleep(10);
            }
        }

    }

    public void setVisible(boolean b) {
        if (!b)
            setSize(0, 0);
        else {
            int nw = this.nw, nh = this.nh;
            if (nw == 0 || nh == 0) {
                nw = nh = 100; 
            }
            setSize(nw, nh);
        }
    }

    public void setPosition(int x, int y) {
        setPositionAndSize(x, y, nw, nh);
    }

    public void setSize(int w, int h) {
        setPositionAndSize(nx, ny, w, h);
    }

    public void setPositionAndSize(int x, int y, int w, int h) {

        if (window == null) return; 

        boolean change = false;
        if (change |= (window.getX() != x))
            nx = x;
        if (change |= (window.getY() != y))
            ny = y;
        if (change |= (window.getWidth() != w))
            nw = w;
        if (change |= (window.getHeight() != h))
            nh = h;

        if (change)
            pre(windowUpdater);

    }

    @Override
    public final void init(GLAutoDrawable drawable) {

        GL2 gl = drawable.getGL().getGL2();

        if (gl.getGLProfile().isHardwareRasterizer()) {
            
            gl.setSwapInterval(1); 
        } else {
            gl.setSwapInterval(4); 
        }

        

        renderer.add(window);

        Draw.init(gl);

        init(gl);

        this.gl = window.getGL().getGL2();

        updater.runFPS(updateFPS);

    }

    public void setFPS(float render, float update) {
        
        logger.info("fps render={} update={}", render, update);
        renderFPS = render;
        updateFPS = update;
        if (updater.isRunning()) {
            renderer.loop.runFPS(renderFPS);
            updater.runFPS(updateFPS);
        }
        
    }

    public void show(String title, int w, int h, boolean async) {
        show(title, w, h, Integer.MIN_VALUE, Integer.MIN_VALUE, async);
    }

    public void addMouseListenerPost(MouseListener m) {
        window.addMouseListener(m);
    }

    public void addMouseListenerPre(MouseListener m) {
        window.addMouseListener(0, m);
    }

    public void addWindowListener(WindowListener m) {
        window.addWindowListener(m);
    }

    public void addKeyListener(KeyListener m) {
        window.addKeyListener(m);
    }





    public On onUpdate(Consumer<JoglWindow> c) {
        return onUpdate.on(c);
    }

    /**
     * adapter
     */
    public On onUpdate(Animated c) {
        return onUpdate.on((JoglWindow s) -> {
            c.animate(dtS);
        });
    }

    public On onUpdate(Runnable c) {
        return onUpdate.on((JoglWindow s) -> c.run());
    }

    /* from: Jake2's */
    class GameAnimatorControl extends AnimatorBase {
        
        public final Loop loop;
        private volatile boolean paused = true;

        GameAnimatorControl() {
            super();

            setIgnoreExceptions(false);
            setPrintExceptions(false);


            




            this.loop = new Loop() {

                @Override
                public String toString() {
                    return JoglWindow.this + ".render";
                }

                @Override
                protected void onStart() {
                    paused = false;
                }

                @Override
                public boolean next() {


                    if (window != null) {
                        if (!preRenderTasks.isEmpty()) {
                            preRenderTasks.removeIf(r -> {
                                r.accept(JoglWindow.this);
                                return true;
                            });
                        }

                        if (!paused) {
                            try {












                                if (!window.isSurfaceLockedByOtherThread()) {
                                    animThread = Thread.currentThread();
                                    drawables.forEach(GLAutoDrawable::display);
                                }

                            } catch (final UncaughtAnimatorException dre) {
                                
                                dre.printStackTrace();
                            }

                    /*else if (pauseIssued && !quitIssued) { 



                        



                        if (exclusiveContext && !drawablesEmpty) {
                            setDrawablesExclCtxState(false);
                            try {
                                display(); 
                            } catch (final UncaughtAnimatorException dre) {
                                dre.printStackTrace();
                                

                            }
                        }









                    }*/
                        }
                    }
                    return true;

                }
            };





















































            loop.runFPS(renderFPS);

            
        }

        @Override
        protected String getBaseName(String prefix) {
            return prefix;
        }

        @Override
        public final boolean start() {
            return false;
        }


        @Override
        public final boolean stop() {
            
            pause();
            loop.stop();
            return true;
        }


        @Override
        public final boolean pause() {




            paused = true;
            return true;
        }

        @Override
        public final boolean resume() {
            paused = false;
            return true;
        }

        @Override
        public final boolean isStarted() {
            return loop.isRunning();
        }

        @Override
        public final boolean isAnimating() {
            return !paused;
        }

        @Override
        public final boolean isPaused() {
            return paused;
        }


    }
























}




















































































