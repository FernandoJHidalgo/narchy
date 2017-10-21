package spacegraph.space;

import com.jogamp.opengl.GL2;
import jcog.Util;
import org.jetbrains.annotations.Nullable;
import spacegraph.SimpleSpatial;
import spacegraph.Surface;
import spacegraph.input.Finger;
import spacegraph.math.Quat4f;
import spacegraph.math.v3;
import spacegraph.phys.Collidable;
import spacegraph.phys.collision.ClosestRay;
import spacegraph.phys.math.Transform;
import spacegraph.phys.shape.SimpleBoxShape;
import spacegraph.phys.shape.SphereShape;
import spacegraph.render.Draw;
import spacegraph.render.JoglPhysics;

import static spacegraph.math.v3.v;

/**
 * https://en.wikipedia.org/wiki/Cuboid
 * Serves as a mount for an attached (forward-facing) 2D surface (embeds a surface in 3D space)
 */
public class Cuboid<X> extends SimpleSpatial<X> {

    @Nullable
    public Surface front;
    static final float zOffset = 0.1f; //relative to scale

    @Nullable
    public Finger mouseFront;
    private v3 mousePick;
    //private float padding;

    public Cuboid(X x, float w, float h) {
        this(x, null, w, h);
    }

    public Cuboid(Surface front, float w, float h) {
        this((X) front, front, w, h);
    }

    public Cuboid(X x, Surface front, float w, float h) {
        this(x, front, w, h, (Math.min(w, h) / 2f));
    }

    public Cuboid(Surface front, float w, float h, float d) {
        this((X) front, front, w, h, d);
    }

    public Cuboid(X x, Surface front, float w, float h, float d) {
        super(x);

        scale(w, h, d);


        setFront(front);

    }

    public void setFront(Surface front) {
        this.front = front;
        if (front != null) {
            front.start(null);
            mouseFront = null; //new Finger(this);
        } else {
            mouseFront = null;
        }
    }

    @Override
    public boolean onKey(Collidable body, v3 hitPoint, char charCode, boolean pressed) {
        if (!super.onKey(body, hitPoint, charCode, pressed)) {

            return front != null && front.onKey(null, charCode, pressed);
        }
        return true;
    }

    @Override
    public Surface onTouch(Collidable body, ClosestRay r, short[] buttons, JoglPhysics space) {

        if (body != null) {

            //rotate to match camera's orientation (billboarding)
            Object d = body.data();
            if (d instanceof SimpleSpatial) {
                SimpleSpatial sd = (SimpleSpatial)d;
                //Quat4f target = Quat4f.angle(-space.camFwd.x, -space.camFwd.y, -space.camFwd.z, 0);
                Quat4f target = new Quat4f();
                // TODO somehow use the object's local transformation ? sd.transform().getRotation(...);
                target.setAngle( -space.camFwd.x, -space.camFwd.y, -space.camFwd.z, 0.01f);

                target.normalize();

                //System.out.print("rotating: " + sd.transform().getRotation(new Quat4f()));
                sd.rotate(target, 0.2f, new Quat4f());
                //System.out.println("  : " + sd.transform().getRotation(new Quat4f()));
            }
//
            Surface s0 = super.onTouch(body, r, buttons, space);
            if (s0 != null)
                return s0;
        }


        if (front != null) {
            Transform it = Transform.t(transform()).inverse();
            v3 localPoint = it.transform(v(r.hitPointWorld));

            if (body != null && body.shape() instanceof SimpleBoxShape) {
                SimpleBoxShape shape = (SimpleBoxShape) body.shape();
                float frontZ = shape.z() / 2;
                float zTolerance = frontZ / 4f;

                if (Util.equals(localPoint.z, frontZ, zTolerance)) { //top surface only, ignore sides and back

                    this.mousePick = r.hitPointWorld;

                    //System.out.println(localPoint + " " + thick);
                    if (mouseFront != null)
                        return mouseFront.on(Float.NaN, Float.NaN, localPoint.x / shape.x() + 0.5f, localPoint.y / shape.y() + 0.5f, buttons);
                    //return mouseFront.update(null, localPoint.x, localPoint.y, buttons);
                }
            } else {

                if (mouseFront != null && mouseFront.off()) {

                }
            }
        }


        return null;
    }


    @Override
    public final void renderRelative(GL2 gl, Collidable body) {
        super.renderRelative(gl, body);


        if (front != null) {

            //float p = this.padding;

            //gl.glPushMatrix();

            //float pp = 1f - (p / 2f);
            //float pp = 1f;

            gl.glTranslatef(-0.5f, -0.5f, 0.5f + (shape instanceof SphereShape ? 10 : 0)+zOffset);
            //gl.glScalef(pp, pp, 1f);

            //Transform t = transform();
            //float tw = t.x;
            //float th = t.y;
            //gl.glDepthMask(false);
            front.render(gl);
            //gl.glDepthMask(true);

            //gl.glPopMatrix();
        }
    }

    @Override
    public void renderAbsolute(GL2 gl) {
        super.renderAbsolute(gl);

        //display pick location (debugging)
        if (mousePick != null) {
            gl.glPushMatrix();
            gl.glTranslatef(mousePick.x, mousePick.y, mousePick.z);
            gl.glScalef(0.25f, 0.25f, 0.25f);
            gl.glColor4f(1f, 1f, 1f, 0.5f);
            gl.glRotated(Math.random() * 360.0, Math.random() - 0.5f, Math.random() - 0.5f, Math.random() - 0.5f);
            //gl.glDepthMask(false);
            Draw.rect(gl, -0.5f, -0.5f, 1, 1);
            //gl.glDepthMask(true);
            gl.glPopMatrix();
        }
    }
}
