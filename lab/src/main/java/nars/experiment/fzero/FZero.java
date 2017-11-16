package nars.experiment.fzero;

import jcog.Util;
import nars.*;
import nars.concept.GoalActionAsyncConcept;
import nars.concept.ScalarConcepts;
import nars.gui.Vis;
import nars.op.video.Scale;
import nars.time.Tense;
import nars.util.signal.CameraSensor;
import org.apache.commons.math3.util.MathUtils;
import org.jetbrains.annotations.NotNull;

import static spacegraph.SpaceGraph.window;

/**
 * Created by me on 3/21/17.
 */
public class FZero extends NAgentX {

    private final FZeroGame fz;

    float fwdSpeed = 9;
    float rotSpeed = 0.05f;

    public static void main(String[] args) {

        float fps = 16f;

        NAgentX.runRT((n) -> {

            FZero a = null;
            try {
                //n.truthResolution.setValue(0.05f);
                a = new FZero(n);
                //a.durations.setValue(2f); //2*
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            }
            a.trace = true;

            return a;

        }, fps);


    }

    public FZero(NAR nar) throws Narsese.NarseseException {
        super("fz", nar);

        this.fz = new FZeroGame();

        CameraSensor<Scale> c = senseCamera(id, new Scale(() -> fz.image,
                32, 24)/*.blur()*/).resolution(0.05f);
//        CameraSensor<Scale> c = senseCameraReduced(id, new Scale(() -> fz.image,
//                128, 64), 8, 8, 2, 2).resolution(0.1f);

//        PixelBag cc = PixelBag.of(()->fz.image, 32, 24);
//        cc.addActions($.the("fz"), this, false, false, true);
//        CameraSensor<PixelBag> sc = senseCamera("fz" /*"(nario,local)"*/, cc)
//                .resolution(0.05f);


        //initToggle();
        initBipolar();

//        actionUnipolar(p("left"), (r) -> {
//            //if (r > 0.5f)
//                fz.playerAngle -= (r) * rotSpeed;
//            return r;
//        });//.resolution.setValue(0.01f);
//        actionUnipolar(p("right"), (r) -> {
//            //if (r > 0.5f)
//                fz.playerAngle += (r) * rotSpeed;
//            return r;
//        });//.resolution.setValue(0.01f);


        //yaw stabilizer (eternal goal)
//        nar.goal(p($.the("x"), $.the("\"+\"")), 0.5f, 0.1f);
//        nar.goal(p($.the("x"), $.the("\"-\"")), 0.5f, 0.1f);

        //keyboard-ish controls:
//actionToggle($.inh(Atomic.the("fwd"),id), (b)-> fz.thrust = b );
//        actionTriState($.inh(Atomic.the("rot"), id ), (dh) -> {
//            switch (dh) {
//                case +1: fz.left = false; fz.right = true; break;
//                case 0: fz.left = fz.right = false; break;
//                case -1: fz.left = true; fz.right = false; break;
//            }
//        });

//        senseNumberDifference($.inh(the("joy"), id), happy).resolution.setValue(0.02f);
        senseNumberDifference($.inh($.the("angVel"), id), () -> (float) fz.playerAngle).resolution(0.02f);
        senseNumberDifference($.inh($.the("accel"), id), () -> (float) fz.vehicleMetrics[0][6]).resolution(0.02f);
        @NotNull ScalarConcepts ang = senseNumber($.the("ang"), () ->
                        (float) (0.5f + 0.5f * MathUtils.normalizeAngle(fz.playerAngle, 0) / (Math.PI)),
                11,
                ScalarConcepts.Needle
                //ScalarConcepts.Fluid
        ).resolution(1f);
        window(
                Vis.conceptBeliefPlots(this, ang, 16), 300, 300);

        //nar.mix.stream("Derive").setValue(1);

//        AgentService p = new AgentService.AgentBuilder(
//                //DQN::new,
//                HaiQAgent::new,
//                //() -> Util.tanhFast(a.dexterity())) //reward function
//                () -> dexterity() * Util.tanhFast(rewardCurrent) /* - lag */) //reward function
//
//                .in(this::dexterity)
//                .in(new FloatNormalized(() -> rewardCurrent).relax(0.01f))
//                .in(new FloatNormalized(
//                        ((Emotivation) nar.emotion).cycleDTRealMean::getValue)
//                        .relax(0.01f)
//                ).in(new FloatNormalized(
//                                () -> nar.emotion.busyVol.getSum()
//                        ).relax(0.01f)
//                ).out(
//                        new StepController((x) -> c.in.preAmp(x), 0, 0.1f, 0.2f, 0.3f, 0.4f, 0.5f)
//                ).out(
//                        new StepController((x) -> ang.in.preAmp(x), 0, 0.1f, 0.2f, 0.3f, 0.4f, 0.5f)
//                ).get(nar);


//        try {
//            new TaskRule("(%1 &&+0 fz:joy)", "(%1 ==>+0 fz:happy)", nar) {
//                @Override
//                public boolean test(@NotNull Task task) {
//                    return task.isBelief();
//                }
//            };
//            new TaskRule("(%1 &&+5 %2)", "seq(%1,%2)", nar) {
//                @Override
//                public boolean test(@NotNull Task task) {
//                    return polarized(task);
//                }
//            };
//            new TaskRule("(seq(%1,%2) &&+5 %3)", "seq(%1,%2,%3)", nar) {
//                @Override
//                public boolean test(@NotNull Task task) {
//                    return polarized(task);
//                }
//            };
//            new TaskRule("((%1 &&+5 %2) &&+5 %3)", "seq(%1,%2,%3)", nar) {
//                @Override
//                public boolean test(@NotNull Task task) {
//                    return polarized(task);
//                }
//            };
//            new TaskRule("(%1 &&+5 (--,%1))", "neg(%1)", nar) {
//                @Override
//                public boolean test(@NotNull Task task) {
//                    return polarized(task);
//                }
//            };
//            new TaskRule("(%1 &&-5 (--,%1))", "pos(%1)", nar) {
//                @Override
//                public boolean test(@NotNull Task task) {
//                    return polarized(task);
//                }
//            };
//            new TaskRule("(%1 &&+0 (--,(fz)))", "--good(%1)", nar) {
//                @Override
//                public boolean test(@NotNull Task task) {
//                    return polarized(task);
//                }
//            };
//            new TaskRule("(%1 &&+0 (fz))", "good(%1)", nar) {
//                @Override
//                public boolean test(@NotNull Task task) {
//                    return polarized(task);
//                }
//            };

//            new TaskRule("(%1 ==>+0 (fz))", "good(%1)", nar) {
//                @Override
//                public boolean test(@NotNull Task task) {
//                    return polarized(task);
//                }
//            };

//            new TaskRule("(%1 &&+0 %2)", "par:{%1,%2}", nar) {
//                @Override
//                public boolean test(@NotNull Task task) {
//                    return polarized(task);
//                }            };
//            new TaskRule("((%1 &| %2) &| %3)", "par:{%1,%2,%3}", nar) {
//                @Override
//                public boolean test(@NotNull Task task) {
//                    return polarized(task);
//                }            };

//            final Term same = $.the("same");
//            new TaskRule("(%1 <-> %2)", "same:{%1,%2}", nar) {
//                @Override
//                public boolean test(@NotNull Task task) {
//                    return polarized(task) && task.term().containsTermRecursively(same);
//                }
//            };
//        } catch (Narsese.NarseseException e) {
//            e.printStackTrace();
//        }


//        action( new BeliefActionConcept($.inh($.the("fwd"), $.the("fz")), nar, (b) -> {
//            if (b!=null) {
//                float f = b.freq();
//                if (f > 0.75f) {
//                    fz.thrust = true;
//                    return;
//                }
//            }
//            fz.thrust = false;
//        }));
//        action( new BeliefActionConcept($.inh($.the("rot"), $.the("fz")), nar, (b) -> {
//            if (b!=null) {
//                float f = b.freq();
//                if (f > 0.75f) {
//                    fz.left = false; fz.right = true;
//                    return;
//                } else if (f < 0.25f) {
//                    fz.left = true; fz.right = false;
//                    return;
//                }
//            }
//            fz.left = fz.right = false;
//        }));

//        actionBipolar($.inh($.the("rot"), $.the("fz")), (dh) -> {
//           fz.playerAngle += dh * 2f;
//           return true;
//        });
//        actionToggle($.inh($.the("left"), $.the("fz")), (b)->{ fz.left = b; });
//        actionToggle($.inh($.the("right"), $.the("fz")), (b)->{ fz.right = b; });

    }

