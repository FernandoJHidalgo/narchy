/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package nars.concept;

import nars.Global;
import nars.NAR;
import nars.Op;
import nars.Premise;
import nars.bag.BLink;
import nars.nal.nal8.Operator;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Stamp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

import static nars.nal.Tense.*;

/**
 * Firing a concept (reasoning event). Derives new Tasks via reasoning rules
 * <p>
 * Concept
 * Task
 * TermLinks
 */
public final class ConceptProcess implements Premise {


    public final NAR nar;
    public final BLink<? extends Task> taskLink;
    public final BLink<? extends Concept> conceptLink;
    public final BLink<? extends Termed> termLink;

    @Nullable
    private final Task belief;
    private final boolean cyclic;
    @Deprecated
    public long occ;


    public ConceptProcess(NAR nar, BLink<? extends Concept> conceptLink,
                          BLink<? extends Task> taskLink,
                          BLink<? extends Termed> termLink, @Nullable Task belief) {
        this.nar = nar;

        this.taskLink = taskLink;
        this.conceptLink = conceptLink;
        this.termLink = termLink;

        this.belief = belief;
        this.cyclic = Stamp.overlapping(task(), belief);
    }

    public static int fireAll(@NotNull NAR nar, BLink<? extends Concept> concept, @NotNull BLink<? extends Task> taskLink, @NotNull BLink<? extends Termed> termLink, @NotNull Consumer<ConceptProcess> cp) {

        Task task = taskLink.get();
        Term tel = termLink.get().term();

        int n = 0;
        if ((tel != null) && /*&& (task.term().hasVarQuery()))*/
                (task.term().hasAny(Op.VAR_QUERY)) /*|| tel.hasAny(Op.VAR_QUERY))*/) {
            n += Premise.unify(Op.VAR_QUERY, task.term(), tel, nar.memory, (u) -> {

                Task belief = fireAll(nar, concept, taskLink, termLink, u, cp);

                //Answers questions containing query variable that have been matched
                if (belief != null && task.isQuestOrQuestion() && !belief.isQuestOrQuestion())
                    nar.memory.onSolve(task, belief);
            });
        }


        if (n == 0) {
            fireAll(nar, concept, taskLink, termLink, tel, cp);
            n++;
        }


        return n; //HACK
    }

    /**
     * returns the corresponding belief task
     */
    public static Task fireAll(@NotNull NAR nar, BLink<? extends Concept> concept, @NotNull BLink<? extends Task> taskLink, @NotNull BLink<? extends Termed> termLink, Term beliefTerm, @NotNull Consumer<ConceptProcess> cp) {
        Concept beliefConcept = nar.concept(beliefTerm);

        Task belief = null;
        if ((beliefConcept != null) && (beliefConcept.hasBeliefs())) {

            //long taskTime = taskLink.get().occurrence();

            long now = nar.time();


            belief = beliefConcept.beliefs().top(
                    now, //taskTime,
                    now);

            if (belief == null || belief.isDeleted()) {
                throw new RuntimeException("deleted belief: " + belief + " " + beliefConcept.hasBeliefs());
            }

        }

        cp.accept(new ConceptProcess(nar, concept,
                taskLink, termLink, belief));

        return belief;
    }

    @Override
    public final Task task() {
        return taskLink.get();
    }

//    /**
//     * @return the current termLink aka BeliefLink
//     */
//    @Override
//    public final BagBudget<Termed> getTermLink() {
//        return termLink;
//    }

    public Concept concept() {
        return conceptLink.get();
    }


//    protected void beforeFinish(final long now) {
//
//        Memory m = nar.memory();
//        m.logic.TASKLINK_FIRE.hit();
//        m.emotion.busy(getTask(), this);
//
//    }

//    @Override
//    final protected Collection<Task> afterDerive(Collection<Task> c) {
//
//        final long now = nar.time();
//
//        beforeFinish(now);
//
//        return c;
//    }

    public final Termed beliefTerm() {
        Task x = belief();
        return x == null ? termLink.get() :
                x.term();
    }

    @Nullable
    @Override
    public final Task belief() {
        return belief;
    }

    @Override
    public final boolean isCyclic() {
        return cyclic;
    }

    @Override
    public String toString() {
        return new StringBuilder().append(
                getClass().getSimpleName())
                .append('[').append(conceptLink).append(',')
                .append(taskLink).append(',')
                .append(termLink).append(',')
                .append(belief())
                .append(']')
                .toString();
    }

    @Override
    public final NAR nar() {
        return nar;
    }

