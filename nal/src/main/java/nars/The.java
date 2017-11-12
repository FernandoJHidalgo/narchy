package nars;

import jcog.list.FasterList;
import jcog.memoize.CaffeineMemoize;
import jcog.memoize.HijackMemoize;
import jcog.memoize.Memoize;
import jcog.memoize.SoftMemoize;
import nars.derive.match.Ellipsis;
import nars.index.term.NewCompound;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.compound.GenericCompound;
import nars.term.compound.UnitCompound1;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static nars.Op.Null;
import static nars.Op.PROD;

/**
 * The the
 * immutable singleton instantiator/interner/etc
 */
public enum The {  ;

    @Deprecated /* @NotNull */ public static final TermContainer subterms(Term... s) {
        return Subterms.the.apply(s);
    }
    /* @NotNull */ public static final TermContainer subterms(List<Term> s) {
        return Subterms.the.apply(s.toArray(new Term[s.size()]));
    }

    @Deprecated /* @NotNull */ protected static Term compound(Op o, Term... subterms) {
        return Compound.the.apply(o, new FasterList<>(subterms));
    }
    /* @NotNull */ protected static Term compound(Op o, List<Term> subterms) {
        return Compound.the.apply(o, subterms);
    }

    public static final class Subterms {

        public static final Function<Term[], TermContainer> RawSubtermBuilder =
                TermVector::the;

        static final Function<NewCompound, TermContainer> rawSubtermBuilderBuilder = (n) -> RawSubtermBuilder.apply(n.subs);

        public static final Supplier<Function<Term[], TermContainer>> SoftSubtermBuilder = () ->
                new MemoizeSubtermBuilder(new SoftMemoize<>(rawSubtermBuilderBuilder, 512 * 1024, true));

        public static final Supplier<Function<Term[], TermContainer>> WeakSubtermBuilder = () ->
                new MemoizeSubtermBuilder(new SoftMemoize<>(rawSubtermBuilderBuilder, 512 * 1024, false));

        public static final Supplier<Function<Term[], TermContainer>> CaffeineSubtermBuilder = () ->
                new MemoizeSubtermBuilder(CaffeineMemoize.build(rawSubtermBuilderBuilder, 512 * 1024, false));


        public static final Supplier<Function<Term[], TermContainer>> HijackSubtermBuilder = () ->
                new Function<>() {

                    final HijackMemoize<NewCompound, TermContainer> cache
                            = new HijackMemoize<>((x) -> RawSubtermBuilder.apply(x.subs),
                            128 * 1024 + 7 /* ~prime */, 4);

                    @Override
                    public TermContainer apply(Term[] o) {
                        return cache.apply(
                                new NewCompound(PROD, o).commit()
                        );
                    }
                };

        public static Function<Term[], TermContainer> the =
                //CaffeineSubtermBuilder.get();
                RawSubtermBuilder;

        private static class MemoizeSubtermBuilder implements Function<Term[], TermContainer> {
            final Memoize<NewCompound, TermContainer> cache;

            private MemoizeSubtermBuilder(Memoize<NewCompound, TermContainer> cache) {
                this.cache = cache;
            }

            @Override
            public TermContainer apply(Term[] terms) {
                return cache.apply(new NewCompound(PROD, terms).commit());
            }
        }

    }

    public static class Compound {

        public static final BiFunction<Op, List<Term>, Term> rawCompoundBuilder = (o, subterms) -> {
            assert (!o.atomic) : o + " is atomic, with subterms: " + (subterms);

            boolean hasEllipsis = false;
            boolean prohibitBool = !o.allowsBool;

            for (Term x : subterms) {
                if (prohibitBool && x instanceof Bool)
                    return Null;
                if (!hasEllipsis && x instanceof Ellipsis)
                    hasEllipsis = true;
            }


            int s = subterms.size();
            assert (o.maxSize >= s) :
                    "subterm overflow: " + o + ' ' + (subterms);
            assert (o.minSize <= s || hasEllipsis) :
                    "subterm underflow: " + o + ' ' + (subterms);

            switch (s) {
                case 1:
                    return new UnitCompound1(o, subterms.get(0));

                default:
                    return new GenericCompound(o, subterms(subterms));
            }

        };

//        public static final Supplier<BiFunction<Op, Term[], Term>> SoftCompoundBuilder = ()->
//                new BiFunction<>() {
//
//                    final SoftMemoize<NewCompound, Term> cache = new SoftMemoize<>((v) -> rawCompoundBuilder.apply(v.op, v.subs), 64 * 1024, true);
//
//                    @Override
//                    public Term apply(Op op, Term[] terms) {
//                        return cache.apply(new NewCompound(op, terms).commit());
//                    }
//                };
//
        public static final Supplier<BiFunction<Op, List<Term>, Term>> CaffeineCompoundBuilder = ()->new BiFunction<>() {

            final CaffeineMemoize<NewCompound, Term> cache = CaffeineMemoize.build((v) -> rawCompoundBuilder.apply(v.op, new FasterList(v.subs) /* HACK */),
                    256 * 1024, false);

            @Override
            public Term apply(Op op, List<Term> terms) {
                return cache.apply(new NewCompound(op, terms).commit());
            }
        };
//
//        public static final Supplier<BiFunction<Op, Term[], Term>> HijackCompoundBuilder = ()->new BiFunction<>() {
//
//            final HijackMemoize<NewCompound, Term> cache
//                    = new HijackMemoize<>((x) -> rawCompoundBuilder.apply(x.op, x.subs),
//                    128 * 1024 + 7 /* ~prime */, 3);
//
//            @Override
//            public Term apply(Op o, Term[] subterms) {
//                return cache.apply(
//                        new NewCompound(o, subterms).commit()
//                );
//            }
//        };
//
        public static BiFunction<Op, List<Term>, Term> the =
                rawCompoundBuilder;
                //CaffeineCompoundBuilder.get();
                //HijackCompoundBuilder;

    }
}
