package nars.derive;

import jcog.Util;
import jcog.bag.Bag;
import jcog.pri.PriReference;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.control.Activate;
import nars.control.Cause;
import nars.derive.budget.DefaultDeriverBudgeting;
import nars.derive.premise.PremiseDeriver;
import nars.derive.premise.PremiseDeriverCompiler;
import nars.derive.premise.PremiseDeriverProto;
import nars.derive.premise.PremiseDeriverRuleSet;
import nars.exe.Causable;
import nars.link.TaskLink;
import nars.link.Tasklinks;
import nars.link.TermlinkTemplates;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * an individual deriver process: executes a particular Deriver model
 * specified by a set of premise rules.
 * <p>
 * runtime intensity is metered and throttled by causal feedback
 */
abstract public class Deriver extends Causable {

    @Deprecated private static final AtomicInteger serial = new AtomicInteger();

    public static final ThreadLocal<Derivation> derivation = ThreadLocal.withInitial(Derivation::new);

    public final PremiseDeriver rules;

    /**
     * source of concepts supplied to this for this deriver
     */
    protected final Consumer<Predicate<Activate>> source;

    private final Consumer<Collection<Task>> target;

    public DeriverBudgeting prioritize =
            new DefaultDeriverBudgeting();
            

    public Deriver(NAR nar, String... rules) {
        this(new PremiseDeriverRuleSet(nar, rules));
    }

    public Deriver(PremiseDeriverRuleSet rules) {
        this(rules, rules.nar);
    }

    public Deriver(Set<PremiseDeriverProto> rules, NAR nar) {
        this(nar.exe::fire, nar::input, rules, nar);
    }

    public Deriver(Consumer<Predicate<Activate>> source, Consumer<Collection<Task>> target, PremiseDeriverRuleSet rules) {
        this(source, target, rules, rules.nar);
    }

    public Deriver(Consumer<Predicate<Activate>> source, Consumer<Collection<Task>> target, Set<PremiseDeriverProto> rules, NAR nar) {
        this(source, target, PremiseDeriverCompiler.the(rules, null), nar);
        if (rules.isEmpty())
            throw new RuntimeException("rules empty");
    }

    public Deriver(Consumer<Predicate<Activate>> source, Consumer<Collection<Task>> target, PremiseDeriver rules, NAR nar) {
        super(
            $.func("deriver", $.the(serial.getAndIncrement())) 
        );
        this.rules = rules;
        this.source = source;
        this.target = target;

        nar.on(this);
    }

    public static Stream<Deriver> derivers(NAR n) {
        return n.services().filter(Deriver.class::isInstance).map(Deriver.class::cast);
    }

    @Override
    protected final int next(NAR n, final int iterations) {

        Derivation d = derivation.get().cycle(n, this);

        derive(n, iterations, d);

        int derived = d.flush(target);
        return iterations; 
    }

    abstract protected void derive(NAR n, int iterations, Derivation d);


    /** tasklink templates */
    protected final void activate(TaskLink tasklink, Concept[] templates, Random r) {

        float pri = tasklink.priElseZero();
        Tasklinks.linkTask(tasklink, pri, templates, r);
//
//
//        for (Concept x : templates)
//            nar.activate(x, pri);
    }

    static protected boolean commit(NAR nar, Bag<?, TaskLink> tasklinks, @Nullable Bag<Term, PriReference<Term>> termlinks) {
        float linkForgetting = nar.forgetRate.floatValue();
        tasklinks.commit(tasklinks.forget(linkForgetting));
        int ntasklinks = tasklinks.size();
        if (ntasklinks == 0)
            return false;


        if (termlinks!=null)
            termlinks.commit(termlinks.forget(linkForgetting));

        return true;

    }

    protected Concept[] templates(Concept concept, NAR nar) {
        TermlinkTemplates t = concept.templates();

        Concept[] templates = t.concepts(nar, true);
        

        return templates;
    }




    @Override
    public final boolean singleton() {
        return false;
    }

    @Override
    public float value() {
        return Util.sum(Cause::value, rules.causes());
    }


    //public final FloatRange sustain = new FloatRange(0f, 0f, 0.99f);
    public int dur() {
        //return Math.round((nar.dur() * (1/(1- sustain.floatValue()))));
        return nar.dur();
    }
}

















































































































