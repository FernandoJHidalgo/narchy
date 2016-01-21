package nars;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import nars.java.AtomObject;
import nars.nal.Tense;
import nars.nal.nal8.Operator;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.*;
import nars.term.atom.Atom;
import nars.term.compound.Compound;
import nars.term.compound.GenericCompound;
import nars.term.match.VarPattern;
import nars.term.variable.Variable;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static nars.Op.*;

/**
 * core utility class for:
    --building any type of value, either programmatically or parsed from string input
       (which can be constructed in a static context)
    --??
 */
public enum $  {
    ;

    public static final TermBuilder terms = new TermBuilder() {

        @NotNull
        @Override
        public Termed make(Op op, int relation, TermContainer subterms, int dt) {
            return new GenericCompound(op, relation, (TermVector)subterms);
        }
    };


    public static final org.slf4j.Logger logger = LoggerFactory.getLogger($.class);
    public static final Function<Object, Term> ToStringToTerm = (x) -> $.the(x.toString());

    public static <T extends Term> T $(String term) {
        Termed normalized = Narsese.the().term(term, terms);
        if (normalized!=null)
            return (T)(normalized.term());
        return null;

        //        try { }
        //        catch (InvalidInputException e) { }
    }

    @Deprecated public static <C extends Compound> MutableTask $(String term, char punc) {
        Term t = Narsese.the().term(term).term();
        //TODO normalize etc
        if (!Task.validTaskTerm(t))
            return null;

        return new MutableTask(t)
                .punctuation(punc)
                .eternal();
                //.normalized();
    }

    @NotNull
    public static <O> AtomObject<O> ref(String term, O instance) {
        return new AtomObject(term, instance);
    }

    @NotNull
    public static Atom the(String id) {
        return Atom.the(id);
    }

    @NotNull
    public static Atom[] the(@NotNull String... id) {
        int l = id.length;
        Atom[] x = new Atom[l];
        for (int i = 0; i < l; i++)
            x[i] = Atom.the(id[i]);
        return x;
    }


    public static Atom the(int i) {
        return Atom.the(i);
    }

    /**
     * Op.INHERITANCE from 2 Terms: subj --> pred
     *  returns a Term if the two inputs are equal to each other
     */
    @Nullable
    public static Term inh(Term subj, Term pred) {

//        if ((predicate instanceof Operator) && if (subject instanceof Product))
//            return new GenericCompound(Op.INHERITANCE, (Operator)predicate, (Product)subject);
//        else

        return the(INHERIT, subj, pred);
    }


    @Nullable
    public static Term inh(String subj, String pred) {
        return inh((Term)$(subj), $(pred));
    }


    @Nullable
    public static Term sim(Term subj, Term pred) {
        return the(SIMILAR, subj, pred);
    }

    /** execution (NARS "operation") */
    @NotNull
    public static Compound exec(String operator, String... args) {
        return exec(operator(operator), $.p(args));
    }


    /** execution (NARS "operation") */
    @NotNull
    public static Compound exec(Operator opTerm, Term... arg) {
        return exec(opTerm, $.p(arg));
    }

//    static Compound oper(Atom opTerm, Compound args) {
//        return oper(new Operator(opTerm), args);
//    }

    /** execution (NARS "operation") */
    @NotNull
    public static Compound exec(Operator opTerm, @Nullable Compound arg) {
        return (Compound) the(
                INHERIT,
                arg == null ? TermIndex.Empty : arg,
                opTerm
        );
    }


    @Nullable
    public static Term impl(Term a, Term b) {
        return the(IMPLICATION, a, b);
    }

    @Nullable
    public static Term neg(Term x) {
        return the(NEGATE, x);
    }

    @NotNull
    public static <T extends Term> Compound<T> p(@NotNull Collection<? super T> t) {
        return $.p(t.toArray((T[]) new Term[t.size()]));
    }

    @NotNull
    public static Compound p(@Nullable Term... t) {
        if (t == null)
            return TermIndex.Empty;

        int l = t.length;
        if (l == 0) //length 0 product are allowd and shared
            return TermIndex.Empty;

        return (Compound) the(PRODUCT, t);
    }

    /** creates from a sublist of a list */
    @NotNull
    static Compound p(@NotNull List<Term> l, int from, int to) {
        Term[] x = new Term[to - from];

        for (int j = 0, i = from; i < to; i++)
            x[j++] = l.get(i);

        return $.p(x);
    }

    @NotNull
    public static Compound<Atom> p(String... t) {
        return $.p((Atom[]) $.the(t));
    }

    @NotNull
    public static Variable v(@NotNull Op type, String s) {
        return v(type.ch, s);
    }


