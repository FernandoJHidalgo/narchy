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

package spacegraph.phys.collision.broad;

import org.codehaus.commons.nullanalysis.NotNull;
import org.jetbrains.annotations.Nullable;
import spacegraph.phys.Collidable;
import spacegraph.phys.Dynamic;

/**
 * BroadphaseProxy is the main class that can be used with the Bullet broadphases.
 * It stores collision shape type information, collision filter information and
 * a client object, typically a {@link Collidable} or {@link Dynamic}.
 * 
 * @author jezek2
 */
public class Broadphasing {

	// Usually the client CollisionObject or Rigidbody class
	@Nullable
	public Collidable data;
	
	// TODO: mask
	public short collisionFilterGroup;
	public short collisionFilterMask;
	
	public Object multiSapParentProxy;
	
	public int uid; // uniqueId is introduced for paircache. could get rid of this, by calculating the address offset etc.


	protected Broadphasing() {

	}
	
	public Broadphasing(@NotNull Collidable userPtr, short collisionFilterGroup, short collisionFilterMask) {
		this(userPtr, collisionFilterGroup, collisionFilterMask, null);
	}
	
	public Broadphasing(Collidable userPtr, short collisionFilterGroup, short collisionFilterMask, Object multiSapParentProxy) {
		this.data = userPtr;
		this.collisionFilterGroup = collisionFilterGroup;
		this.collisionFilterMask = collisionFilterMask;
		this.multiSapParentProxy = multiSapParentProxy;
	}

	@Override
	public final int hashCode() {
		return uid;
	}
}
