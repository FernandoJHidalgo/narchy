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
package org.jbox2d.dynamics.joints;

import org.jbox2d.common.*;
import org.jbox2d.dynamics.Body2D;
import org.jbox2d.dynamics.Dynamics2D;
import org.jbox2d.dynamics.SolverData;
import org.jbox2d.pooling.IWorldPool;
import spacegraph.math.Tuple2f;
import spacegraph.math.v2;

//Point-to-point constraint
//C = p2 - p1
//Cdot = v2 - v1
//   = v2 + cross(w2, r2) - v1 - cross(w1, r1)
//J = [-I -r1_skew I r2_skew ]
//Identity used:
//w k % (rx i + ry j) = w * (-ry i + rx j)

//Motor constraint
//Cdot = w2 - w1
//J = [0 0 -1 0 0 1]
//K = invI1 + invI2

/**
 * A revolute joint constrains two bodies to share a common point while they are free to rotate
 * about the point. The relative rotation about the shared point is the joint angle. You can limit
 * the relative rotation with a joint limit that specifies a lower and upper angle. You can use a
 * motor to drive the relative rotation about the shared point. A maximum motor torque is provided
 * so that infinite forces are not generated.
 *
 * @author Daniel Murphy
 */
public class RevoluteJoint extends Joint {

    // Solver shared
    protected final Tuple2f m_localAnchorA = new Vec2();
    protected final Tuple2f m_localAnchorB = new Vec2();
    private final Vec3 m_impulse = new Vec3();
    private float m_motorImpulse;

    private boolean m_enableMotor;
    private float m_maxMotorTorque;
    private float m_motorSpeed;

    private boolean m_enableLimit;
    protected final float m_referenceAngle;
    private float m_lowerAngle;
    private float m_upperAngle;

    // Solver temp
    private int m_indexA;
    private int m_indexB;
    private final Tuple2f m_rA = new Vec2();
    private final Tuple2f m_rB = new Vec2();
    private final Tuple2f m_localCenterA = new Vec2();
    private final Tuple2f m_localCenterB = new Vec2();
    private float m_invMassA;
    private float m_invMassB;
    private float m_invIA;
    private float m_invIB;
    private final Mat33 m_mass = new Mat33(); // effective mass for point-to-point constraint.
    private float m_motorMass; // effective mass for motor/limit angular constraint.
    private LimitState m_limitState;

    /** how important it is to resolve position 'error' (distance from point-point).
     * 1 = normal revolute joint behavior
     * ~ = somewhat solve it
     * 0 = does not resolve point-to-point distance 'error'
     */
    public float positionFactor = 1f;

    public RevoluteJoint(Dynamics2D world, RevoluteJointDef def) {
        this(world.pool, def);
    }

    public RevoluteJoint(IWorldPool argWorld, RevoluteJointDef def) {
        super(argWorld, def);
        m_localAnchorA.set(def.localAnchorA);
        m_localAnchorB.set(def.localAnchorB);
        m_referenceAngle = def.referenceAngle;

        m_motorImpulse = 0;

        m_lowerAngle = def.lowerAngle;
        m_upperAngle = def.upperAngle;
        m_maxMotorTorque = def.maxMotorTorque;
        m_motorSpeed = def.motorSpeed;
        m_enableLimit = def.enableLimit;
        m_enableMotor = def.enableMotor;
        m_limitState = LimitState.INACTIVE;
    }

