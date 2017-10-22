/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 *
 * Bullet Continuous Collision Detection and Physics Library
 * Copyright (c) 2003-2008 Erwin Coumans  http://www.bulletphysics.com/
 *
 * This software is provided 'as-is', without any express or implied warranty.
 * In no event will the authors be held liable for any damages arising from
 * the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose, 
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 * 1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software. If you use this software
 *    in a product, an acknowledgment in the product documentation would be
 *    appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be
 *    misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 */

package spacegraph.phys;

import jcog.Util;
import spacegraph.math.Matrix3f;
import spacegraph.math.Quat4f;
import spacegraph.math.v3;
import spacegraph.phys.collision.CollidableType;
import spacegraph.phys.collision.CollisionFlags;
import spacegraph.phys.collision.broad.Broadphasing;
import spacegraph.phys.constraint.TypedConstraint;
import spacegraph.phys.math.MatrixUtil;
import spacegraph.phys.math.Transform;
import spacegraph.phys.math.TransformUtil;
import spacegraph.phys.shape.CollisionShape;
import spacegraph.phys.util.OArrayList;

import static jcog.Util.clamp;
import static spacegraph.math.v3.v;


/**
 * RigidBody is the main class for rigid body objects. It is derived from
 * {@link Collidable}, so it keeps reference to {@link CollisionShape}.<p>
 * 
 * It is recommended for performance and memory use to share {@link CollisionShape}
 * objects whenever possible.<p>
 * 
 * There are 3 types of rigid bodies:<br>
 * <ol>
 * <li>Dynamic rigid bodies, with positive mass. Motion is controlled by rigid body dynamics.</li>
 * <li>Fixed objects with zero mass. They are not moving (basically collision objects).</li>
 * <li>Kinematic objects, which are objects without mass, but the user can move them. There
 *     is on-way interaction, and Bullet calculates a velocity based on the timestep and
 *     previous and current world transform.</li>
 * </ol>
 * 
 * Bullet automatically deactivates dynamic rigid bodies, when the velocity is below
 * a threshold for a given time.<p>
 * 
 * Deactivated (sleeping) rigid bodies don't take any processing time, except a minor
 * broadphase collision detection impact (to allow active objects to activate/wake up
 * sleeping objects).
 * 
 * @author jezek2
 */
public class Dynamic<X> extends Collidable<X> {

	private static final float MAX_ANGVEL = BulletGlobals.SIMD_HALF_PI;
	
	public final Matrix3f invInertiaTensorWorld = new Matrix3f();
	public final v3 linearVelocity = new v3();
	public final v3 angularVelocity = new v3();
	private float inverseMass;
	private float angularFactor;

	private final v3 gravity = new v3();
	public final v3 invInertiaLocal = new v3();
	private final v3 totalForce = new v3();
	private final v3 totalTorque = new v3();


	private float linearDamping;
	private float angularDamping;

//	private boolean additionalDamping;
//	private float additionalDampingFactor;
//	private float additionalLinearDampingThresholdSqr;
//	private float additionalAngularDampingThresholdSqr;
//	private float additionalAngularDampingFactor;

	private float linearSleepingThreshold;
	private float angularSleepingThreshold;

//	// optionalMotionState allows to automatic synchronize the world transform for active objects
//	private MotionState optionalMotionState;

	// keep track of typed constraints referencing this rigid body
	private final OArrayList<TypedConstraint> constraintRefs = new OArrayList<>();

	// for experimental overriding of friction/contact solver func
	public int contactSolverType;
	public int frictionSolverType;
	

	public Dynamic(float mass, Transform t, CollisionShape collisionShape) {
		this(mass, t, collisionShape, v());
	}

