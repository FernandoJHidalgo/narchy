package nars.nar.util;

import nars.Op;
import nars.bag.Bag;
import nars.bag.impl.CurveBag;
import nars.budget.merge.BudgetMerge;
import nars.budget.policy.ConceptPolicy;
import nars.budget.policy.DefaultConceptPolicy;
import nars.concept.AtomConcept;
import nars.concept.CompoundConcept;
import nars.concept.Concept;
import nars.concept.OperationConcept;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.obj.Termject;
import nars.term.obj.TermjectConcept;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

import static nars.nal.Tense.DTERNAL;

/**
 * Created by me on 2/24/16.
 */
public class DefaultConceptBuilder implements Concept.ConceptBuilder {

    final Function<Atomic, AtomConcept> atomBuilder =
            (Atomic a) -> {
                Map map = new HashMap();
                switch (a.op()) {
                    case OBJECT:
                        return new TermjectConcept<>((Termject)a, termbag(map), taskbag(map));
                    default:
                        return new AtomConcept(a, termbag(map), taskbag(map));
                }

            };

    private final ConceptPolicy init, awake, sleep;


    //private static volatile int serial = 0;

//    final Function<Variable, VariableConcept> varBuilder =
//            (Variable v) -> new VariableConcept(v);

    @Nullable
    final Concept newConcept(@NotNull Compound t){

        if (t.op().temporal && t.dt()!=DTERNAL)
            throw new RuntimeException("temporality in concept term: " + t);


        Map map = new HashMap();
        @NotNull Bag<Term> termbag = termbag(map);
        @NotNull Bag<Task> taskbag = taskbag(map);

        switch (t.op()) {

            case INH:
                if (Op.isOperation(t))
                    return new OperationConcept(t, termbag, taskbag);
                break;

            case NEG:
                throw new RuntimeException("negation terms must not be conceptualized");

        }

        return new CompoundConcept<>(t, termbag, taskbag);
    }



    //return (!(t instanceof Space)) ?
    //new SpaceConcept((Space) t, taskLinks, termLinks);

    @NotNull
    public Bag<Task> taskbag(Map map) {
        return new CurveBag<>(1, defaultCurveSampler, mergeDefault, map);
    }


    @NotNull
    public Bag<Term> termbag(Map map) {
        return new CurveBag<>(1, defaultCurveSampler, mergeDefault, map);
    }


    private final BudgetMerge mergeDefault = BudgetMerge.plusBlend;




    final static Logger logger = LoggerFactory.getLogger(DefaultConceptBuilder.class);


    @NotNull
    public final Random rng; //shared
    @NotNull
    public final CurveBag.CurveSampler defaultCurveSampler; //shared


    public DefaultConceptBuilder(@NotNull Random r) {
        this.rng = r;

        this.defaultCurveSampler =
                new CurveBag.NormalizedSampler(
                        //new CurveBag.DirectSampler(
                        //CurveBag.linearBagCurve,
                        //CurveBag.power2BagCurve,
                        CurveBag.power4BagCurve,
                        //CurveBag.power6BagCurve,
                        rng);

        this.sleep = new DefaultConceptPolicy(10, 8, 2, 16, 8);
        this.init = sleep;

        this.awake = new DefaultConceptPolicy(12, 10, 4, 24, 12);


    }


    @Override
    @Nullable
    public Termed apply(@NotNull Term term) {

        //already a concept, assume it is from here
        if (term instanceof Concept) {
            return term;
        }

        Concept result = null;
        if (term instanceof Compound) {
            result = newConcept(  (Compound) term );
        } else {

            if (term instanceof Variable) {
                //final int s = this.serial;
                //serial++;
                //result = varBuilder.apply((Variable) term);
                return term;
            } else if (term instanceof Atomic) {
                result = atomBuilder.apply((Atomic) term);
            }

        }
        if (result == null) {
            throw new UnsupportedOperationException(
                    "unknown conceptualization method for term \"" +
                            term + "\" of class: "  + term.getClass()
            );
        }


        //logger.trace("{} conceptualized to {}", term, result);

        return result;

    }

    @Override
    public ConceptPolicy init() {
        return init;
    }
    @Override
    public ConceptPolicy awake() {
        return awake;
    }

    @Override
    public ConceptPolicy sleep() {
        return sleep;
    }

}