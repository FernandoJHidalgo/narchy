package nars.derive.rule;

import com.google.common.base.Splitter;
import com.google.common.collect.Streams;
import jcog.Util;
import jcog.map.FileHashMap;
import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.derive.PrediTerm;
import nars.derive.TrieDeriver;
import nars.index.term.PatternTermIndex;
import nars.index.term.TermIndex;
import nars.term.Compound;
import nars.term.Term;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


/**
 * Holds an set of derivation rules and a pattern index of their components
 */
public class PremiseRuleSet extends HashSet<PremiseRule> {

    private final boolean permuteBackwards, permuteForwards;

    @NotNull
    public static PremiseRuleSet rules(boolean permute, String... name) {

        final PatternTermIndex p = new PatternTermIndex();
        PremiseRuleSet rs = new PremiseRuleSet(parsedRules(p, name), p, permute);

        //logger.info("{} totalRules={}, uniqueComponents={}", name, rs.rules.size(), rs.patterns.size());
        if (rs.errors[0] > 0) {
            logger.error("{} errors={}", name, rs.errors[0]);
        }

        return rs;
    }

    public static Stream<Pair<PremiseRule, String>> parsedRules(PatternTermIndex p, String... name) {
        return Stream.of(name)./*parallel().*/flatMap(n -> {

                    InputStream nn = NAR.class.getResourceAsStream("nal/" + n);
                    byte[] bb;
                    try {
                        bb = nn.readAllBytes();
                    } catch (IOException e) {
                        e.printStackTrace();
                        bb = ArrayUtils.EMPTY_BYTE_ARRAY;
                    }
                    return parse(load(bb), p);

                }
        );
    }


    @NotNull
    public final PatternTermIndex patterns;


    private static final Logger logger = LoggerFactory.getLogger(PremiseRuleSet.class);


    public PremiseRuleSet(PatternTermIndex index, @NotNull PremiseRule... rules) {
        this(false, index, rules);
    }

    public PremiseRuleSet(@NotNull String... rules) {
        this(new PatternTermIndex(), rules);
    }

    public PremiseRuleSet(PatternTermIndex index, @NotNull String... rules) {
        this(index, (PremiseRule[]) parse($.terms, rules));
    }

    public PremiseRuleSet(boolean permute, PatternTermIndex index, @NotNull PremiseRule... rules) {
        super();
        this.patterns = index;
        for (PremiseRule p : rules) {
            try {
                this.add(normalize(p, this.patterns));
            } catch (RuntimeException e) {
                logger.error(" {}", e);
            }
        }
        this.permuteBackwards = this.permuteForwards = permute;
    }

    final int[] errors = {0};


    public PremiseRuleSet(@NotNull Stream<Pair<PremiseRule, String>> parsed, @NotNull PatternTermIndex patterns, boolean permute) {
        this.patterns = patterns;
        this.permuteBackwards = this.permuteForwards = permute;
        permute(parsed, patterns).forEach(this::add);
    }


    @NotNull
    static Stream<String> load(@NotNull byte[] data) {
        return preprocess( Streams.stream(Splitter.on('\n').split(new String(data)) ) );
    }

