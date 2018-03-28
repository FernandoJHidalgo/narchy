/*******************************************************************************
 * Copyright (c) 2013, Daniel Murphy
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 	* Redistributions of source code must retain the above copyright notice,
 * 	  this list of conditions and the following disclaimer.
 * 	* Redistributions in binary form must reproduce the above copyright notice,
 * 	  this list of conditions and the following disclaimer in the documentation
 * 	  and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package spacegraph.space2d.phys.common;

import com.jogamp.opengl.GL2;
import spacegraph.util.math.Tuple2f;
import spacegraph.util.math.v2;

// updated to rev 100

/**
 * A transform contains translation and rotation. It is used to represent the position and
 * orientation of rigid frames.
 */
public class Transform extends Rot {

    /**
     * The translation caused by the transform
     */
    public final Tuple2f pos;

    /**
     * The default constructor.
     */
    public Transform() {
        pos = new v2();
    }


    /**
     * Set this to equal another transform.
     */
    public final Transform set(final Transform xf) {
        pos.set(xf.pos);
        this.set((Rot) xf);
        return this;
    }

    /**
     * Set this based on the position and angle.
     *
     * @param p
     * @param angle
     */
    public final void set(Tuple2f p, float angle) {
        this.pos.set(p);
        this.set(angle);
    }

    /**
     * Set this to the identity transform.
     */
    public final void setIdentity() {
        pos.setZero();
        super.setIdentity();
    }

    public final static Tuple2f mul(final Transform T, final Tuple2f v) {
        return new v2((T.c * v.x - T.s * v.y) + T.pos.x, (T.s * v.x + T.c * v.y) + T.pos.y);
    }

    public final static void mulToOut(final Transform T, final Tuple2f v, final Tuple2f out) {
        float ts = T.s;
        float vx = v.x;
        float vy = v.y;
        float tc = T.c;
        final float tempy = (ts * vx + tc * vy) + T.pos.y;
        out.x = (tc * vx - ts * vy) + T.pos.x;
        out.y = tempy;
    }

    public final static void mulToOutUnsafe(final Transform T, final Tuple2f v, final Tuple2f out) {
        assert (v != out);
        Rot tq = T;
        out.x = (tq.c * v.x - tq.s * v.y) + T.pos.x;
        out.y = (tq.s * v.x + tq.c * v.y) + T.pos.y;
    }
    public final static void mulToOutUnsafe(final Transform T, final Tuple2f v, float scale, final Tuple2f out) {
        assert (v != out);
        float vy = v.y * scale;
        float vx = v.x * scale;
        Rot tq = T;
        Tuple2f pos = T.pos;
        float tqs = tq.s;
        float tqc = tq.c;
        out.x = (tqc * vx - tqs * vy) + pos.x;
        out.y = (tqs * vx + tqc * vy) + pos.y;
    }

    public final static void mulToOutUnsafe(final Transform T, final Tuple2f v, float scale, final GL2 gl) {
        float vy = v.y * scale;
        float vx = v.x * scale;
        Rot tq = T;
        Tuple2f pos = T.pos;
        float tqs = tq.s;
        float tqc = tq.c;
        gl.glVertex2f(
            (tqc * vx - tqs * vy) + pos.x,
            (tqs * vx + tqc * vy) + pos.y
        );

    }

    public final static Tuple2f mulTrans(final Transform T, final Tuple2f v) {
        final float px = v.x - T.pos.x;
        final float py = v.y - T.pos.y;
        float y = (-T.s * px + T.c * py);
        return new v2((T.c * px + T.s * py), y);
    }

    public final static void mulTransToOut(final Transform T, final Tuple2f v, final Tuple2f out) {
        final float px = v.x - T.pos.x;
        final float py = v.y - T.pos.y;
        final float tempy = (-T.s * px + T.c * py);
        out.x = (T.c * px + T.s * py);
        out.y = tempy;
    }

    public final static void mulTransToOutUnsafe(final Transform T, final Tuple2f v, final Tuple2f out) {
        assert (v != out);
        final float px = v.x - T.pos.x;
        final float py = v.y - T.pos.y;
        out.x = (T.c * px + T.s * py);
        out.y = (-T.s * px + T.c * py);
    }

    public final static Transform mul(final Transform A, final Transform B) {
        Transform C = new Transform();
        mulUnsafe(A, B, C);
        Rot.mulToOutUnsafe(A, B.pos, C.pos);
        C.pos.added(A.pos);
        return C;
    }

    public final static void mulToOut(final Transform A, final Transform B, final Transform out) {
        assert (out != A);
        mul(A, B, out);
        Rot.mulToOut(A, B.pos, out.pos);
        out.pos.added(A.pos);
    }

    public final static void mulToOutUnsafe(final Transform A, final Transform B, final Transform out) {
        assert (out != B);
        assert (out != A);
        mulUnsafe(A, B, out);
        Rot.mulToOutUnsafe(A, B.pos, out.pos);
        out.pos.added(A.pos);
    }

//    private static final Tuple2f pool = new Vec2();
//
//    public final static Transform mulTrans(final Transform A, final Transform B) {
//        Transform C = new Transform();
//        Rot.mulTransUnsafe(A.rotMatrix, B.rotMatrix, C.rotMatrix);
//        pool.set(B.pos).subbed(A.pos);
//        Rot.mulTransUnsafe(A.rotMatrix, pool, C.pos);
//        return C;
//    }
//
//    public final static void mulTransToOut(final Transform A, final Transform B, final Transform out) {
//        assert (out != A);
//        Rot.mulTrans(A.rotMatrix, B.rotMatrix, out.rotMatrix);
//        pool.set(B.pos).subbed(A.pos);
//        Rot.mulTrans(A.rotMatrix, pool, out.pos);
//    }

    public final static void mulTransToOutUnsafe(final Transform A, final Transform B,
                                                 final Transform out) {
        assert (out != A);
        assert (out != B);
        mulTransUnsafe(A, B, out);
        v2 pool = new v2();
        pool.set(B.pos).subbed(A.pos);
        mulTransUnsafe(A, pool, out.pos);
    }

    @Override
    public final String toString() {
        String s = "XForm:\n";
        s += "Position: " + pos + '\n';
        s += "R: \n" + super.toString() + '\n';
        return s;
    }
}
