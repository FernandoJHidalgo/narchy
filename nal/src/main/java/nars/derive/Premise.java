/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package nars.derive;

import jcog.Util;
import jcog.pri.Pri;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.concept.Concept;
import nars.table.BeliefTable;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.subst.UnifySubst;
import org.jetbrains.annotations.Nullable;

import java.util.function.ToLongFunction;

import static nars.Op.BELIEF;
import static nars.time.Tense.ETERNAL;

/**
 * Defines the conditions used in an instance of a derivation
 * Contains the information necessary for generating derivation Tasks via reasoning rules.
 * <p>
 * It is meant to be disposable and should not be kept referenced longer than necessary
 * to avoid GC loops, so it may need to be weakly referenced.
 */
public class Premise extends Pri {

    public final Task task;
    public final Term term;
    private final int hash;



    Premise(Task task, Term term, float taskLinkPri) {
        super(taskLinkPri);
        this.task = task;
        this.term = term;
        this.hash = Util.hashCombine(task.hashCode(), term.hashCode());
    }

    final static int var = Op.VAR_QUERY.bit | Op.VAR_DEP.bit | Op.VAR_INDEP.bit;

    /**
     * resolve the most relevant belief of a given term/concept
     * <p>
     * patham9 project-eternalize
     * patham9 depending on 4 cases
     * patham9 https://github.com/opennars/opennars2/blob/a143162a559e55c456381a95530d00fee57037c4/src/nal/deriver/projection_eternalization.clj
     * sseehh__ ok ill add that in a bit
     * patham9 you need  project-eternalize-to
     * sseehh__ btw i disabled immediate eternalization entirely
     * patham9 so https://github.com/opennars/opennars2/blob/a143162a559e55c456381a95530d00fee57037c4/src/nal/deriver/projection_eternalization.clj#L31
     * patham9 especially try to understand the "temporal temporal" case
     * patham9 its using the result of higher confidence
     * <p>
     * returns ttl used, -1 if failed before starting
     *
     * @param matchTime - temporal focus control: determines when a matching belief or answer should be projected to
     */
    @Nullable
    public Derivation match(Derivation d, ToLongFunction<Task> matchTime, int matchTTL) {

        NAR n = d.nar;

        //nar.emotion.count("Premise_run");


        if (task == null || task.isDeleted()) {
//            Task fwd = task.meta("@");
//            if (fwd!=null)
//                task = fwd; //TODO multihop dereference like what happens in tasklink bag
//            else {
//                delete();
//                return;
//            }
            return null;
        }


        //n.conceptualize(task.term(), (c)->{});


        int dur = d.dur;

        Term beliefTerm = term;


        Term taskTerm = task.term();

        final boolean[] beliefConceptCanAnswerTaskConcept = {false};
        boolean unifiedBelief = false;

        Op to = taskTerm.op();
        Op bo = beliefTerm.op();
        if (to == bo) {
            if (taskTerm.equalsRoot(beliefTerm)) {
                beliefConceptCanAnswerTaskConcept[0] = true;
            } else {

                //non-symmetric unify only variables in the task by belief contents
                if ((!beliefTerm.op().conceptualizable) && (taskTerm.hasAny(var))) {

                    Term _beliefTerm = beliefTerm;
                    final Term[] unifiedBeliefTerm = new Term[]{null};
                    UnifySubst u = new UnifySubst(null, n, (y) -> {
                        if (y.op().conceptualizable && !y.hasAny(Op.BOOL)) {
                            y = y.normalize();

                            beliefConceptCanAnswerTaskConcept[0] = true;

                            if (!y.equals(_beliefTerm)) {
                                unifiedBeliefTerm[0] = y;
                                return false; //stop
                            }
                        }
                        return true; //keep going
                    }, matchTTL);
                    u.varSymmetric = false;
                    u.varCommonalize = true;
                    if (u.unify(taskTerm, beliefTerm, true)) {
                        if (unifiedBeliefTerm[0] != null) {
                            beliefTerm = unifiedBeliefTerm[0];
                            unifiedBelief = true;
                        }
                    }
                }
            }
        }
        beliefTerm = beliefTerm.unneg(); //HACK ?? assert(beliefTerm.op()!=NEG);

        //QUESTION ANSWERING and TERMLINK -> TEMPORALIZED BELIEF TERM projection
        Task belief = null;

        final Concept beliefConcept = beliefTerm.op().conceptualizable ? n.concept(beliefTerm) : null;
        if (beliefConcept != null) {

            if (!beliefTerm.hasVarQuery()) { //doesnt make sense to look for a belief in a term with query var, it will have none

                if (task.isQuestOrQuestion()) {
                    if (beliefConceptCanAnswerTaskConcept[0]) {
                        final BeliefTable answerTable =
                                (task.isGoal() || task.isQuest()) ?
                                        beliefConcept.goals() :
                                        beliefConcept.beliefs();

//                            //see if belief unifies with task (in reverse of previous unify)
//                            if (questionTerm.varQuery() == 0 || (unify((Compound)beliefConcept.term(), questionTerm, nar) == null)) {
//
//                            } else {
//
//                            }

                        Task match = answerTable.answer(task.start(), task.end(), dur, task, beliefTerm, n, d::add);
                        if (match != null) {
                            assert (task.isQuest() || match.punc() == BELIEF) : "quest answered with a belief but should be a goal";

                            @Nullable Task answered = task.onAnswered(match, n);
                            if (answered != null) {
                                n.emotion.onAnswer(task, answered);
                            }

                            if (match.isBelief()) {
                                belief = match;
                            }

                        }
                    }
                }

                if (belief == null) {
                    long focus = matchTime.applyAsLong(task);
                    long focusStart, focusEnd;
                    if (focus == ETERNAL) {
                        focusStart = focusEnd = ETERNAL;
                    } else {
                        focusStart =
                                focus - dur / 2;
                        //focus - dur;
                        //focus;
                        focusEnd =
                                focus + dur / 2;
                        //focus + dur;
                        //focus;
                    }

                    belief = beliefConcept.beliefs().match(focusStart, focusEnd, beliefTerm, n,
                            beliefConcept.term().equals(task.term().concept()) ? (x) -> {
                                return !x.equals(task);
                            } : null);
                }
            }


            if (unifiedBelief) {
                Concept originalBeliefConcept = n.concept(term);
                if (originalBeliefConcept != null)
                    linkVariable(originalBeliefConcept, beliefConcept);
            }

        }


        if (belief != null) {
            beliefTerm = belief.term().unneg(); //use the belief's actual possibly-temporalized term

//            if (belief.equals(task)) { //do not repeat the same task for belief
//                belief = null; //force structural transform; also prevents potential inductive feedback loop
//            }
        }

        if (beliefTerm instanceof Bool) {
            //logger.warn("{} produced Bool beliefTerm", this);
            return null;
        }


        //assert (!(beliefTerm instanceof Bool)): "beliefTerm boolean; termLink=" + termLink + ", belief=" + belief;

        d.reset().proto(task, belief, beliefTerm);
        return d;
    }


