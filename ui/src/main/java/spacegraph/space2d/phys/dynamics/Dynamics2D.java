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
package spacegraph.space2d.phys.dynamics;

import jcog.data.map.ConcurrentFastIteratingHashSet;
import jcog.data.list.FasterList;
import jcog.data.list.MetalConcurrentQueue;
import jcog.math.FloatSupplier;
import spacegraph.space2d.phys.callbacks.*;
import spacegraph.space2d.phys.collision.AABB;
import spacegraph.space2d.phys.collision.RayCastInput;
import spacegraph.space2d.phys.collision.RayCastOutput;
import spacegraph.space2d.phys.collision.TimeOfImpact;
import spacegraph.space2d.phys.collision.broadphase.BroadPhase;
import spacegraph.space2d.phys.collision.broadphase.BroadPhaseStrategy;
import spacegraph.space2d.phys.collision.broadphase.DefaultBroadPhaseBuffer;
import spacegraph.space2d.phys.collision.broadphase.DynamicTree;
import spacegraph.space2d.phys.collision.shapes.PolygonShape;
import spacegraph.space2d.phys.collision.shapes.Shape;
import spacegraph.space2d.phys.common.*;
import spacegraph.space2d.phys.dynamics.contacts.Contact;
import spacegraph.space2d.phys.dynamics.contacts.ContactEdge;
import spacegraph.space2d.phys.dynamics.joints.Joint;
import spacegraph.space2d.phys.dynamics.joints.JointDef;
import spacegraph.space2d.phys.dynamics.joints.JointEdge;
import spacegraph.space2d.phys.fracture.Fracture;
import spacegraph.space2d.phys.fracture.FractureListener;
import spacegraph.space2d.phys.fracture.fragmentation.Smasher;
import spacegraph.space2d.phys.particle.*;
import spacegraph.space2d.phys.pooling.IWorldPool;
import spacegraph.space2d.phys.pooling.Vec2Array;
import spacegraph.space2d.phys.pooling.normal.DefaultWorldPool;
import spacegraph.util.math.Tuple2f;
import spacegraph.util.math.v2;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * The world class manages all physics entities, dynamic simulation, and asynchronous queries. The
 * world also contains efficient memory management facilities.
 *
 * @author Daniel Murphy
 */
public class Dynamics2D {
    private static final int WORLD_POOL_SIZE = 256;
    private static final int WORLD_POOL_CONTAINER_SIZE = 16;

    public static final int NEW_FIXTURE = 0x0001;
    
    private static final int CLEAR_FORCES = 0x0004;

    /** set to System.currentTimeMS() at start of solver iteration */
    public long realtimeMS;

    



    int flags;

    final ContactManager contactManager;

    private final Set<Body2D> bodies = new ConcurrentFastIteratingHashSet<>(new Body2D[0]);

    private final Set<Joint> joints = new ConcurrentFastIteratingHashSet<>(new Joint[0]);

    private int jointCount;

    private final Tuple2f m_gravity = new v2();
    private boolean m_allowSleep;

    

    private DestructionListener m_destructionListener;
    private ParticleDestructionListener m_particleDestructionListener;
    

    public final IWorldPool pool;

    /**
     * This is used to compute the time step ratio to support a variable time step.
     */
    private float m_inv_dt0;

    
    private boolean m_warmStarting;
    private boolean m_continuousPhysics;
    private boolean m_subStepping;

    private boolean m_stepComplete;

    private final Profile m_profile;

    public final ParticleSystem particles;


    private final Smasher smasher = new Smasher();


    private final MetalConcurrentQueue<Runnable> queue = new MetalConcurrentQueue<>(512);






    private final Island toiIsland = new Island(this);
    private final TimeOfImpact.TOIInput toiInput = new TimeOfImpact.TOIInput();
    private final TimeOfImpact.TOIOutput toiOutput = new TimeOfImpact.TOIOutput();
    private final TimeStep subStep = new TimeStep();
    private final Body2D[] tempBodies = new Body2D[2];
    private final Sweep backup1 = new Sweep();
    private final Sweep backup2 = new Sweep();

    public Dynamics2D() {
        this(new v2());
    }

    /**
     * Construct a world object.
     *
     * @param gravity the world gravity vector.
     */
    public Dynamics2D(Tuple2f gravity) {
        this(gravity, new DefaultWorldPool(WORLD_POOL_SIZE, WORLD_POOL_CONTAINER_SIZE));
    }

    /**
     * Construct a world object.
     *
     * @param gravity the world gravity vector.
     */
    private Dynamics2D(Tuple2f gravity, IWorldPool pool) {
        this(gravity, pool, new DynamicTree());
    }

    private Dynamics2D(Tuple2f gravity, IWorldPool pool, BroadPhaseStrategy strategy) {
        this(gravity, pool, new DefaultBroadPhaseBuffer(strategy));
    }

    private Dynamics2D(Tuple2f gravity, IWorldPool pool, BroadPhase broadPhase) {

        this.pool = pool;

        m_destructionListener = null;

        jointCount = 0;

        m_warmStarting = true;
        m_continuousPhysics = true;
        m_subStepping = false;
        m_stepComplete = true;

        m_allowSleep = true;
        m_gravity.set(gravity);

        flags = CLEAR_FORCES;

        m_inv_dt0 = 0f;

        contactManager = new ContactManager(this, broadPhase);
        m_profile = new Profile();

        particles = new ParticleSystem(this);

    }

    public void setAllowSleep(boolean flag) {
        if (flag == m_allowSleep) {
            return;
        }

        m_allowSleep = flag;
        if (!m_allowSleep) {
            bodies(b->b.setAwake(true));
        }
    }

    public void setSubStepping(boolean subStepping) {
        this.m_subStepping = subStepping;
    }

    public boolean isSubStepping() {
        return m_subStepping;
    }

    public boolean isAllowSleep() {
        return m_allowSleep;
    }

    public void addFracture(Fracture fracture) {
        
        
        Fracture f = smasher.fractures.get(fracture);
        if (f != null) {
            if (f.normalImpulse < fracture.normalImpulse) {
                smasher.fractures.remove(f);
                smasher.fractures.add(fracture);
            }
        } else {
            smasher.fractures.add(fracture);
        }
    }

