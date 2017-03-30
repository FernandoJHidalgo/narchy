package nars.index.term;

import nars.*;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.conceptualize.ConceptBuilder;
import nars.derive.meta.match.EllipsisMatch;
import nars.index.TermBuilder;
import nars.premise.Derivation;
import nars.task.util.InvalidTaskException;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.container.TermContainer;
import nars.term.subst.MapSubst;
import nars.term.subst.MapSubst1;
import nars.term.subst.Subst;
import nars.term.transform.CompoundTransform;
import nars.term.transform.VariableNormalization;
import nars.term.util.InvalidTermException;
import nars.term.var.Variable;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static nars.term.Term.False;
import static nars.term.Terms.compoundOrNull;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

/**
 *
 */
public abstract class TermIndex extends TermBuilder {


    private static final Logger logger = LoggerFactory.getLogger(TermIndex.class);
    protected NAR nar;


    /**
     * get if not absent
     */
    @Nullable
    public Termed get(@NotNull Term t) {
        return get(t, false);
    }

    @Nullable
    public abstract Termed get(@NotNull Term key, boolean createIfMissing);

    @Override
    protected int dt(int dt) {
        NAR n = this.nar;
        if (n == null)
            return dt;

        switch (dt) {
            case DTERNAL:
            case XTERNAL:
            case 0:
                return dt; //no-change
        }

        return Math.abs(dt) < n.dur() ? 0 : dt;
    }

    /**
     * sets or replaces the existing value, unless the existing value is a PermanentConcept it must not
     * be replaced with a non-Permanent concept
     */
    public abstract void set(@NotNull Term src, Termed target);

    public final void set(@NotNull Termed t) {
        set(t.term(), t);
    }


    abstract public void clear();

    abstract public void forEach(Consumer<? super Termed> c);


    /**
     * called when a concept has been modified, ie. to trigger persistence
     */
    public void commit(Concept c) {
        //by default does nothing
    }

    public void start(NAR nar) {
        this.nar = nar;
        conceptBuilder().start(nar);
    }

    /**
     * # of contained terms
     */
    public abstract int size();

    @NotNull
    abstract public ConceptBuilder conceptBuilder();


    /**
     * a string containing statistics of the index's current state
     */
    @NotNull
    public abstract String summary();

    public abstract void remove(@NotNull Term entry);


//    public final HijacKache<Compound, Term> normalizations =
//            new HijacKache<>(Param.NORMALIZATION_CACHE_SIZE, 4);
//    public final HijacKache<ProtoCompound, Term> terms =
//            new HijacKache<>(Param.TERM_CACHE_SIZE, 4);

//    final Function<? super ProtoCompound, ? extends Term> termizer = pc -> {
//
//        return theSafe(pc.op(), pc.dt(), pc.terms() );
//    };
//
//    private int volumeMax(Op op) {
//        if (nar!=null) {
//            return nar.termVolumeMax.intValue();
//        } else {
//            return Param.COMPOUND_VOLUME_MAX;
//        }
//    }

    @NotNull
    private final Term theSafe(@NotNull Op o, int dt, @NotNull Term[] u) {
        try {
            return super.the(o, dt, u);
            //return t == null ? False : t;
        } catch (@NotNull InvalidTermException | InvalidTaskException x) {
            if (Param.DEBUG_EXTRA) {
                logger.warn("{x} : {} {} {}", x, o, dt, u);
            }
        } catch (Throwable e) {
            logger.error("{x} : {} {} {}", e, o, dt, u);
        }
        return False; //place a False placeholder so that a repeat call will not have to discover this manually
    }


