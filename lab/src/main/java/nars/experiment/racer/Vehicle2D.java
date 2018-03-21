package nars.experiment.racer;

import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.dynamics.Body2D;
import org.jbox2d.dynamics.Dynamics2D;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.joints.Joint;
import org.jbox2d.dynamics.joints.RevoluteJoint;
import org.jbox2d.dynamics.joints.RevoluteJointDef;
import spacegraph.math.v2;

public class Vehicle2D implements Racer.IVehicleControl/*, IUpdateHandler */{

    protected static final int WHEEL_HEIGHT = 20;
    protected static final int WHEEL_WIDTH = 10;

    public static final float DEGTORAD = 0.0174532925199432957f;
    public static final float RADTODEG = 57.295779513082320876f;

    public static final int ACCELERATE = 1;
    public static final int ACCELERATE_NONE = 0;
    public static final int BREAK = -1;

    public static final int STEER_RIGHT = 1;
    public static final int STEER_NONE = 0;
    public static final int STEER_LEFT = -1;
    public final float width;
    public final float height;

    float m_currentTraction = 1;
    // Char caracteristics
    float m_maxForwardSpeed = 1;
    float m_maxBackwardSpeed = 1;
    float m_maxDriveForce = 10000;
    float m_maxLateralImpulse = 10000;

    private Wheel rightFront;
    private Wheel leftFront;
    private Wheel rightBack;
    private Wheel leftBack;

    private final boolean powered = true;

    protected Body2D mCarBody;
    protected PolygonShape mCarShape;

    private int steer = STEER_NONE;
    private int accelerate = ACCELERATE_NONE;

    //BaseGameActivity context;
    Dynamics2D mPhysicsWorld;
    //Scene mScene;

    public Vehicle2D(Dynamics2D physicWorld) {

        this.mPhysicsWorld = physicWorld;



        this.width = 30;
        this.height = 50;
        mCarShape = this.makeColoredRectangle(0, 0, width,height);

//        mScene.attachChild(mCarShape);

        final float pDensity = 0.1f;
        final float pElasticity = 0.5f;
        final float pFriction = 0.5f;

        final FixtureDef carFixtureDef = new FixtureDef(mCarShape, pDensity, pFriction);
        carFixtureDef.restitution = pElasticity;
        this.mCarBody = mPhysicsWorld.addDynamic(carFixtureDef);
        //mCarBody.setLinearDamping(5);
        //mCarBody.setAngularDamping(20);

//        MassData data = mCarBody.getMassData();
//        data.mass = 3.9f;
//        mCarBody.setMassData(data);

//        System.out.println("Mass:"+mCarBody.getMass()+" density:"+pDensity+" volume:"+mCarShape.getWidth()*mCarShape.getHeight());

        //this.mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(this.mCarShape, this.mCarBody, true, true));

        rightFront = new Wheel(mPhysicsWorld,this,Wheel.WheelPosition.FRONT_RIGHT);
        leftFront = new Wheel(mPhysicsWorld,this,Wheel.WheelPosition.FRONT_LEFT);
        rightBack = new Wheel(mPhysicsWorld,this,Wheel.WheelPosition.BACK_RIGHTs);
        leftBack = new Wheel(mPhysicsWorld,this,Wheel.WheelPosition.BACK_LEFT);
    }

    protected void setCharacteristics(final float maxForwardSpeed, final float maxBackwardSpeed,
            final float backTireMaxDriveForce, final float backTireMaxLateralImpulse) {
        this.m_maxForwardSpeed = maxForwardSpeed;
        this.m_maxBackwardSpeed = maxBackwardSpeed;
        this.m_maxDriveForce = backTireMaxDriveForce;
        this.m_maxLateralImpulse = backTireMaxLateralImpulse;
    }

    @Override
    public void steerLeft() {
        steer = STEER_LEFT;
    }

    @Override
    public void steerRight() {
        steer = STEER_RIGHT;
    }

    @Override
    public void steerNone() {
        steer = STEER_NONE;
    }

    @Override
    public void pedalAccelerate() {
        accelerate = ACCELERATE;
    }

