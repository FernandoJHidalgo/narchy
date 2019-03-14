///*
//Copyright 2007 Brian Tanner
//http://rl-library.googlecode.com/
//brian@tannerpages.com
//http://brian.tannerpages.com
//
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
// */
//package nars.experiment.tetris;
//
//import com.google.common.collect.Iterables;
//import com.gs.collections.api.tuple.Twin;
//import com.gs.collections.impl.tuple.Tuples;
//import nars.$;
//import nars.NAR;
//import nars.agent.NAgent;
//import nars.experiment.DiscreteEnvironment;
//import nars.experiment.tetris.visualizer.TetrisVisualizer;
//import nars.gui.BeliefTableChart;
//import nars.gui.NARSpace;
//import nars.index.CaffeineIndex;
//import nars.learn.Agent;
//import nars.nar.Default;
//import nars.nar.util.DefaultConceptBuilder;
//import nars.op.ArithmeticInduction;
//import nars.op.time.MySTMClustered;
//import nars.task.Task;
//import nars.term.Compound;
//import nars.term.Termed;
//import nars.term.obj.Termject;
//import nars.time.FrameClock;
//import nars.util.data.random.XorShift128PlusRandom;
//
//import java.util.*;
//
//import static nars.experiment.pong.Pong.numericSensor;
//
//
//public class Tetris extends TetrisState implements DiscreteEnvironment {
//
//    public static final int runCycles = 10000;
//    public static final int cyclesPerFrame = 128;
//    static int frameDelay = 100;
//
//    private final TetrisVisualizer vis;
//    private double currentScore;
//
//    private double previousScore;
//    private final static boolean NO_BACKWARDS_ROTATION = true;
//
//    /**
//     *
//     * @param width
//     * @param height
//     * @param timePerFall larger is slower gravity
//     */
//    public Tetris(int width, int height, int timePerFall) {
//        super(width, height, timePerFall);
//        vis = new TetrisVisualizer(this, 64);
//
//
//
//
//        restart();
//    }
//
//
//
//    @Override
//    public float pre(int t, float[] ins) {
//
//        if (this.seen == null)
//            this.seen = new float[ins.length];
//
//        //post process to monochrome bitmap
//        for (int i = 0; i < ins.length; i++) {
//            float v = seen[i];
//            if (v <= 0) v = 0;
//            if (v > 0) v = 1;
//            ins[i] = v;
//        }
//        //System.out.println(Texts.n4(ins));
//
//
//
//        float r = (float)getReward();
//        //System.out.println("rew=" + r);
//        return r;
//    }
//
//    @Override
//    public void preStart(Agent a) {
//        if (a instanceof NAgent) {
//            //provide custom sensor input names for the nars agent
//
//
//            NAgent ag = (NAgent) a;
//            NAR nar = ag.nar;
//
//////            //number relations
////            for (int i = 0; i < Math.max(getWidth(),getHeight()); i++) {
////                if (i > 0) {
////                    nar.believe($.inh($.p($.the(i - 1), $.the(i)), $.the("next")), 1f, 1f);
////                }
//////                    nar.believe($.inh($.p($.the(i),$.the(i-1)), $.the("prev")), 1f, 1f);
//////                    nar.believe($.inst($.secte($.the(i-1),$.the(i)), $.the("tang")), 1f, 1f);
//////                    //nar.believe($.inh($.sete($.the(i-1),$.the(i)), $.the("seq")), 1f, 1f);
//////                }
////            }
////            nar.ask("(&&,t:(#a,#b),t:(#c,#d),tang:{(#a & #c)})");
////            nar.ask("(&&,t:(#a,#b),t:(#c,#d),tang:{(#b & #d)})");
//            //nar.ask("(&&,t:(#a,#b),t:(#c,#d),(prev|next):(#b,#d))");
//            //nar.ask("(&&,t:(#a,#b),t:(#c,#d),seq:{#a,#c})");
//            //nar.ask("(&&,t:(#a,#b),t:(#c,#d),seq:{#b,#d})");
//
//            ag.setSensorNamer((i) -> {
//                int x = x(i);
//                int y = y(i);
//
//                //Compound squareTerm = $.inh($.p($.the(x), $.the(y)), $.the("t"));
//                Compound squareTerm = $.p(new Termject.IntTerm(x), new Termject.IntTerm(y));
//                return squareTerm;
//
////                int dx = (visionRadius  ) - ax;
////                int dy = (visionRadius  ) - ay;
////                Atom dirX, dirY;
////                if (dx == 0) dirX = $.the("v"); //vertical
////                else if (dx > 0) dirX = $.the("r"); //right
////                else /*if (dx < 0)*/ dirX = $.the("l"); //left
////                if (dy == 0) dirY = $.the("h"); //horizontal
////                else if (dy > 0) dirY = $.the("u"); //up
////                else /*if (dy < 0)*/ dirY = $.the("d"); //down
////                Term squareTerm = $.p(
////                        //$.p(dirX, $.the(Math.abs(dx))),
////                        $.inh($.the(Math.abs(dx)), dirX),
////                        //$.p(dirY, $.the(Math.abs(dy)))
////                        $.inh($.the(Math.abs(dy)), dirY)
////                );
////                //System.out.println(dx + " " + dy + " " + squareTerm);
////
////                //return $.p(squareTerm, typeTerm);
////                return $.prop(squareTerm, typeTerm);
////                //return (Compound)$.inh($.the(square), typeTerm);
//            });
//        }
//    }
//
//
//
//    @Override
//    public void post(int t, int action, float[] ins, Agent a) {
//        step(action);
//        System.out.println(a.summary());
//    }
//
//    public int numActions() {
//        return 6;
//    }
//
//    public double getReward() {
//        return Math.max(-30, Math.min(30, currentScore - previousScore))/10.0;
//    }
//
//
//
//
//    public void restart() {
//        reset();
//        spawn_block();
//        running = true;
//        previousScore = 0;
//        currentScore = -50;
//    }
//
//    public double step(int nextAction) {
//
//
//        if (nextAction > 5 || nextAction < 0) {
//            throw new RuntimeException("Invalid action selected in Tetrlais: " + nextAction);
//        }
//
//        if (running) {
//            take_action(nextAction);
//            update();
//        } else {
//            spawn_block();
//        }
//
//        toVector(false, seen);
//        //vis.repaint();
//
//
//        if (!gameOver()) {
//            previousScore = currentScore;
//            currentScore = score;
//            return currentScore;
//        } else {
//            //System.out.println("restart");
//            restart();
//            return 0;
//        }
//
//    }
//
//
//    public int getWidth() {
//        return width;
//    }
//
//    public int getHeight() {
//        return height;
//    }
//
//    @Override
//    public Twin<Integer> start() {
//        return Tuples.twin(getWidth()*getHeight(),NO_BACKWARDS_ROTATION ? numActions()-1 : numActions());
//    }
//
//    public static void main(String[] args) {
//        Random rng = new XorShift128PlusRandom(1);
//
//        //Multi nar = new Multi(3,512,
//        Default nar = new Default(1024,
//                4, 2, 2, rng,
//                new CaffeineIndex(new DefaultConceptBuilder(rng), 1 * 10000000, false)
//
//                ,new FrameClock());
//        nar.inputActivation.setValue(0.07f);
//        nar.derivedActivation.setValue(0.05f);
//
//
//        nar.beliefConfidence(0.9f);
//        nar.goalConfidence(0.8f);
//        nar.DEFAULT_BELIEF_PRIORITY = 0.35f;
//        nar.DEFAULT_GOAL_PRIORITY = 0.5f;
//        nar.DEFAULT_QUESTION_PRIORITY = 0.3f;
//        nar.DEFAULT_QUEST_PRIORITY = 0.4f;
//        nar.cyclesPerFrame.set(cyclesPerFrame);
//        nar.confMin.setValue(0.05f);
//
////        nar.on(new TransformConcept("seq", (c) -> {
////            if (c.size() != 3)
////                return null;
////            Term X = c.term(0);
////            Term Y = c.term(1);
////
////            Integer x = intOrNull(X);
////            Integer y = intOrNull(Y);
////            Term Z = (x!=null && y!=null)? ((Math.abs(x-y) <= 1) ? $.the("TRUE") : $.the("FALSE")) : c.term(2);
////
////
////            return $.inh($.p(X, Y, Z), $.oper("seq"));
////        }));
////        nar.believe("seq(#1,#2,TRUE)");
////        nar.believe("seq(#1,#2,FALSE)");
//
//        //nar.log();
//        //nar.logSummaryGT(System.out, 0.1f);
//
////		nar.log(System.err, v -> {
////			if (v instanceof Task) {
////				Task t = (Task)v;
////				if (t instanceof DerivedTask && t.punc() == '!')
////					return true;
////			}
////			return false;
////		});
//
//        //Global.DEBUG = true;
//
//        //new Abbreviation2(nar, "_");
//
//        MySTMClustered stm = new MySTMClustered(nar, 512, '.', 2);
//        MySTMClustered stmGoal = new MySTMClustered(nar, 512, '!', 2);
//
//        new ArithmeticInduction(nar);
//
//
//
//        Tetris t = new Tetris(6, 12, 2) {
//            @Override
//            protected int nextBlock() {
//                //return super.nextBlock(); //all blocks
//                return 1; //square blocks
//                //return 0; //long blocks
//            }
//        };
//
//        Iterable<Termed> cheats = Iterables.concat(
//                numericSensor(() -> t.currentX, nar, 0.3f,
//                        "(cursor_x)")
//                        //"(active,a)","(active,b)","(active,c)","(active,d)","(active,e)","(active,f)","(active,g)","(active,h)")
//                        //"I(a)","I(b)","I(c)","I(d)","I(e)","I(f)","I(g)","I(h)")
//                        //"(active,x)")
//                        .resolution(0.5f/t.width),
//                numericSensor(() -> t.currentY, nar, 0.3f,
//                        "(cursor_y)")
//                        //"active:(y,t)", "active:(y,b)")
//                        //"(active,y)")
//                        .resolution(0.5f/t.height)
//        );
//
//        NAgent n = new NAgent(nar) {
//            @Override
//            public void start(int inputs, int actions) {
//                super.start(inputs, actions);
//
//                List<Termed> charted = new ArrayList(super.actions);
//
//                charted.add(sad);
//                charted.add(happy);
//                Iterables.addAll(charted, cheats);
//
//                if (nar instanceof Default) {
//
//                    new BeliefTableChart(nar, charted).show(600, 900);
//
//                    //BagChart.show((Default) nar, 128);
//
//                    //STMView.show(stm, 800, 600);
//
//
//                    NARSpace.newConceptWindow((Default) nar, 128, 8);
//                }
//
//
//            }
//
//            @Override
//            protected Collection<Task> perceive(Set<Task> inputs) {
//                return super.perceive(inputs);
//            }
//        };
//
//
//        //addCamera(t, nar, 8, 8);
//
//
//        t.run(n, runCycles, frameDelay);
//
//        nar.index.print(System.out);
//        NAR.printTasks(nar, true);
//        NAR.printTasks(nar, false);
//        n.printActions();
//        //nar.forEachActiveConcept(System.out::println);
//    }
//
////    static void addCamera(Tetris t, NAR n, int w, int h) {
////        //n.framesBeforeDecision = GAME_DIVISOR;
////        SwingCamera s = new SwingCamera(t.vis);
////
////        NARCamera nc = new NARCamera("t", n, s, (x, y) -> $.p($.the(x), $.the(y)));
////
////        NARCamera.newWindow(s);
////
////        s.input(0, 0, t.vis.getWidth(),t.vis.getHeight());
////        s.output(w, h);
////
////        n.onFrame(nn -> {
////            s.update();
////        });
////    }
//
//}