    /**
     * returns the resolved term according to the substitution
     */
    @Nullable
    public Term transform(@NotNull Term src, @NotNull Subst f) {


        Term y = f.xy(src);
        if (y != null)
            return y; //an assigned substitution, whether a variable or other type of term

        Op op = src.op();
        switch (op) {
            case ATOM:
            case INT:
            case VAR_DEP:
            case VAR_INDEP:
            case VAR_QUERY:
                return src; //unassigned literal atom or non-pattern var
            case VAR_PATTERN:
                return null; //unassigned pattern variable
        }

        //shortcut for premise evaluation matching:
        //no variables that could be substituted, so return this constant
        if (f instanceof Derivation && (src.vars() + src.varPattern() == 0))
            return src;


        boolean strict = !(this instanceof PatternTermIndex); //f instanceof Derivation;

        Compound crc = (Compound) src;
        TermContainer subs = crc.subterms();

        int len = subs.size();
        List<Term> sub = $.newArrayList(len /* estimate */);


        boolean changed = false;
        Op cop = crc.op();

        //early prefilter for True/False subterms
        boolean filterTrueFalse = disallowTrueOrFalse(cop);

        //use COMPOUND_VOLUME_MAX instead of trying for the nar's to provide construction head-room that can allow terms
        //to reduce and potentially meet the requirement
        int volLimit = Param.COMPOUND_VOLUME_MAX - 1; /* -1 for the wrapping compound contribution of +1 volume if succesful */
        int volSum = 0, volAt = 0, subAt = 0;
        for (int i = 0; i < len; i++) {
            Term t = subs.term(i);
            Term u = transform(t, f);


            if (u instanceof EllipsisMatch) {

                ((EllipsisMatch) u).expand(op, sub);
                subAt = sub.size();

                for (; volAt < subAt; volAt++) {
                    Term st = sub.get(volAt);
                    if (filterTrueFalse && isTrueOrFalse(st)) return null;
                    volSum += st.volume();
                    if (volSum >= volLimit) {
                        return null;
                    } //HARD VOLUME LIMIT REACHED
                }

                changed = true;

            } else {

                if (u == null) {

                    if (strict) {
                        return null;
                    }

                    u = t; //keep value

                } else {
                    changed |= (u != t);
                }

                if (filterTrueFalse && isTrueOrFalse(u))
                    return null;
                volSum += u.volume();
                if (volSum >= volLimit) {
                    return null;
                } //HARD VOLUME LIMIT REACHED

                sub.add(u);

                subAt++;

            }


        }

        Term transformed;
        int ss = sub.size();
//        if (!changed || (ss == len && crc.equalTerms(sub)))
//            transformed = crc;
//        else {
        transformed = the(cop, crc.dt(), sub.toArray(new Term[ss]));
        //}

//        //cache the result
//        if (transformed!=null) //TODO store false for 'null' result
//            f.cache(src, transformed);

        return transformed;
    }


//    @NotNull
//    public final Term the(@NotNull Compound csrc, @NotNull TermContainer newSubs) {
//        if (csrc.subterms().equals(newSubs)) {
//            return csrc;
//        } else {
//            return the(csrc.op(), csrc.dt(), newSubs.terms());
//        }
//    }

    @NotNull
    public final Term the(@NotNull Compound csrc, @NotNull Term... args) {
        return csrc.equalTerms(args) ? csrc : the(csrc.op(), csrc.dt(), args);
    }

    @NotNull
    public final Term the(@NotNull Compound csrc, int newDT) {
        return csrc.dt() == newDT ? csrc : the(csrc.op(), newDT, csrc.terms());
    }

//    @Override
//    public final @NotNull Term the(@NotNull Op op, int dt, @NotNull Term[] args) throws InvalidTermException {
//
////        int totalVolume = 0;
////        for (Term x : u)
////            totalVolume += x.volume();
//
////        if (totalVolume > volumeMax(op))
////            throw new InvalidTermException(op, dt, u, "Too voluminous");
//
//        boolean cacheable =
//                //(totalVolume > 2)
//                        //&&
//                (op !=INH) || !(args[0].op() == PROD && args[1].op()==ATOM && get(args[1]) instanceof Functor) //prevents caching for potential transforming terms
//                ;
//
//        if (cacheable) {
//
//            return terms.computeIfAbsent(new ProtoCompound.RawProtoCompound(op, dt, args), termizer);
//
//        } else {
//            return super.the(op, dt, args);
//        }
//    }

//    @Deprecated
//    public final @NotNull Term the(@NotNull Op op, @NotNull Term... tt) {
//        return the(op, DTERNAL, tt); //call this implementation's, not super class's
//    }


