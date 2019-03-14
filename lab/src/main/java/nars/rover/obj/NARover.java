/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package nars.rover.obj;

import com.artemis.Entity;
import com.gs.collections.api.block.function.primitive.FloatToFloatFunction;
import com.gs.collections.api.block.function.primitive.FloatToObjectFunction;
import nars.$;
import nars.NAR;
import nars.budget.UnitBudget;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.DefaultTruth;
import nars.truth.Truth;
import nars.util.Util;
import nars.util.math.FloatSupplier;
import nars.util.signal.SensorConcept;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;


/**
 * Triangular mobile vehicle
 */
public class NARover extends AbstractRover {

    public final NAR nar;


    //final Lobjects objs;

    final SensorConcept speedFore, speedBack, leftSpeed, rightSpeed,
            hungrySensor, sickSensor;

//    public static final Compound EAT_FOOD = $.p($.the("eat"), $.the("food"));
//    public static final Compound EAT_POISON = $.p($.the("eat"), $.the("poison"));
//    public static final Compound SPEED_LEFT = $.p($.the("speed"), $.the("Left"));
//    public static final Compound SPEED_RIGHT = $.p($.the("speed"), $.the("Right"));
//    public static final Compound SPEED_FORE = $.p($.the("speed"), $.the("Fore"));
//    public static final Compound SPEED_BACK = $.p($.the("speed"), $.the("Back"));
    public static final Compound EAT_FOOD = $.p($.the("eat"), $.the("food"));
    public static final Compound EAT_POISON = $.p($.the("eat"), $.the("poison"));
    public static final Compound SPEED_LEFT = $.p( $.the("feelleft"));
    public static final Compound SPEED_RIGHT = $.p( $.the("feelright"));
    public static final Compound SPEED_FORE = $.p( $.the("feelfore"));
    public static final Compound SPEED_BACK = $.p( $.the("feelback"));


//    final SimpleAutoRangeTruthFrequency linearVelocity;
//    final SimpleAutoRangeTruthFrequency motionAngle;
//    final SimpleAutoRangeTruthFrequency facingAngle;

    //public class DistanceInput extends ChangedTextInput

    //final static Logger logger = LoggerFactory.getLogger(NARover.class);

    //private MotorControls motors;

    public static final FloatToObjectFunction<Truth> tf = (v) -> {
        return new DefaultTruth(v, 0.9f);
    };

