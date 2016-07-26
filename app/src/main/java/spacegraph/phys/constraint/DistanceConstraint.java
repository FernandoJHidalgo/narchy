package spacegraph.phys.constraint;

import spacegraph.math.Matrix3f;
import spacegraph.math.v3;
import spacegraph.phys.Dynamic;
import spacegraph.phys.math.Transform;
import spacegraph.phys.math.VectorUtil;
import spacegraph.phys.solve.JacobianEntry;

import static spacegraph.math.v3.v;

/**
 * http://bulletphysics.org/Bullet/phpBB3/viewtopic.php?t=4864
 */
public class DistanceConstraint extends TypedConstraint {

    public float dist;
    public float speed;

    private final JacobianEntry jac = new JacobianEntry();

    private final v3 pivotInA = new v3();
    private final v3 pivotInB = new v3();

    /**
     * strength
     */
    public float tau = 0.3f;

    public float damping = 1f;
    public float impulseClamp;
    private float error;

    public DistanceConstraint(Dynamic rbA, Dynamic rbB, float dist, float speed, float impulseClamp, float tau) {
        this(rbA, rbB, v(), v(), dist, speed, impulseClamp, tau);
    }
    public DistanceConstraint(Dynamic rbA, Dynamic rbB, v3 pivotInA, v3 pivotInB, float dist, float speed, float impulseClamp, float tau) {
        super(TypedConstraintType.POINT2POINT_CONSTRAINT_TYPE, rbA, rbB);
        this.pivotInA.set(pivotInA);
        this.pivotInB.set(pivotInB);
        this.dist = dist;
        this.speed = speed;
        this.impulseClamp = impulseClamp;
        this.tau = tau;
    }


    @Override
    public void solveConstraint(float timeStep) {

        v3 tmp = new v3();
        v3 tmp2 = new v3();
        //v3 tmpVec = new v3();

        Transform centerOfMassA = rbA.getCenterOfMassTransform(new Transform());
        Transform centerOfMassB = rbB.getCenterOfMassTransform(new Transform());

        v3 pivotAInW = new v3(pivotInA);
        centerOfMassA.transform(pivotAInW);

        v3 pivotBInW = new v3(pivotInB);
        centerOfMassB.transform(pivotBInW);

        //v3 angvelA = m_rbA.getCenterOfMassTransform().getBasis().transpose() * m_rbA.getAngularVelocity();
        //v3 angvelB = m_rbB.getCenterOfMassTransform().getBasis().transpose() * m_rbB.getAngularVelocity();

        v3 rel_pos1 = new v3();
        rel_pos1.sub(pivotAInW, centerOfMassA);
        v3 rel_pos2 = new v3();
        rel_pos2.sub(pivotBInW, centerOfMassB);


        v3 vel1 = rbA.getVelocityInLocalPoint(rel_pos1, new v3());
        v3 vel2 = rbB.getVelocityInLocalPoint(rel_pos2, new v3());
        v3 vel = new v3();
        vel.sub(vel1, vel2);

        v3 normal = jac.linearJointAxis;
        float rel_vel = vel.dot(normal);


			/*
            //velocity error (first order error)
			btScalar rel_vel = m_jac.getRelativeVelocity(m_rbA.getLinearVelocity(),angvelA,
			m_rbB.getLinearVelocity(),angvelB);
			 */

        // positional error (zeroth order error)
        tmp.sub(pivotAInW, pivotBInW);
        float depth = -tmp.dot(normal); //this is the error projected on the normal

        float impulse = error * ((depth * tau / timeStep) - (damping * rel_vel ));

        float impulseClamp = this.impulseClamp;
        if (impulseClamp > 0f) {
            if (impulse < -impulseClamp) {
                impulse = -impulseClamp;
            }
            if (impulse > impulseClamp) {
                impulse = impulseClamp;
            }
        }

        appliedImpulse += impulse;
        v3 impulse_vector = new v3();
        impulse_vector.scale(impulse, normal);

        tmp.sub(pivotAInW, centerOfMassA);
        rbA.impulse(impulse_vector, tmp);
        tmp.negate(impulse_vector);
        tmp2.sub(pivotBInW, centerOfMassB);
        rbB.impulse(tmp, tmp2);


    }

    @Override
    public void buildJacobian() {

        appliedImpulse = 0;

        Transform posA = rbA.getCenterOfMassTransform(new Transform());
        Transform posB = rbB.getCenterOfMassTransform(new Transform());

        v3 relA = new v3(pivotInA);
        posA.transform(relA);

        v3 relB = new v3(pivotInB);
        posB.transform(relB);

        v3 del = new v3();
        del.sub(posB, posA);

        float currDist = (float) Math.sqrt(del.dot(del));

        v3 ortho = del;
        ortho.scale(1f / currDist);


        Matrix3f tmpMat1 = new Matrix3f(), tmpMat2 = new Matrix3f();
        tmpMat1.transpose(posA.basis);
        tmpMat2.transpose(posB.basis);

        v3 tmp1 = v(pivotInA), tmp2 = v(pivotInB);
        posA.transform(tmp1);
        tmp1.sub(rbA.getCenterOfMassPosition(v()));

        posB.transform(tmp2);
        tmp2.sub(rbB.getCenterOfMassPosition(v()));

        jac.init(
                tmpMat1,
                tmpMat2,
                tmp1,
                tmp2,
                ortho,
                rbA.getInvInertiaDiagLocal(new v3()),
                rbA.getInvMass(),
                rbB.getInvInertiaDiagLocal(new v3()),
                rbB.getInvMass());

        this.error = (currDist - dist) * speed;

        //System.out.println("dist=" + (currDist - dist) + " " + jac.linearJointAxis);


    }
}