    public boolean isFractured(Fixture fx) {
        return smasher.fractures.contains(fx);
    }


    public DestructionListener getDestructionListener() {
        return m_destructionListener;
    }

    public ParticleDestructionListener getParticleDestructionListener() {
        return m_particleDestructionListener;
    }

    public void setParticleDestructionListener(ParticleDestructionListener listener) {
        m_particleDestructionListener = listener;
    }


    /**
     * Register a destruction listener. The listener is owned by you and must remain in scope.
     *
     * @param listener
     */
    public void setDestructionListener(DestructionListener listener) {
        m_destructionListener = listener;
    }


    /**
     * Register a contact event listener. The listener is owned by you and must remain in scope.
     *
     * @param listener
     */
    public void setContactListener(ContactListener listener) {
        contactManager.contactListener = listener;
    }

    /**
     * Registruje FractureListener.
     *
     * @param listener
     */
    public void setFractureListener(FractureListener listener) {
        contactManager.m_fractureListener = listener;
    }











    /**
     * create a rigid body given a definition. No reference to the definition is retained.
     *
     * @param def
     * @return
     * @warning This function is locked during callbacks.
     */
    public Body2D addBody(BodyDef def) {
        return addBody(new Body2D(def, this));
    }
    public Body2D addDynamic(FixtureDef def) {
        return addBody(new BodyDef(BodyType.DYNAMIC), def);
    }
    public Body2D addStatic(FixtureDef def) {
        return addBody(new BodyDef(BodyType.STATIC), def);
    }

    public Body2D addBody(BodyDef def, FixtureDef... fd) {
        return addBody(new Body2D(def, this), fd);
    }

    public Body2D addBody(Body2D b, FixtureDef... fd) {

        if (bodies.add(b)) {
            if (fd.length > 0) {
                invoke(() -> {
                    for (FixtureDef f : fd)
                        b.addFixture(f);
                });
            }
        }

        return b;
    }

    /**
     * destroy a rigid body given a definition. No reference to the definition is retained. This
     * function is locked during callbacks.
     *
     * @param b
     * @warning This automatically deletes all associated shapes and joints.
     * @warning This function is locked during callbacks.
     */
    public void removeBody(Body2D b) {

        if (bodies.remove(b)) {

            invoke(() -> {

                b.onRemoval();

                b.setActive(false);

                
                JointEdge je = b.joints;
                while (je != null) {
                    JointEdge je0 = je;
                    je = je.next;
                    if (m_destructionListener != null) {
                        m_destructionListener.beforeDestruct(je0.joint);
                    }

                    removeJoint(je0.joint);

                    b.joints = je;
                }
                b.joints = null;

                
                ContactEdge ce = b.contacts;
                while (ce != null) {
                    ContactEdge ce0 = ce;
                    ce = ce.next;
                    contactManager.destroy(ce0.contact);
                }
                b.contacts = null;

                Fixture f = b.fixtures;
                while (f != null) {
                    Fixture f0 = f;
                    f = f.next;

                    if (m_destructionListener != null) {
                        m_destructionListener.beforeDestruct(f0);
                    }

                    f0.destroyProxies(contactManager.broadPhase);
                    f0.destroy();
                    
                    b.fixtures = f;
                    b.fixtureCount -= 1;
                }
                b.fixtures = null;
                b.fixtureCount = 0;

            });
        }
    }

    /**
     * create a joint to constrain bodies together. No reference to the definition is retained. This
     * may cause the connected bodies to cease colliding.
     *
     * @param def
     * @return
     * @warning This function is locked during callbacks.
     */
    public Joint addJoint(JointDef def) {
        return addJoint(Joint.build(this, def));
    }

    public Joint addJoint( Joint j) {
        if (joints.add(j)) {

            invoke(() -> {

                ++jointCount;

                
                j.edgeA.joint = j;
                Body2D B = j.getBodyB();
                j.edgeA.other = B;
                j.edgeA.prev = null;
                Body2D A = j.getBodyA();
                j.edgeA.next = A.joints;
                if (A.joints != null) {
                    A.joints.prev = j.edgeA;
                }
                A.joints = j.edgeA;

                j.edgeB.joint = j;
                j.edgeB.other = A;
                j.edgeB.prev = null;
                j.edgeB.next = B.joints;
                if (B.joints != null) {
                    B.joints.prev = j.edgeB;
                }
                B.joints = j.edgeB;



                
                if (!j.getCollideConnected()) {
                    Body2D bodyA = j.getBodyA();
                    Body2D bodyB = j.getBodyB();

                    ContactEdge edge = bodyB.contacts();
                    while (edge != null) {
                        if (edge.other == bodyA) {
                            
                            
                            edge.contact.flagForFiltering();
                        }

                        edge = edge.next;
                    }
                }

                

            });
        }

        return j;
    }

    /**
     * destroy a joint. This may cause the connected bodies to begin colliding.
     *
     * @param joint
     * @warning This function is locked during callbacks.
     */
    public void removeJoint(Joint j) {

        if (joints.remove(j)) {
            invoke(() -> {


                boolean collideConnected = j.getCollideConnected();

                
                
                Body2D bodyA = j.getBodyA();
                bodyA.setAwake(true);

                Body2D bodyB = j.getBodyB();
                bodyB.setAwake(true);

                
                if (j.edgeA.prev != null) {
                    j.edgeA.prev.next = j.edgeA.next;
                }

                if (j.edgeA.next != null) {
                    j.edgeA.next.prev = j.edgeA.prev;
                }

                if (j.edgeA == bodyA.joints) {
                    bodyA.joints = j.edgeA.next;
                }

                j.edgeA.prev = null;
                j.edgeA.next = null;

                
                if (j.edgeB.prev != null) {
                    j.edgeB.prev.next = j.edgeB.next;
                }

                if (j.edgeB.next != null) {
                    j.edgeB.next.prev = j.edgeB.prev;
                }

                if (j.edgeB == bodyB.joints) {
                    bodyB.joints = j.edgeB.next;
                }

                j.edgeB.prev = null;
                j.edgeB.next = null;

                Joint.destroy(j);

                assert (jointCount > 0);
                --jointCount;

                
                if (!collideConnected) {
                    ContactEdge edge = bodyB.contacts();
                    while (edge != null) {
                        if (edge.other == bodyA) {
                            
                            
                            edge.contact.flagForFiltering();
                        }

                        edge = edge.next;
                    }
                }
            });
        }
    }

    
    private final TimeStep step = new TimeStep();
    private final Timer stepTimer = new Timer();
    private final Timer tempTimer = new Timer();