    public void print(@NotNull PrintStream out) {
        forEach(out::println);
        out.println();
    }


//    private boolean cacheNormalization(@NotNull Compound src) {
//        return false;
//    }


//    @Nullable
//    public Term the(@NotNull Compound src, @NotNull List<Term> newSubs) {
//        if (src.size() == newSubs.size() && src.equalTerms(newSubs) )
//            return src;
//        else
//            return the(src.op(), src.dt(), newSubs.toArray(new Term[newSubs.size()]));
//    }

    public Term normalize(Compound x) {

        Term y;

        try {
            int vars = x.vars();
            int pVars = x.varPattern();
            int totalVars = vars + pVars;

            if (totalVars > 0) {
                y = transform(x,
                        (vars == 1 && pVars == 0) ?
                                VariableNormalization.singleVariableNormalization //special case for efficiency
                                :
                                new VariableNormalization(totalVars /* estimate */)
                );
            } else {
                y = x;
            }


        } catch (InvalidTermException e) {

            if (Param.DEBUG_EXTRA)
                logger.warn("normalize {} : {}", x, e);

            return InvalidCompound;
        }

        if (y instanceof Compound) {

            //if (c!=null) {
            //c = compoundOrNull($.unneg((Compound) c));
            ((Compound) y).setNormalized();
            //}
        }

        return y;
    }

    @Nullable
    public Term transform(@NotNull Compound src, @NotNull CompoundTransform t) {
        if (!t.testSuperTerm(src)) {
            return src;
        } else {
            int dt = src.dt();

            TermContainer tc = transform(src, src, dt, t);
            return (tc == null) ? null : ((tc != src) ? the(src.op(), dt, tc) : src);
        }
    }

    @Nullable
    public Term transform(@NotNull Compound src, int newDT, @NotNull CompoundTransform t) {
        if (src.dt() == newDT)
            return transform(src, t); //no dt change, use non-DT changing method that has early fail
        else {
            TermContainer subs = transform(src, src, newDT, t);
            return subs == null ? null : the(src.op(), newDT, subs);
        }
    }

    @Nullable
    private TermContainer transform(@NotNull TermContainer src, Compound superterm, int dt, @NotNull CompoundTransform t) {

        int modifications = 0;

        Op superOp = superterm.op();
        boolean filterTrueAndFalse = disallowTrueOrFalse(superOp);

        int s = src.size();
        Term[] target = new Term[s];
        for (int i = 0; i < s; i++) {

            Term x = src.term(i), y;

            y = t.apply(superterm, x);

            if (y == x && x instanceof Compound) {
                y = transform((Compound) x, t); //recurse
                if (y == null)
                    return null;
            }

            if (y != x) {
                if (filterTrueAndFalse && isTrueOrFalse(y)) {
                    return null;
                }

                //            if (y != null)
                //                y = y.eval(this);

                //if (x != y) { //must be refernce equality test for some variable normalization cases
                //if (!x.equals(y)) { //must be refernce equality test for some variable normalization cases
                modifications++;
                //}
            }

            target[i] = y;
        }

        //TODO does it need to recreate the container if the dt has changed because it may need to be commuted ... && (superterm.dt()==dt) but more specific for the case: (XTERNAL -> 0 or DTERNAL)

        return modifications == 0 ? src : TermContainer.the(superOp, dt, target);
    }

    static boolean disallowTrueOrFalse(Op superOp) {

        switch (superOp) {
            case EQUI:
            case IMPL:
            case CONJ:
                return false; //allow for these because reductions may apply
            default:
                return true;
        }
    }