    public int getMaxMatches() {
        final float min = Global.MIN_TERMUTATIONS_PER_MATCH, max = Global.MAX_TERMUTATIONS_PER_MATCH;
        return (int) Math.ceil(task().pri() * (max - min) + min);
    }

    /**
     * apply temporal characteristics to a newly derived term according to the premise's
     */
    public final Compound temporalize(Compound derived, Term taskPattern, Term beliefPattern, Term conclusionPattern) {

        occ = occurrenceTarget(); //reset

        Term cc = conclusionPattern;
        if (Op.isOperation(cc)) {
            //unwrap operation, work with its first argument
            cc = Operator.opArgsArray((Compound) cc)[0];
        }

        Compound tt = task().term();
        Compound bb = belief() != null ? belief().term() : null;

        int td = tt.t();
        int bd = bb != null ? bb.t() : ITERNAL;

        if (derived.op().isTemporal() && cc.isCompound()) {


            //System.out.println(tt + " "  + bb);

            /* CASES:
                conclusion pattern size 1
                    equal to task subterm
                    equal to belief subterm
                    unique term
                conclusion pattern size 2 (a, b)
                    a equal to task subterm
                    b equal to task subterm
                    a equal to belief subterm
                    b equal to belief subterm

             */
            int s = cc.size();
            if (s == 2) {
                Compound ccc = (Compound) cc;
                Term ca = ccc.term(0);
                Term cb = ccc.term(1);

                int t = ITERNAL;
                if ((taskPattern.size() == 2) && (beliefPattern.size() == 2)) {
                    Compound tpp = (Compound) taskPattern;
                    Compound bpp = (Compound) beliefPattern;
                    if (td!=ITERNAL && bd!=ITERNAL) {
                        if (tpp.term(1).equals(bpp.term(0))) {
                            t = td + bd;

                            //chained inner
                            if (!cb.equals(bpp.term(1))) {
                                t = -t; //invert direction
                            }
                        } else if (tpp.term(0).equals(bpp.term(1))) {
                            //chain outer
                            t = td + bd; //?? CHECK
                        } else if (tpp.term(0).equals(bpp.term(0))) {
                            //common left
                            t = td - bd;
                        } else if (tpp.term(1).equals(bpp.term(1))) {
                            //common right
                            t = bd - td;
                        } else {
                            throw new RuntimeException("unhandled case");
                        }
                    } else if (td == ITERNAL && bd == ITERNAL && belief!=null) {
                        long to = task().occurrence();
                        long bo = belief().occurrence();
                        t = (int)(to - bo);
                        occ -= t;
                    } else {
                        //throw new RuntimeException("unhandled case");
                    }
                } else if ((taskPattern.size() == 0) && (beliefPattern.size() == 0) && belief()!=null) {
                    long aTask = taskPattern.subtermTime(ca, td);
                    long aBelief = beliefPattern.subtermTime(ca, bd);
                    long bTask = taskPattern.subtermTime(cb, td);
                    long bBelief = beliefPattern.subtermTime(cb, bd);

                    if (aTask != ETERNAL && aBelief == ETERNAL &&
                            bBelief != ETERNAL && bTask == ETERNAL) {
                        //forward: task -> belief
                        t = (int) (belief().occurrence() - task().occurrence());
                        //occ += 0;

                    }
                    else if (aTask == ETERNAL && aBelief != ETERNAL &&
                            bBelief == ETERNAL && bTask != ETERNAL) {
                        //reverse: belief -> task
                        t = (int) (task().occurrence() - belief().occurrence());
                        //occ += 0;

                    } else {
                        throw new RuntimeException("unhandled case");

                    }

                }

                //System.out.println(derived + " " + a + ":"+ aTask + "|" + aBelief + ", " + b + ":" + bTask + "|" + bBelief);

                if (t != ITERNAL) {
                    return derived.t(t);
                }

            } else {
                //throw new RuntimeException("unhandled case");
            }
        } else {
            //if (cc.size() == 0) {

            long ot = taskPattern.subtermTime(cc, td);
            long ob = beliefPattern.subtermTime(cc, bd);

            if (occ > TIMELESS ) {
                if (ot != ETERNAL) {
                    if (taskPattern.isCompound()) {
                        Compound ctp = (Compound)taskPattern;
                        if (ctp.term(0).equals(cc)) {
                            ot-=td;
                        }
                    }
                    occ = occ + ot; //occ + ot;
                } else if (ob != ETERNAL) {

                    if (belief().occurrence()!=task().occurrence()) { //why?
                        if (beliefPattern.isCompound()) {
                            Compound cbp = (Compound) beliefPattern;
                            if (!cbp.term(1).equals(cc)) {
                                ob -= bd;
                            }
                        }
                    }

                    occ = occ + ob;

                } else {
                    //neither, remain eternal
                }
            }
            //}
        }

        return derived;
        //return nar.memory.index.transformRoot(derived, temporalize);
    }


//    /** max(tasktime, belieftime) */
//    public long getMaxOccurrenceTime() {
//        long occ= getTask().getOccurrenceTime();
//        Task b = getBelief();
//        if (b!=null) {
//            occ = Math.max(occ, b.getOccurrenceTime());
//        }
//        return occ;
//    }