    @Override
    public void pedalBreak() {
        accelerate = BREAK;
    }

    @Override
    public void pedalNone() {
        accelerate = ACCELERATE_NONE;
    }

    public void onUpdate(final float pSecondsElapsed) {
        //Driving
        updateDrive();

        //control steering
        float lockAngle = 35f * DEGTORAD;
        float turnSpeedPerSec = 160f * DEGTORAD;//from lock to lock in 0.5 sec
        float turnPerTimeStep = turnSpeedPerSec / 60.0f;

        float desiredAngle = 0f;
        switch (steer) {
        case STEER_LEFT:
            desiredAngle = -lockAngle;
            break;
        case STEER_RIGHT:
            desiredAngle = lockAngle;
            break;
        default:
            ;// nothing
        }

        RevoluteJoint leftJoint = (RevoluteJoint) leftFront.getJoint();
        RevoluteJoint rightJoint = (RevoluteJoint) rightFront.getJoint();

        float angleNow = leftJoint.getJointAngle();
        float angleToTurn = desiredAngle - angleNow;
        angleToTurn = b2Clamp(angleToTurn, -turnPerTimeStep, turnPerTimeStep);
        float newAngle = angleNow + angleToTurn;

        leftJoint.setLimits(newAngle, newAngle);
        rightJoint.setLimits(newAngle, newAngle);
    }

    protected void updateDrive(){

        // find desired speed
        float desiredSpeed = 0;
        switch (accelerate) {
        case ACCELERATE:
            desiredSpeed = m_maxForwardSpeed;
            break;
        case BREAK:
            desiredSpeed = m_maxBackwardSpeed;
            break;
        default:
            return;// do nothing
        }

        //find current speed in forward direction
        v2 localPoint = new v2(0, 1);
        v2 currentForwardNormal = new v2(leftFront.getBody().getWorldVector(localPoint));

        v2 forwardVelocity = getForwardVelocity(leftFront.getBody());
        float currentSpeed = b2Dot(forwardVelocity, currentForwardNormal);


        // apply necessary force
        //apply necessary force
        float force = 0;
        if ( desiredSpeed > currentSpeed ) {
            force = m_maxDriveForce;
        } else if ( desiredSpeed < currentSpeed ) {
            force = -m_maxDriveForce;
        } else {
            return;
        }

        v2 forceVector = currentForwardNormal.scale(m_currentTraction * force*-1) ;


        leftFront.getBody().applyForce(forceVector, leftFront.getBody().getWorldCenter());
        rightFront.getBody().applyForce(forceVector, rightFront.getBody().getWorldCenter());

    }


    protected float b2Clamp(final float a, final float low, final float high) {
        return Math.max(low, Math.min(a, high));
    }

    protected float b2Dot(final v2 a, final v2 b) {
        return a.x * b.x + a.y * b.y;
    }

    protected v2 getForwardVelocity(final Body2D body) {
        v2 localPoint = new v2(0, 1);
        v2 currentForwardNormal = body.getWorldVector(localPoint);

        return currentForwardNormal.scale(b2Dot(currentForwardNormal, body.getLinearVelocity()));
    }

    protected v2 getLateralVelocity(final Body2D body) {
        v2 localPoint = new v2(1, 0);
        v2 currentForwardNormal = body.getWorldVector(localPoint);

        return currentForwardNormal.scale(b2Dot(currentForwardNormal, body.getLinearVelocity()));
    }

    protected Body2D getBody() {
        return mCarBody;
    }

    protected PolygonShape makeColoredRectangle(final float pX, final float pY) {
        return makeColoredRectangle(pX, pY, WHEEL_WIDTH, WHEEL_HEIGHT);
    }

    protected PolygonShape makeColoredRectangle(final float pX, final float pY,
                                                final float width, final float height) {
//        final Rectangle coloredRect = new Rectangle(pX, pY, width, height,context.getVertexBufferObjectManager());
//        coloredRect.setColor(pRed, pGreen, pBlue);
        return new PolygonShape(4).setAsBox(width/2, height/2, new v2(pX-width/2, pY-height/2), 0);
    }

    public PolygonShape getShape() {
        return mCarShape;
    }

