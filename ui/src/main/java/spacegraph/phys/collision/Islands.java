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

import jcog.list.FasterList;
import spacegraph.phys.BulletStats;
import spacegraph.phys.Collidable;
import spacegraph.phys.Collisions;
import spacegraph.phys.collision.broad.BroadphasePair;
import spacegraph.phys.collision.broad.Intersecter;
import spacegraph.phys.collision.narrow.PersistentManifold;
import spacegraph.phys.math.MiscUtil;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * SimulationIslandManager creates and handles simulation islands, using {@link UnionFind}.
 * 
 * @author jezek2
 */
public class Islands {

	//public final UnionFind find = new UnionFind();
	public final UnionFind2 find = new UnionFind2();

	private final FasterList<PersistentManifold> islandmanifold = new FasterList<>();
	private final FasterList<Collidable> islandBodies = new FasterList<>();

	public void initUnionFind(int n) {
		find.reset(n);
	}

	public void findUnions(Intersecter intersecter, Collisions colWorld) {
		FasterList<BroadphasePair> pairPtr = colWorld.pairs().getOverlappingPairArray();
		for (int i=0; i<pairPtr.size(); i++) {
			//return array[index];
			BroadphasePair collisionPair = pairPtr.get(i);

			Collidable colObj0 = collisionPair.pProxy0.data;
			if (colObj0 == null)
				continue;
			Collidable colObj1 = collisionPair.pProxy1.data;
			if (colObj1 == null)
				continue;

			if (( ((colObj0).mergesSimulationIslands())) &&
					( ((colObj1).mergesSimulationIslands()))) {

				find.unite((colObj0).getIslandTag(), (colObj1).getIslandTag());
			}
		}
	}

	public final void updateActivationState(Collisions<?> colWorld, Intersecter intersecter) {
		int num = colWorld.getNumCollisionObjects();

		initUnionFind(num);

		final int[] i = {0};
		colWorld.collidables().forEach((collidable)->{
			collidable.setIslandTag(i[0]++);
			collidable.setCompanionId(-1);
			collidable.setHitFraction(1f);
		});

		findUnions(intersecter, colWorld);
	}

	public final void storeIslandActivationState(Collisions<?> world) {
		// put the islandId ('find' value) into m_tag
		int i = 0;
		for (Collidable collidable : world.collidables()) {
			storeIslandActivationState(i++, collidable);
		}
    }

	final boolean storeIslandActivationState(int i, Collidable c) {
		if (!c.isStaticOrKinematicObject()) {
			c.setIslandTag(find.find(i));
			c.setCompanionId(-1);
		} else {
			c.setIslandTag(-1);
			c.setCompanionId(-2);
		}
		return true;
	}

	private static int getIslandId(PersistentManifold lhs) {
		int islandId;
		Collidable rcolObj0 = (Collidable) lhs.getBody0();
		Collidable rcolObj1 = (Collidable) lhs.getBody1();
		islandId = rcolObj0.getIslandTag() >= 0? rcolObj0.getIslandTag() : rcolObj1.getIslandTag();
		return islandId;
	}