    //    /** supplies at most 1 premise containing the pair of next tasklink and termlink into a premise */
//    public static Stream<Task> nextPremise(NAR nar, final Concept concept, float taskLinkForgetDurations, Function<ConceptProcess,Stream<Task>> proc) {
//
//        TaskLink taskLink = concept.getTaskLinks().forgetNext(taskLinkForgetDurations, nar.memory());
//        if (taskLink == null) return Stream.empty();
//
//        TermLink termLink = concept.getTermLinks().forgetNext(nar.memory().termLinkForgetDurations, nar.memory());
//        if (termLink == null) return Stream.empty();
//
//
//        return proc.apply(premise(nar, concept, taskLink, termLink));
//
//    }

//    public static ConceptProcess premise(NAR nar, Concept concept, TaskLink taskLink, TermLink termLink) {
////        if (Terms.equalSubTermsInRespectToImageAndProduct(taskLink.getTerm(), termLink.getTerm()))
////            return null;
//
////        if (taskLink.isDeleted())
////            throw new RuntimeException("tasklink null"); //bag should not have returned this
//
//    }


//    public abstract Stream<Task> derive(final Deriver p);

//    public static void forEachPremise(NAR nar, @Nullable final Concept concept, @Nullable TaskLink taskLink, int termLinks, float taskLinkForgetDurations, Consumer<ConceptProcess> proc) {
//        if (concept == null) return;
//
//        concept.updateLinks();
//
//        if (taskLink == null) {
//            taskLink = concept.getTaskLinks().forgetNext(taskLinkForgetDurations, concept.getMemory());
//            if (taskLink == null)
//                return;
//        }
//
//
//
//
//        proc.accept( new ConceptTaskLinkProcess(nar, concept, taskLink) );
//
//        if ((termLinks > 0) && (taskLink.type!=TermLink.TRANSFORM))
//            ConceptProcess.forEachPremise(nar, concept, taskLink,
//                    termLinks,
//                    proc
//            );
//    }

//    /** generates a set of termlink processes by sampling
//     * from a concept's TermLink bag
//     * @return how many processes generated
//     * */
//    public static int forEachPremise(NAR nar, Concept concept, TaskLink t, final int termlinksToReason, Consumer<ConceptProcess> proc) {
//
//        int numTermLinks = concept.getTermLinks().size();
//        if (numTermLinks == 0)
//            return 0;
//
//        TermLink[] termlinks = new TermLink[termlinksToReason];
//
//        //int remainingProcesses = Math.min(termlinksToReason, numTermLinks);
//
//        //while (remainingProcesses > 0) {
//
//            Arrays.fill(termlinks, null);
//
//            concept.getPremiseGenerator().nextTermLinks(concept, t, termlinks);
//
//            int created = 0;
//            for (TermLink tl : termlinks) {
//                if (tl == null) break;
//
//                proc.accept(
//                    new ConceptTaskTermLinkProcess(nar, concept, t, tl)
//                );
//                created++;
//            }
//
//
//          //  remainingProcesses--;
//
//
//        //}
//
//        /*if (remainingProcesses == 0) {
//            System.err.println(now + ": " + currentConcept + ": " + remainingProcesses + "/" + termLinksToFire + " firings over " + numTermLinks + " termlinks" + " " + currentTaskLink.getRecords() + " for TermLinks "
//                    //+ currentConcept.getTermLinks().values()
//            );
//            //currentConcept.taskLinks.printAll(System.out);
//        }*/
//
//        return created;
//
//    }

//    /** override-able filter for derivations which can be applied
//     * once the term and the truth value are known */
//    public boolean validJudgment(Term derivedTerm, Truth truth) {
//        return true;
//    }
//
//    /** override-able filter for derivations which can be applied
//     * once the term and the truth value are known */
//    public boolean validGoal(Term derivedTerm, Truth truth) {
//        return true;
//    }

}