    private void initToggle() {

        actionToggle($.inh($.the("left"), id), (b) -> {
            if (b && fz.right) {
                fz.left = fz.right = false;
            } else {
                fz.left = b;
            }
        });
        actionToggle($.inh($.the("right"), id), (b) -> {
            if (b && fz.left) {
                fz.left = fz.right = false;
            } else {
                fz.right = b;
            }
        });
        actionToggle($.inh($.the("fwd"), id), (b) -> {
            fz.thrust = b;
        });
        actionToggle($.inh($.the("brake"), id), () -> {
            //fz.left = fz.right = false;
            fz.vehicleMetrics[0][6] *= 0.9f;
        });

    }

    public void initBipolar() {
        GoalActionAsyncConcept[] f = actionBipolar($.the("fwd"), (a) -> {
            //if (f > 0) {
            //accelerator
            //if (f > 0.5f)
            if (a > 0)
                fz.vehicleMetrics[0][6] = /*+=*/ (a) * (fwdSpeed);
            else
                fz.vehicleMetrics[0][6] *= 1 - (-a);
//            else {
//                float brake = 0.5f - f;
//                fz.vehicleMetrics[0][6] *= (1f - brake);
//            }
            return a;
        });
//        //eternal bias to stop
//        nar.goal(f[0].term, Tense.Eternal, 0.5f, 0.01f);
//        nar.goal(f[1].term, Tense.Eternal, 0.5f, 0.01f);

        GoalActionAsyncConcept[] x = actionBipolar($.the("x"), (a) -> {
            fz.playerAngle += (a) * rotSpeed;
            return a;
        });
//        //eternal bias to stop
//        nar.goal(x[0].term, Tense.Eternal, 0.5f, 0.01f);
//        nar.goal(x[1].term, Tense.Eternal, 0.5f, 0.01f);
    }

