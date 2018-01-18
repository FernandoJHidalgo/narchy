package nars.op;

import nars.$;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.concept.NodeConcept;
import nars.concept.PermanentConcept;
import nars.subterm.Subterms;
import nars.task.NALTask;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

import static nars.Op.ATOM;
import static nars.Op.INH;

/**
 * Operator interface specifically for goal and command punctuation
 * Allows dynamic handling of argument list like a functor,
 * but at the task level
 * <patham9_> when a goal task is processed, the following happens: In order to decide on whether it is relevant for the current situation, at first it is projected to the current time, then it is revised with previous "desires", then it is checked to what extent this projected revised desire is already fullfilled (which revises its budget) , if its below satisfaction threshold then it is pursued, if its an operation it is additionally checked if
 * <patham9_> executed
 * <patham9_> the consequences of this, to give examples, are a lot:
 * <patham9_> 1 the system wont execute something if it has a strong objection against it. (example: it wont jump down again 5 meters if it previously observed that this damages it, no matter if it thinks about that situation again or not)
 * <patham9_> 2. the system wont lose time with thoughts about already satisfied goals (due to budget shrinking proportional to satisfaction)
 * <patham9_> 3. the system wont execute and pursue what is already satisfied
 * <patham9_> 4. the system wont try to execute and pursue things in the current moment which are "sheduled" to be in the future.
 * <patham9_> 5. the system wont pursue a goal it already pursued for the same reason (due to revision, it is related to 1)
 */
public class Operator extends NodeConcept implements PermanentConcept, Atomic {

    public static final String LOG_FUNCTOR = String.valueOf(Character.valueOf((char) 8594)); //RIGHT ARROW

    public final BiFunction<Task, NAR, Task> execute;

    public Operator(Atom atom, BiFunction<Task, NAR, Task> execute, NAR n) {
        super(atom, n);
        this.execute = execute;
    }

    @Override
    public Term term() {
        return this;
    }

    @Override
    public int opX() {
        return Atom.AtomString;
    }

    @Override
    public byte[] bytes() {
        return ((Atomic) term).bytes();
    }

    /**
     * returns the arguments of an operation (task or term)
     */
    public static Subterms args(Termed operation) {
        assert (operation.op() == INH && operation.subIs(1, ATOM));
        return operation.sub(0).subterms();
    }

    public static Atom func(Termed operation) {
        return (Atom) operation.sub(1);
    }

    public static Task error(Task x, Throwable error, long when) {
        //StringWriter ss = new StringWriter();
        //ExceptionUtils.printRootCauseStackTrace(error, new PrintWriter(ss));
        return Operator.command("error", when, $.quote(x.toString()),
                //$.quote(ss.toString())
                error!=null ? $.quote(error.getMessage()!=null ? error.getMessage() : error.toString()) : $.the("?") //TODO stack trace or something
        );
    }


    static Task command(Term content, long when) {
        return new NALTask(content, Op.COMMAND, null, when, when, when, ArrayUtils.EMPTY_LONG_ARRAY);
    }

    public static Task log(long when, Object content) {
        return Operator.command(LOG_FUNCTOR, when, $.the(content));
    }

    static Task command(String func, long now, @NotNull Term... args) {
        return Operator.command($.func(func, args), now);
    }

//    public static void log(NAR nar, @NotNull String... msg) {
//        nar.input( Operator.log(nar, $.the(msg)) );
//    }
//
//    public static void log(NAR nar, @NotNull Object x) {
//        nar.input( Operator.log(nar, x) );
//    }

}
