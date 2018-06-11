package nars.derive.premise;

import com.google.common.base.Splitter;
import com.google.common.collect.Streams;
import jcog.list.ArrayUnenforcedSet;
import jcog.memoize.CaffeineMemoize;
import jcog.memoize.Memoize;
import nars.NAR;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * intermediate representation of a set of compileable Premise Rules
 * TODO remove this class, just use Set<PremiseDeriverProto>'s
 */
public class PremiseDeriverRuleSet extends ArrayUnenforcedSet<PremiseDeriverProto> {

    public final NAR nar;

    public PremiseDeriverRuleSet(NAR nar, String... rules) {
        this(new PremisePatternIndex(nar), rules);
    }

    PremiseDeriverRuleSet(PremisePatternIndex index, String... rules) {
        this(index, PremiseDeriverSource.parse(rules));
    }

    private PremiseDeriverRuleSet(PremisePatternIndex patterns, Stream<PremiseDeriverSource> parsed) {
        assert (patterns.nar != null);
        this.nar = patterns.nar;
        parsed.distinct().forEach(rule -> super.add(new PremiseDeriverProto(rule, patterns)));
    }

    final static Memoize<String, Collection<PremiseDeriverSource>> ruleCache = CaffeineMemoize.build((String n) -> {

        byte[] bb;
        try (InputStream nn =
                     NAR.class.getClassLoader().getResourceAsStream(n)) {


            bb = nn.readAllBytes();

        } catch (IOException e) {

            e.printStackTrace();
            bb = ArrayUtils.EMPTY_BYTE_ARRAY;

        }
        return (PremiseDeriverSource.parse(load(bb)).collect(Collectors.toSet()));

    }, 32, false);

    public static PremiseDeriverRuleSet files(NAR nar, Collection<String> filename) {
        return new PremiseDeriverRuleSet(
                new PremisePatternIndex(nar),
                filename.stream().flatMap(n -> PremiseDeriverRuleSet.ruleCache.apply(n).stream()));
    }

    static Stream<String> load(byte[] data) {
        return preprocess(Streams.stream(Splitter.on('\n').split(new String(data))));
    }

    static Stream<String> preprocess(Stream<String> lines) {

        return lines.map(String::trim).filter(s -> !s.isEmpty() && !s.startsWith("//")).map(s -> {

            if (s.contains("..")) {
                s = s.replace("A..", "%A.."); //add var pattern manually to ellipsis
                //s = s.replace("%A..B=_", "%A..%B=_"); //add var pattern manually to ellipsis
                s = s.replace("B..", "%B.."); //add var pattern manually to ellipsis
                //s = s.replace("%A.._=B", "%A.._=%B"); //add var pattern manually to ellipsis
            }


            return s;

        });


    }

    @Override
    public boolean add(PremiseDeriverProto rule) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends PremiseDeriverProto> c) {
        throw new UnsupportedOperationException();
    }


}