    @NotNull
    static Stream<String> preprocess(@NotNull Stream<String> lines) {

        return lines.map(s -> {

            s = s.trim(); //HACK write a better file loader

            if (s.isEmpty() || s.startsWith("//")) {
                return null;
            }

            if (s.contains("..")) {
                s = s.replace("A..", "%A.."); //add var pattern manually to ellipsis
                s = s.replace("%A..B=_", "%A..%B=_"); //add var pattern manually to ellipsis
                s = s.replace("B..", "%B.."); //add var pattern manually to ellipsis
                s = s.replace("%A.._=B", "%A.._=%B"); //add var pattern manually to ellipsis
            }

//            if (s.startsWith("try:")) {
//                filtering = true;
//            }

            return s;
            //lines2.add(s);

        }).filter(Objects::nonNull);


//        if (filtering) {
//            StringBuilder current_rule = new StringBuilder(256);
//            List<String> unparsed_rules = $.newArrayList(1024);
//            for (String s : lines) {
//
//                s = s.trim(); //HACK write a better file loader
//
//                boolean currentRuleEmpty = current_rule.length() == 0;
//                if (s.startsWith("//") || spacePattern.matcher(s).replaceAll(Matcher.quoteReplacement("")).isEmpty()) {
//
//                    if (!currentRuleEmpty) {
//
//                        if (!filtering || filtering && current_rule.toString().contains("try:")) {
//                            unparsed_rules.add(current_rule.toString().trim().replace("try:", "")); //rule is finished, add it
//                        }
//                        current_rule.setLength(0); //start identifying a new rule
//                    }
//
//                } else {
//                    //note, it can also be that the current_rule is not empty and this line contains |- which means
//                    //its already a new rule, in which case the old rule has to be added before we go on
//                    if (!currentRuleEmpty && s.contains("|-")) {
//
//                        if (!filtering || filtering && current_rule.toString().contains("try:")) {
//                            unparsed_rules.add(current_rule.toString().trim().replace("try:", "")); //rule is finished, add it
//                        }
//                        current_rule.setLength(0); //start identifying a new rule
//
//                    }
//                    current_rule.append(s).append('\n');
//                }
//            }
//
//            if (current_rule.length() > 0) {
//                if (!filtering || filtering && current_rule.toString().contains("try:")) {
//                    unparsed_rules.add(current_rule.toString());
//                }
//            }
//
//            return unparsed_rules;
//            //.parallelStream();
//            //.stream();
//        } else {
//            return lines;//.stream();
//        }

    }


    final static Map<String,PremiseRule> lines = new ConcurrentHashMap<>();
//    static {
//        Map<String, Compound> m;
//        try {
//            m = new FileHashMap<>();
//        } catch (IOException e) {
//            e.printStackTrace();
//            m = new ConcurrentHashMap();
//        }
//        lines = m;
//    }


//    final static com.github.benmanes.caffeine.cache.Cache<String, Pair<Compound, String>> lines = Caffeine.newBuilder()
//            .maximumSize(4 * 1024)
//            .build();

    @NotNull
    static Stream<Pair<PremiseRule, String>> parse(@NotNull Stream<String> rawRules, @NotNull PatternTermIndex index) {

        return rawRules.map(src -> Tuples.pair(lines.computeIfAbsent(src, s -> {
            try {
                return PremiseRuleSet.parse( s, index);
            } catch (Narsese.NarseseException e) {
                logger.error("rule parse: {}:\t{}", e, src);
                return null;
            }
        }), src)).filter(x -> x.getOne()!=null);

    }

    @NotNull
    public static PremiseRule parse(@NotNull String src) throws Narsese.NarseseException {
        return parse(src, $.terms);
    }


    public static PremiseRule[] parse(@NotNull TermIndex index, @NotNull String... src) {
        return Util.map((s -> {
            try {
                return parse(s, index);
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
                return null;
            }
        }), new PremiseRule[src.length], src);
    }

    @NotNull
    public static PremiseRule parse(@NotNull String src, @NotNull TermIndex index) throws Narsese.NarseseException {
        return new PremiseRule(parseRuleComponents(src, index));
    }

    @NotNull
    public static TermContainer parseRuleComponents(@NotNull String src, @NotNull TermIndex index) throws Narsese.NarseseException {

        //(Compound) index.parseRaw(src)
        String[] ab = src.split("\\|\\-");
        if (ab.length != 2) {
            throw new Narsese.NarseseException("Rule component must have arity=2, separated by \"|-\": " + src);
        }

        String A = '(' + ab[0].trim() + ')';
        Term a = index.termRaw(A);
        if (!(a instanceof Compound)) {
            throw new Narsese.NarseseException("Left rule component must be compound: " + src);
        }

        String B = '(' + ab[1].trim() + ')';
        Term b = index.termRaw(B);
        if (!(b instanceof Compound)) {
            throw new Narsese.NarseseException("Right rule component must be compound: " + src);
        }

        return TermVector.the((Compound) a, (Compound) b);
    }