	public Dynamic(float mass, Transform t, CollisionShape collisionShape, v3 localInertia) {
		super(CollidableType.RIGID_BODY, t);

		linearVelocity.set(0f, 0f, 0f);
		angularVelocity.set(0f, 0f, 0f);
		angularFactor = 1f;
		gravity.set(0f, 0f, 0f);
		totalForce.set(0f, 0f, 0f);
		totalTorque.set(0f, 0f, 0f);


		float linearSleepingThreshold = 0.8f;
		float angularSleepingThreshold = 1.0f;


		linearDamping = 0f;
		angularDamping = 0.5f;
		this.linearSleepingThreshold = linearSleepingThreshold;
		this.angularSleepingThreshold = angularSleepingThreshold;
		//this.optionalMotionState = motionState;
		contactSolverType = 0;
		frictionSolverType = 0;

		/**
		 * Additional damping can help avoiding lowpass jitter motion, help stability for ragdolls etc.
		 * Such damping is undesirable, so once the overall simulation quality of the rigid body dynamics
		 * system has improved, this should become obsolete.
		 */
//		float additionalDampingFactor = 0.005f;
//		float additionalLinearDampingThresholdSqr = 0.01f;
//		float additionalAngularDampingThresholdSqr = 0.01f;
//		float additionalAngularDampingFactor = 0.01f;

//		this.additionalDamping = false;
//		this.additionalDampingFactor = additionalDampingFactor;
//		this.additionalLinearDampingThresholdSqr = additionalLinearDampingThresholdSqr;
//		this.additionalAngularDampingThresholdSqr = additionalAngularDampingThresholdSqr;
//		this.additionalAngularDampingFactor = additionalAngularDampingFactor;

//		if (optionalMotionState != null)
//		{
//			optionalMotionState.getWorldTransform(worldTransform);
//		} else
//		{
			this.worldTransform.setIdentity();
//		}

		interpolationWorldTransform.set(worldTransform);
		interpolationLinearVelocity.set(0f, 0f, 0f);
		interpolationAngularVelocity.set(0f, 0f, 0f);


		/** Best simulation results when friction is non-zero. */
		friction = 0.5f;

		/** Best simulation results using zero restitution. */
		restitution = 0f;

		setCollisionShape(collisionShape);

		setMass(mass, localInertia);
		setDamping(0,0);
		updateInertiaTensor();
	}
	
	public void destroy(Collisions world) {
		// No constraints should point to this rigidbody
		// Remove constraints from the dynamics world before you delete the related rigidbodies. 
		assert (constraintRefs.isEmpty());
		forceActivationState(Collidable.DISABLE_SIMULATION);
		data = null;

        Broadphasing bp = broadphase;
		if (bp != null) {
			//
			// only clear the cached algorithms
			//

			world.broadphase.getOverlappingPairCache().cleanProxyFromPairs(bp, world.intersecter);
			world.broadphase.destroyProxy(bp, world.intersecter);
			broadphase(null);
		} /*else {
        	//System.err.println(collidable + " missing broadphase");
			throw new RuntimeException(collidable + " missing broadphase");
		}*/

//		if (broadphaseHandle!=null) {
//			//broadphaseHandle.data = null;
//			broadphaseHandle = null;
//		}

//		if (collisionShape!=null) {
//			collisionShape.setUserPointer(null);
//			collisionShape = null;
//		}

	}


	public void proceedToTransform(Transform newTrans) {
		setCenterOfMassTransform(newTrans);
	}
	
	/**
	 * To keep collision detection and dynamics separate we don't store a rigidbody pointer,
	 * but a rigidbody is derived from CollisionObject, so we can safely perform an upcast.
	 */
	public static <X> Dynamic<X> ifDynamic(Collidable<X> colObj) {
		return colObj.getInternalType() == CollidableType.RIGID_BODY ? (Dynamic) colObj : null;
	}

	public static <X> Dynamic<X> ifDynamicAndActive(Collidable<X> colObj) {
		Dynamic<X> d = ifDynamic(colObj);
		return ((d == null) || !d.isActive()) ? null : d;
	}

	/**
	 * Continuous collision detection needs prediction.
	 */
	public void predictIntegratedTransform(float timeStep, Transform predictedTransform) {
		TransformUtil.integrateTransform(worldTransform, linearVelocity, angularVelocity, timeStep, predictedTransform);
	}


	public void saveKinematicState(float timeStep) {
		if (!isKinematicObject())
			return;

		//todo: clamp to some (user definable) safe minimum timestep, to limit maximum angular/linear velocities
		if (timeStep != 0f) {
//			//if we use motionstate to synchronize world transforms, get the new kinematic/animated world transform
//            if (optionalMotionState != null) {
//                optionalMotionState.getWorldTransform(worldTransform);
//			}
			//Vector3f linVel = new Vector3f(), angVel = new Vector3f();

			TransformUtil.calculateVelocity(interpolationWorldTransform, worldTransform, timeStep, linearVelocity, angularVelocity);
			interpolationLinearVelocity.set(linearVelocity);
			interpolationAngularVelocity.set(angularVelocity);
			interpolationWorldTransform.set(worldTransform);
		//printf("angular = %f %f %f\n",m_angularVelocity.getX(),m_angularVelocity.getY(),m_angularVelocity.getZ());
		}
	}
	
