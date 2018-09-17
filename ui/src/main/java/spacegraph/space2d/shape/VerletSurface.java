package spacegraph.space2d.shape;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.event.Off;
import jcog.tree.rtree.rect.RectFloat2D;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.util.animate.Animated;
import spacegraph.video.Draw;
import toxi.geom.QuadtreeIndex;
import toxi.geom.Vec2D;
import toxi.physics2d.VerletParticle2D;
import toxi.physics2d.VerletPhysics2D;
import toxi.physics2d.VerletSpring2D;
import toxi.physics2d.constraints.ParticleConstraint2D;

import java.lang.ref.WeakReference;
import java.util.List;

import static org.eclipse.collections.impl.tuple.Tuples.pair;

public class VerletSurface extends Surface implements Animated {

    private Off update;

    float timeScale = 1f;

    public VerletPhysics2D physics;

    private boolean animateWhenInvisible = false;

    /**
     * constrained to the surface's rectangular bounds
     */
    private boolean bounded = true;

    public VerletSurface(float w, float h) {
        this(RectFloat2D.X0Y0WH(0, 0, w, h));
    }

    public VerletSurface() {
        this(1, 1);
    }

    public VerletSurface(RectFloat2D bounds) {
        super();


        pos(bounds);

        physics = new VerletPhysics2D(null, 2, 0);
        physics.setDrag(0.05f);

        physics.setIndex(
                new QuadtreeIndex(bounds.x - 1, bounds.y - 1, bounds.w + 1, bounds.h + 1)
                //new RTreeQuadTree()
        );

//            physics.addBehavior(new GravityBehavior2D(new Vec2D(0, 0.1f)));
    }

    @Override
    protected void starting() {
        update = root().animate(this);
    }

    @Override
    protected void stopping() {
        update.off();
        update = null;
    }

    @Override
    public boolean animate(float dt) {
        if (animateWhenInvisible || showing()) {

            if (bounded)
                physics.setBounds(bounds);
            else
                physics.setBounds(null);

            physics.update(dt * timeScale);
        }
        return true;
    }

    @Override
    protected void paint(GL2 gl, SurfaceRender surfaceRender) {
        VerletSurface.render(physics, gl);
    }

    public enum VerletSurfaceBinding {

        Center {
            @Override
            Vec2D targetVerlet(VerletParticle2D particle, Surface s) {
                return new Vec2D(s.cx(), s.cy());
            }

            @Override
            public Vec2D targetSurface(VerletParticle2D p, Surface ss) {
                return new Vec2D(p.x, p.y );
            }
        },
        NearestSurfaceEdge {
            @Override
            Vec2D targetVerlet(VerletParticle2D p, Surface ss) {
                float px = p.x;
                float L = ss.left();
                float distLeft = Math.abs(px - L);
                float R = ss.right();
                float distRight = Math.abs(px - R);
                float distLR = Math.min(distLeft, distRight);
                float py = p.y;
                float T = ss.top();
                float distTop = Math.abs(py - T);
                float B = ss.bottom();
                float distBottom = Math.abs(py - B);
                float distTB = Math.min(distTop, distBottom);

                if (distLR < distTB) {
                    //along either left or right
                    return new Vec2D((distLeft < distRight) ? L : R, Util.clamp(py, T, B));
                } else {
                    //along either top or bottom
                    return new Vec2D(Util.clamp(px, L, R), (distTop < distBottom) ? T : B);
                }
//
//                float x = px < ss.cx() ? ss.left() : ss.right();
//                float y = py < ss.cy() ? ss.top() : ss.bottom();
//                return new Vec2D(x, y);
            }

            @Override
            public Vec2D targetSurface(VerletParticle2D p, Surface ss) {
                //unsupported
                return null;
            }
        };

        abstract Vec2D targetVerlet(VerletParticle2D particle, Surface s);

        @Nullable
        abstract public Vec2D targetSurface(VerletParticle2D p, Surface ss);
    }


    public VerletParticle2D addParticleBind(Surface a, VerletSurfaceBinding b) {
        VerletParticle2D ap = new VerletParticle2D(a.cx(), a.cy());
        bind(a, ap, true, b);

        physics.addParticle(ap);
        return ap;
    }

    @Override
    public <S extends Surface> S pos(RectFloat2D next) {
        if (physics != null)
            physics.bounds(next);
        return super.pos(next);
    }