	public void buildIslands(Intersecter intersecter, List<Collidable> collidables) {

		//System.out.println("build islands");

		BulletStats.pushProfile("islandUnionFindAndQuickSort");
		try {
			islandmanifold.clear();

			// we are going to sort the unionfind array, and store the element id in the size
			// afterwards, we clean unionfind, to make sure no-one uses it anymore

            find.sortIslands();
            int numElem = find.size();

			int endIslandIndex = 1;
			int startIslandIndex;

			// update the sleeping state for bodies, if all are sleeping
			for (startIslandIndex = 0; startIslandIndex < numElem; startIslandIndex = endIslandIndex) {
                int islandId = find.getId(startIslandIndex);
                for (endIslandIndex = startIslandIndex + 1; (endIslandIndex < numElem) && (find.getId(endIslandIndex) == islandId); endIslandIndex++) {
				}

				//int numSleeping = 0;

				boolean allSleeping = true;

				int idx;
				for (idx = startIslandIndex; idx < endIslandIndex; idx++) {
                    int i = find.getSz(idx);

					//return array[index];
					Collidable colObj0 = collidables.get(i);
					if ((colObj0.getIslandTag() != islandId) && (colObj0.getIslandTag() != -1)) {
						islandError(colObj0);
						continue;
					}

					assert ((colObj0.getIslandTag() == islandId) || (colObj0.getIslandTag() == -1));
					if (colObj0.getIslandTag() == islandId) {
						if (colObj0.getActivationState() == Collidable.ACTIVE_TAG) {
							allSleeping = false;
						}
						if (colObj0.getActivationState() == Collidable.DISABLE_DEACTIVATION) {
							allSleeping = false;
						}
					}
				}


				if (allSleeping) {
					//int idx;
					for (idx = startIslandIndex; idx < endIslandIndex; idx++) {
                        int i = find.getSz(idx);
						//return array[index];
						Collidable colObj0 = collidables.get(i);
						if ((colObj0.getIslandTag() != islandId) && (colObj0.getIslandTag() != -1)) {
							islandError(colObj0);
							continue;
						}

						assert ((colObj0.getIslandTag() == islandId) || (colObj0.getIslandTag() == -1));

						if (colObj0.getIslandTag() == islandId) {
							colObj0.setActivationState(Collidable.ISLAND_SLEEPING);
						}
					}
				}
				else {

					//int idx;
					for (idx = startIslandIndex; idx < endIslandIndex; idx++) {
                        int i = find.getSz(idx);

						//return array[index];
						Collidable colObj0 = collidables.get(i);
						int tag = colObj0.getIslandTag();
						if ((tag != islandId) && (tag != -1)) {
							islandError(colObj0);
							continue;
						}

						assert ((tag == islandId) || (tag == -1));

						if (tag == islandId) {
							if (colObj0.getActivationState() == Collidable.ISLAND_SLEEPING) {
								colObj0.setActivationState(Collidable.WANTS_DEACTIVATION);
							}
						}
					}
				}
			}


			int i;
			int maxNumManifolds = intersecter.getNumManifolds();

			//#define SPLIT_ISLANDS 1
			//#ifdef SPLIT_ISLANDS
			//#endif //SPLIT_ISLANDS

			for (i = 0; i < maxNumManifolds; i++) {
				PersistentManifold manifold = intersecter.getManifoldByIndexInternal(i);

				Collidable colObj0 = (Collidable) manifold.getBody0();
				Collidable colObj1 = (Collidable) manifold.getBody1();

				// todo: check sleeping conditions!
				if (((colObj0 != null) && colObj0.getActivationState() != Collidable.ISLAND_SLEEPING) ||
						((colObj1 != null) && colObj1.getActivationState() != Collidable.ISLAND_SLEEPING)) {

					// kinematic objects don't merge islands, but wake up all connected objects
					if (colObj0.isKinematicObject() && colObj0.getActivationState() != Collidable.ISLAND_SLEEPING) {
						colObj1.activate();
					}
					if (colObj1.isKinematicObject() && colObj1.getActivationState() != Collidable.ISLAND_SLEEPING) {
						colObj0.activate();
					}
					//#ifdef SPLIT_ISLANDS
					//filtering for response
					if (intersecter.needsResponse(colObj0, colObj1)) {
						islandmanifold.add(manifold);
					}
					//#endif //SPLIT_ISLANDS
				}
			}
		}
		finally {
			BulletStats.popProfile();
		}
	}

	public void islandError(Collidable colObj0) {
		//System.err.println("error in island management: " + colObj0 + " " + colObj0.data());
	}