    public NARover(String id, Entity e, NAR nar) {
        super(id, e);


        this.nar = nar;



        int minUpdateTime = -1;
        int maxUpdateTime = -1;


        FloatToFloatFunction speedThresholdToFreq = (speed) -> {
            return speed < 0.01 ? 0 : Util.clamp(0.5f + speed);
        };
        FloatToFloatFunction sigmoid = (n) -> {
            return Util.sigmoid(n);
        };
        FloatToFloatFunction sigmoidIfPositive = (n) -> {
            return n > 0 ? Util.sigmoid(n) : -1; //Float.NaN;
        };
        FloatToFloatFunction sigmoidIfNegative = (n) -> {
            return n < 0 ? Util.sigmoid(-n) : -1; //Float.NaN;
        };
        FloatToFloatFunction linearPositive = (n) -> {
            return n > 0 ? Util.clamp(0.5f + n/2f) : 0; //Float.NaN;
        };
        FloatToFloatFunction linearNegative = (n) -> {
            return n < 0 ? Util.clamp(0.5f + (-n)/2f) : 0; //Float.NaN;
        };

        Vec2 forwardVec = new Vec2(1, 0f);
        Vec2 tmp = new Vec2(), tmp2 = new Vec2();
        FloatSupplier linearSpeed =
                () -> {

                    //float thresh = 0.01f;

                    Vec2 lv = torso.getLinearVelocityFromLocalPointToOut(Vec2.ZERO, tmp2);
                    Vec2 worldForward = torso.getWorldPointToOut(forwardVec, tmp).subLocal(torso.getWorldCenter());
                    float v = Vec2.dot(
                            lv,
                            worldForward
                    );

                    //System.out.println("linear vel=" + v);

                    v *= 0.75f;

                    /*if (Math.abs(v) < thresh)
                        v = 0;*/
                    return v;
                };



        this.speedFore = new SensorConcept(SPEED_FORE, nar,
                () -> linearPositive.valueOf(linearSpeed.asFloat()), tf)
                .timing(minUpdateTime, maxUpdateTime);

        this.speedBack = new SensorConcept(SPEED_BACK, nar,
                () -> linearNegative.valueOf(linearSpeed.asFloat()), tf)
                .timing(minUpdateTime, maxUpdateTime);


        FloatSupplier angleSpeed = () -> {
            float v = torso.getAngularVelocity();

            //System.out.println("angle vel=" + angularVelocity);

            v *= 1.5f; //sensitivity

            return v;
        };

        this.leftSpeed = new SensorConcept(SPEED_LEFT, nar,
                () -> linearPositive.valueOf(angleSpeed.asFloat()),
                tf).timing(minUpdateTime, maxUpdateTime);

        this.rightSpeed = new SensorConcept(SPEED_RIGHT, nar,
                () -> linearNegative.valueOf(angleSpeed.asFloat()),
                tf).timing(minUpdateTime, maxUpdateTime);

        hungrySensor = new SensorConcept(EAT_FOOD, nar, () -> health.nutrition, tf)
                .timing(minUpdateTime, maxUpdateTime);

        sickSensor = new SensorConcept(EAT_POISON, nar, () -> health.damage, tf)
                .timing(minUpdateTime, maxUpdateTime);


        int minMotorFeedbackCycles = -1; ////nar.duration() / 2;
        int maxMotorFeedbackCycles = -1; //nar.duration() * 3;

//        MotorConcept motorLeft = new MotorConcept("motor(left)", nar, (b,l) -> {
//            if ((b > l) || (l < 0.5f)) return 0;
//            return motor.left(l-b);
//        }).setFeedbackTiming(minMotorFeedbackCycles, maxMotorFeedbackCycles);
//
//        MotorConcept motorRight = new MotorConcept("motor(right)", nar, (b,l) -> {
//            if ((b > l) || (l < 0.5f)) return 0;
//            return motor.right(l-b);
//        }).setFeedbackTiming(minMotorFeedbackCycles, maxMotorFeedbackCycles);
//
//        MotorConcept motorFore = new MotorConcept("motor(fore)", nar, (b,l) -> {
//            if ((b > l) || (l < 0.5f)) return 0;
//            return motor.forward(l-b);
//        }).setFeedbackTiming(minMotorFeedbackCycles, maxMotorFeedbackCycles);
//
//        MotorConcept motorBack = new MotorConcept("motor(back)", nar, (b,l) -> {
//            if ((b > l) || (l < 0.5f)) return 0;
//            return motor.backward(l-b);
//        }).setFeedbackTiming(minMotorFeedbackCycles, maxMotorFeedbackCycles);
//
//        MotorConcept motorStop = new MotorConcept("motor(stop)", nar, (b,l) -> {
//            if ((b > l) || (l < 0.5f)) return 0;
//            return motor.stop(l-b);
//        }).setFeedbackTiming(minMotorFeedbackCycles, maxMotorFeedbackCycles);
//
//        MotorConcept turretFire = new MotorConcept("turret(fire)", nar, (b,s) -> {
//
////            if (s > motorThresh) {
////                if (gun.fire(torso, s)) {
////                    return s;
////                }
////            }
//            return Float.NaN; //unfired;
//
//        }).setFeedbackTiming(minMotorFeedbackCycles, maxMotorFeedbackCycles);
//

    }



//    @Override
//    protected void onEat(Body eaten, Material m) {
//        if (m instanceof Sim.FoodMaterial) {
//            logger.warn("food");
//            //nar.input("eat:food. :|: %1.0;0.9%");
//            hungry = 0;
//
//            //nar.input("goal:{food}. :|: %1.00;0.75%");
//            //nar.input("goal:{health}. :|: %1.00;0.75%");
//        }
//        else if (m instanceof Sim.PoisonMaterial) {
//            logger.warn("poison");
//            //nar.input("eat:poison. :|:");
//            sick = Util.clamp(sick + 1f);
//
//            //nar.input("goal:{food}. :|: %0.00;0.90%");
//            //nar.input("goal:{health}. :|: %0.00;0.90%");
//        }
//
//    }


//    @Override
//    public void step(int dt) {
//        super.step(dt);
//
//        gun.step(dt);
//
//        try {
//            nar.step();
//        } catch (RuntimeException e) {
//            e.printStackTrace();
//            nar.stop();
//        }
//
//        sick = Util.clamp(sick - 0.002f);
//        hungry = Util.clamp(hungry + 0.002f);
//    }

//    public void inputMission() {
//
//        //alpha curiosity parameter
//        //long t = nar.time();
//
//
//        //nar.input("<{left,right,forward,reverse} --> direction>.");
//        //nar.input("<{wall,empty,food,poison} --> material>.");
//        //nar.input("<{0,x,xx,xxx,xxxx,xxxxx,xxxxxx,xxxxxxx,xxxxxxxx,xxxxxxxxx,xxxxxxxxxx} --> magnitude>.");
//        //nar.input("<{0,1,2,3,4,5,6,7,8,9} --> magnitude>.");
//
//        //nar.input("< ( ($n,#x) &| ($n,#y) ) =/> lessThan(#x,#y) >?");
//
//        /*
//        for (int i = 0; i < 2; i++) {
//            String x = "lessThan(" + XORShiftRandom.global.nextInt(10) + "," +
//                    XORShiftRandom.global.nextInt(10) + ")?";
//
//            nar.input(x);
//        }
//        */
//
////        nar.input("<0 <-> x>. %0.60;0.60%");
////        nar.input("<x <-> xx>. %0.60;0.60%");
////        nar.input("<xx <-> xxx>. %0.60;0.60%");
////        nar.input("<xxx <-> xxxx>. %0.60;0.60%");
////        nar.input("<xxxx <-> xxxxx>. %0.60;0.60%");
////        nar.input("<xxxxx <-> xxxxxx>. %0.60;0.60%");
////        nar.input("<xxxxxx <-> xxxxxxx>. %0.60;0.60%");
////        nar.input("<xxxxxxx <-> xxxxxxxxx>. %0.60;0.60%");
////        nar.input("<xxxxxxxx <-> xxxxxxxxxx>. %0.60;0.60%");
////        nar.input("<0 <-> xxxxxxxxx>. %0.00;0.90%");
//
//        //nar.input("goal:{health}! %1.0;0.95%");
//        //nar.input("goal:{health}! :|:");
//
//        //nar.believe("goal:{health}", Tense.Present, 0.5f, 0.9f); //reset
//
//
//
//        //nar.input("<?x ==> goal:{$y}>? :|:");
//
//        try {
//
//            /*if (t < 1500)
//                train(t);
//            else */ if (mission == 0) {
//                //seek food
//                //curiosity = 0.05f;
//
//                //nar.goal("goal:{food}", 1.00f, 0.90f);
//                //nar.input("goal:{food}!");
//
//                //clear appetite:
//                //nar.input("eat:food. :|: %0.00;0.75%");
//                //nar.input("eat:poison. :|: %0.00;0.75%");
//

//
//                //nar.input("speed:forward! %1.00;0.7%");
//                //nar.input("eat:poison! %0.0|0.9%");
//                //nar.input("(--, <eat:food <-> eat:poison>). %1.00;0.95%");
//                //nar.input("(?x ==> eat:#y)?");
//                //nar.input("(?x && eat:#y)?");
//
//                //nar.input("MotorControls(#x,motor,(),#z)! :|: %1.0;0.25%"); //create demand for action
//                //nar.input("MotorControls(?x,motor,(),#z)! :|: %1.0;0.25%");
//                //nar.concept("MotorControls(?x,motor,?y,#z)").print();
//                //nar.concept("MotorControls(#x,motor,#y,#z)").print();
//
//
//                //((Default)nar).core.active.printAll();
//                //nar.concept("MotorControls(forward,motor,(),#1)").print();
//
//                //nar.input("motion:#anything! :|:"); //move
//
//            } else if (mission == 1) {
//                //rest
//                nar.input("eat:#anything! :|: %0%");
//                //nar.input("motion:(0, #x)! :|:"); //stop
//            }
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
//        //..
//    }


//    protected void train(long t) {
//        //float freq = 0.5f + 0.5f * (1/(1f + t/5000f)), conf = 0.85f;
//        float freq = 1f, conf = 0.9f;
//        //nar.input("MotorControls(random,motor,(),#x)! %1.0;0.1%");
//        nar.input("MotorControls(random,motor,(),#x)! %" + n2(freq) + "|" + n2(conf) + "%");
//        System.out.println("@" + t + " Curiosity Trained @ freq=" + freq);
//    }


//    @Override
//    public BeingMaterial getMaterial() {
//        return material;
//    }
//
//    @Override
//    protected Body newTorso() {
//
//        Body torso = newTriangle(getWorld(), mass);
//
//        torso.setUserData(getMaterial());
//
//        return torso;
//    }


//    /** http://stackoverflow.com/questions/20904171/how-to-create-snake-like-body-in-box2d-and-cocos2dx */
//    public Arm addArm(Being b, String id, NarQ controller, float ax, float ay, float angle) {
//
//        int segs = 4;
//        float segLength = 3.7f;
//        float thick = 1.5f;
//
//        Arm a = new Arm(id, sim, torso, ax, ay, angle, segs, segLength, thick);
//
//        //addEye(b, id + "e", controller, a.segments.getLast(), 9, new Vec2(0.5f, 0), 1f, (float)(+Math.PI/4f), 2.5f);
//
//        a.joints.forEach((Consumer<RevoluteJoint>) jj -> {
//
//            for (float speed : new float[] { +1 , -1 }){
//                controller.output.add(new NarQ.Action() {
//
//                    @Override
//                    public void run(float strength) {
//                        ///System.out.println("motor " + finalIdx + " " + strength);
//                        jj.setMotorSpeed(strength * speed);
//                    }
//
//                    @Override
//                    public float ran() {
//                        return jj.getMotorSpeed();
//                    }
//                });
//            }
//
//        });
//
//        return a;
//    }