    @Override
    public void initVelocityConstraints(final SolverData data) {
        m_indexA = A.island;
        m_indexB = B.island;
        m_localCenterA.set(A.sweep.localCenter);
        m_localCenterB.set(B.sweep.localCenter);
        m_invMassA = A.m_invMass;
        m_invMassB = B.m_invMass;
        m_invIA = A.m_invI;
        m_invIB = B.m_invI;

        // Vec2 cA = data.positions[m_indexA].c;
        float aA = data.positions[m_indexA].a;
        Tuple2f vA = data.velocities[m_indexA];
        float wA = data.velocities[m_indexA].w;

        // Vec2 cB = data.positions[m_indexB].c;
        float aB = data.positions[m_indexB].a;
        Tuple2f vB = data.velocities[m_indexB];
        float wB = data.velocities[m_indexB].w;
        final Rot qA = pool.popRot();
        final Rot qB = pool.popRot();
        final Tuple2f temp = new v2();

        qA.set(aA);
        qB.set(aB);

        // Compute the effective masses.
        Rot.mulToOutUnsafe(qA, temp.set(m_localAnchorA).subbed(m_localCenterA), m_rA);
        Rot.mulToOutUnsafe(qB, temp.set(m_localAnchorB).subbed(m_localCenterB), m_rB);

        // J = [-I -r1_skew I r2_skew]
        // [ 0 -1 0 1]
        // r_skew = [-ry; rx]

        // Matlab
        // K = [ mA+r1y^2*iA+mB+r2y^2*iB, -r1y*iA*r1x-r2y*iB*r2x, -r1y*iA-r2y*iB]
        // [ -r1y*iA*r1x-r2y*iB*r2x, mA+r1x^2*iA+mB+r2x^2*iB, r1x*iA+r2x*iB]
        // [ -r1y*iA-r2y*iB, r1x*iA+r2x*iB, iA+iB]

        float mA = m_invMassA, mB = m_invMassB;
        float iA = m_invIA, iB = m_invIB;

        boolean fixedRotation = (iA + iB == 0.0f);

        m_mass.ex.x = mA + mB + m_rA.y * m_rA.y * iA + m_rB.y * m_rB.y * iB;
        m_mass.ey.x = -m_rA.y * m_rA.x * iA - m_rB.y * m_rB.x * iB;
        m_mass.ez.x = -m_rA.y * iA - m_rB.y * iB;
        m_mass.ex.y = m_mass.ey.x;
        m_mass.ey.y = mA + mB + m_rA.x * m_rA.x * iA + m_rB.x * m_rB.x * iB;
        m_mass.ez.y = m_rA.x * iA + m_rB.x * iB;
        m_mass.ex.z = m_mass.ez.x;
        m_mass.ey.z = m_mass.ez.y;
        m_mass.ez.z = iA + iB;

        m_motorMass = iA + iB;
        if (m_motorMass > 0.0f) {
            m_motorMass = 1.0f / m_motorMass;
        }

        if (!m_enableMotor || fixedRotation) {
            m_motorImpulse = 0.0f;
        }

        if (m_enableLimit && !fixedRotation) {
            float jointAngle = aB - aA - m_referenceAngle;
            if (Math.abs(m_upperAngle - m_lowerAngle) < 2.0f * Settings.angularSlop) {
                m_limitState = LimitState.EQUAL;
            } else if (jointAngle <= m_lowerAngle) {
                if (m_limitState != LimitState.AT_LOWER) {
                    m_impulse.z = 0.0f;
                }
                m_limitState = LimitState.AT_LOWER;
            } else if (jointAngle >= m_upperAngle) {
                if (m_limitState != LimitState.AT_UPPER) {
                    m_impulse.z = 0.0f;
                }
                m_limitState = LimitState.AT_UPPER;
            } else {
                m_limitState = LimitState.INACTIVE;
                m_impulse.z = 0.0f;
            }
        } else {
            m_limitState = LimitState.INACTIVE;
        }

        if (data.step.warmStarting) {
            final Tuple2f P = new v2();
            // Scale impulses to support a variable time step.
            m_impulse.x *= data.step.dtRatio;
            m_impulse.y *= data.step.dtRatio;
            m_motorImpulse *= data.step.dtRatio;

            P.x = m_impulse.x;
            P.y = m_impulse.y;

            vA.x -= mA * P.x;
            vA.y -= mA * P.y;
            wA -= iA * (Tuple2f.cross(m_rA, P) + m_motorImpulse + m_impulse.z);

            vB.x += mB * P.x;
            vB.y += mB * P.y;
            wB += iB * (Tuple2f.cross(m_rB, P) + m_motorImpulse + m_impulse.z);
        } else {
            m_impulse.setZero();
            m_motorImpulse = 0.0f;
        }
        // data.velocities[m_indexA].v.set(vA);
        data.velocities[m_indexA].w = wA;
        // data.velocities[m_indexB].v.set(vB);
        data.velocities[m_indexB].w = wB;


        pool.pushRot(2);
    }