    @NotNull
    Stream<PremiseRule> permute(@NotNull Stream<Pair<PremiseRule, String>> rawRules, @NotNull PatternTermIndex index) {
        return rawRules.map(rawAndSrc -> {

            String src = rawAndSrc.getTwo();

            Collection<PremiseRule> ur = $.newArrayList(4);
            try {
                PremiseRule preNorm = new PremiseRule(rawAndSrc.getOne());
                permute(preNorm, src, index, ur);
            } catch (RuntimeException ex) {
                throw new RuntimeException("Invalid TaskRule: " + src, ex);
            }

            return ur;
        }).flatMap(Collection::stream);
    }

    @NotNull
    public Set<PremiseRule> permute(@NotNull PremiseRule preNorm) {
        Set<PremiseRule> ur;
        permute(preNorm, "", new PatternTermIndex(), ur = $.newHashSet(1));
        return ur;
    }

    public void permute(@NotNull PremiseRule preNormRule, String src, @NotNull PatternTermIndex index, @NotNull Collection<PremiseRule> ur) {
        add(preNormRule, src, ur, index,
                (PremiseRule r) -> permuteSwap(r, src, index, ur,
                        (PremiseRule s) -> permuteBackward(src, index, ur, r)));
    }

    void permuteBackward(String src, @NotNull PatternTermIndex index, @NotNull Collection<PremiseRule> ur, @NotNull PremiseRule r) {
        if (permuteBackwards && r.permuteBackward) {

            r.backwardPermutation(index, (q, reason) -> {
                PremiseRule b = add(q, src + ':' + reason, ur, index);
                //System.out.println("BACKWARD: " + b);

                //                    //2nd-order backward
                //                    if (forwardPermutes(b)) {
                //                        permuteSwap(b, src, index, ur);
                //                    }
            });
        }
    }

    protected static void add(@NotNull PremiseRule preNorm, String src, @NotNull Collection<PremiseRule> ur, @NotNull PatternTermIndex index, @NotNull Consumer<PremiseRule> each) {

//        Term[] pp = getPremise().terms().clone();
//        pp = ArrayUtils.add(pp, TaskPositive.proto);
//        Compound newPremise = (Compound) $.the(getPremise().op(), pp);
//
//        PremiseRule r = new PremiseRule(newPremise, getConclusion());
//        @NotNull PremiseRule pos = normalize(r, index);
//
//        //System.err.println(term(0) + " |- " + term(1) + "  " + "\t\t" + remapped);

        PremiseRule pos = add(preNorm, src, ur, index);
        if (pos != null)
            each.accept(pos);

//        if (Global.NEGATIVE_RULES) {
//            PremiseRule neg = add(preNorm.negative(index), src + ":Negated", ur, index);
//            if (neg != null)
//                each.accept(neg);
//        }
    }


    public void permuteSwap(@NotNull PremiseRule r, String src, @NotNull PatternTermIndex index, @NotNull Collection<PremiseRule> ur, @NotNull Consumer<PremiseRule> then) {

        then.accept(r);

        if (permuteForwards && r.permuteForward) {

            PremiseRule bSwap = r.swapPermutation(index);
            if (bSwap != null)
                then.accept(add(bSwap, src + ":forward", ur, index));
        }

    }


    @Nullable
    static PremiseRule add(@Nullable PremiseRule q, String src, @NotNull Collection<PremiseRule> target, @NotNull PatternTermIndex index) {
        if (q == null)
            return null;
//            throw new RuntimeException("null: " + q + ' ' + src);


        PremiseRule normalized = normalize(q, index);
        normalized.setSource(src);
        return target.add(normalized) ? normalized : null;
    }

    @NotNull
    static PremiseRule normalize(@Nullable PremiseRule q, @NotNull PatternTermIndex index) {
        return q.normalizeRule(index).setup(index);
    }

}

