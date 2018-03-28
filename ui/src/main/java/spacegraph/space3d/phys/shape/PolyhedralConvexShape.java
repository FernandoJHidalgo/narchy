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

package spacegraph.space3d.phys.shape;

import spacegraph.space3d.phys.math.AabbUtil2;
import spacegraph.space3d.phys.math.Transform;
import spacegraph.space3d.phys.math.VectorUtil;
import spacegraph.util.math.v3;

/**
 * PolyhedralConvexShape is an internal interface class for polyhedral convex shapes.
 * 
 * @author jezek2
 */
public abstract class PolyhedralConvexShape extends ConvexInternalShape {

	private static final v3[] _directions = {
		new v3( 1f,  0f,  0f),
		new v3( 0f,  1f,  0f),
		new v3( 0f,  0f,  1f),
		new v3(-1f,  0f,  0f),
		new v3( 0f, -1f,  0f),
		new v3( 0f,  0f, -1f)
	};

	private static final v3[] _supporting = {
		new v3(0f, 0f, 0f),
		new v3(0f, 0f, 0f),
		new v3(0f, 0f, 0f),
		new v3(0f, 0f, 0f),
		new v3(0f, 0f, 0f),
		new v3(0f, 0f, 0f)
	};
	
	protected final v3 localAabbMin = new v3(1f, 1f, 1f);
	protected final v3 localAabbMax = new v3(-1f, -1f, -1f);
	protected boolean isLocalAabbValid;

//	/** optional Hull is for optional Separating Axis Test Hull collision detection, see Hull.cpp */
//	public Hull optionalHull = null;
	
	@Override
	public v3 localGetSupportingVertexWithoutMargin(v3 vec0, v3 out) {
		int i;
		v3 supVec = out;
		supVec.set(0f, 0f, 0f);

		float maxDot = -1e30f;

		v3 vec = new v3(vec0);
		float lenSqr = vec.lengthSquared();
		if (lenSqr < 0.0001f) {
			vec.set(1f, 0f, 0f);
		}
		else {
			float rlen = 1f / (float) Math.sqrt(lenSqr);
			vec.scale(rlen);
		}

		v3 vtx = new v3();
		float newDot;

		for (i = 0; i < getNumVertices(); i++) {
			getVertex(i, vtx);
			newDot = vec.dot(vtx);
			if (newDot > maxDot) {
				maxDot = newDot;
				supVec = vtx;
			}
		}

		return out;
	}

	@Override
	public void batchedUnitVectorGetSupportingVertexWithoutMargin(v3[] vectors, v3[] supportVerticesOut, int numVectors) {
		int i;

		v3 vtx = new v3();
		float newDot;

		// JAVA NOTE: rewritten as code used W coord for temporary usage in Vector3
		// TODO: optimize it
		float[] wcoords = new float[numVectors];

		for (i = 0; i < numVectors; i++) {
			// TODO: used w in vector3:
			//supportVerticesOut[i].w = -1e30f;
			wcoords[i] = -1e30f;
		}

		for (int j = 0; j < numVectors; j++) {
			v3 vec = vectors[j];

			for (i = 0; i < getNumVertices(); i++) {
				getVertex(i, vtx);
				newDot = vec.dot(vtx);
				//if (newDot > supportVerticesOut[j].w)
				if (newDot > wcoords[j]) {
					//WARNING: don't swap next lines, the w component would get overwritten!
					supportVerticesOut[j].set(vtx);
					//supportVerticesOut[j].w = newDot;
					wcoords[j] = newDot;
				}
			}
		}
	}

	@Override
	public void calculateLocalInertia(float mass, v3 inertia) {
		// not yet, return box inertia

		float margin = getMargin();

		Transform ident = new Transform();
		ident.setIdentity();
		v3 aabbMin = new v3(), aabbMax = new v3();
		getAabb(ident, aabbMin, aabbMax);

		v3 halfExtents = new v3();
		halfExtents.sub(aabbMax, aabbMin);
		halfExtents.scale(0.5f);

		float lx = 2f * (halfExtents.x + margin);
		float ly = 2f * (halfExtents.y + margin);
		float lz = 2f * (halfExtents.z + margin);
		float x2 = lx * lx;
		float y2 = ly * ly;
		float z2 = lz * lz;
		float scaledmass = mass * 0.08333333f;

		inertia.set(y2 + z2, x2 + z2, x2 + y2);
		inertia.scale(scaledmass);
	}

	private void getNonvirtualAabb(Transform trans, v3 aabbMin, v3 aabbMax, float margin) {
		// lazy evaluation of local aabb
		assert (isLocalAabbValid);

		AabbUtil2.transformAabb(localAabbMin, localAabbMax, margin, trans, aabbMin, aabbMax);
	}
	
	@Override
	public void getAabb(Transform trans, v3 aabbMin, v3 aabbMax) {
		getNonvirtualAabb(trans, aabbMin, aabbMax, getMargin());
	}

	protected final void _PolyhedralConvexShape_getAabb(Transform trans, v3 aabbMin, v3 aabbMax) {
		getNonvirtualAabb(trans, aabbMin, aabbMax, getMargin());
	}

	public void recalcLocalAabb() {
		isLocalAabbValid = true;

		//#if 1

		batchedUnitVectorGetSupportingVertexWithoutMargin(_directions, _supporting, 6);

		for (int i=0; i<3; i++) {
			VectorUtil.setCoord(localAabbMax, i, VectorUtil.coord(_supporting[i], i) + collisionMargin);
			VectorUtil.setCoord(localAabbMin, i, VectorUtil.coord(_supporting[i + 3], i) - collisionMargin);
		}
		
		//#else
		//for (int i=0; i<3; i++) {
		//	Vector3f vec = new Vector3f();
		//	vec.set(0f, 0f, 0f);
		//	VectorUtil.setCoord(vec, i, 1f);
		//	Vector3f tmp = localGetSupportingVertex(vec, new Vector3f());
		//	VectorUtil.setCoord(localAabbMax, i, VectorUtil.getCoord(tmp, i) + collisionMargin);
		//	VectorUtil.setCoord(vec, i, -1f);
		//	localGetSupportingVertex(vec, tmp);
		//	VectorUtil.setCoord(localAabbMin, i, VectorUtil.getCoord(tmp, i) - collisionMargin);
		//}
		//#endif
	}

	@Override
	public void setLocalScaling(v3 scaling) {
		super.setLocalScaling(scaling);
		recalcLocalAabb();
	}

	public abstract int getNumVertices();

	public abstract int getNumEdges();

	public abstract void getEdge(int i, v3 pa, v3 pb);

	public abstract void getVertex(int i, v3 vtx);

	public abstract int getNumPlanes();

	public abstract void getPlane(v3 planeNormal, v3 planeSupport, int i);
	
//	public abstract  int getIndex(int i) const = 0 ; 
	
	public abstract boolean isInside(v3 pt, float tolerance);
	
}