    @Override
    public void solveVelocityConstraints(final SolverData data) {
        Tuple2f vA = data.velocities[m_indexA];
        float wA = data.velocities[m_indexA].w;
        Tuple2f vB = data.velocities[m_indexB];
        float wB = data.velocities[m_indexB].w;

        float mA = m_invMassA, mB = m_invMassB;
        float iA = m_invIA, iB = m_invIB;

        boolean fixedRotation = (iA + iB == 0.0f);

        // Solve motor constraint.
        if (m_enableMotor && m_limitState != LimitState.EQUAL && !fixedRotation) {
            float Cdot = wB - wA - m_motorSpeed;
            float impulse = -m_motorMass * Cdot;
            float oldImpulse = m_motorImpulse;
            float maxImpulse = data.step.dt * m_maxMotorTorque;
            m_motorImpulse = MathUtils.clamp(m_motorImpulse + impulse, -maxImpulse, maxImpulse);
            impulse = m_motorImpulse - oldImpulse;

            wA -= iA * impulse;
            wB += iB * impulse;
        }
        final Tuple2f temp = new v2();

        // Solve limit constraint.
        if (m_enableLimit && m_limitState != LimitState.INACTIVE && !fixedRotation) {

            final Tuple2f Cdot1 = new v2();
            final Vec3 Cdot = new Vec3();

            // Solve point-to-point constraint
            Tuple2f.crossToOutUnsafe(wA, m_rA, temp);
            Tuple2f.crossToOutUnsafe(wB, m_rB, Cdot1);
            Cdot1.added(vB).subbed(vA).subbed(temp).scaled(positionFactor);
            float Cdot2 = wB - wA;
            Cdot.set(Cdot1.x, Cdot1.y, Cdot2);

            Vec3 impulse = new Vec3();
            m_mass.solve33ToOut(Cdot, impulse);
            impulse.negateLocal();

            switch (m_limitState) {
                case EQUAL:
                    m_impulse.addLocal(impulse);
                    break;
                case AT_LOWER: {
                    float newImpulse = m_impulse.z + impulse.z;
                    if (newImpulse < 0.0f) {
                        final Tuple2f rhs = new v2();
                        rhs.set(m_mass.ez.x, m_mass.ez.y).scaled(m_impulse.z).subbed(Cdot1);
                        m_mass.solve22ToOut(rhs, temp);
                        impulse.x = temp.x;
                        impulse.y = temp.y;
                        impulse.z = -m_impulse.z;
                        m_impulse.x += temp.x;
                        m_impulse.y += temp.y;
                        m_impulse.z = 0.0f;
                    } else {
                        m_impulse.addLocal(impulse);
                    }
                    break;
                }
                case AT_UPPER: {
                    float newImpulse = m_impulse.z + impulse.z;
                    if (newImpulse > 0.0f) {
                        final Tuple2f rhs = new v2();
                        rhs.set(m_mass.ez.x, m_mass.ez.y).scaled(m_impulse.z).subbed(Cdot1);
                        m_mass.solve22ToOut(rhs, temp);
                        impulse.x = temp.x;
                        impulse.y = temp.y;
                        impulse.z = -m_impulse.z;
                        m_impulse.x += temp.x;
                        m_impulse.y += temp.y;
                        m_impulse.z = 0.0f;
                    } else {
                        m_impulse.addLocal(impulse);
                    }
                    break;
                }
            }
            final Tuple2f P = new v2();

            P.set(impulse.x, impulse.y);

            vA.x -= mA * P.x;
            vA.y -= mA * P.y;
            wA -= iA * (Tuple2f.cross(m_rA, P) + impulse.z);

            vB.x += mB * P.x;
            vB.y += mB * P.y;
            wB += iB * (Tuple2f.cross(m_rB, P) + impulse.z);

        } else {

            // Solve point-to-point constraint
            Tuple2f Cdot = new v2();
            Tuple2f impulse = new v2();

            Tuple2f.crossToOutUnsafe(wA, m_rA, temp);
            Tuple2f.crossToOutUnsafe(wB, m_rB, Cdot);
            Cdot.added(vB).subbed(vA).subbed(temp).scaled(positionFactor);
            m_mass.solve22ToOut(Cdot.negated(), impulse); // just leave negated

            m_impulse.x += impulse.x;
            m_impulse.y += impulse.y;

            vA.x -= mA * impulse.x;
            vA.y -= mA * impulse.y;
            wA -= iA * Tuple2f.cross(m_rA, impulse);

            vB.x += mB * impulse.x;
            vB.y += mB * impulse.y;
            wB += iB * Tuple2f.cross(m_rB, impulse);

        }

        // data.velocities[m_indexA].v.set(vA);
        data.velocities[m_indexA].w = wA;
        // data.velocities[m_indexB].v.set(vB);
        data.velocities[m_indexB].w = wB;


    }