    public final void invoke(Runnable r) {
        if (!queue.offer(r)) {
            throw new RuntimeException(this + " queue overflow");
        }
    }

    /**
     * Take a time step. This performs collision detection, integration, and constraint solution.
     *
     * @param dt                 the amount of time to simulate, this should not vary.
     * @param velocityIterations for the velocity constraint solver.
     * @param positionIterations for the position constraint solver.
     */
    public void step(float dt, int velocityIterations, int positionIterations) {








        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        


        sync();

        invoke(() -> {

            realtimeMS = System.currentTimeMillis();

            stepTimer.reset();
            tempTimer.reset();





            
            
            if ((flags & NEW_FIXTURE) == NEW_FIXTURE) {
                
                contactManager.findNewContacts();
                flags &= ~NEW_FIXTURE;
            }

            step.dt = dt;
            step.velocityIterations = velocityIterations;
            step.positionIterations = positionIterations;
            step.inv_dt = (dt > 0.0f) ? (1.0f / dt) : 0.0f;

            step.dtRatio = m_inv_dt0 * dt;

            step.warmStarting = m_warmStarting;
            m_profile.stepInit.record(tempTimer::getMilliseconds);

            
            tempTimer.reset();
            contactManager.collide();
            m_profile.collide.record(tempTimer::getMilliseconds);

            
            if (m_stepComplete && step.dt > 0.0f) {

                tempTimer.reset();
                particles.solve(step); 
                m_profile.solveParticleSystem.record(tempTimer::getMilliseconds);

                tempTimer.reset();
                solve(step);

                m_profile.solve.record(tempTimer::getMilliseconds);
            }

            
            if (m_continuousPhysics && step.dt > 0.0f) {
                tempTimer.reset();
                solveTOI(step);
                m_profile.solveTOI.record(tempTimer::getMilliseconds);
            }

            if (step.dt > 0.0f) {
                m_inv_dt0 = step.inv_dt;
            }

            if ((flags & CLEAR_FORCES) == CLEAR_FORCES) {
                clearForces();
            }






            m_profile.step.record(stepTimer.getMilliseconds());



            smasher.fractures.forEach(f -> f.smash(smasher, dt));
            smasher.fractures.clear();


        });



    }

    public void sync() {
        Runnable next;
        while ((next = queue.poll()) != null)
            next.run();
    }

    /**
     * Call this after you are done with time steps to clear the forces. You normally call this after
     * each call to Step, unless you are performing sub-steps. By default, forces will be
     * automatically cleared, so you don't need to call this function.
     *
     * @see setAutoClearForces
     */
    public void clearForces() {
        bodies(b -> {
           b.force.setZero();
           b.torque = 0;
        });
    }










    /**
     * Query the world for all fixtures that potentially overlap the provided AABB.
     *
     * @param callback a user implemented callback class.
     * @param aabb     the query box.
     */
    public void queryAABB(Predicate<Fixture> callback, AABB aabb) {
        contactManager.broadPhase.query(new WorldQueryWrapper(callback), aabb);
    }

    /**
     * Query the world for all fixtures and particles that potentially overlap the provided AABB.
     *
     * @param callback         a user implemented callback class.
     * @param particleCallback callback for particles.
     * @param aabb             the query box.
     */
    public void queryAABB(Predicate<Fixture> callback, ParticleQueryCallback particleCallback, AABB aabb) {
        contactManager.broadPhase.query(new WorldQueryWrapper(callback), aabb);
        particles.queryAABB(particleCallback, aabb);
    }

    /**
     * Query the world for all particles that potentially overlap the provided AABB.
     *
     * @param particleCallback callback for particles.
     * @param aabb             the query box.
     */
    public void queryAABB(ParticleQueryCallback particleCallback, AABB aabb) {
        particles.queryAABB(particleCallback, aabb);
    }

    private final WorldRayCastWrapper wrcwrapper = new WorldRayCastWrapper();
    private final RayCastInput input = new RayCastInput();

    /**
     * Ray-cast the world for all fixtures in the path of the ray. Your callback controls whether you
     * get the closest point, any point, or n-points. The ray-cast ignores shapes that contain the
     * starting point.
     *
     * @param callback a user implemented callback class.
     * @param point1   the ray starting point
     * @param point2   the ray ending point
     */
    public void raycast(RayCastCallback callback, Tuple2f point1, Tuple2f point2) {
        wrcwrapper.broadPhase = contactManager.broadPhase;
        wrcwrapper.callback = callback;
        input.maxFraction = 1.0f;
        input.p1.set(point1);
        input.p2.set(point2);
        contactManager.broadPhase.raycast(wrcwrapper, input);
    }

    /**
     * Ray-cast the world for all fixtures and particles in the path of the ray. Your callback
     * controls whether you get the closest point, any point, or n-points. The ray-cast ignores shapes
     * that contain the starting point.
     *
     * @param callback         a user implemented callback class.
     * @param particleCallback the particle callback class.
     * @param point1           the ray starting point
     * @param point2           the ray ending point
     */
    public void raycast(RayCastCallback callback, ParticleRaycastCallback particleCallback,
                        Tuple2f point1, Tuple2f point2) {
        wrcwrapper.broadPhase = contactManager.broadPhase;
        wrcwrapper.callback = callback;
        input.maxFraction = 1.0f;
        input.p1.set(point1);
        input.p2.set(point2);
        contactManager.broadPhase.raycast(wrcwrapper, input);
        particles.raycast(particleCallback, point1, point2);
    }