    @Override
    public boolean equals(Object obj) {
        return hash == obj.hashCode() && ((Premise)obj).task.equals(task) && ((Premise)obj).term.equals(term);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    /**
     * x has variables, y unifies with x and has less or no variables
     */
    private void linkVariable(Concept lessConstant, Concept moreConstant) {


//        /** creates a tasklink/termlink proportional to the tasklink's priority
//         *  and inversely proportional to the increase in term complexity of the
//         *  unified variable.  ie. $x -> (y)  would get a stronger link than  $x -> (y,z)
//         */
//        PriReference taskLink = this;
//        Term moreConstantTerm = moreConstant.term();
//        Term lessConstantTerm = lessConstant.term();
//        float pri = taskLink.priElseZero()
//                * (1f/lessConstantTerm.volume());
//                //* Util.unitize(lessConstantTerm.complexity() / ((float) moreConstantTerm.complexity()));
//
//        moreConstant.termlinks().putAsync(new PLink<>(lessConstantTerm, pri));
//        lessConstant.termlinks().putAsync(new PLink<>(moreConstantTerm, pri));
//        //moreConstant.termlinks().putAsync(new PLink<>(taskConcept.term(), pri));
//        //taskConcept.termlinks().putAsync(new PLink<>(moreConstantTerm, pri));
//
//
//        //Tasklinks.linkTask(this.task.get(), pri, moreConstant);

    }

    @Override
    public String toString() {
        return "Premise(" +
                task +
                " * " + term +
                ')';
    }


//    public void merge(Premise incoming) {
//        //WARNING this isnt thread safe but collisions should be rare
//
//        Collection<Concept> target = this.links;
//        Collection<Concept> add = incoming.links;
//
//        if (target == add || add == null)
//            return; //same or no change
//
//        if (target == null || target.isEmpty()) {
//            this.links = add;
//            return; //just replace it
//        }
//
//        if (!(target instanceof Set)) {
//            Set<Concept> merge =
//                    new HashSet(target.size() + add.size());
//                    //Collections.newSetFromMap(new ConcurrentHashMap<>(target.size() + add.size()));
//            merge.addAll(target);
//            merge.addAll(add);
//            this.links = merge;
//        } else {
//            target.addAll(add);
//        }
//    }

}
