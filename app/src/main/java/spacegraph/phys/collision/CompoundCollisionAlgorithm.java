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

package spacegraph.phys.collision;

import spacegraph.phys.Collidable;
import spacegraph.phys.collision.broad.CollisionAlgorithm;
import spacegraph.phys.collision.broad.CollisionAlgorithmConstructionInfo;
import spacegraph.phys.collision.broad.DispatcherInfo;
import spacegraph.phys.collision.narrow.PersistentManifold;
import spacegraph.phys.math.Transform;
import spacegraph.phys.shape.CollisionShape;
import spacegraph.phys.shape.CompoundShape;
import spacegraph.phys.util.OArrayList;


/**
 * CompoundCollisionAlgorithm supports collision between {@link CompoundShape}s and
 * other collision shapes.
 * 
 * @author jezek2
 */
public class CompoundCollisionAlgorithm extends CollisionAlgorithm {

	private final OArrayList<CollisionAlgorithm> childCollisionAlgorithms = new OArrayList<CollisionAlgorithm>();
	private boolean isSwapped;
	
	public void init(CollisionAlgorithmConstructionInfo ci, Collidable body0, Collidable body1, boolean isSwapped) {
		super.init(ci);

		this.isSwapped = isSwapped;

		Collidable colObj = isSwapped ? body1 : body0;
		Collidable otherObj = isSwapped ? body0 : body1;
		assert (colObj.shape().isCompound());

		CompoundShape compoundShape = (CompoundShape) colObj.shape();
		int numChildren = compoundShape.getNumChildShapes();
		int i;

		//childCollisionAlgorithms.resize(numChildren);
		for (i = 0; i < numChildren; i++) {
			CollisionShape tmpShape = colObj.shape();
			CollisionShape childShape = compoundShape.getChildShape(i);
			colObj.internalSetTemporaryCollisionShape(childShape);
			childCollisionAlgorithms.add(ci.intersecter1.findAlgorithm(colObj, otherObj));
			colObj.internalSetTemporaryCollisionShape(tmpShape);
		}
	}

	@Override
	public void destroy() {
		int numChildren = childCollisionAlgorithms.size();
		for (int i=0; i<numChildren; i++) {
			//childCollisionAlgorithms.get(i).destroy();
            //return array[index];
            intersecter.freeCollisionAlgorithm(childCollisionAlgorithms.get(i));
		}
		childCollisionAlgorithms.clear();
	}

	@Override
	public void processCollision(Collidable body0, Collidable body1, DispatcherInfo dispatchInfo, ManifoldResult resultOut) {
		Collidable colObj = isSwapped ? body1 : body0;
		Collidable otherObj = isSwapped ? body0 : body1;

		assert (colObj.shape().isCompound());
		CompoundShape compoundShape = (CompoundShape) colObj.shape();

		// We will use the OptimizedBVH, AABB tree to cull potential child-overlaps
		// If both proxies are Compound, we will deal with that directly, by performing sequential/parallel tree traversals
		// given Proxy0 and Proxy1, if both have a tree, Tree0 and Tree1, this means:
		// determine overlapping nodes of Proxy1 using Proxy0 AABB against Tree1
		// then use each overlapping node AABB against Tree0
		// and vise versa.

		Transform tmpTrans = new Transform();
		Transform orgTrans = new Transform();
		Transform childTrans = new Transform();
		Transform orgInterpolationTrans = new Transform();
		Transform newChildWorldTrans = new Transform();

		int numChildren = childCollisionAlgorithms.size();
		int i;
		for (i = 0; i < numChildren; i++) {
			// temporarily exchange parent btCollisionShape with childShape, and recurse
			CollisionShape childShape = compoundShape.getChildShape(i);

			// backup
			colObj.getWorldTransform(orgTrans);
			colObj.getInterpolationWorldTransform(orgInterpolationTrans);

			compoundShape.getChildTransform(i, childTrans);
			newChildWorldTrans.mul(orgTrans, childTrans);
			colObj.setWorldTransform(newChildWorldTrans);
			colObj.setInterpolationWorldTransform(newChildWorldTrans);

			// the contactpoint is still projected back using the original inverted worldtrans
			CollisionShape tmpShape = colObj.shape();
			colObj.internalSetTemporaryCollisionShape(childShape);
            //return array[index];
            childCollisionAlgorithms.get(i).processCollision(colObj, otherObj, dispatchInfo, resultOut);
			// revert back
			colObj.internalSetTemporaryCollisionShape(tmpShape);
			colObj.setWorldTransform(orgTrans);
			colObj.setInterpolationWorldTransform(orgInterpolationTrans);
		}
	}