    /**
     * Ray-cast the world for all particles in the path of the ray. Your callback controls whether you
     * get the closest point, any point, or n-points.
     *
     * @param particleCallback the particle callback class.
     * @param point1           the ray starting point
     * @param point2           the ray ending point
     */
    public void raycast(ParticleRaycastCallback particleCallback, Tuple2f point1, Tuple2f point2) {
        particles.raycast(particleCallback, point1, point2);
    }


    public Iterable<Body2D> bodies() {
        return bodies;
    }

    public void bodies(Consumer<Body2D> each) {
        bodies.forEach(each);
    }

    public Iterable<Joint> joints() {
        return joints;
    }

    public void joints(Consumer<Joint> each) {
        joints.forEach(each);
    }

    /**
     * Get the world contact list. With the returned contact, use Contact.getNext to get the next
     * contact in the world list. A null contact indicates the end of the list.
     *
     * @return the head of the world contact list.
     * @warning contacts are created and destroyed in the middle of a time step. Use ContactListener
     * to avoid missing contacts.
     */
    public Contact getContactList() {
        return contactManager.m_contactList;
    }

    public boolean isSleepingAllowed() {
        return m_allowSleep;
    }


    /**
     * Enable/disable warm starting. For testing.
     *
     * @param flag
     */
    public void setWarmStarting(boolean flag) {
        m_warmStarting = flag;
    }

    public boolean isWarmStarting() {
        return m_warmStarting;
    }

    /**
     * Enable/disable continuous physics. For testing.
     *
     * @param flag
     */
    public void setContinuousPhysics(boolean flag) {
        m_continuousPhysics = flag;
    }

    public boolean isContinuousPhysics() {
        return m_continuousPhysics;
    }


    /**
     * Get the number of broad-phase proxies.
     *
     * @return
     */
    public int getProxyCount() {
        return contactManager.broadPhase.getProxyCount();
    }

    /**
     * Get the number of bodies.
     *
     * @return
     */
    public int getBodyCount() {
        return bodies.size();
    }

    /**
     * Get the number of joints.
     *
     * @return
     */
    public int getJointCount() {
        return jointCount;
    }

    /**
     * Get the number of contacts (each may have 0 or more contact points).
     *
     * @return
     */
    public int getContactCount() {
        return contactManager.m_contactCount;
    }

    /**
     * Gets the height of the dynamic tree
     *
     * @return
     */
    public int getTreeHeight() {
        return contactManager.broadPhase.getTreeHeight();
    }

    /**
     * Gets the balance of the dynamic tree
     *
     * @return
     */
    public int getTreeBalance() {
        return contactManager.broadPhase.getTreeBalance();
    }

    /**
     * Gets the quality of the dynamic tree
     *
     * @return
     */
    public float getTreeQuality() {
        return contactManager.broadPhase.getTreeQuality();
    }

    /**
     * Change the global gravity vector.
     *
     * @param gravity
     */
    public void setGravity(Tuple2f gravity) {
        m_gravity.set(gravity);
    }

    /**
     * Get the global gravity vector.
     *
     * @return
     */
    public Tuple2f getGravity() {
        return m_gravity;
    }










    /**
     * Set flag to control automatic clearing of forces after each time step.
     *
     * @param flag
     */
    public void setAutoClearForces(boolean flag) {
        if (flag) {
            flags |= CLEAR_FORCES;
        } else {
            flags &= ~CLEAR_FORCES;
        }
    }

    /**
     * Get the flag that controls automatic clearing of forces after each time step.
     *
     * @return
     */
    public boolean getAutoClearForces() {
        return (flags & CLEAR_FORCES) == CLEAR_FORCES;
    }

    /**
     * Get the contact manager for testing purposes
     *
     * @return
     */
    public ContactManager getContactManager() {
        return contactManager;
    }

    public Profile getProfile() {
        return m_profile;
    }

    private final Island island = new Island(this);

    private final Timer broadphaseTimer = new Timer();

    private void solve(TimeStep step) {
        m_profile.solveInit.startAccum();
        m_profile.solveVelocity.startAccum();
        m_profile.solvePosition.startAccum();

        List<Body2D> preRemove = new FasterList(0);
        bodies(b->{
            
            b.flags &= ~Body2D.e_islandFlag;

            if (!b.preUpdate()) {
                preRemove.add(b);
            } else {
                
                b.transformPrev.set(b);
            }
        });

        if (!preRemove.isEmpty()) {
            preRemove.forEach(this::removeBody);
            preRemove.clear();
        }

        
        int bodyCount = bodies.size();
        island.init(bodyCount + 1, contactManager.m_contactCount, jointCount,
                contactManager.contactListener);




        for (Contact c = contactManager.m_contactList; c != null; c = c.m_next) {
            c.m_flags &= ~Contact.ISLAND_FLAG;
        }

        joints(j->j.islandFlag = false);

        
        int stackSize = bodyCount;
        Body2D[] stack = new Body2D[stackSize]; 

        bodies(seed->{
            if ((seed.flags & Body2D.e_islandFlag) == Body2D.e_islandFlag)
                return;

            if (!seed.isAwake() || !seed.isActive())
                return;

            
            if (seed.getType() == BodyType.STATIC)
                return;

            
            island.clear();
            int stackCount = 0;
            stack[stackCount++] = seed;
            seed.flags |= Body2D.e_islandFlag;

            
            while (stackCount > 0) {
                
                Body2D b = stack[--stackCount];
                if (!b.isActive())
                    continue;
                
                island.add(b);

                
                b.setAwake(true);

                
                
                if (b.getType() == BodyType.STATIC)
                    continue;

                
                for (ContactEdge ce = b.contacts; ce != null; ce = ce.next) {
                    Contact contact = ce.contact;

                    
                    if ((contact.m_flags & Contact.ISLAND_FLAG) == Contact.ISLAND_FLAG) {
                        continue;
                    }

                    
                    if (!contact.isEnabled() || !contact.isTouching()) {
                        continue;
                    }

                    
                    boolean sensorA = contact.aFixture.isSensor;
                    boolean sensorB = contact.bFixture.isSensor;
                    if (sensorA || sensorB) {
                        continue;
                    }

                    island.add(contact);
                    contact.m_flags |= Contact.ISLAND_FLAG;

                    Body2D other = ce.other;

                    
                    if ((other.flags & Body2D.e_islandFlag) == Body2D.e_islandFlag) {
                        continue;
                    }

                    assert (stackCount < stackSize);
                    stack[stackCount++] = other;
                    other.flags |= Body2D.e_islandFlag;
                }

                
                for (JointEdge je = b.joints; je != null; je = je.next) {
                    if (je.joint.islandFlag) {
                        continue;
                    }

                    Body2D other = je.other;

                    
                    if (!other.isActive()) {
                        continue;
                    }

                    island.add(je.joint);
                    je.joint.islandFlag = true;

                    if ((other.flags & Body2D.e_islandFlag) == Body2D.e_islandFlag) {
                        continue;
                    }

                    assert (stackCount < stackSize);
                    stack[stackCount++] = other;
                    other.flags |= Body2D.e_islandFlag;
                }
            }
            island.solve(m_profile, step, m_gravity, m_allowSleep);

            
            for (int i = 0; i < island.m_bodyCount; ++i) {
                
                Body2D b = island.bodies[i];
                if (b.getType() == BodyType.STATIC) {
                    b.flags &= ~Body2D.e_islandFlag;
                }
            }
        });

        m_profile.solveInit.endAccum();
        m_profile.solveVelocity.endAccum();
        m_profile.solvePosition.endAccum();

        broadphaseTimer.reset();
        
        bodies(b->{
            
            if ((b.flags & Body2D.e_islandFlag) == 0 || b.getType() == BodyType.STATIC) return;

            
            b.synchronizeFixtures();
            b.postUpdate();
        });

        
        contactManager.findNewContacts();
        m_profile.broadphase.record(broadphaseTimer.getMilliseconds());
    }


