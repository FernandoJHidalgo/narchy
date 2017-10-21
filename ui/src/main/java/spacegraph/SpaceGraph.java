package spacegraph;

import com.jogamp.nativewindow.util.Point;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL2;
import jcog.list.FasterList;
import jcog.map.MRUCache;
import org.eclipse.collections.api.block.procedure.primitive.IntObjectProcedure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.FPSLook;
import spacegraph.input.KeyXYZ;
import spacegraph.input.OrbMouse;
import spacegraph.math.v3;
import spacegraph.phys.constraint.BroadConstraint;
import spacegraph.render.JoglPhysics;
import spacegraph.render.JoglSpace;
import spacegraph.render.SpaceGraphFlat;
import spacegraph.space.ListSpace;
import spacegraph.widget.meta.ReflectionSurface;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by me on 6/20/16.
 */
public class SpaceGraph<X> extends JoglPhysics<X> {


    final List<Ortho> orthos = new FasterList<>(1);

    final List<AbstractSpace<X, Spatial<X>>> inputs = new FasterList<>(1);

    final MRUCache<X, Spatial<X>> atoms;

    final List<Ortho> preAdd = new FasterList();
    final List<Consumer<SpaceGraph>> frameListeners = new FasterList();
    public int windowX, windowY;

    @Override
    public void windowDestroyed(WindowEvent windowEvent) {
        super.windowDestroyed(windowEvent);
        atoms.clear();//invalidateAll();
        orthos.clear();
        inputs.clear();
        frameListeners.clear();
        preAdd.clear();
    }


    /**
     * number of items that will remain cached, some (ideally most)
     * will not be visible but once were and may become visible again
     */
    public SpaceGraph() {
        super();

        final int MAX_ATOMS = 4096; //TODO make parameter
        atoms = new MRUCache<>(MAX_ATOMS) {
            @Override
            protected void onEvict(Map.Entry<X, Spatial<X>> entry) {
                SpaceGraph.this.remove(entry.getValue());
            }
        };
//        Cache<X, Spatial<X>> atoms =
//                //new NonBlockingHashMap(cacheCapacity);
//                //new ConcurrentHashMap<>(cacheCapacity);
//                Caffeine.newBuilder()
//                        //.softValues().builder();
//                        .removalListener((X k, Spatial<X> v, RemovalCause c) -> {
//                            if (v!=null)
//                                v.delete(dyn);
//                        })
//                        //.maximumSize(cacheCapacity)
//                        .weakValues()
//                        .build();
//        this.atoms = atoms;

    }


    public SpaceGraph(AbstractSpace<X, ?>... cc) {
        this();

        for (AbstractSpace c : cc)
            add(c);
    }

    public SpaceGraph(Spatial<X>... cc) {
        this();

        add(cc);
    }


    public void addFrameListener(Consumer<SpaceGraph> f) {
        frameListeners.add(f);
    }

    public void removeFrameListener(Consumer<SpaceGraph> f) {
        frameListeners.remove(f);
    }


    public SpaceGraph ortho(Surface ortho) {
        return add(new Ortho(ortho).maximize());
    }

    public SpaceGraph add(Ortho c) {
        if (window == null) {
            preAdd.add(c);
        } else {
            _add(c);
        }
        return this;
    }

    private void _add(Ortho c) {
        this.orthos.add(c);
        c.start(this);
    }

    public SpaceGraph add(AbstractSpace<X, Spatial<X>> c) {
        if (inputs.add(c))
            c.start(this);
        return this;
    }

    public void removeSpace(AbstractSpace<X, ?> c) {
        if (inputs.remove(c)) {
            c.stop();
        }
    }


    public <Y extends Spatial<X>> Y getOrAdd(X x, Function<X,Y> materializer) {
        //Spatial y = atoms.get(x, materializer);
        Spatial y = atoms.computeIfAbsent(x, materializer);
        y.activate();
        return (Y) y;
    }

    public <Y extends Spatial<X>> Y get(X x) {
        //Spatial y = atoms.getIfPresent(x);
        Spatial y = atoms.get(x);
        if (y != null)
            y.activate();
        return (Y) y;
    }

    final Queue<Spatial> toRemove = new ConcurrentLinkedQueue();

    public void remove(X x) {
        Spatial<X> y = atoms.remove(x);
        if (y != null) {
            remove(y);
        }
    }

    public void remove(Spatial<X> y) {
        toRemove.add(y);
    }


//    public @Nullable Spatial getIfActive(X t) {
//        Spatial v = atoms.getIfPresent(t);
//        return v != null && v.preactive() ? v : null;
//    }


    public SpaceGraph setGravity(v3 v) {
        dyn.setGravity(v);
        return this;
    }


    public static float r(float range) {
        return (-0.5f + (float) Math.random()) * 2f * range;
    }