    @Nullable
    public Term transform(@NotNull Compound src, @NotNull ByteList path, @NotNull Term replacement) {
        return transform(src, path, 0, replacement);
    }

    @Nullable
    private Term transform(@NotNull Term src, @NotNull ByteList path, int depth, @NotNull Term replacement) {
        int ps = path.size();
        if (ps == depth)
            return replacement;
        if (ps < depth)
            throw new RuntimeException("path overflow");

        if (!(src instanceof Compound))
            return src; //path wont continue inside an atom

        int n = src.size();
        Compound csrc = (Compound) src;

        Term[] target = new Term[n];


        boolean changed = false;
        for (int i = 0; i < n; ) {
            Term x = csrc.term(i);
            Term y;
            if (path.get(depth) != i) {
                //unchanged subtree
                y = x;
            } else {
                //replacement is in this subtree
                y = transform(x, path, depth + 1, replacement);
                changed = true;
            }

            target[i++] = y;
        }

        if (!changed)
            return csrc;

        return the(csrc.op(), csrc.dt(), target);
    }


    @NotNull
    public Term parseRaw(@NotNull String termToParse) throws Narsese.NarseseException {
        return Narsese.the().term(termToParse, this, false);
    }

    @Nullable
    public <T extends Termed> T parse(@NotNull String termToParse) throws Narsese.NarseseException {
        return (T) (Narsese.the().term(termToParse, this, true));
    }

    /**
     * applies normalization and anonymization to resolve the term of the concept the input term maps t
     * term should be conceptualizable prior to calling this
     */
    @Nullable
    public final Concept concept(@NotNull Term term, boolean createIfMissing) {

        @Nullable Termed c = get(term, createIfMissing);
        if (!(c instanceof Concept)) {
//            if (createIfMissing) {
//                throw new Concept.InvalidConceptException(term, "Failed to build concept");
//            }
            return null;
        }

        Concept cc = (Concept) c;
        if (cc.isDeleted()) {
            cc.state(conceptBuilder().init(), nar);
        }

        return cc;
    }

    @Nullable
    public Term conceptualizable(@NotNull Term term) {
        Term termPre = null;
        while (term instanceof Compound && termPre != term) {
//            //shouldnt need to check for this here
//            if (isTrueOrFalse(term))
//                throw new UnsupportedOperationException();

            termPre = term;

            switch (term.op()) {
                case VAR_DEP:
                case VAR_INDEP:
                case VAR_QUERY:
                case VAR_PATTERN:
                    //throw new InvalidConceptException((Compound)term, "variables can not be conceptualized");
                    return null;

                case NEG:
                    term = term.unneg(); //fallthru

                default:

                    if (term instanceof Compound) {
                        term = normalize((Compound) term);
                    }

                    if (term instanceof Compound) {
                        term = atemporalize((Compound) term);
                    }

                    break;

            }
        }

        if (term == null || (term instanceof Variable) || (TermBuilder.isTrueOrFalse(term)))
            return null;

        return term;
    }


    @Nullable
    public Term replace(@NotNull Term src, Map<Term, Term> m) {
        return transform(src, new MapSubst(m));
    }

    @Nullable
    public Term replace(@NotNull Term src, Term from, Term to) {
        return transform(src, new MapSubst1(from, to));
    }


    /**
     * implementations can override this to update the index when a concept's state changes, ex: to re-evaluate it's features
     */
    public void onStateChanged(Concept c) {
        /* nothing */
    }

    @NotNull
    public Term atemporalize(@NotNull Term t) {
        return t instanceof Compound ? atemporalize((Compound) t) : t;
    }