    private void solveTOI(final TimeStep step) {

        final Island island = toiIsland;
        island.init(2 * Settings.maxTOIContacts, Settings.maxTOIContacts, 0,
                contactManager.contactListener);
        if (m_stepComplete) {
            bodies(b->{
                b.flags &= ~Body2D.e_islandFlag;
                b.sweep.alpha0 = 0.0f;
            });

            for (Contact c = contactManager.m_contactList; c != null; c = c.m_next) {
                
                c.m_flags &= ~(Contact.TOI_FLAG | Contact.ISLAND_FLAG);
                c.m_toiCount = 0;
                c.m_toi = 1.0f;
            }
        }

        
        for (; ; ) {
            
            Contact minContact = null;
            float minAlpha = 1.0f;

            for (Contact c = contactManager.m_contactList; c != null; c = c.m_next) {
                
                if (!c.isEnabled()) {
                    continue;
                }

                
                if (c.m_toiCount > Settings.maxSubSteps) {
                    continue;
                }

                float alpha = 1.0f;
                if ((c.m_flags & Contact.TOI_FLAG) != 0) {
                    
                    alpha = c.m_toi;
                } else {
                    Fixture fA = c.aFixture;
                    Fixture fB = c.bFixture;

                    
                    if (fA.isSensor() || fB.isSensor()) {
                        continue;
                    }

                    Body2D bA = fA.getBody();
                    Body2D bB = fB.getBody();

                    BodyType typeA = bA.type;
                    BodyType typeB = bB.type;
                    assert (typeA == BodyType.DYNAMIC || typeB == BodyType.DYNAMIC);

                    boolean activeA = bA.isAwake() && typeA != BodyType.STATIC;
                    boolean activeB = bB.isAwake() && typeB != BodyType.STATIC;

                    
                    if (!activeA && !activeB) {
                        continue;
                    }

                    boolean collideA = bA.isBullet() || typeA != BodyType.DYNAMIC;
                    boolean collideB = bB.isBullet() || typeB != BodyType.DYNAMIC;

                    
                    if (!collideA && !collideB) {
                        continue;
                    }

                    
                    
                    float alpha0 = bA.sweep.alpha0;

                    if (bA.sweep.alpha0 < bB.sweep.alpha0) {
                        alpha0 = bB.sweep.alpha0;
                        bA.sweep.advance(alpha0);
                    } else if (bB.sweep.alpha0 < bA.sweep.alpha0) {
                        alpha0 = bA.sweep.alpha0;
                        bB.sweep.advance(alpha0);
                    }

                    assert (alpha0 < 1.0f);

                    int indexA = c.aIndex;
                    int indexB = c.bIndex;

                    
                    final TimeOfImpact.TOIInput input = toiInput;
                    input.proxyA.set(fA.shape(), indexA);
                    input.proxyB.set(fB.shape(), indexB);
                    input.sweepA.set(bA.sweep);
                    input.sweepB.set(bB.sweep);
                    input.tMax = 1.0f;

                    pool.getTimeOfImpact().timeOfImpact(toiOutput, input);

                    
                    float beta = toiOutput.t;
                    if (toiOutput.state == TimeOfImpact.TOIOutputState.TOUCHING) {
                        alpha = MathUtils.min(alpha0 + (1.0f - alpha0) * beta, 1.0f);
                    } else {
                        alpha = 1.0f;
                    }

                    c.m_toi = alpha;
                    c.m_flags |= Contact.TOI_FLAG;
                }

                if (alpha < minAlpha) {
                    
                    minContact = c;
                    minAlpha = alpha;
                }
            }

            if (minContact == null || 1.0f - 10.0f * Settings.EPSILON < minAlpha) {
                
                m_stepComplete = true;
                break;
            }

            
            Fixture fA = minContact.aFixture;
            Fixture fB = minContact.bFixture;
            Body2D bA = fA.getBody();
            Body2D bB = fB.getBody();

            backup1.set(bA.sweep);
            backup2.set(bB.sweep);

            bA.advance(minAlpha);
            bB.advance(minAlpha);

            
            minContact.update(contactManager.contactListener);
            minContact.m_flags &= ~Contact.TOI_FLAG;
            ++minContact.m_toiCount;

            
            if (!minContact.isEnabled() || !minContact.isTouching()) {
                
                minContact.setEnabled(false);
                bA.sweep.set(backup1);
                bB.sweep.set(backup2);
                bA.synchronizeTransform();
                bB.synchronizeTransform();
                continue;
            }

            bA.setAwake(true);
            bB.setAwake(true);

            
            island.clear();
            island.add(bA);
            island.add(bB);
            island.add(minContact);

            bA.flags |= Body2D.e_islandFlag;
            bB.flags |= Body2D.e_islandFlag;
            minContact.m_flags |= Contact.ISLAND_FLAG;

            
            tempBodies[0] = bA;
            tempBodies[1] = bB;
            for (int i = 0; i < 2; ++i) {
                Body2D body = tempBodies[i];
                if (body.type == BodyType.DYNAMIC) {
                    for (ContactEdge ce = body.contacts; ce != null; ce = ce.next) {
                        if (island.m_bodyCount == island.m_bodyCapacity) {
                            break;
                        }

                        if (island.m_contactCount == island.m_contactCapacity) {
                            break;
                        }

                        Contact contact = ce.contact;

                        
                        if ((contact.m_flags & Contact.ISLAND_FLAG) != 0) {
                            continue;
                        }

                        
                        Body2D other = ce.other;
                        if (other.type == BodyType.DYNAMIC && !body.isBullet()
                                && !other.isBullet()) {
                            continue;
                        }

                        
                        boolean sensorA = contact.aFixture.isSensor;
                        boolean sensorB = contact.bFixture.isSensor;
                        if (sensorA || sensorB) {
                            continue;
                        }

                        
                        backup1.set(other.sweep);
                        if ((other.flags & Body2D.e_islandFlag) == 0) {
                            other.advance(minAlpha);
                        }

                        
                        contact.update(contactManager.contactListener);

                        
                        if (!contact.isEnabled()) {
                            other.sweep.set(backup1);
                            other.synchronizeTransform();
                            continue;
                        }

                        
                        if (!contact.isTouching()) {
                            other.sweep.set(backup1);
                            other.synchronizeTransform();
                            continue;
                        }

                        
                        contact.m_flags |= Contact.ISLAND_FLAG;
                        island.add(contact);

                        
                        if ((other.flags & Body2D.e_islandFlag) != 0) {
                            continue;
                        }

                        
                        other.flags |= Body2D.e_islandFlag;

                        if (other.type != BodyType.STATIC) {
                            other.setAwake(true);
                        }

                        island.add(other);
                    }
                }
            }

            subStep.dt = (1.0f - minAlpha) * step.dt;
            subStep.inv_dt = 1.0f / subStep.dt;
            subStep.dtRatio = 1.0f;
            subStep.positionIterations = 20;
            subStep.velocityIterations = step.velocityIterations;
            subStep.warmStarting = false;
            island.solveTOI(subStep, bA.island, bB.island);

            
            for (int i = 0; i < island.m_bodyCount; ++i) {
                Body2D body = island.bodies[i];
                body.flags &= ~Body2D.e_islandFlag;

                if (body.type != BodyType.DYNAMIC) {
                    continue;
                }

                body.synchronizeFixtures();

                
                for (ContactEdge ce = body.contacts; ce != null; ce = ce.next) {
                    ce.contact.m_flags &= ~(Contact.TOI_FLAG | Contact.ISLAND_FLAG);
                }
            }

            
            
            contactManager.findNewContacts();

            if (m_subStepping) {
                m_stepComplete = false;
                break;
            }
        }
    }


    
    