    @Override
    public void init(GL2 gl) {
        super.init(gl);

        for (Ortho f : preAdd) {
            _add(f);
        }
        preAdd.clear();


        initInput();
        updateWindowInfo();

    }


    protected void initInput() {

        //default 3D input controls
        addMouseListener(new FPSLook(this));
        addMouseListener(new OrbMouse(this));
        addKeyListener(new KeyXYZ(this));

    }


    @Override
    final public void forEachIntSpatial(IntObjectProcedure<Spatial<X>> each) {
        int n = 0;
        for (int i = 0, inputsSize = inputs.size(); i < inputsSize; i++) {
            n += inputs.get(i).forEachWithInt(n, each);
        }
    }

    @Override
    protected void render() {
        super.render();
        renderHUD();
    }

    @Override
    protected void update() {

        toRemove.forEach(x -> x.delete(dyn));
        toRemove.clear();

        this.inputs.forEach(this::update);

        super.update();

        frameListeners.forEach(f -> f.accept(this));
    }

    protected void renderHUD() {

        int facialsSize = orthos.size();
        if (facialsSize > 0) {

            ortho();

            gl.glDisable(GL2.GL_DEPTH_TEST);

            GL2 gl = this.gl;
            for (int i = 0; i < facialsSize; i++) {
                orthos.get(i).render(gl);
            }

            gl.glEnable(GL2.GL_DEPTH_TEST);
        }
    }


    final void update(AbstractSpace<X, Spatial<X>> s) {

        s.forEach(x -> x.update(dyn));

        s.update(this);

    }

    public String summary() {
        return this.atoms.size() + " cached; " + "\t" + dyn.summary();
    }

//    void print(AbstractSpace s) {
//        System.out.println();
//        //+ active.size() + " active, "
//        System.out.println(s + ": " + this.atoms.estimatedSize() + " cached; " + "\t" + dyn.summary());
//        /*s.forEach(System.out::println);
//        dyn.objects().forEach(x -> {
//            System.out.println("\t" + x.getUserPointer());
//        });*/
//        System.out.println();
//    }

    public ListSpace<X, ?> add(Spatial<X>... s) {
        ListSpace<X, Spatial<X>> l = new ListSpace<>(s);
        add(l);
        return l;
    }

//    @Override
//    public void windowGainedFocus(WindowEvent windowEvent) {
//        updateWindowInfo();
//    }

    @Override
    public void windowResized(WindowEvent windowEvent) {
        updateWindowInfo();
    }

    @Override
    public void windowMoved(WindowEvent windowEvent) {
        updateWindowInfo();
    }

    private AtomicBoolean gettingScreenPointer = new AtomicBoolean(false);

    private void updateWindowInfo() {
        GLWindow rww = window;
        if (rww == null)
            return;
        if (!rww.isRealized() || !rww.isVisible() || !rww.isNativeValid()) {
            return;
        }

        if (gettingScreenPointer.compareAndSet(false, true)) {

            window.getScreen().getDisplay().getEDTUtil().invoke(false, () -> {
                try {
                    Point p = rww.getLocationOnScreen(new Point());
                    windowX = p.getX();
                    windowY = p.getY();
                } finally {
                    gettingScreenPointer.set(false);
                }
            });
        }
    }

    public static SpaceGraph window(Surface s, int w, int h) {
        SpaceGraph win = new SpaceGraphFlat(
                new ZoomOrtho(s)
                        //.scale(Math.min(w,h))
                        .maximize()
        );
        if (w > 0 && h > 0) {

            win.show(w, h);
        }
        return win;
    }

    public static SpaceGraph window(Object o, int w, int h) {
        if (o instanceof SpaceGraph) {
            SpaceGraph s = (SpaceGraph) o;
            s.show(w, h);
            return s;
        } else if (o instanceof Spatial) {
            return window(((Spatial) o), w, h);
        } else if (o instanceof Surface) {
            return window(((Surface) o), w, h);
        } else {
            return window(new ReflectionSurface(o), w, h);
        }
    }

    public static SpaceGraph window(Spatial s, int w, int h) {
        return window(w, h, s);
    }

    public static SpaceGraph window(int w, int h, Spatial... s) {
        SpaceGraph win = new SpaceGraph(s);
        win.show(w, h);
        return win;
    }

    @Deprecated
    public SpaceGraph with(BroadConstraint b) {
        dyn.addBroadConstraint(b);
        return this;
    }

    public JoglSpace camPos(float x, float y, float z) {
        camPos.set(x, y, z);
        return this;
    }


    //    public static class PickDragMouse extends SpaceMouse {
//
//        public PickDragMouse(JoglPhysics g) {
//            super(g);
//        }
//    }
//    public static class PickZoom extends SpaceMouse {
//
//        public PickZoom(JoglPhysics g) {
//            super(g);
//        }
//    }

}