    protected boolean polarized(@NotNull Task task) {
        if (task.isQuestOrQuestion())
            return true;
        float f = task.freq();
        return f <= 0.2f || f >= 0.8f;
    }

    double lastDistance;

    @Override
    protected float act() {

        double distance = fz.vehicleMetrics[0][1];
        double deltaDistance;
        deltaDistance = (distance - lastDistance) / 35f;
        if (deltaDistance > 1f) deltaDistance = 1f;
        if (deltaDistance < -1f) deltaDistance = -1f;

        lastDistance = distance;

        //lifesupport
        fz.power = Math.max(FZeroGame.FULL_POWER * 0.5f, Math.min(FZeroGame.FULL_POWER, fz.power * 1.15f));

        //System.out.println("head=" + fz.playerAngle%(2*3.14f) + " pow=" + fz.power + " vel=" + fz.vehicleMetrics[0][6] + " deltaDist=" + deltaDistance);


        float ambientSadness = 0f;

        return Util.clamp(
                //-0.5f /* bias */ +
                (float) (-(FZeroGame.FULL_POWER - ((float) fz.power)) / FZeroGame.FULL_POWER +
                        //((float)fz.vehicleMetrics[0][6]/100f)+
                        deltaDistance), -1f, +1f) - ambientSadness;
    }


}