    private static final Integer LIQUID_INT = 1234598372;
    private final float liquidLength = .12f;
    private final float averageLinearVel = -1;
    private final Tuple2f liquidOffset = new v2();
    private final Tuple2f circCenterMoved = new v2();
    private final Color3f liquidColor = new Color3f(.4f, .4f, 1f);

    private final Tuple2f center = new v2();
    private final Tuple2f axis = new v2();
    private final Tuple2f V = new v2();
    private final Tuple2f W = new v2();
    private final Vec2Array tlvertices = new Vec2Array();


    /**
     * Create a particle whose properties have been defined. No reference to the definition is
     * retained. A simulation step must occur before it's possible to interact with a newly created
     * particle. For example, DestroyParticleInShape() will not destroy a particle until Step() has
     * been called.
     *
     * @return the index of the particle.
     * @warning This function is locked during callbacks.
     */
    public int createParticle(ParticleDef def) {




        int p = particles.createParticle(def);
        return p;
    }

    /**
     * Destroy a particle. The particle is removed after the next step.
     *
     * @param index
     */
    public void destroyParticle(int index) {
        destroyParticle(index, false);
    }

    /**
     * Destroy a particle. The particle is removed after the next step.
     *
     * @param Index   of the particle to destroy.
     * @param Whether to call the destruction listener just before the particle is destroyed.
     */
    private void destroyParticle(int index, boolean callDestructionListener) {
        particles.destroyParticle(index, callDestructionListener);
    }

    /**
     * Destroy particles inside a shape without enabling the destruction callback for destroyed
     * particles. This function is locked during callbacks. For more information see
     * DestroyParticleInShape(Shape&, Transform&,bool).
     *
     * @param Shape     which encloses particles that should be destroyed.
     * @param Transform applied to the shape.
     * @return Number of particles destroyed.
     * @warning This function is locked during callbacks.
     */
    public int destroyParticlesInShape(Shape shape, Transform xf) {
        return destroyParticlesInShape(shape, xf, false);
    }

    /**
     * Destroy particles inside a shape. This function is locked during callbacks. In addition, this
     * function immediately destroys particles in the shape in contrast to DestroyParticle() which
     * defers the destruction until the next simulation step.
     *
     * @param Shape     which encloses particles that should be destroyed.
     * @param Transform applied to the shape.
     * @param Whether   to call the world b2DestructionListener for each particle destroyed.
     * @return Number of particles destroyed.
     * @warning This function is locked during callbacks.
     */
    private int destroyParticlesInShape(Shape shape, Transform xf, boolean callDestructionListener) {




        return particles.destroyParticlesInShape(shape, xf, callDestructionListener);
    }