	public void applyGravity() {
		if (isStaticOrKinematicObject())
			return;

		applyCentralForce(gravity);
	}
	
	@Override
    public void setGravity(v3 acceleration) {
		if (inverseMass != 0f) {
			gravity.scale(1f / inverseMass, acceleration);
		}
	}

	public v3 getGravity(v3 out) {
		out.set(gravity);
		return out;
	}

	public void setDamping(float lin_damping, float ang_damping) {
		linearDamping = clamp(lin_damping, 0f, 1f);
		angularDamping = clamp(ang_damping, 0f, 1f);
	}

	public float getLinearDamping() {
		return linearDamping;
	}

	public float getAngularDamping() {
		return angularDamping;
	}

	public float getLinearSleepingThreshold() {
		return linearSleepingThreshold;
	}

	public float getAngularSleepingThreshold() {
		return angularSleepingThreshold;
	}

	/**
	 * Damps the velocity, using the given linearDamping and angularDamping.
	 */
	public void applyDamping(float timeStep) {
		// On new damping: see discussion/issue report here: http://code.google.com/p/bullet/issues/detail?id=74
		// todo: do some performance comparisons (but other parts of the engine are probably bottleneck anyway

		//#define USE_OLD_DAMPING_METHOD 1
		//#ifdef USE_OLD_DAMPING_METHOD
		//linearVelocity.scale(MiscUtil.GEN_clamped((1f - timeStep * linearDamping), 0f, 1f));
		//angularVelocity.scale(MiscUtil.GEN_clamped((1f - timeStep * angularDamping), 0f, 1f));
		//#else
		if (linearDamping > 0)
			linearVelocity.scale((float) Math.pow(1f - linearDamping, timeStep));
		if (angularDamping > 0)
			angularVelocity.scale((float) Math.pow(1f - angularDamping, timeStep));
		//#endif

//		if (additionalDamping) {
//			// Additional damping can help avoiding lowpass jitter motion, help stability for ragdolls etc.
//			// Such damping is undesirable, so once the overall simulation quality of the rigid body dynamics system has improved, this should become obsolete
//			if ((angularVelocity.lengthSquared() < additionalAngularDampingThresholdSqr) &&
//					(linearVelocity.lengthSquared() < additionalLinearDampingThresholdSqr)) {
//				angularVelocity.scale(additionalDampingFactor);
//				linearVelocity.scale(additionalDampingFactor);
//			}
//
//			float speed = linearVelocity.length();
//			if (speed < linearDamping) {
//				float dampVel = 0.005f;
//				if (speed > dampVel) {
//					v3 dir = new v3(linearVelocity);
//					dir.normalize();
//					dir.scale(dampVel);
//					linearVelocity.sub(dir);
//				}
//				else {
//					linearVelocity.set(0f, 0f, 0f);
//				}
//			}
//
//			float angSpeed = angularVelocity.length();
//			if (angSpeed < angularDamping) {
//				float angDampVel = 0.005f;
//				if (angSpeed > angDampVel) {
//					v3 dir = new v3(angularVelocity);
//					dir.normalize();
//					dir.scale(angDampVel);
//					angularVelocity.sub(dir);
//				}
//				else {
//					angularVelocity.set(0f, 0f, 0f);
//				}
//			}
//		}
	}

	public void setMass(float mass, v3 inertia) {
		setMass(mass);

		invInertiaLocal.set(inertia.x != 0f ? 1f / inertia.x : 0f,
				inertia.y != 0f ? 1f / inertia.y : 0f,
				inertia.z != 0f ? 1f / inertia.z : 0f);
	}

	public void setMass(float mass) {
		if (mass == 0f) {
			collisionFlags |= CollisionFlags.STATIC_OBJECT;
			inverseMass = 0f;
		}
		else {
			collisionFlags &= (~CollisionFlags.STATIC_OBJECT);
			inverseMass = 1f / mass;
		}
	}

	public float mass() {
		return 1f/inverseMass;
	}

	public float getInvMass() {
		return inverseMass;
	}

	public Matrix3f getInvInertiaTensorWorld(Matrix3f out) {
		out.set(invInertiaTensorWorld);
		return out;
	}
	
