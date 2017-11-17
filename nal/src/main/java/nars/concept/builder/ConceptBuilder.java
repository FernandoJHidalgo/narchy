package nars.concept.builder;

import jcog.bag.Bag;
import nars.NAR;
import nars.concept.state.ConceptState;
import nars.table.BeliefTable;
import nars.table.QuestionTable;
import nars.table.TemporalBeliefTable;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

/**
 * Created by me on 3/23/16.
 */
public interface ConceptBuilder extends BiFunction<Term, Termed, Termed> {

    @NotNull ConceptState init();
    @NotNull ConceptState awake();
    @NotNull ConceptState sleep();

    QuestionTable questionTable(Term term, boolean questionOrQuest);
    BeliefTable beliefTable(Term t, boolean beliefOrGoal);
    TemporalBeliefTable newTemporalBeliefTable(Term c);

    void start(NAR nar);


    /** passes through terms without creating any concept anything */
    ConceptBuilder Null = new ConceptBuilder() {

        @Override
        public Termed apply(Term term, Termed termed) {
            return term;
        }


        @Override
        public @NotNull ConceptState init() {
            return ConceptState.Abstract;
        }

        @Override
        public @NotNull ConceptState awake() {
            return ConceptState.Abstract;
        }

        @Override
        public @NotNull ConceptState sleep() {
            return ConceptState.Abstract;
        }

        @Override
        public void start(NAR nar) {

        }

        @Override
        public TemporalBeliefTable newTemporalBeliefTable(Term c) {
            return TemporalBeliefTable.Empty;
        }

        @Override
        public BeliefTable beliefTable(Term t, boolean beliefOrGoal) {
            return BeliefTable.Empty;
        }

        @Override
        public QuestionTable questionTable(Term term, boolean questionOrQuest) {
            return QuestionTable.Empty;
        }

        @Override
        public Bag[] newLinkBags(@NotNull Term term) {
            return new Bag[] { Bag.EMPTY, Bag.EMPTY };
        }
    };


    Bag[] newLinkBags(@NotNull Term term);
}