    public static class Wheel {
        static final float PIXEL_TO_METER_RATIO_DEFAULT = 32.0f;

        protected enum WheelPosition{
            FRONT_LEFT,
            FRONT_RIGHT,
            BACK_LEFT,
            BACK_RIGHTs
        }

        private final Dynamics2D mDynamics2D;
        //private final BaseGameActivity context;

        private Joint joint;

        private PolygonShape mWheelShape;
        private Body2D mWheelBody;

        private final Vehicle2D vehicle;

        protected final WheelPosition position;

        protected Wheel(final Dynamics2D Dynamics2D, final Vehicle2D vehicle, final WheelPosition position) {
            this.mDynamics2D = Dynamics2D;
            this.vehicle = vehicle;
            this.position = position;

            PolygonShape shape = vehicle.getShape();

            switch (position) {
            case FRONT_RIGHT:
                mWheelShape = vehicle.makeColoredRectangle(shape.centroid.x+vehicle.width- WHEEL_WIDTH, shape.centroid.y);
                break;
            case FRONT_LEFT:
                mWheelShape = vehicle.makeColoredRectangle(shape.centroid.x, shape.centroid.y);
                break;
            case BACK_LEFT:
                mWheelShape = vehicle.makeColoredRectangle(shape.centroid.x, shape.centroid.y+vehicle.height- WHEEL_HEIGHT);
                break;
            case BACK_RIGHTs:
                mWheelShape = vehicle.makeColoredRectangle(shape.centroid.x+vehicle.width- WHEEL_WIDTH, shape.centroid.y+vehicle.height- WHEEL_HEIGHT);
                break;
            default:
                throw new IllegalArgumentException("Wheel position invalid");
            }
            //vehicle.mScene.attachChild(mWheelShape);


            final FixtureDef wheelFixtureDef = new FixtureDef(mWheelShape, 1, 0.5f);
            wheelFixtureDef.restitution = 0.5f;
            wheelFixtureDef.isSensor = false;

            this.mWheelBody = mDynamics2D.addDynamic( wheelFixtureDef);

            //mWheelBody.setLinearDamping(1);
            //mWheelBody.setAngularDamping(1);

            //this.mDynamics2D.registerPhysicsConnector(new PhysicsConnector(this.mWheelShape, this.mWheelBody, true, true));

            createJoint(WHEEL_WIDTH, WHEEL_HEIGHT);
        }

        private void createJoint(float wheelWidth, float wheelHeight){
            Body2D body = vehicle.getBody();

            // Tody: get the correct center, if its rotated/scaled, etc
            float xCar = vehicle.mCarBody.getWorldCenter().x + vehicle.width/2.0f;
            float xWheel = mWheelBody.getWorldCenter().x + wheelWidth/2.0f;

            float yCar = vehicle.mCarBody.getWorldCenter().y + vehicle.height/2.0f;
            float yWheel = mWheelBody.getWorldCenter().y + wheelHeight/2.0f;

            RevoluteJointDef revoluteJointDefLeft = new RevoluteJointDef();
            revoluteJointDefLeft.initialize(body, mWheelBody, mWheelBody.getWorldCenter());
            revoluteJointDefLeft.collideConnected = false;
            configureJoint(revoluteJointDefLeft);

            // Spitze minus Schaft
            revoluteJointDefLeft.localAnchorA.set((xWheel-xCar)/PIXEL_TO_METER_RATIO_DEFAULT,(yWheel-yCar)/PIXEL_TO_METER_RATIO_DEFAULT);

            joint = this.mDynamics2D.addJoint(revoluteJointDefLeft);
        }

        protected void configureJoint(final RevoluteJointDef revoluteJointDef) {
            revoluteJointDef.enableMotor = false;

            revoluteJointDef.enableLimit = true;
            revoluteJointDef.lowerAngle = 0;
            revoluteJointDef.upperAngle = 0;

            revoluteJointDef.collideConnected = false;
            revoluteJointDef.localAnchorB.set(0, 0);
        }

        protected Joint getJoint() {
            return joint;
        }

        protected Body2D getBody() {
            return mWheelBody;
        }
    }
}