    public List<SensorConcept> addEye(String id, Body base, int detail, Vec2 center, float arc, float centerAngle, float distance) {
        return addEye(id, base, 1, detail, center, arc, centerAngle, distance, (v) -> {
        });
    }

    public List<SensorConcept> addEyeWithMouth(String id, Body base, int pixels, int detail, Vec2 center, float arc, float centerAngle, float distance, float mouthArc) {
        return addEye(id, base, pixels, detail, center, arc, centerAngle, distance, (v) -> {
            //float angle = v.angle;
            v.setEats(true);
            //((angle < mouthArc / 2f) || (angle > (Math.PI * 2f) - mouthArc / 2f)));
        });
    }

    public List<SensorConcept> addEye(String id, Body base, int pixels, int resolution, Vec2 center,
                                      float arc, float centerAngle, float distance, Consumer<VisionRay> each) {

        float aStep = arc / pixels;

        float startAngle = centerAngle - arc / 2;



        List<SensorConcept> sensorConcepts = $.newArrayList(pixels * resolution);

        Term[] materials = {$.the("food"), $.the("poison"), $.the("wall")};

        float pixelPri = 0.05f; //1f / (float)Math.sqrt(materials.length * pixels);


        for (int i = 0; i < pixels; i++) {
            final float angle = (startAngle + (aStep * i));

            Termed visionTerm = nar.activate($.the(id + i), UnitBudget.Zero);

            VisionRay v = new VisionRay(center, angle, aStep, base, distance, resolution, (dist, c) -> {
                float p = nar.conceptPriority(visionTerm);
                //if (Float.isFinite(dist)) {
                float closeness = 1f - dist;
                c.x = p/2f * (0.5f + 0.5f * closeness); c.y = 0;
                c.z = closeness;
                //} else {
                  //  c.x = 0; c.y = p; c.z = 0;
                //}
            });

            each.accept(v);


            for (Term material : materials) {

                DoubleSupplier value = () -> {
                    if (v.hit(material)) {
                        float x = (1f - v.seenDist); //closer = larger number (up to 1.0)
                        if (x < 0.01f) return 0.5f;
                        else return 0.5f + 0.45f * x;
                    }
                    return 0; //nothing seen within the range
                };


                Compound term =
                        //$.imageInt(1, visionTerm.term(), material);
                        $.instprop(visionTerm.term(), material);

                SensorConcept visionSensor = new SensorConcept(

                        //$.prop(v.visionTerm, $.the(material)),
                        //$.p(v.visionTerm, $.the(material)),
                        term,

                        nar,

                        () -> (float) value.getAsDouble(), tf

                ).resolution(0.1f)/*.timing(1, 4)*/.
                        pri(pixelPri);
                sensorConcepts.add(visionSensor);

            }

            entity.getWorld().createEntity().edit()
                .add(v)
                .add(new DrawAbove(v));


            //entity.edit().add(v);
            //senses.add(v);
        }

        return sensorConcepts;


    }
}