    @Override
    public boolean solvePositionConstraints(final SolverData data) {
        final Rot qA = pool.popRot();
        final Rot qB = pool.popRot();
        Tuple2f cA = data.positions[m_indexA];
        float aA = data.positions[m_indexA].a;
        Tuple2f cB = data.positions[m_indexB];
        float aB = data.positions[m_indexB].a;

        qA.set(aA);
        qB.set(aB);

        float angularError = 0.0f;
        float positionError = 0.0f;

        boolean fixedRotation = (m_invIA + m_invIB == 0.0f);

        // Solve angular limit constraint.
        if (m_enableLimit && m_limitState != LimitState.INACTIVE && !fixedRotation) {
            float angle = aB - aA - m_referenceAngle;
            float limitImpulse = 0.0f;

            switch (m_limitState) {
                case EQUAL: {
                    // Prevent large angular corrections
                    float C =
                            MathUtils.clamp(angle - m_lowerAngle, -Settings.maxAngularCorrection,
                                    Settings.maxAngularCorrection);
                    limitImpulse = -m_motorMass * C;
                    angularError = Math.abs(C);
                    break;
                }
                case AT_LOWER: {
                    float C = angle - m_lowerAngle;
                    angularError = -C;

                    // Prevent large angular corrections and allow some slop.
                    C = MathUtils.clamp(C + Settings.angularSlop, -Settings.maxAngularCorrection, 0.0f);
                    limitImpulse = -m_motorMass * C;
                    break;
                }
                case AT_UPPER: {
                    float C = angle - m_upperAngle;
                    angularError = C;

                    // Prevent large angular corrections and allow some slop.
                    C = MathUtils.clamp(C - Settings.angularSlop, 0.0f, Settings.maxAngularCorrection);
                    limitImpulse = -m_motorMass * C;
                    break;
                }
            }

            aA -= m_invIA * limitImpulse;
            aB += m_invIB * limitImpulse;
        }
        // Solve point-to-point constraint.
        {
            qA.set(aA);
            qB.set(aB);

            final Tuple2f rA = new v2();
            final Tuple2f rB = new v2();
            final Tuple2f C = new v2();
            final Tuple2f impulse = new v2();

            Rot.mulToOutUnsafe(qA, C.set(m_localAnchorA).subbed(m_localCenterA), rA);
            Rot.mulToOutUnsafe(qB, C.set(m_localAnchorB).subbed(m_localCenterB), rB);
            C.set(cB).added(rB).subbed(cA).subbed(rA).scaled(positionFactor);
            positionError = C.length();


            float mA = m_invMassA, mB = m_invMassB;
            float iA = m_invIA, iB = m_invIB;

            final Mat22 K = pool.popMat22();
            K.ex.x = mA + mB + iA * rA.y * rA.y + iB * rB.y * rB.y;
            K.ex.y = -iA * rA.x * rA.y - iB * rB.x * rB.y;
            K.ey.x = K.ex.y;
            K.ey.y = mA + mB + iA * rA.x * rA.x + iB * rB.x * rB.x;
            K.solveToOut(C, impulse);
            impulse.negated();

            cA.x -= mA * impulse.x;
            cA.y -= mA * impulse.y;
            aA -= iA * Tuple2f.cross(rA, impulse);

            cB.x += mB * impulse.x;
            cB.y += mB * impulse.y;
            aB += iB * Tuple2f.cross(rB, impulse);

            pool.pushMat22(1);
        }
        // data.positions[m_indexA].c.set(cA);
        data.positions[m_indexA].a = aA;
        // data.positions[m_indexB].c.set(cB);
        data.positions[m_indexB].a = aB;

        pool.pushRot(2);

        return positionError <= Settings.linearSlop && angularError <= Settings.angularSlop;
    }