    public ParticleConstraint2D bind(Surface s, VerletParticle2D v, boolean surfaceOverrides, VerletSurfaceBinding b) {

        WeakReference<Surface> wrs = new WeakReference<>(s);

        float speed = 0.5f;

        //if (!surfaceOverrides) {
            v.addBehavior((vv) -> {
                Surface ss = wrs.get();

                Vec2D pNext = b.targetVerlet(vv, ss);
                if (pNext != null) {
                    //p.next.set(pNext);
                    //System.out.println(vv.id + " " + vv.x + "," + vv.y);
                    vv.addForce(pNext.sub(vv).scaleSelf(speed));
//                    vv.set(pNext);
//                    vv.prev.set(pNext);
                    //vv.next.set(pNext);
                }
            });
        //}

        v.set(b.targetVerlet(v, s));
        v.constrainAll(physics.bounds);
        v.next.set(v);
        v.prev.set(v);


        if (!surfaceOverrides) {

        //pre
        v.addConstraint(vv -> {
            Surface ss = wrs.get();
//                vv.next.set(b.targetVerlet(vv, ss));
//                vv.constrainAll(physics.bounds);

                if (ss == null) {
                    physics.removeParticle(vv);
                    return;
                }

                Vec2D sNext = b.targetSurface(vv, ss);
                if (sNext != null) {
                    //ss.pos(Util.lerp(0.5f, sNext.x, ss.x()))
                    //ss.pos(RectFloat2D.XYWH(sNext.x, sNext.y, ss.w(), ss.h()));
                    ss.pos(ss.bounds.posLerp(sNext.x, sNext.y, 0.5f));
                }
//            } else {

//                Vec2D pNext = b.targetVerlet(vv, ss);
//                if (pNext != null) {
//                    //p.next.set(pNext);
//                    //float speed = 0.05f;
////                        System.out.println(vv.id + " " + vv.x + "," + vv.y);
//                    //vv.addForce(pNext.sub(vv).normalize().scaleSelf(speed));
////                    vv.clearForce();
////                    vv.clearVelocity();
//                    vv.next.set(pNext);
//                    vv.prev.set(pNext);
//                    vv.set(pNext);
//
//                }
            });
        }
        return null;
    }

    public final Pair<List<VerletParticle2D>, List<VerletSpring2D>> addParticleChain(VerletParticle2D x, VerletParticle2D y, int num, float strength) {
        return addParticleChain(x, y, num, Float.NaN, strength);
    }

    public Pair<List<VerletParticle2D>, List<VerletSpring2D>> addParticleChain(VerletParticle2D a, VerletParticle2D b, int num, float chainLength, float strength) {
        assert (num > 0);
        assert (a != b);

        if (chainLength != chainLength) {
            //auto
            chainLength = a.distanceTo(b);
        }
        float linkLength = chainLength / (num + 1);
        VerletParticle2D prev = a;
        FasterList pp = new FasterList(num);
        FasterList ss = new FasterList(num + 1);
        for (int i = 0; i < num; i++) {
            float p = ((float) i + 1) / (num + 1);
            VerletParticle2D next =
                    new VerletParticle2D(
                            Util.lerp(p, a.x, b.x),
                            Util.lerp(p, a.y, b.y)
                    );
            next.mass(Util.lerp(p, a.mass(), b.mass()));
            pp.add(next);
            physics.addParticle(next);
            VerletSpring2D s = new VerletSpring2D(prev, next, linkLength, strength);
            ss.add(s);
            physics.addSpring(s);
            prev = next;
        }
        {
            VerletSpring2D s = new VerletSpring2D(prev, b, linkLength, strength);
            ss.add(s);
            physics.addSpring(s);
        }

        return pair(pp, ss);
    }

    /**
     * basic renderer
     */
    public static void render(VerletPhysics2D physics, GL2 gl) {
        for (VerletParticle2D p : physics.particles) {
            float t = 2 * p.mass();
            Draw.colorGrays(gl, 0.3f + 0.7f * Util.tanhFast(p.getSpeed()), 0.7f);
            Draw.rect(gl, p.x - t / 2, p.y - t / 2, t, t);
        }

        gl.glColor3f(0, 0.5f, 0);
        for (VerletSpring2D s : physics.springs) {
            VerletParticle2D a = s.a, b = s.b;
            gl.glLineWidth(Math.min(a.mass(), b.mass()));
            Draw.line(gl, a.x, a.y, b.x, b.y);
        }
    }
}
