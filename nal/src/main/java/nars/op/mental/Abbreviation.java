package nars.op.mental;


import jcog.math.MutableIntRange;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.bag.leak.TaskLeak;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.task.NALTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

import static nars.Op.BELIEF;
import static nars.term.Terms.compoundOrNull;
import static nars.time.Tense.ETERNAL;

/**
 * compound<->dynamic atom abbreviation.
 *
 * @param S serial term type
 */
public class Abbreviation/*<S extends Term>*/ {


    /**
     * generated abbreviation belief's confidence
     */
    public final MutableFloat abbreviationConfidence;
    private final TaskLeak bag;

    /**
     * whether to use a (strong, proxying) alias atom concept
     */


    private static final Logger logger = LoggerFactory.getLogger(Abbreviation.class);

    private static final AtomicInteger currentTermSerial = new AtomicInteger(0);

    private final String termPrefix;

    /**
     * accepted volume range, inclusive
     */
    public final MutableIntRange volume;


    public Abbreviation(NAR nar, String termPrefix, int volMin, int volMax, float selectionRate, int capacity) {
        super();
        bag = new TaskLeak(capacity, selectionRate, nar)//new PLinkArrayBag<Compound>(PriMerge.plus, capacity)
       {//     @Nullable
//            @Override
//            public Compound key(PriReference<Compound> l) {
//                return l.get();
//            } }
        //, new MutableFloat(selectionRate)) {
            @Override
            public float value() {
                return 1f; //HACK TODO use Cause
            }

            @Override
            protected float leak(Task b) {
                return input(b, nar) && abbreviate(b, nar) ? 1f : 0f;
            }

            //            @Override
//            protected Random random() {
//                return nar.random();
//            }
//
//            @Override
//            protected float receive(PriReference<Compound> b) {
//                return abbreviate(b.get(), b, nar) ? 1f : 0f;
//            }
        };
        //bag.setCapacity(capacity);

        this.termPrefix = termPrefix;
        this.abbreviationConfidence =
                new MutableFloat(nar.confDefault(BELIEF));
        //new MutableFloat(1f - nar.truthResolution.floatValue());
        //new MutableFloat(nar.confidenceDefault(Symbols.BELIEF));
        volume = new MutableIntRange(volMin, volMax);
    }

//    @Override
//    protected void starting(NAR nar) {
//        onDur = DurService.on(nar, this::update);
//    }




//    protected void update(NAR nar) {
//        bag.commit(nar, 1f);
//    }



//    @Override
//    public void clear() {
//        bag.clear();
//    }

//    @Override
//    public void accept(NAR nar, Task task) {
//
//        Term taskTerm = task.term();
//        if ((!(taskTerm instanceof Compound)) || taskTerm.vars() > 0)
//            return;
//
//        Prioritized b = task;
//
//        input(b, bag.bag::put, (Compound) taskTerm, 1f, nar);
//    }

    private boolean input(Task t, NAR nar) {
        int vol = t.volume();
        if (vol < volume.lo())
            return false;

        if (vol <= volume.hi()) {
            if (t.concept(nar,true).equals(t) /* identical to its conceptualize */) {
                Concept abbreviable = nar.concept(t);
                if ((abbreviable != null) &&
                        !(abbreviable instanceof PermanentConcept) &&
                                abbreviable.meta("abbr") == null) {

                    //each.accept(new PLink<>(t, b.priElseZero()));
                    return true;
                }
            }
        } else {
//            //recursiely try subterms of a temporal or exceedingly large concept
//            //budget with a proportion of this compound relative to their volume contribution
//            float subScale = 1f / (1 + t.subs());
//            t.forEach(x -> {
//                if (x.subs() > 0)
//                    input(b, each, ((Compound) x), subScale, nar);
//            });
        }
        return false;
    }


    @NotNull
    protected String nextSerialTerm() {

        return termPrefix + Integer.toString(currentTermSerial.incrementAndGet(), 36);

//        int olen = name.length();
//        switch (olen) {
//            case 0:
//                throw new RuntimeException("empty atom name: " + name);
//
////            //re-use short term names
////            case 1:
////            case 2:
////                return theCached(name);
//
//            default:
//                if (olen > Short.MAX_VALUE/2)
//                    throw new RuntimeException("atom name too long");

        //  }
    }

//    private float scoreIfExceeds(Budget task, float min) {
//        float s = or(task.priIfFiniteElseZero(), task.qua());
//        if (s >= min) {
//            s *= abbreviationProbability.floatValue();
//            if (s >= min) {
//
//                //higher probability for terms nearer the thresh. smaller and larger get less chance
////                s *= 1f - unitize(
////                        Math.abs(task.volume() - volThresh) /
////                                (threshFalloffRate) );
//
//                if (s >= min)
//                    return s;
//            }
//        }
//        return -1;
//    }