    public Tuple2f getLocalAnchorA() {
        return m_localAnchorA;
    }

    public Tuple2f getLocalAnchorB() {
        return m_localAnchorB;
    }

    public float getReferenceAngle() {
        return m_referenceAngle;
    }

    @Override
    public void getAnchorA(Tuple2f argOut) {
        A.getWorldPointToOut(m_localAnchorA, argOut);
    }

    @Override
    public void getAnchorB(Tuple2f argOut) {
        B.getWorldPointToOut(m_localAnchorB, argOut);
    }

    @Override
    public void getReactionForce(float inv_dt, Tuple2f argOut) {
        argOut.set(m_impulse.x, m_impulse.y).scaled(inv_dt);
    }

    @Override
    public float getReactionTorque(float inv_dt) {
        return inv_dt * m_impulse.z;
    }

    public float getJointAngle() {
        final Body2D b1 = A;
        final Body2D b2 = B;
        return b2.sweep.a - b1.sweep.a - m_referenceAngle;
    }

    public float getJointSpeed() {
        final Body2D b1 = A;
        final Body2D b2 = B;
        return b2.velAngular - b1.velAngular;
    }

    public boolean isMotorEnabled() {
        return m_enableMotor;
    }

    public void enableMotor(boolean flag) {
        A.setAwake(true);
        B.setAwake(true);
        m_enableMotor = flag;
    }

    public float getMotorTorque(float inv_dt) {
        return m_motorImpulse * inv_dt;
    }

    public void setMotorSpeed(final float speed) {
        A.setAwake(true);
        B.setAwake(true);
        m_motorSpeed = speed;
    }

    public void setMaxMotorTorque(final float torque) {
        A.setAwake(true);
        B.setAwake(true);
        m_maxMotorTorque = torque;
    }

    public float getMotorSpeed() {
        return m_motorSpeed;
    }

    public float getMaxMotorTorque() {
        return m_maxMotorTorque;
    }

    public boolean isLimitEnabled() {
        return m_enableLimit;
    }

    public void enableLimit(final boolean flag) {
        if (flag != m_enableLimit) {
            A.setAwake(true);
            B.setAwake(true);
            m_enableLimit = flag;
            m_impulse.z = 0.0f;
        }
    }

    public float getLowerLimit() {
        return m_lowerAngle;
    }

    public float getUpperLimit() {
        return m_upperAngle;
    }

    public void setLimits(final float lower, final float upper) {
        assert (lower <= upper);
        if (lower != m_lowerAngle || upper != m_upperAngle) {
            A.setAwake(true);
            B.setAwake(true);
            m_impulse.z = 0.0f;
            m_lowerAngle = lower;
            m_upperAngle = upper;
        }
    }
}
