package nars.term.index;

import com.google.common.cache.CacheBuilder;
import com.gs.collections.impl.map.mutable.primitive.IntObjectHashMap;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by me on 1/2/16.
 */
public class MapIndex2 extends AbstractMapIndex {

    private static final int SUBTERM_RELATION = Integer.MIN_VALUE;

    final Map<Object /* vector(t) */, IntObjectHashMap> data;
    int count;

    public MapIndex2(Map<Object, IntObjectHashMap> data) {

        this.data = data;

    }

    @NotNull
    static Object vector(@NotNull Term t) {
        if (t.isCompound()) return ((Compound)t).subterms();
        return t;
    }

    static final Function<Object, IntObjectHashMap> groupBuilder =
            (k) -> new IntObjectHashMap(8);

    /** returns previous value */
    public Object putItem(Object vv, int index, Object value) {

        IntObjectHashMap g = group(vv);
        Object res = g.put(index, value);
        if (res==null) {
            //insertion
            g.compact();
        }
        return res;
    }

    public IntObjectHashMap group(Object vv) {
        return data.computeIfAbsent(vv, groupBuilder);
    }


    @Nullable
    @Override
    public Termed getTermIfPresent(@NotNull Termed t) {
        return (Termed) getItemIfPresent(
                vector(t.term()), t.opRel());
    }


    @Nullable
    public Object getItemIfPresent(Object vv, int index) {
        IntObjectHashMap group = data.get(vv);
        if (group == null) return null;
        return group.get(index);
    }

    @Nullable
    @Override
    protected TermContainer getSubtermsIfPresent(TermContainer subterms) {
        return (TermContainer) getItemIfPresent(
                subterms, SUBTERM_RELATION);
    }

    @Override
    public void putTerm(@NotNull Termed t) {
        Object replaced = putItem(vector(t.term()), t.opRel(), t);
        if (replaced == null)
            count++;
    }

    @Override
    protected void putSubterms(TermContainer subterms) {
        putItem(subterms, SUBTERM_RELATION, subterms);
    }


    @Override
    public void clear() {
        count = 0;
        data.clear();
    }

    @Override
    public int subtermsCount() {
        return data.size();
    }

    @Override
    public int size() {
        /** WARNING: not accurate */
        return count;
    }



    @Override
    public void forEach(Consumer<? super Termed> c) {
        throw new RuntimeException("unimpl");
    }
}