	@Override
	public float calculateTimeOfImpact(Collidable body0, Collidable body1, DispatcherInfo dispatchInfo, ManifoldResult resultOut) {
		Collidable colObj = isSwapped ? body1 : body0;
		Collidable otherObj = isSwapped ? body0 : body1;

		assert (colObj.shape().isCompound());

		CompoundShape compoundShape = (CompoundShape) colObj.shape();

		// We will use the OptimizedBVH, AABB tree to cull potential child-overlaps
		// If both proxies are Compound, we will deal with that directly, by performing sequential/parallel tree traversals
		// given Proxy0 and Proxy1, if both have a tree, Tree0 and Tree1, this means:
		// determine overlapping nodes of Proxy1 using Proxy0 AABB against Tree1
		// then use each overlapping node AABB against Tree0
		// and vise versa.

		Transform tmpTrans = new Transform();
		Transform orgTrans = new Transform();
		Transform childTrans = new Transform();
		float hitFraction = 1f;

		int numChildren = childCollisionAlgorithms.size();
		int i;
		for (i = 0; i < numChildren; i++) {
			// temporarily exchange parent btCollisionShape with childShape, and recurse
			CollisionShape childShape = compoundShape.getChildShape(i);

			// backup
			colObj.getWorldTransform(orgTrans);

			compoundShape.getChildTransform(i, childTrans);
			//btTransform	newChildWorldTrans = orgTrans*childTrans ;
			tmpTrans.set(orgTrans);
			tmpTrans.mul(childTrans);
			colObj.setWorldTransform(tmpTrans);

			CollisionShape tmpShape = colObj.shape();
			colObj.internalSetTemporaryCollisionShape(childShape);
            //return array[index];
            float frac = childCollisionAlgorithms.get(i).calculateTimeOfImpact(colObj, otherObj, dispatchInfo, resultOut);
			if (frac < hitFraction) {
				hitFraction = frac;
			}
			// revert back
			colObj.internalSetTemporaryCollisionShape(tmpShape);
			colObj.setWorldTransform(orgTrans);
		}
		return hitFraction;
	}

	@Override
	public void getAllContactManifolds(OArrayList<PersistentManifold> manifoldArray) {
		for (int i=0; i<childCollisionAlgorithms.size(); i++) {
            //return array[index];
            childCollisionAlgorithms.get(i).getAllContactManifolds(manifoldArray);
		}
	}

	////////////////////////////////////////////////////////////////////////////

	public static class CreateFunc extends CollisionAlgorithmCreateFunc {

		@Override
		public CollisionAlgorithm createCollisionAlgorithm(CollisionAlgorithmConstructionInfo ci, Collidable body0, Collidable body1) {
			CompoundCollisionAlgorithm algo = new CompoundCollisionAlgorithm();
			algo.init(ci, body0, body1, false);
			return algo;
		}

		@Override
		public void releaseCollisionAlgorithm(CollisionAlgorithm algo) {

		}
	}

	public static class SwappedCreateFunc extends CollisionAlgorithmCreateFunc {

		@Override
		public CollisionAlgorithm createCollisionAlgorithm(CollisionAlgorithmConstructionInfo ci, Collidable body0, Collidable body1) {
			CompoundCollisionAlgorithm algo = new CompoundCollisionAlgorithm();
			algo.init(ci, body0, body1, true);
			return algo;
		}

		@Override
		public void releaseCollisionAlgorithm(CollisionAlgorithm algo) {

		}
	}

}
