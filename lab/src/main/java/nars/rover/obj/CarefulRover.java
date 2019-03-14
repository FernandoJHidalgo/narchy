//package nars.rover.robot;
//
//import nars.Global;
//import nars.NAR;
//import nars.rover.obj.NARVisionRay;
//import nars.rover.obj.VisionRay;
//import nars.term.Term;
//import PolygonShape;
//import Vec2;
//import Body;
//import BodyDef;
//import BodyType;
//import Fixture;
//
//import java.util.ArrayList;
//import java.util.Map;
//
///**
// * Motor with full and precise control over its sense input
// */
//public class CarefulRover extends AbstractPolygonBot {
//
//    private final NAR nar;
//    float linearDamping = 0.9f;
//    float angularDamping = 0.6f;
//    float restitution = 0.9f; //bounciness
//    float friction = 0.5f;
//    int numPixels = 8;
//
//
//    final ArrayList<String> randomActions = new ArrayList<>();
//    private final float visionDistanceFactor = 1f;
//    Map<Term,VisionRay> pixels = Global.newHashMap();
//
////    public void randomAction() {
////        int x = (int) (Math.random() * randomActions.size());
////        nar.input(randomActions.get(x));
////    }
//
////    public static String command(Operation operation, Memory memory) {
////        Term[] args = operation.args();
////        Term t1 = args[0];
////
////        //float priority = operation.getTask().getPriority();
////
////        int al = args.length;
////        if (args[al - 1].equals(memory.self()))
////            al--;
////
////        String command = "";
////        if (al == 1) {
////            command = t1.toString();
////        }
////        if (al == 2) {
////            Term t2 = args[1];
////            command = t1.toString() + "," + t2.toString();
////        } else if (al == 3) {
////            Term t2 = args[1];
////            Term t3 = args[2];
////            command = t1.toString() + "," + t2.toString() + "," + t3.toString();
////        }
////        return command;
////    }
//
//    public CarefulRover(String id, NAR nar) {
//        super(id);
//        this.nar = nar;
//    }
//
//    protected void initSensors(Body torso) {
//        Vec2 center = new Vec2(0, 0);
//
//        float da = (float) (Math.PI * 2 / numPixels);
//        float a = 0;
//        for (int i = 0; i < numPixels; i++) {
//            NARVisionRay v = new NARVisionRay(nar, torso,
//                        /*eats ?*/ center /*: new Vec2(0,0)*/,
//                    a, da, 3, 10, 1f/numPixels) {
//
//                @Override
//                protected float getDistance() {
//                    return distance * visionDistanceFactor;
//                }
//            };
//            pixels.put(v.angleTerm, v);
//
//            draw.addLayer(v);
//            senses.add(v);
//
//            a += da;
//        }
//
//    }
//    protected void initMotors(Body torso) {
////        nar.on(new NullOperator("vision") {
////            @Override
////            public List<Task> apply(Operation o) {
////
////                final String command = command(o, o.getMemory());
////
////                String[] sections = command.split(",");
////                if (sections.length == 1) {
////
//////                    switch (sections[0]) {
//////                        case "far":
//////                            dist = 20f;
//////                            break;
//////                        case "near":
//////                            dist = 10f;
//////                            break;
//////                        default:
//////                            return null;
//////                    }
////                    String angle = sections[0];
////
////                    float dist;
////                    dist = 5 + 20f * o.getTask().getTruth().getExpectation();
////                    VisionRay vr = pixels.get(angle);
////                    if (vr != null)
////                        vr.setDistance(dist);
////
////
////                }
////
////                return null;
////            }
////        });
//
//        System.out.println(pixels);
//        for (Term a : pixels.keySet()) {
//            randomActions.add("vision(" + a + ")! :!: %1%");
//            randomActions.add("vision(" + a + ")! :|: %0%");
//        }
//
//        randomActions.add("motor(left)! :|:");
//        //randomActions.add("motor(left,left)!");
//        randomActions.add("motor(right)! :|:");
//        //randomActions.add("motor(right,right)!");
//        //randomActions.add("motor(forward,forward)!"); //too much actions are not good,
//        randomActions.add("motor(forward)! :|:"); //however i would agree if <motor(forward,forward) --> motor(forward)>.
//        //randomActions.add("motor(forward,forward)!");
//        //randomActions.add("motor(forward)!");
//        randomActions.add("motor(reverse)! :|:");
//        randomActions.add("motor(stop)! :|:");
//        //randomActions.add("motor(random)!");
//
//
////        nar.on(new NullOperator("motor") {
////
////            @Override
////            public List<Task> apply(Compound operation) {
////
////                String command = command(operation, operation.getMemory());
////
////                //System.out.println(operation + " "+ command);
////
////                if (command.equals("$1")) {
////                    //variable causes random movement
////                    double v = Math.random();
////                    if (v < 0.25f) {
////                        command = "left";
////                    } else if (v < 0.5f) {
////                        command = "right";
////                    } else if (v < 0.75f) {
////                        command = "forward";
////                    } else {
////                        command = "reverse";
////                    }
////                }
////
////                float strength = operation.getTaskExpectation();
////                float rotSpeed = 15 * strength;
////                float linSpeed = 1 * strength;
////                switch (command) {
////                    case "right":
////                        rotateRelative(-rotSpeed);
////                        break;
////                    case "right,right":
////                        rotateRelative(-rotSpeed * 2);
////                        break;
////                    case "left":
////                        rotateRelative(+rotSpeed);
////                        break;
////                    case "left,left":
////                        rotateRelative(+rotSpeed * 2);
////                        break;
////                    case "forward,forward":
////                        thrustRelative(+2 * linSpeed);
////                        break;
////                    case "forward":
////                        thrustRelative(+linSpeed);
////                        break;
////                    case "reverse":
////                        thrustRelative(-linSpeed);
////                        break;
////                    case "stop":
////                        stop();
////                        break;
////                    case "random":
////                        randomAction();
////                        break;
////                }
////
////                return null;
////            }
////        });
//
//    }
//
//    @Override
//    protected void feelMotion() {
//
//    }
//
//    @Override
//    public RoboticMaterial getMaterial() {
//        return new RoboticMaterial(this) {
//
//
//        };
//    }
//
//    @Override
//    protected Body newTorso() {
//        PolygonShape shape = new PolygonShape();
//
//        Vec2[] vertices = {new Vec2(3.0f, 0.0f), new Vec2(-1.0f, +2.0f), new Vec2(-1.5f, 0), new Vec2(-1.0f, -2.0f)};
//        shape.set(vertices, vertices.length);
//        //shape.m_centroid.set(bodyDef.position);
//        BodyDef bd = new BodyDef();
//        bd.linearDamping = (linearDamping);
//        bd.angularDamping = (angularDamping);
//        bd.type = BodyType.DYNAMIC;
//        bd.position.set(0, 0);
//
//        Body torso = getWorld().createBody(bd);
//        Fixture f = torso.createFixture(shape, mass);
//        f.setRestitution(restitution);
//        f.setFriction(friction);
//
//        initSensors(torso);
//        initMotors(torso);
//
//        return torso;
//
//    }
//
//}