    @NotNull
    public Compound atemporalize(@NotNull Compound c) {

        if (!c.hasTemporal())
            return c;

        TermContainer psubs = c.subterms();
        Term[] newSubs;

        Op o = c.op();
        int pdt = c.dt();
        if (psubs.hasAny(Op.TemporalBits)) {
            boolean subsChanged = false;
            int cs = psubs.size();
            Term[] ss = new Term[cs];
            for (int i = 0; i < cs; i++) {

                Term x = psubs.term(i), y;
                if (x instanceof Compound) {
                    subsChanged |= (x != (y = atemporalize((Compound) x)));
                } else {
                    y = x;
                }

                ss[i] = y;

            }


            newSubs = subsChanged ? ss : null;


        } else {
            newSubs = null;
        }

        //resolve XTERNAL temporals to lexical order
        if (pdt == XTERNAL /*&& cs == 2*/) {
            boolean swap = false;
            if (newSubs == null) {
                if (psubs.term(0).compareTo(psubs.term(1)) > 0) {
                    newSubs = psubs.terms();
                    swap = true;
                }
            } else {
                if (newSubs[0].compareTo(newSubs[1]) > 0) {
                    swap = true;
                }
            }

            if (swap) {
                Term x = newSubs[0];
                newSubs[0] = newSubs[1];
                newSubs[1] = x;
            }
        }


        boolean dtChanged = (pdt != DTERNAL && o.temporal);
        boolean subsChanged = (newSubs != null);

        if (subsChanged || dtChanged) {

            if (subsChanged && o.temporal && newSubs.length == 1) {
                //it was a repeat which collapsed, so use XTERNAL and repeat the subterm

                if (pdt != DTERNAL)
                    pdt = XTERNAL;

                Term s = newSubs[0];
                newSubs = new Term[]{s, s};
            } else {
                if (o.temporal)
                    pdt = DTERNAL;
            }
//            if (o.temporal && newSubs!=null && newSubs.size() == 1) {
//                System.out.println("?");
//            }

            Compound xx = compoundOrNull(
                    newCompound(o,
                            pdt,
                            subsChanged ? intern(newSubs) : psubs)
            );
            if (xx == null)
                throw new InvalidTermException("unable to atemporalize", c);

            if (c.isNormalized())
                xx.setNormalized();

            //Termed exxist = get(xx, false); //early exit: atemporalized to a concept already, so return
            //if (exxist!=null)
            //return exxist.term();


            //x = i.the(xx).term();
            return xx;
        } else {
            return c;
        }
    }


    protected void onRemove(Termed value) {
        if (value instanceof Concept) {
            if (value instanceof PermanentConcept) {
                //refuse deletion
                set(value.term(), value);
            } else {
                ((Concept) value).delete(nar);
            }
        }
    }

    @Nullable
    public Compound eval(Compound x) {

        //eval before normalizing
        Compound z = compoundOrNull(x.eval(this));
        if (z == null)
            return null;

        return compoundOrNull(normalize(z));
    }

    /**
     * changes all 'XTERNAL' to 'DTERNAL' and applies reductions in the process.  a change requires renormalization
     */
    @Nullable
    public Compound retemporalize(@NotNull Compound x) {

        int dt = x.dt();
        Term y = transform(x, dt == XTERNAL ? DTERNAL : dt, retemporalization);
        if (!(y instanceof Compound)) {
            return null;
        } else {
            return compoundOrNull(normalize((Compound) y));
        }

    }

    final CompoundTransform retemporalization = new CompoundTransform() {

        @Nullable
        @Override
        public Term apply(@Nullable Compound parent, @NotNull Term term) {
            if (term instanceof Compound && ((Compound) term).dt() == XTERNAL) {
                Compound cs = (Compound) term;
                return the(cs.op(), DTERNAL, cs.terms());
            }
            return term;
        }
    };

    //    @Override
//    protected @NotNull Term statement(@NotNull Op op, int dt, @NotNull Term subject, @NotNull Term predicate) {
//        if ( op == INH && predicate instanceof Atom && !(predicate instanceof Concept) && transformImmediates() ) {
//            //resolve atomic statement predicates in inheritance, for inline term rewriting
//            Termed existingPredicate = get(predicate);
//            if (existingPredicate!=null)
//                predicate = existingPredicate.term();
//        }
//
//        return super.statement(op, dt, subject, predicate);
//    }
}
