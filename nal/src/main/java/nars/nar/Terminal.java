package nars.nar;

import nars.NAR;
import nars.budget.Budgeted;
import nars.concept.Concept;
import nars.index.Indexes;
import nars.term.Termed;
import nars.time.Clock;
import nars.time.FrameClock;
import nars.util.data.random.XORShiftRandom;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.Consumer;

/**
 * Terminal only executes commands and does not
 * reason.  however it will produce an event
 * stream which can be delegated to other
 * components like other NAR's
 */
public class Terminal extends AbstractNAR {

    //final Predicate<Task> taskFilter = Task::isCommand;

    public Terminal() {
        this(1024);
    }

    public Terminal(int capacity) {
        this(capacity, new XORShiftRandom(1), new FrameClock());
    }
    public Terminal(int capacity, @NotNull Random random, @NotNull Clock c) {
        super(c, new Indexes.DefaultTermIndex(capacity, random), random);
    }


    @Override
    public Concept activate(@NotNull Termed termed, @NotNull Budgeted activation, float conceptActivation, float linkActivation, MutableFloat overflow) {
        return concept(termed, true); //ignore activation
    }

    @Override
    public void clear() {
        //nothing to clear, terminal is always clear
    }

    @Override
    public float conceptPriority(@NotNull Termed termed) {
        return 0;
    }

    //    @Override
//    protected Concept doConceptualize(Term term, Budget b, float scale) {
//        Concept exists = memory.concept(term);
//        if (exists!=null) {
//            return exists;
//        }
//        else {
//            Concept c = apply(term);
//            memory.index.put(term, c);
//            return c;
//        }
//
//    }





    @Nullable
    @Override
    public NAR forEachActiveConcept(@NotNull Consumer<Concept> recip) {
        return null;
    }


}