    @NotNull
    public static Variable varDep(int i) {
        return v(VAR_DEP, i);
    }

    @NotNull
    public static Variable varDep(String s) {
        return v(VAR_DEP, s);
    }

    @NotNull
    public static Variable varIndep(int i) {
        return v(VAR_INDEP, i);
    }

    @NotNull
    public static Variable varIndep(String s) {
        return v(VAR_INDEP, s);
    }

    @NotNull
    public static Variable varQuery(int i) {
        return v(VAR_QUERY, i);
    }

    @NotNull
    public static Variable varQuery(String s) {
        return v(VAR_QUERY, s);
    }

    @NotNull
    public static VarPattern varPattern(int i) {
        return (VarPattern) v(VAR_PATTERN, i);
    }


    /**
     * Try to make a new compound from two components. Called by the logic rules.
     * <p>
     *  A {-- B becomes {A} --> B
     * @param subj The first component
     * @param pred The second component
     * @return A compound generated or null
     */
    @Nullable
    public static Term inst(Term subj, Term pred) {
        return terms.inst(subj, pred);
    }
    @Nullable
    public static Term instprop(Term subject, Term predicate) {
        return terms.instprop(subject, predicate);
    }
    @Nullable
    public static Term prop(Term subject, Term predicate) {
        return terms.prop(subject, predicate);
    }

//    public static Term term(final Op op, final Term... args) {
//        return Terms.term(op, args);
//    }

    @NotNull
    public static MutableTask belief(Compound term, @NotNull Truth copyFrom) {
        return belief(term, copyFrom.getFrequency(), copyFrom.getConfidence());
    }

    @NotNull
    public static MutableTask belief(Compound term, float freq, float conf) {
        return new MutableTask(term).belief().truth(freq, conf);
    }

    @NotNull
    public static MutableTask goal(Compound term, float freq, float conf) {
        return new MutableTask(term).goal().truth(freq, conf);
    }

    @NotNull
    public static Compound sete(@NotNull Collection<? extends Term> t) {
        return (Compound) the(SET_EXT, t);
    }

   private static Term[] array(@NotNull Collection<? extends Term> t) {
        return t.toArray(new Term[t.size()]);
    }

    @NotNull
    public static Compound seti(@NotNull Collection<Term> t) {
        return $.seti(array(t));
    }

    @NotNull
    public static Compound sete(Term... t) {
        return (Compound) the(SET_EXT, t);
    }

    /** shorthand for extensional set */
    @NotNull
    public static Compound s(Term... t) {
        return sete(t);
    }

    @NotNull
    public static Compound seti(Term... t) {
        return (Compound) the(SET_INT, t);
    }

    /**
     * Try to make a new compound from two components. Called by the logic rules.
     * <p>
     *  A --] B becomes A --> [B]
     * @param subject The first component
     * @param predicate The second component
     * @return A compound generated or null
     */
    @Nullable
    public static Term property(Term subject, Term predicate) {
        return inh(subject, $.seti(predicate));
    }

    @NotNull
    public static Variable v(char ch, String name) {

//        if (name.length() < 3) {
//            int digit = Texts.i(name, -1);
//            if (digit != -1) {
//                Op op = Variable.typeIndex(ch);
//                return Variable.the(op, digit);
//            }
//        }

        switch (ch) {
            case Symbols.VAR_DEPENDENT:
                return new Variable.VarDep(name);
            case Symbols.VAR_INDEPENDENT:
                return new Variable.VarIndep(name);
            case Symbols.VAR_QUERY:
                return new Variable.VarQuery(name);
            case Symbols.VAR_PATTERN:
                return new VarPattern(name);
            default:
                throw new RuntimeException("invalid variable type: " + ch);
        }

    }

    @NotNull
    public static Variable v(@NotNull Op type, int counter) {
        if (counter < Variable.MAX_VARIABLE_CACHED_PER_TYPE) {
            Variable[] vct = Variable.varCache[typeIndex(type)];
            Variable existing = vct[counter];
            return existing != null ? existing : (vct[counter] = v(type.ch, String.valueOf(counter)));
        }

        return v(type.ch, String.valueOf(counter));
    }

    @Nullable
    public static Term conj(Term... a) {
        return the(CONJUNCTION, a);
    }

    @Nullable
    public static Term disj(Term... a) {
        return the(DISJUNCTION, a);
    }