	public void integrateVelocities(float step) {
		if (isStaticOrKinematicObject()) {
			return;
		}

		linearVelocity.scaleAdd(inverseMass * step, totalForce, linearVelocity);
		v3 tmp = new v3(totalTorque);
		invInertiaTensorWorld.transform(tmp);
		angularVelocity.scaleAdd(step, tmp, angularVelocity);

		// clamp angular velocity. collision calculations will fail on higher angular velocities	
		float angvel = angularVelocity.lengthSquared();
		if (angvel > Util.sqr(MAX_ANGVEL/step)) {
			angularVelocity.scale((MAX_ANGVEL / step) / angvel);
		}
	}

	public void setCenterOfMassTransform(Transform xform) {
		interpolationWorldTransform.set(isStaticOrKinematicObject() ? worldTransform : xform);
		getLinearVelocity(interpolationLinearVelocity);
		getAngularVelocity(interpolationAngularVelocity);
		worldTransform.set(xform);
		updateInertiaTensor();
	}

	public void applyCentralForce(v3 force) {
		totalForce.add(force);
	}
	
	public v3 getInvInertiaDiagLocal(v3 out) {
		out.set(invInertiaLocal);
		return out;
	}

	public void setInvInertiaDiagLocal(v3 diagInvInertia) {
		invInertiaLocal.set(diagInvInertia);
	}

	public void setSleepingThresholds(float linear, float angular) {
		linearSleepingThreshold = linear;
		angularSleepingThreshold = angular;
	}

	public void torque(v3 torque) {
		totalTorque.add(torque);
	}

	public void force(v3 force, v3 rel_pos) {
		applyCentralForce(force);
		
		v3 tmp = new v3();
		tmp.cross(rel_pos, force);
		tmp.scale(angularFactor);
		torque(tmp);
	}

	/** applied to the center */
	public void impulse(v3 impulse) {
		linearVelocity.scaleAdd(inverseMass, impulse, linearVelocity);
	}
	public void impulse(float ix, float iy, float iz) {
		linearVelocity.scaleAdd(inverseMass, ix, iy, iz, linearVelocity);
	}
	

	public void torqueImpulse(v3 torque) {
		v3 tmp = new v3(torque);
		invInertiaTensorWorld.transform(tmp);
		angularVelocity.add(tmp);
	}


	public void impulse(v3 impulse, v3 rel_pos) {
		if (inverseMass != 0f) {
			impulse(impulse);
			if (angularFactor != 0f) {
				v3 tmp = new v3();
				tmp.cross(rel_pos, impulse);
				tmp.scale(angularFactor);
				torqueImpulse(tmp);
			}
		}
	}

	/**
	 * Optimization for the iterative solver: avoid calculating constant terms involving inertia, normal, relative position.
	 */
	public void internalApplyImpulse(v3 linearComponent, v3 angularComponent, float impulseMagnitude) {
		if (inverseMass != 0f) {
			linearVelocity.scaleAdd(impulseMagnitude, linearComponent, linearVelocity);
			if (angularFactor != 0f) {
				angularVelocity.scaleAdd(impulseMagnitude * angularFactor, angularComponent, angularVelocity);
			}
		}
	}

	public void clearForces() {
		totalForce.set(0f, 0f, 0f);
		totalTorque.set(0f, 0f, 0f);
	}
	
	public void updateInertiaTensor() {
		Matrix3f mat1 = new Matrix3f();
		MatrixUtil.scale(mat1, worldTransform.basis, invInertiaLocal);

		Matrix3f mat2 = new Matrix3f(worldTransform.basis);
		mat2.transpose();

		invInertiaTensorWorld.mul(mat1, mat2);
	}

	@Deprecated public v3 getCenterOfMassPosition(v3 out) {
		out.set(worldTransform);
		return out;
	}

	@Deprecated public Quat4f getOrientation(Quat4f out) {
		MatrixUtil.getRotation(worldTransform.basis, out);
		return out;
	}
	
	@Deprecated public Transform getCenterOfMassTransform(Transform out) {
		out.set(worldTransform);
		return out;
	}


	public v3 getLinearVelocity(v3 out) {
		out.set(linearVelocity);
		return out;
	}

	public v3 getAngularVelocity(v3 out) {
		out.set(angularVelocity);
		return out;
	}


	public void setLinearVelocity(float x, float y, float z) {
		linearVelocity.set(x, y, z);
	}

	public void setLinearVelocity(v3 lin_vel) {
		assert (collisionFlags != CollisionFlags.STATIC_OBJECT);
		linearVelocity.set(lin_vel);
	}

