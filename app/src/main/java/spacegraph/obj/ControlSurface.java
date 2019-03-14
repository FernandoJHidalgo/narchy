package spacegraph.obj;

import nars.$;
import nars.util.Texts;
import nars.util.data.list.FasterList;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.infinispan.cdi.common.util.Reflections;
import spacegraph.Facial;
import spacegraph.SpaceGraph;
import spacegraph.Surface;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Generic widget control panel for arbitrary POJOs
 */
public class ControlSurface extends PanelSurface {

    private static final int DEFAULT_DEPTH = 2;
    /** the object being controlled */
    public Object o;


    final IdentityHashMap built;

    public static void newControlWindow(Object... oo) {
        SpaceGraph<?> s = new SpaceGraph();
        for (Object o : oo) {
            s.add(new RectWidget(
                    new ControlSurface(o), 4f /* width */, 4f /* height */
            ));
        }

        //s.add(new Facial(new ConsoleSurface(new ConsoleSurface.DummyTerminal(80, 25))).scale(500f, 400f));
        s.add(new Facial(new CrosshairSurface(s)));
        s.show(1200, 800);
    }

    public ControlSurface(Object o) {
        this(o.toString(), o, DEFAULT_DEPTH,  null);
    }

    public ControlSurface(Object label, Object o, int maxDepth, IdentityHashMap built) {
        super(label.toString(), new GridSurface());

        this.built = built == null ?  new IdentityHashMap() : built;

        this.o = o;

        this.built.put(o, o);



        bottom().setChildren(build(label, o, maxDepth, this.built));
    }



    protected Surface build(Object k, Object v, int remainingDepth, IdentityHashMap built) {


//        ConsoleSurface vc = new ConsoleSurface(24, 4);
//        try {
//            vc.term.putLine(k + "\n  " + v);
//        } catch (IOException e) {
//
//        }
        if (v instanceof Surface) {
            return ((Surface) v);
        } else if (v instanceof MutableFloat) {
            return build(k, (MutableFloat)v);
        } else if (v instanceof Iterable) {
            return build((Iterable)v, remainingDepth, built);
        } else if (v.getClass().isPrimitive()) {
            return new LabelSurface(k.toString());
        } else {
            if (v == o) {
                return new GridSurface(children(o, remainingDepth, built));
            } else if (remainingDepth > 1) {
                return new ControlSurface(k, v, remainingDepth, built);
            }
        }

        //else..
        return new LabelSurface(k + ":" + v);



        //return new GridSurface(
                //vc.term.putLine(k + "\n  " + v);
                //vc );

    }

    private Surface build(Object k, MutableFloat f) {
//
//        if (k instanceof Field) {
//            System.out.println(k);
//        }

        //
        // return new SliderSurface(0.5f, 0, 1);

        return new LabelSurface(k + " " + Texts.n4(f.floatValue()));
//        int ki = 0;
//        for (Object x : c) {
//            g.children.add(build(ki, x, remainingDepth, built));
//            ki++;
//        }
//        return g;
    }

    private Surface build(Iterable c, int remainingDepth, IdentityHashMap built) {
        GridSurface g = new GridSurface();
        int ki = 0;
        for (Object x : c) {
            g.children.add(build(ki, x, remainingDepth, built));
            ki++;
        }
        g.layout();
        return g;
    }

    private FasterList<Surface> children(Object V, int remainingDepth, IdentityHashMap built) {


        Class<?> aClass = o.getClass();
//        Map fields = OgnlRuntime.getFields(aClass);
//        try {
//
//            Iterator z = OgnlRuntime.getElementsAccessor(aClass).getElements(o).asIterator();
//            while (z.hasNext()) {
//                System.out.println(z.next());
//            }
//
//        } catch (OgnlException e) {
//            e.printStackTrace();
//        }
//        fields.forEach((k,v) -> {
//            w.addIfNotNull(field(remainingDepth-1, k, v, built));
//        });

        FasterList<Surface> w = $.newArrayList();

        Set<Field> fields = Reflections.getAllDeclaredFields(aClass);
        for (Field f : fields) {

            if (f.getDeclaringClass()!=Object.class) {
                if (Modifier.isPublic(f.getModifiers())) {
                    try {
                        w.addIfNotNull(field(remainingDepth - 1, f, f.get(V), built));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        try {
            Set<Method> methods = Reflections.getAllDeclaredMethods(aClass);
            methods.forEach((m) -> {
                if (m.getDeclaringClass() != Object.class) {
                    if (Modifier.isPublic(m.getModifiers())) {
                        w.addIfNotNull(method(remainingDepth - 1, m.getName(), m, built));
                    }
                }
            });
        } catch (NoClassDefFoundError e) {

        }

        return w;

    }

    private Surface field(int remainingDepth, Object k, Object v, IdentityHashMap built) {
        if (v == null)
            return new LabelSurface("null");
        if (alreadyAdded(remainingDepth, v, built)) return null;
        return build(k, v, remainingDepth, built);
    }


    private Surface method(int remainingDepth, Object k, Object v, IdentityHashMap built) {
        if (alreadyAdded(remainingDepth, v, built)) return null;
        return build(k, v, remainingDepth, built);
    }


    private synchronized boolean alreadyAdded(int remainingDepth, Object v, IdentityHashMap built) {
        if (remainingDepth <= 0)
            return true;
        if (v == null)
            return true;
        return built.putIfAbsent(v, v) != null;
    }

}