    /**
     * Create a particle group whose properties have been defined. No reference to the definition is
     * retained.
     *
     * @warning This function is locked during callbacks.
     */
    public ParticleGroup addParticles(ParticleGroupDef def) {
        return particles.createParticleGroup(this, def);
    }

    /**
     * Join two particle groups.
     *
     * @param the first group. Expands to encompass the second group.
     * @param the second group. It is destroyed.
     * @warning This function is locked during callbacks.
     */
    public void joinParticleGroups(ParticleGroup groupA, ParticleGroup groupB) {
        invoke(()-> particles.joinParticleGroups(groupA, groupB));
    }

    /**
     * Destroy particles in a group. This function is locked during callbacks.
     *
     * @param The     particle group to destroy.
     * @param Whether to call the world b2DestructionListener for each particle is destroyed.
     * @warning This function is locked during callbacks.
     */
    private void destroyParticlesInGroup(ParticleGroup group, boolean callDestructionListener) {
        invoke(()-> particles.destroyParticlesInGroup(group, callDestructionListener));
    }

    /**
     * Destroy particles in a group without enabling the destruction callback for destroyed particles.
     * This function is locked during callbacks.
     *
     * @param The particle group to destroy.
     * @warning This function is locked during callbacks.
     */
    public void destroyParticlesInGroup(ParticleGroup group) {
        invoke(()-> destroyParticlesInGroup(group, false));
    }

    /**
     * Get the world particle group list. With the returned group, use ParticleGroup::GetNext to get
     * the next group in the world list. A NULL group indicates the end of the list.
     *
     * @return the head of the world particle group list.
     */
    public ParticleGroup[] getParticleGroupList() {
        return particles.getParticleGroupList();
    }

    /**
     * Get the number of particle groups.
     *
     * @return
     */
    public int getParticleGroupCount() {
        return particles.getParticleGroupCount();
    }

    /**
     * Get the number of particles.
     *
     * @return
     */
    public int getParticleCount() {
        return particles.getParticleCount();
    }

    /**
     * Get the maximum number of particles.
     *
     * @return
     */
    public int getParticleMaxCount() {
        return particles.getParticleMaxCount();
    }

    /**
     * Set the maximum number of particles.
     *
     * @param count
     */
    public void setParticleMaxCount(int count) {
        particles.setParticleMaxCount(count);
    }

    /**
     * Change the particle density.
     *
     * @param density
     */
    public void setParticleDensity(float density) {
        particles.setParticleDensity(density);
    }

    /**
     * Get the particle density.
     *
     * @return
     */
    public float getParticleDensity() {
        return particles.getParticleDensity();
    }

    /**
     * Change the particle gravity scale. Adjusts the effect of the global gravity vector on
     * particles. Default value is 1.0f.
     *
     * @param gravityScale
     */
    public void setParticleGravityScale(float gravityScale) {
        particles.setParticleGravityScale(gravityScale);

    }

    /**
     * Get the particle gravity scale.
     *
     * @return
     */
    public float getParticleGravityScale() {
        return particles.getParticleGravityScale();
    }

    /**
     * Damping is used to reduce the velocity of particles. The damping parameter can be larger than
     * 1.0f but the damping effect becomes sensitive to the time step when the damping parameter is
     * large.
     *
     * @param damping
     */
    public void setParticleDamping(float damping) {
        particles.setParticleDamping(damping);
    }

    /**
     * Get damping for particles
     *
     * @return
     */
    public float getParticleDamping() {
        return particles.getParticleDamping();
    }

    /**
     * Change the particle radius. You should set this only once, on world start. If you change the
     * radius during execution, existing particles may explode, shrink, or behave unexpectedly.
     *
     * @param radius
     */
    public void setParticleRadius(float radius) {
        particles.setParticleRadius(radius);
    }

    /**
     * Get the particle radius.
     *
     * @return
     */
    public float getParticleRadius() {
        return particles.getParticleRadius();
    }

    /**
     * Get the particle data. @return the pointer to the head of the particle data.
     *
     * @return
     */
    public int[] getParticleFlagsBuffer() {
        return particles.getParticleFlagsBuffer();
    }

    public Tuple2f[] getParticlePositionBuffer() {
        return particles.getParticlePositionBuffer();
    }

    public Tuple2f[] getParticleVelocityBuffer() {
        return particles.getParticleVelocityBuffer();
    }

    public ParticleColor[] getParticleColorBuffer() {
        return particles.getParticleColorBuffer();
    }

    public ParticleGroup[] getParticleGroupBuffer() {
        return particles.getParticleGroupBuffer();
    }

    public Object[] getParticleUserDataBuffer() {
        return particles.getParticleUserDataBuffer();
    }

    /**
     * Set a buffer for particle data.
     *
     * @param buffer is a pointer to a block of memory.
     * @param size   is the number of values in the block.
     */
    public void setParticleFlagsBuffer(int[] buffer, int capacity) {
        particles.setParticleFlagsBuffer(buffer, capacity);
    }

    public void setParticlePositionBuffer(v2[] buffer, int capacity) {
        particles.setParticlePositionBuffer(buffer, capacity);

    }

    public void setParticleVelocityBuffer(v2[] buffer, int capacity) {
        particles.setParticleVelocityBuffer(buffer, capacity);

    }

    public void setParticleColorBuffer(ParticleColor[] buffer, int capacity) {
        particles.setParticleColorBuffer(buffer, capacity);

    }

    public void setParticleUserDataBuffer(Object[] buffer, int capacity) {
        particles.setParticleUserDataBuffer(buffer, capacity);
    }

    /**
     * Get contacts between particles
     *
     * @return
     */
    public ParticleContact[] getParticleContacts() {
        return particles.m_contactBuffer;
    }

    public int getParticleContactCount() {
        return particles.m_contactCount;
    }

    /**
     * Get contacts between particles and bodies
     *
     * @return
     */
    public ParticleBodyContact[] getParticleBodyContacts() {
        return particles.m_bodyContactBuffer;
    }