	public void setAngularVelocity(v3 ang_vel) {
		assert (collisionFlags != CollisionFlags.STATIC_OBJECT);
		angularVelocity.set(ang_vel);
	}

	public v3 getVelocityInLocalPoint(v3 rel_pos, v3 out) {
		// we also calculate lin/ang velocity for kinematic objects
		v3 vec = out;
		vec.cross(angularVelocity, rel_pos);
		vec.add(linearVelocity);
		return out;

		//for kinematic objects, we could also use use:
		//		return 	(m_worldTransform(rel_pos) - m_interpolationWorldTransform(rel_pos)) / m_kinematicTimeStep;
	}

	public void translate(v3 v) {
		worldTransform.add(v);
	}


	public void getAabb(v3 aabbMin, v3 aabbMax) {
		shape().getAabb(worldTransform, aabbMin, aabbMax);
	}

	public float computeImpulseDenominator(v3 pos, v3 normal) {
		v3 r0 = new v3();
		r0.sub(pos, worldTransform);

		v3 c0 = new v3();
		c0.cross(r0, normal);

		v3 tmp = new v3();
		MatrixUtil.transposeTransform(tmp, c0, invInertiaTensorWorld);

		v3 vec = new v3();
		vec.cross(tmp, r0);

		return inverseMass + normal.dot(vec);
	}

	public float computeAngularImpulseDenominator(v3 axis) {
		v3 vec = new v3();
		MatrixUtil.transposeTransform(vec, axis, invInertiaTensorWorld);
		return axis.dot(vec);
	}

	public void updateDeactivation(float timeStep) {
		int state = getActivationState();
		if ((state == ISLAND_SLEEPING) || (state == DISABLE_DEACTIVATION)) {
			return;
		}

		if ((linearVelocity.lengthSquared() < linearSleepingThreshold * linearSleepingThreshold) &&
				(angularVelocity.lengthSquared() < angularSleepingThreshold * angularSleepingThreshold)) {
			deactivationTime += timeStep;
		}
		else {
			deactivationTime = 0f;
			setActivationState(0);
		}
	}

	public boolean wantsSleeping() {
		if (getActivationState() == DISABLE_DEACTIVATION) {
			return false;
		}

		// disable deactivation
		if (BulletGlobals.isDeactivationDisabled() || (BulletGlobals.getDeactivationTime() == 0f)) {
			return false;
		}

		if ((getActivationState() == ISLAND_SLEEPING) || (getActivationState() == WANTS_DEACTIVATION)) {
			return true;
		}

		return deactivationTime > BulletGlobals.getDeactivationTime();
	}


//	public MotionState getMotionState() {
//		return optionalMotionState;
//	}

//	public void setMotionState(MotionState motionState) {
//		this.optionalMotionState = motionState;
//		if (optionalMotionState != null) {
//			motionState.getWorldTransform(worldTransform);
//		}
//	}

	public void setAngularFactor(float angFac) {
		angularFactor = angFac;
	}

	public float getAngularFactor() {
		return angularFactor;
	}

//	/**
//	 * Is this rigidbody added to a CollisionWorld/DynamicsWorld/Broadphase?
//	 */
//	public boolean isInWorld() {
//        return (broadphaseHandle != null);
//	}

	@Override
	public boolean checkCollideWithOverride(Collidable co) {
		// TODO: change to cast
		Dynamic otherRb = ifDynamic(co);
		if (otherRb == null) {
			return true;
		}

		for (int i = 0; i < constraintRefs.size(); ++i) {
			//return array[index];
			TypedConstraint c = constraintRefs.get(i);
			if (c.getRigidBodyA() == otherRb || c.getRigidBodyB() == otherRb) {
				return false;
			}
		}

		return true;
	}

	public void addConstraintRef(TypedConstraint c) {
		int index = constraintRefs.indexOf(c);
		if (index == -1) {
			constraintRefs.add(c);
		}

		checkCollideWith = true;
	}
	
	public void removeConstraintRef(TypedConstraint c) {
		constraintRefs.remove(c);
		checkCollideWith = !constraintRefs.isEmpty();
	}

	public TypedConstraint getConstraintRef(int index) {
		return constraintRefs.get(index);
		//return array[index];
	}

	public void velAdd(v3 delta) {
		linearVelocity.add(delta);
	}

	public void force(v3 delta) {
		applyCentralForce(delta);
	}


//
//	public int getNumConstraintRefs() {
//		return constraintRefs.size();
//	}

}