    protected boolean abbreviate(Task t, NAR nar) {

        Term abbreviated = t.term();
        Concept abbrConcept = t.concept(nar, false);

        if (abbrConcept != null && !(abbrConcept instanceof AliasConcept) && !(abbrConcept instanceof PermanentConcept)) {

            //final boolean[] succ = {false};

            //abbrConcept.meta("abbr", (ac) -> {

//                Term abbreviatedTerm =abbreviated.term();

            Term aliasTerm = Atomic.the(nextSerialTerm());
                AliasConcept a1 = new AliasConcept(aliasTerm, abbrConcept, nar);
                nar.on(a1);
//                nar.concepts.set(abbreviated.term(), a1); //set the abbreviated term to resolve to the abbreviation
//                if (!abbreviatedTerm.equals(abbreviated.term()))
//                    nar.concepts.set(abbreviatedTerm, a1); //set the abbreviated term to resolve to the abbreviation


            Compound abbreviation = newRelation(abbreviated, aliasTerm);
                if (abbreviation == null)
                    return false; //maybe could happen

                Task abbreviationTask = Task.tryTask(abbreviation, BELIEF,
                        $.t(1f, abbreviationConfidence.floatValue()),
                        (te, tr) -> {

                            NALTask ta = new NALTask(
                                    te, BELIEF, tr,
                                    nar.time(), ETERNAL, ETERNAL,
                                    nar.evidence()
                            );
//
//
                            //ta.meta(Abbreviation.class, new Term[]{abbreviatedTerm, aliasTerm.term()});
                            ta.log("Abbreviate"); //, abbreviatedTerm, aliasTerm
                            ta.pri(t.priElseZero()); //same as input task
//
                            return ta;
                        });
//

//
//                            return ta;
//
//                            //if (abbreviation != null) {
//
//                            //logger.info("{} <=== {}", alias, abbreviatedTerm);
//
//                        });


                //abbreviationTask.priority();
//        if (srcCopy == null) {
//            delete();
//        } else {
//            float p = srcCopy.priSafe(-1);
//            if (p < 0) {
//                delete();
//            } else {
//                setPriority(p);
//            }
//        }
//
//        return this;


//                succ[0] = true;
//                return a1;
//
//            });


//            return succ[0];

            if (abbreviationTask!=null) {
                nar.input(abbreviationTask);
                logger.info("{}", abbreviationTask.term());

                return true;
            }

        }

        return false;
    }

//        final NLPGen nlpGen = new NLPGen();
//
//        @Nullable private String newCanonicalTerm(Termed abbreviated) {
//            if (abbreviated.volume() < 12)
//                return "\"" + nlpGen.toString(abbreviated.term(), 1f, 1f, Tense.Eternal) + "\"";
//            return null;
//        }

    @Nullable
    Compound newRelation(Term abbreviated, Term id) {
        return compoundOrNull(
                $.sim(abbreviated, id)
                //$.equi
        );
        //(Compound) $.equi(abbreviated.term(), id);
        //(Compound) $.secte(abbreviated.term(), id);

        //old 1.6 pattern:
        //Operation compound = Operation.make(
        //    Product.make(termArray(termAbbreviating)), abbreviate);*/
    }


//    public static class AbbreviationAlias extends Abbreviation {
//        public AbbreviationAlias(NAR n, String termPrefix, int volMin, int volMax, float selectionRate, int capacity) {
//            super(n, termPrefix, volMin, volMax, selectionRate, capacity);
//        }
//
//        protected void abbreviate(CompoundConcept abbreviated, Budget b) {
//
//            String id = newSerialTerm();
//
//            Compound abbreviatedTerm = abbreviated.term();
//
//            AliasConcept alias = new AliasConcept(id, abbreviated, nar);
//            nar.concepts.set(alias, alias);
//            nar.concepts.set(abbreviatedTerm, alias); //override the abbreviated on lookups
//            logger.info("{} <=== {}", alias, abbreviatedTerm);
//
//        }
//    }
//


//    public static class AbbreviationTask extends NALTask {
//
//        @NotNull
//        private final Compound abbreviated;
//        @NotNull
//        private final Term alias;
//
//        public AbbreviationTask(Compound term, byte punc, Truth truth, long creation, long start, long end, long[] evidence, Compound abbreviated, Termed alias) {
//            super(term, punc, truth, creation, start, end, evidence);
//            this.abbreviated = abbreviated;
//            this.alias = alias.term();
//        }
//
//
////        @Override
////        public void feedback(TruthDelta delta, float deltaConfidence, float deltaSatisfaction, NAR nar) {
////
////            super.feedback(delta, deltaConfidence, deltaSatisfaction, nar);
////
////
////            if (deltaConfidence == deltaConfidence /* wasn't deleted, even for questions */ && !isDeleted()) {
////                @Nullable Concept c = concept(nar);
////                c.put(Abbreviation.class, alias);
////
////            }
////        }
//    }
}
