package nars.concept;

import jcog.data.map.CompactArrayMap;
import nars.NAR;
import nars.Task;
import nars.concept.util.ConceptBuilder;
import nars.link.TermLinker;
import nars.table.BeliefTable;
import nars.table.question.QuestionTable;
import nars.term.Term;
import nars.term.Termed;

import java.util.function.Function;
import java.util.stream.Stream;



/** a 'blank' concept which does not store any tasks */
public class NodeConcept implements Concept {

    public final Term term;


    private final TermLinker linker;

    /** cached here, == target.hashCode() */
    private final int hash;

    public final CompactArrayMap<String, Object> meta = new CompactArrayMap<>();

    public NodeConcept(Term term, NAR nar) {
        this(term, nar.conceptBuilder);
    }

    public NodeConcept(Term term, ConceptBuilder b) {
        this(term, b.termlinker(term));
    }

    protected NodeConcept(Term term, TermLinker linker) {
        assert (term.op().conceptualizable): term + " not conceptualizable";
        this.term = term;
        this.hash = term.hashCode();

        this.linker = linker;

        if (!(this instanceof PermanentConcept))
            meta.put(DELETED, DELETED); //HACK start deleted to avoid re-deleting if flyweight dynamic
    }


    @Override public BeliefTable beliefs() { return BeliefTable.Empty; }

    @Override public BeliefTable goals() { return BeliefTable.Empty; }

    @Override public QuestionTable questions() { return QuestionTable.Empty; }

    @Override public QuestionTable quests() { return QuestionTable.Empty; }

    @Override
    public Term term() {
        return term;
    }

    @Override
    public final TermLinker linker() {
        return linker;
    }

    @Override
    public Stream<Task> tasks(boolean includeBeliefs, boolean includeQuestions, boolean includeGoals, boolean includeQuests) {
        return Stream.empty();
    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj || (obj instanceof Termed && term.equals(((Termed) obj).term()));
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public final String toString() {
        return term.toString();
    }


    @Override
    public boolean delete( NAR nar) {
        Object[] c = meta.clearPut(DELETED, DELETED);
        if (c==null || (c.length!=2 || c[0]!=DELETED)) {
//            if (linker instanceof TemplateTermLinker) ((FasterList)linker).clear(); //HACK TODO maybe add Linker.clear()

            return true;
        }
        return false;
    }

    @Override
    public <X> X meta(String key, Function<String,X> valueIfAbsent) {
        return (X) meta.computeIfAbsent(key, valueIfAbsent);
    }

    @Override
    public <X> X meta(String key, Object value) {
        return (X) meta.put(key, value);
    }

    @Override
    public <X> X meta(String key) {
        return (X) meta.get(key);
    }




}
