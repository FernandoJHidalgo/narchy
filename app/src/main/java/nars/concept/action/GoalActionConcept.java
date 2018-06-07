package nars.concept.action;

import jcog.math.FloatRange;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.dynamic.ScalarBeliefTable;
import nars.task.ITask;
import nars.task.signal.SignalTask;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.stream.Stream;

import static nars.Op.GOAL;
import static nars.truth.TruthFunctions.c2w;


/**
 * ActionConcept which is driven by Goals that are interpreted into feedback Beliefs
 */
public class GoalActionConcept extends ActionConcept {

    
    private final ScalarBeliefTable feedback;

    private final FloatRange curiosity;

    private final MotorFunction motor;



    public GoalActionConcept(@NotNull Term c, @NotNull NAR n, @NotNull FloatRange curiosity, @NotNull MotorFunction motor) {
        super(c, n);

        this.curiosity = curiosity;


        

        this.feedback = (ScalarBeliefTable)beliefs();
        
        


        this.motor = motor;
        


    }



    @Override
    public Stream<ITask> update(long pPrev, long pNow, int dur, NAR nar) {











        float cur = curiosity.floatValue();


        Truth goal;

        long gStart =
                
                pNow - dur/2;
        long gEnd =
                
                pNow + dur/2;

        goal = this.goals().truth(gStart, gEnd, nar);




        boolean curi;
        if (nar.random().nextFloat() < cur * (1f - (goal!=null ? goal.conf() : 0))) {


            float curiConf =

                    
                    Math.min(Param.TRUTH_MAX_CONF, nar.confMin.floatValue()

                            * 8
                    );




                    

            curi = true;
            



            










            goal = Truth.theDithered(nar.random().nextFloat(), c2w(curiConf), nar);
            













        } else {
            curi = false;

            


            
            

            

            
            
            
            
            

            


        }


        Truth belief = this.beliefs().truth(gStart, gEnd, nar);

        Truth feedback = this.motor.apply(belief, goal);

        Task feedbackBelief = feedback!=null ?
                this.feedback.add(feedback, gStart,gEnd, dur, nar) : null;

        Task curiosityGoal = null;
        if (curi && feedbackBelief!=null) {
            curiosityGoal = curiosity(nar,
                    goal,
                    
                    term, gStart, gEnd, nar.time.nextStamp());
        }

        this.feedback.clean(nar);

        return Stream.of(feedbackBelief, (ITask)curiosityGoal).filter(Objects::nonNull);
        
        
    }


    static SignalTask curiosity(NAR nar, Truth goal, Term term, long pStart, long pEnd, long curiosityStamp) {
        long now = nar.time();

        SignalTask curiosity = new SignalTask(term, GOAL, goal, now, pStart, pEnd, curiosityStamp);
        
        curiosity.pri(nar.priDefault(GOAL));
        

        return curiosity;
    }


    
































































































































































































    













































































}
