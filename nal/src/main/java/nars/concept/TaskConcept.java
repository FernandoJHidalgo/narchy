package nars.concept;

import jcog.bag.Bag;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.util.ConceptBuilder;
import nars.control.MetaGoal;
import nars.table.BeliefTable;
import nars.table.QuestionTable;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;

public class TaskConcept extends NodeConcept implements Concept {

    private final BeliefTable beliefs;
    private final BeliefTable goals;
    private final QuestionTable quests;
    private final QuestionTable questions;

    public TaskConcept(Term term, @Nullable BeliefTable beliefs, @Nullable BeliefTable goals, ConceptBuilder conceptBuilder) {
        this(term,
                beliefs != null ? beliefs : conceptBuilder.newTable(term, true),
                goals != null ? goals : conceptBuilder.newTable(term, false),
                conceptBuilder.questionTable(term, true),
                conceptBuilder.questionTable(term, false),
                conceptBuilder.newLinkBags(term));
    }


    public TaskConcept(Term term, ConceptBuilder b) {
        this(term, b.newTable(term, true), b.newTable(term, false),
                b.questionTable(term, true), b.questionTable(term, false),
                b.newLinkBags(term));
    }

    /**
     * Constructor, called in Memory.getConcept only
     *
     * @param term      A term corresponding to the concept
     * @param termLinks
     * @param taskLinks
     */
    public TaskConcept(Term term,
                       BeliefTable beliefs, BeliefTable goals,
                       QuestionTable questions, QuestionTable quests,
                       Bag[] bags) {
        super(term, bags);
        this.beliefs = beliefs;
        this.goals = goals;
        this.questions = questions;
        this.quests = quests;

    }


    @Override
    public QuestionTable quests() {
        return quests;
    }

    @Override
    public QuestionTable questions() {
        return questions;
    }

    /**
     * Judgments directly made about the term Use ArrayList because of access
     * and insertion in the middle
     */
    @Override
    public BeliefTable beliefs() {
        return beliefs;
    }

    /**
     * Desire values on the term, similar to the above one
     */
    @Override
    public BeliefTable goals() {
        return goals;
    }

    protected void beliefCapacity(int be, int bt, int ge, int gt) {

        beliefs.setCapacity(be, bt);
        goals.setCapacity(ge, gt);

    }


    @Override
    protected void stateChanged() {
        super.stateChanged();
        int be = state.beliefCap(this, true, true);
        int bt = state.beliefCap(this, true, false);

        int ge = state.beliefCap(this, false, true);
        int gt = state.beliefCap(this, false, false);

        beliefCapacity(be, bt, ge, gt);

        if (questions != null)
            questions.capacity(state.questionCap(this, true));
        if (quests != null)
            quests.capacity(state.questionCap(this, false));

    }

    /**
     * Directly process a new task, if belief tables agree to store it.
     * Called exactly once on each task.
     */
    public boolean add(Task t, NAR n) {
        return table(t.punc()).add(t, this, n);
    }

    public void value(Task t, float activation, NAR n) {

        byte punc = t.punc();
        if (punc == BELIEF || punc == GOAL) {
            MetaGoal p = punc == BELIEF ? MetaGoal.Believe : MetaGoal.Desire;
            p.learn(t.cause(), Param.beliefValue(t) * activation, n.causes);
        }


        
        
        






















    }


    public void forEachTask(boolean includeConceptBeliefs, boolean includeConceptQuestions, boolean includeConceptGoals, boolean includeConceptQuests, @NotNull Consumer<Task> each) {
        if (includeConceptBeliefs && beliefs != null) beliefs.forEachTask(each);
        if (includeConceptQuestions && questions != null) questions.forEachTask(each);
        if (includeConceptGoals && goals != null) goals.forEachTask(each);
        if (includeConceptQuests && quests != null) quests.forEachTask(each);
    }

    public void forEachTask(Consumer<Task> each) {
        if (beliefs != null) beliefs.forEachTask(each);
        if (questions != null) questions.forEachTask(each);
        if (goals != null) goals.forEachTask(each);
        if (quests != null) quests.forEachTask(each);
    }

    @Override
    public Stream<Task> tasks(boolean includeBeliefs, boolean includeQuestions, boolean includeGoals, boolean includeQuests) {
        int c = 0;
        if (includeBeliefs) c++;
        if (includeGoals) c++;
        if (includeQuestions) c++;
        if (includeQuests) c++;
        assert(c>0);

        Stream[] s = new Stream[c];
        int j = 0;
        if (includeBeliefs) s[j++] = (beliefs().streamTasks());
        if (includeGoals) s[j++] = (goals().streamTasks());
        if (includeQuestions) s[j++] = (questions().streamTasks());
        if (includeQuests) s[j++] = (quests().streamTasks());

        
        return (j == 1 ? s[0] : Stream.of(s).flatMap(x -> x))
                .filter(Objects::nonNull);
    }


    @Override
    public void delete(NAR nar) {
        if (beliefs!=null) beliefs.clear();
        if (goals!=null) goals.clear();
        if (questions!=null) questions.clear();
        if (quests!=null) quests.clear();
        super.delete(nar);
    }

}


































































































































