    static {
//        // assume SLF4J is bound to logback in the current environment
//        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
//
//        try {
//            JoranConfigurator configurator = new JoranConfigurator();
//            configurator.setContext(context);
//            // Call context.reset() to clear any previous configuration, e.g. default
//            // configuration. For multi-step configuration, omit calling context.reset().
//            context.reset();
//            //configurator.doConfigure(args[0]);
//        } catch (Exception je) {
//            // StatusPrinter will handle this
//        }
//        StatusPrinter.printInCaseOfErrorsOrWarnings(context);
//
//        Logger logger = LoggerFactory.getLogger($.class);
//        logger.info("Entering application.");
//
//
//
//        logger.info("Exiting application.");
//
//        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
//        // print logback's internal status
//        StatusPrinter.print(lc);
//
//        // assume SLF4J is bound to logback-classic in the current environment
//        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
//        loggerContext.start();
//        //loggerContext.stop();
    }

    @NotNull
    public static final Logger logRoot;

    /** NALogging non-axiomatic logging encoder. log events expressed in NAL terms */
    @NotNull
    public static final PatternLayoutEncoder logEncoder;

    static {
        Thread.currentThread().setName("$");

        //http://logback.qos.ch/manual/layouts.html

        logRoot = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

        LoggerContext loggerContext = logRoot.getLoggerContext();
        // we are not interested in auto-configuration
        loggerContext.reset();

        logEncoder = new PatternLayoutEncoder();
        logEncoder.setContext(loggerContext);
        logEncoder.setPattern("\\( %highlight(%level),%green(%thread),%yellow(%logger{0}) \\): \"%message\".%n");
        logEncoder.start();


        ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
        appender.setContext(loggerContext);
        appender.setEncoder(logEncoder);
        appender.start();
        logRoot.addAppender(appender);

//        rootLogger.debug("Message 1");
//        rootLogger.info("Message 1");
//        rootLogger.warn("Message 2");
//        rootLogger.error("Message 2");
    }

    @Nullable
    public static Term equiv(Term subject, Term pred) {
        return the(EQUIV, subject, pred);
    }

    @Nullable
    public static Term diffInt(Term a, Term b) {
        return the(DIFF_INT, a, b);
    }

    @Nullable
    public static Term diffExt(Term a, Term b) {
        return the(DIFF_EXT, a, b);
    }

    @Nullable
    public static Term imageExt(Term... x) {
        return the(IMAGE_EXT, x);
    }
    @Nullable
    public static Term imageInt(Term... x) {
        return the(IMAGE_INT, x);
    }
    @Nullable
    public static Term sect(Term... x) {
        return the(INTERSECT_EXT, x);
    }
    @Nullable
    public static Term sectInt(Term... x) {
        return the(INTERSECT_INT, x);
    }


    @NotNull
    public static Operator operator(String name) {
        return new Operator(name);
    }


    @Nullable
    public static Term the(@NotNull Op op, Term... subterms) {
        return the(op, -1, subterms);
    }
    @Nullable
    public static Term the(@NotNull Op op, int relation, Term... subterms) {
        return the(op, relation, TermContainer.the(op, subterms));
    }

    @Nullable
    public static Term the(@NotNull Op op, @NotNull Collection<? extends Term> subterms) {
        return the(op, -1, subterms);
    }
    @Nullable
    public static Term the(@NotNull Op op, int relation, @NotNull Collection<? extends Term> subterms) {
        return the(op, relation, TermContainer.the(op, subterms));
    }

    @Nullable
    public static Term the(@NotNull Op op, int relation, TermContainer subterms) {
        return the(op, relation, Tense.ITERNAL, subterms);
    }

    @Nullable
    public static Term the(@NotNull Op op, int relation, int t, TermContainer subterms) {
        return terms.newTerm(op, relation, t, subterms);
    }


    public static int typeIndex(@NotNull Op o) {
        switch (o) {
            case VAR_PATTERN:
                return 0;
            case VAR_DEP:
                return 1;
            case VAR_INDEP:
                return 2;
            case VAR_QUERY:
                return 3;
        }
        throw new RuntimeException(o + " not a variable");
    }

    /** construct set_ext of key,value pairs from a Map */
    @NotNull
    public static Compound seteMap(@NotNull Map<Term,Term> map) {
        return $.sete(
                (Collection<? extends Term>) map.entrySet().stream().map(
                    e -> $.p(e.getKey(),e.getValue()))
                .collect( toList())
        );
    }
    @NotNull
    public static <X> Compound seteMap(@NotNull Map<Term,? extends X> map, @NotNull Function<X, Term> toTerm) {
        return $.sete(
                (Collection<? extends Term>) map.entrySet().stream().map(
                    e -> $.p(e.getKey(), toTerm.apply(e.getValue())))
                .collect( toList())
        );
    }


}
