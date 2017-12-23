package nars;

import nars.control.DurService;
import nars.task.ActiveQuestionTask;
import nars.task.ITask;
import nars.task.signal.Truthlet;
import nars.task.signal.TruthletTask;
import nars.term.Term;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.LongFunction;
import java.util.function.Predicate;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;

/**
 * NAR Input methods
 */
public interface NARIn {

    void input(ITask... t);

    @Nullable
    default Task question(@NotNull String questionTerm, long occ, @NotNull BiConsumer<ActiveQuestionTask,Task> eachAnswer) throws Narsese.NarseseException {
        return question($.$(questionTerm), occ, eachAnswer);
    }

    @Nullable
    default Task question(@NotNull Term term, long occ, @NotNull BiConsumer<ActiveQuestionTask,Task> eachAnswer) {
        return question(term, occ, Op.QUESTION, eachAnswer);
    }

    @Nullable
    default ActiveQuestionTask question(@NotNull Term term, long occ, byte punc /* question or quest */, @NotNull BiConsumer<ActiveQuestionTask, Task> eachAnswer) {
        assert(punc == Op.QUESTION || punc == Op.QUEST);
        return inputTask( new ActiveQuestionTask(term, punc, occ, 16, (NAR)this, eachAnswer) );
    }

    @Nullable
    default ActiveQuestionTask ask(@NotNull Term term, long occ, byte punc /* question or quest */, @NotNull Consumer<Task> eachAnswer) {
        assert(punc == Op.QUESTION || punc == Op.QUEST);
        return inputTask( new ActiveQuestionTask(term, punc, occ, 16, (NAR)this, eachAnswer) );
    }




    /** parses one and only task */
    @NotNull default <T extends Task> T inputTask(T t) {
        input(t);
        return t;
    }


    default DurService believeWhile(Term term, Truth t, Predicate<Task> cond) {
        return taskWhile(term, BELIEF, t, cond);
    }

    default DurService wantWhile(Term term, Truth t, Predicate<Task> cond) {
        return taskWhile(term, GOAL, t, cond);
    }

    default DurService taskWhile(Term term, byte punc, Truth tru, Predicate<Task> cond) {
        NAR n = (NAR)this; //HACK

        long start = n.time();
        float activeFreq = tru.freq();


        float inactiveFreq = 0f;
        float evi = tru.evi();
        LongFunction<Truthlet> stepUntil = (toWhen) -> {
            return Truthlet.step(inactiveFreq, start, activeFreq, toWhen, activeFreq, evi);
        };

        TruthletTask t = new TruthletTask(term, punc, stepUntil.apply(start), n);
        float pri = n.priDefault(punc);
        t.priMax(pri);

        n.input(t);

        return DurService.onWhile(n, (nn)->{

//            nn.runLater(()->{
//                t.concept(nn, false).goals().print();
//                System.out.println();
//            });

            long now = nn.time();
            boolean kontinue;
            Truthlet tt;
            if (!cond.test(t)) {
                //convert from step function to impulse function which
                // stops at the current time and end the service
                tt = Truthlet.impulse(start, now, activeFreq, inactiveFreq, evi);
                kontinue = false;
            } else {
                //stretch the step function to current time
                tt = stepUntil.apply(now);
                kontinue = true;
            }
            t.priMax(pri);
            t.truth(tt, true, nn);
            return kontinue;
        });
    }
}
