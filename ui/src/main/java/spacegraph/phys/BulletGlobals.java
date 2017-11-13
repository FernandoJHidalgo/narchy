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

import spacegraph.phys.collision.ContactAddedCallback;
import spacegraph.phys.collision.ContactDestroyedCallback;
import spacegraph.phys.collision.ContactProcessedCallback;


/**
 * Bullet global settings and constants.
 * 
 * @author jezek2
 */
public class BulletGlobals {
	
	public static final boolean DEBUG = false;
	
	public static final float CONVEX_DISTANCE_MARGIN = 0.04f;
	public static final float FLT_EPSILON = 1.19209290e-07f;
	public static final float SIMD_EPSILON = FLT_EPSILON;
	
	public static final float SIMD_2_PI = 6.283185307179586232f;
	public static final float SIMD_PI = SIMD_2_PI * 0.5f;
	public static final float SIMD_HALF_PI = SIMD_2_PI * 0.25f;
	public static final float SIMD_RADS_PER_DEG = SIMD_2_PI / 360f;
	public static final float SIMD_DEGS_PER_RAD = 360f / SIMD_2_PI;
	public static final float SIMD_INFINITY = Float.MAX_VALUE;

	////////////////////////////////////////////////////////////////////////////

	public static final ThreadLocal<BulletGlobals> the = ThreadLocal.withInitial(BulletGlobals::new);

	private ContactDestroyedCallback gContactDestroyedCallback;
	private ContactAddedCallback gContactAddedCallback;
	private ContactProcessedCallback gContactProcessedCallback;

	protected float contactBreakingThreshold = 0.02f;
	// RigidBody
	private float deactivationTime = 2f;
	private boolean disableDeactivation;

	public ContactAddedCallback getContactAddedCallback() {
		return gContactAddedCallback;
	}

	public void setContactAddedCallback(ContactAddedCallback callback) {
		gContactAddedCallback = callback;
	}

	public ContactDestroyedCallback getContactDestroyedCallback() {
		return gContactDestroyedCallback;
	}

	public void setContactDestroyedCallback(ContactDestroyedCallback callback) {
		gContactDestroyedCallback = callback;
	}

	public ContactProcessedCallback getContactProcessedCallback() {
		return gContactProcessedCallback;
	}

	public void setContactProcessedCallback(ContactProcessedCallback callback) {
		gContactProcessedCallback = callback;
	}
	
	////////////////////////////////////////////////////////////////////////////

	public float getContactBreakingThreshold() {
		return contactBreakingThreshold;
	}

	public void setContactBreakingThreshold(float threshold) {
		contactBreakingThreshold = threshold;
	}

	public float getDeactivationTime() {
		return deactivationTime;
	}

	public void setDeactivationTime(float time) {
		deactivationTime = time;
	}

	public boolean isDeactivationDisabled() {
		return disableDeactivation;
	}

	public void setDeactivationDisabled(boolean disable) {
		disableDeactivation = disable;
	}

	////////////////////////////////////////////////////////////////////////////

	/**
	 * Cleans all current thread specific settings and caches.
	 */
//	public static void cleanCurrentThread() {
//		threadLocal.remove();
//		ArrayPool.cleanCurrentThread();
//	}

}