    public int getParticleBodyContactCount() {
        return particles.m_bodyContactCount;
    }

    /**
     * Compute the kinetic energy that can be lost by damping force
     *
     * @return
     */
    public float computeParticleCollisionEnergy() {
        return particles.computeParticleCollisionEnergy();
    }

    public Body2D newDynamicBody(Shape shape, float density, float friction) {
        return addBody(new BodyDef(BodyType.DYNAMIC, new v2()),
                new FixtureDef(shape, density, friction));
    }


    public static class Profile {

        boolean active = false;

        private static final int LONG_AVG_NUMS = 20;
        private static final float LONG_FRACTION = 1f / LONG_AVG_NUMS;
        private static final int SHORT_AVG_NUMS = 5;
        private static final float SHORT_FRACTION = 1f / SHORT_AVG_NUMS;

        public class ProfileEntry {
            float longAvg;
            float shortAvg;
            float min;
            float max;
            float accum;

            ProfileEntry() {
                min = Float.MAX_VALUE;
                max = -Float.MAX_VALUE;
            }

            void record(FloatSupplier value) {
                if (active)
                    record(value.asFloat());
            }

            void record(float value) {
                longAvg = longAvg * (1 - LONG_FRACTION) + value * LONG_FRACTION;
                shortAvg = shortAvg * (1 - SHORT_FRACTION) + value * SHORT_FRACTION;
                min = MathUtils.min(value, min);
                max = MathUtils.max(value, max);
            }

            void startAccum() {
                accum = 0;
            }

            public void accum(FloatSupplier value) {
                if (active)
                    accum(value.asFloat());
            }

            void accum(float value) {
                accum += value;
            }

            void endAccum() {
                record(accum);
            }

            @Override
            public String toString() {
                return String.format("%.2f (%.2f) [%.2f,%.2f]", shortAvg, longAvg, min, max);
            }
        }

        final ProfileEntry step = new ProfileEntry();
        final ProfileEntry stepInit = new ProfileEntry();
        final ProfileEntry collide = new ProfileEntry();
        final ProfileEntry solveParticleSystem = new ProfileEntry();
        final ProfileEntry solve = new ProfileEntry();
        public final ProfileEntry solveInit = new ProfileEntry();
        public final ProfileEntry solveVelocity = new ProfileEntry();
        public final ProfileEntry solvePosition = new ProfileEntry();
        final ProfileEntry broadphase = new ProfileEntry();
        final ProfileEntry solveTOI = new ProfileEntry();

        public void toDebugStrings(List<String> strings) {
            strings.add("Profile:");
            strings.add(" step: " + step);
            strings.add("  init: " + stepInit);
            strings.add("  collide: " + collide);
            strings.add("  particles: " + solveParticleSystem);
            strings.add("  solve: " + solve);
            strings.add("   solveInit: " + solveInit);
            strings.add("   solveVelocity: " + solveVelocity);
            strings.add("   solvePosition: " + solvePosition);
            strings.add("   broadphase: " + broadphase);
            strings.add("  solveTOI: " + solveTOI);
        }
    }

    private class WorldQueryWrapper implements TreeCallback {

        final Predicate<Fixture> callback;

        WorldQueryWrapper(Predicate<Fixture> callback) {
            this.callback = callback;
        }

        public boolean treeCallback(int nodeId) {
            return callback.test(((FixtureProxy) contactManager.broadPhase.get(nodeId)).fixture);
        }

    }


    @Deprecated public static void staticBox(Dynamics2D world, float x1, float y1, float x2, float y2) {
        staticBox(world, x1, y1, x2, y2, true, true, true, true);
    }

    /** TODO use one Body2D with 4 fixtures */
    @Deprecated public static void staticBox(Dynamics2D world, float x1, float y1, float x2, float y2, boolean top, boolean right, boolean bottom, boolean left) {

        float cx = (x1 + x2) / 2f;
        float cy = (y1 + y2) / 2f;
        float w = Math.abs(x2 - x1);
        float h = Math.abs(y2 - y1);

        float thick = Math.min(w, h) / 20f;

        if (bottom) {
            Body2D _bottom = world.addBody(new BodyDef(BodyType.STATIC),
                    new FixtureDef(PolygonShape.box(w / 2 - thick / 2, thick / 2),
                            0, 0));
            _bottom.setTransform(new v2(cx, y1), 0);
        }

        if (top) {
            Body2D _top = world.addBody(new BodyDef(BodyType.STATIC),
                    new FixtureDef(PolygonShape.box(w / 2 - thick / 2, thick / 2),
                            0, 0));
            _top.setTransform(new v2(cx, y2), 0);
        }

        if (left) {
            Body2D _left = world.addBody(new BodyDef(BodyType.STATIC),
                    new FixtureDef(PolygonShape.box(thick / 2, h / 2 - thick / 2),
                            0, 0));
            _left.setTransform(new v2(x1, cy), 0);

        }

        if (right) {
            Body2D _right = world.addBody(new BodyDef(BodyType.STATIC),
                    new FixtureDef(PolygonShape.box(thick / 2, h / 2 - thick / 2),
                            0, 0));
            _right.setTransform(new v2(x2, cy), 0);
        }









    }


}





class WorldRayCastWrapper implements TreeRayCastCallback {

    
    private final RayCastOutput output = new RayCastOutput();
    private final Tuple2f temp = new v2();
    private final Tuple2f point = new v2();

    public float raycastCallback(RayCastInput input, int nodeId) {
        Object userData = broadPhase.get(nodeId);
        FixtureProxy proxy = (FixtureProxy) userData;
        Fixture fixture = proxy.fixture;
        int index = proxy.childIndex;
        boolean hit = fixture.raycast(output, input, index);

        if (hit) {
            float fraction = output.fraction;
            
            temp.set(input.p2).scaled(fraction);
            point.set(input.p1).scaled(1 - fraction).added(temp);
            return callback.reportFixture(fixture, point, output.normal, fraction);
        }

        return input.maxFraction;
    }

    BroadPhase broadPhase;
    RayCastCallback callback;
}





































































































































































































































