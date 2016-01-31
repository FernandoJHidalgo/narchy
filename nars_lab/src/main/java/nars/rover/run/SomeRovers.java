package nars.rover.run;

import javassist.scopedpool.SoftValueHashMap;
import nars.Global;
import nars.Memory;
import nars.guifx.demo.NARide;
import nars.nar.Default;
import nars.rover.RoverWorld;
import nars.rover.Sim;
import nars.rover.robot.Rover;
import nars.rover.robot.Spider;
import nars.rover.robot.Turret;
import nars.rover.world.FoodSpawnWorld1;
import nars.term.index.MapIndex2;
import nars.time.SimulatedClock;

/**
 * Created by me on 6/20/15.
 */
public class SomeRovers {

    private static final SimulatedClock clock = new SimulatedClock();

    public static void main(String[] args) {

        Global.DEBUG = Global.EXIT_ON_EXCEPTION = false;


        //world = new ReactorWorld(this, 32, 48, 48*2);

        RoverWorld world = new FoodSpawnWorld1(128, 48, 48, 0.5f);

        //RoverWorld world = new GridSpaceWorld(GridSpaceWorld.newMazePlanet());


        final Sim game = new Sim(clock, world);


        game.add(new Turret("turret"));

        game.add(new Spider("spider",
                3, 3, 0.618f, 30, 30));
                {
            int conceptsFirePerCycle = 4;
            Default nar = new Default(
                    new Memory(clock, new MapIndex2(
                        new SoftValueHashMap())),
                        1200, conceptsFirePerCycle, 2, 3);

//            nar.memory.DEFAULT_JUDGMENT_PRIORITY = 0.35f;
//            nar.memory.DEFAULT_JUDGMENT_DURABILITY = 0.35f;
//            nar.memory.DEFAULT_GOAL_PRIORITY = 0.7f;
//            nar.memory.DEFAULT_GOAL_DURABILITY = 0.7f;
//            nar.memory.DEFAULT_QUESTION_PRIORITY = 0.6f;
//            nar.memory.DEFAULT_QUESTION_DURABILITY = 0.6f;

            //nar.initNAL9();
            //nar.memory.the(new Anticipate(nar));


            //nar.memory.perfection.setValue(0.15f);
            nar.core.confidenceDerivationMin.setValue(0.02f);
            nar.core.activationRate.setValue(0.9f/conceptsFirePerCycle /* approxmimate */);
            nar.memory.duration.set(10);
            nar.memory.cyclesPerFrame.set(3);
            nar.memory.shortTermMemoryHistory.set(3);
            //nar.memory.executionExpectationThreshold.setValue(0.95f);


            boolean gui = false;
            if (gui) {
                NARide.loop(nar, false);
            }



            game.add(new Rover("r1", nar));



        }
//        {
//            NAR nar = new Default();
//
//            //nar.param.outputVolume.set(0);
//
//            game.add(new CarefulRover("r2", nar));
//        }

        float fps = 50;
        game.run(fps);

    }

//    private static class InputActivationController extends CycleReaction {
//
//        private final NAR nar;
//
//        final int windowSize;
//
//        final DescriptiveStatistics busyness;
//
//        public InputActivationController(NAR nar) {
//            super(nar);
//            this.nar = nar;
//            this.windowSize = nar.memory.duration();
//            this.busyness = new DescriptiveStatistics(windowSize);
//        }
//
//        @Override
//        public void onCycle() {
//
//            final float bInst = nar.memory.emotion.busy();
//            busyness.addValue(bInst);
//
////            float bAvg = (float)busyness.getMean();
//
////            float busyMax = 3f;
//
////            double a = nar.memory.inputActivationFactor.get();
////            if (bAvg > busyMax) {
////                a -= 0.01f;
////            }
////            else  {
////                a += 0.01f;
////            }
////
////            final float min = 0.01f;
////            if (a < min) a = min;
////            if (a > 1f) a = 1f;
////
////            //System.out.println("act: " + a + " (" + bInst + "," + bAvg);
////
////            nar.param.inputActivationFactor.set(a);
////            nar.param.conceptActivationFactor.set( 0.5f * (1f + a) /** half as attenuated */ );
//        }
//    }

//    public static NAR newDefault(int threads) {
////
////        int cycPerFrame = 5;
////
////        //Alann d = new ParallelAlann(64, threads);
////        //DefaultAlann d = new DefaultAlann(32);
////        //d.tlinkToConceptExchangeRatio = 1f;
////
////        Default d = new Default();
////
////        //d.param.conceptActivationFactor.set(0.25f);
////        //d.param.inputsMaxPerCycle.set(4);
////
////        //Default d = new Equalized(1024, 16, 10);
////        //d.setTermLinkBagSize(16);
////        //d.setTaskLinkBagSize(16);
////
//////
//////            @Override
//////            public Concept newConcept(final Term t, final Budget b, final Memory m) {
//////
//////                Bag<Sentence, TaskLink> taskLinks =
//////                        new CurveBag(rng, /*sentenceNodes,*/ getConceptTaskLinks());
//////                        //new ChainBag(rng,  getConceptTaskLinks());
//////
//////                Bag<TermLinkKey, TermLink> termLinks =
//////                        new CurveBag(rng, /*termlinkKeyNodes,*/ getConceptTermLinks());
//////                        //new ChainBag(rng, /*termlinkKeyNodes,*/ getConceptTermLinks());
//////
//////                return newConcept(t, b, taskLinks, termLinks, m);
//////            }
//////
//////        };
////        //d.setInternalExperience(null);
////        //d.param.setClock(clock);
////        //d.setClock(clock);
////
////        //d.param.conceptTaskTermProcessPerCycle.set(4);
////
////
////        //d.param.setCyclesPerFrame(cycPerFrame);
////        d.setCyclesPerFrame(cycPerFrame);
////        //d.param.duration.set(cycPerFrame);
////        //d.param.conceptBeliefsMax.set(16);
////        //d.param.conceptGoalsMax.set(8);
////
////        //TextOutput.out(nar).setShowInput(true).setShowOutput(false);
////
////
////        //N/A for solid
////        //nar.param.inputsMaxPerCycle.set(32);
////        //nar.param.conceptsFiredPerCycle.set(4);
////
////        //d.param.conceptCreationExpectation.set(0);
////
////        //d.termLinkForgetDurations.set(4);
////
////
////
////        return d;
////    }
}