	public <X> void buildAndProcessIslands(Intersecter intersecter, List<Collidable> collidables, IslandCallback callback) {
		buildIslands(intersecter, collidables);

		int endIslandIndex = 1;
		int startIslandIndex;
        int numElem = find.size();

		BulletStats.pushProfile("processIslands");
		try {
			//#ifndef SPLIT_ISLANDS
			//btPersistentManifold** manifold = dispatcher->getInternalManifoldPointer();
			//
			//callback->ProcessIsland(&collisionObjects[0],collisionObjects.size(),manifold,maxNumManifolds, -1);
			//#else
			// Sort manifolds, based on islands
			// Sort the vector using predicate and std::sort
			//std::sort(islandmanifold.begin(), islandmanifold.end(), btPersistentManifoldSortPredicate);

			int numManifolds = islandmanifold.size();

			// we should do radix sort, it it much faster (O(n) instead of O (n log2(n))
			//islandmanifold.heapSort(btPersistentManifoldSortPredicate());

			// JAVA NOTE: memory optimized sorting with caching of temporary array
			//Collections.sort(islandmanifold, persistentManifoldComparator);
			MiscUtil.quickSort(islandmanifold, persistentManifoldComparator);

			// now process all active islands (sets of manifolds for now)

			int startManifoldIndex = 0;
			int endManifoldIndex = 1;

			//int islandId;

			//printf("Start Islands\n");

			// traverse the simulation islands, and call the solver, unless all objects are sleeping/deactivated
			for (startIslandIndex = 0; startIslandIndex < numElem; startIslandIndex = endIslandIndex) {
                int islandId = find.getId(startIslandIndex);
				boolean islandSleeping = false;

				for (endIslandIndex = startIslandIndex; (endIslandIndex < numElem) && ((find.getId(endIslandIndex) == islandId)); endIslandIndex++) {
					int i = find.getSz(endIslandIndex);
					//return array[index];
					Collidable colObj0 = collidables.get(i);
					islandBodies.add(colObj0);
					if (!colObj0.isActive()) {
						islandSleeping = true;
					}
				}


				// find the accompanying contact manifold for this islandId
				int numIslandManifolds = 0;
				//ObjectArrayList<PersistentManifold> startManifold = null;
				int startManifold_idx = -1;

				if (startManifoldIndex < numManifolds) {
					//return array[index];
					int curIslandId = getIslandId(islandmanifold.get(startManifoldIndex));
					if (curIslandId == islandId) {
						//startManifold = &m_islandmanifold[startManifoldIndex];
						//startManifold = islandmanifold.subList(startManifoldIndex, islandmanifold.size());
						startManifold_idx = startManifoldIndex;

						//return array[index];
						for (endManifoldIndex = startManifoldIndex + 1; (endManifoldIndex < numManifolds) && (islandId == getIslandId(islandmanifold.get(endManifoldIndex))); endManifoldIndex++) {

						}
						// Process the actual simulation, only if not sleeping/deactivated
						numIslandManifolds = endManifoldIndex - startManifoldIndex;
					}

				}


				if (!islandSleeping) {
					callback.processIsland(islandBodies, islandmanifold, startManifold_idx, numIslandManifolds, islandId);
					//printf("Island callback of size:%d bodies, %d manifolds\n",islandBodies.size(),numIslandManifolds);
				}

				if (numIslandManifolds != 0) {
					startManifoldIndex = endManifoldIndex;
				}

				islandBodies.clear();
			}


			//#endif //SPLIT_ISLANDS
		}
		finally {
			BulletStats.popProfile();
		}
	}

	////////////////////////////////////////////////////////////////////////////
	
	public static abstract class IslandCallback {
		public abstract void processIsland(Collection<Collidable> bodies, FasterList<PersistentManifold> manifolds, int manifolds_offset, int numManifolds, int islandId);
	}
	
	private static final Comparator<PersistentManifold> persistentManifoldComparator = (lhs, rhs) -> getIslandId(lhs) < getIslandId(rhs)? -1 : +1;
	
}
