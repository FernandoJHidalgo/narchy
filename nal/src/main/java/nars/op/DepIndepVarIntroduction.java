package nars.op;

import nars.$;
import nars.Op;
import nars.term.Term;
import nars.term.Terms;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.ToIntFunction;

import static nars.Op.*;

/**
 * 1-iteration DepVar and IndepVar introduction that emulates and expands the original NAL6 Variable Introduction Rules
 */
public class DepIndepVarIntroduction extends VarIntroduction {

    public static final DepIndepVarIntroduction the = new DepIndepVarIntroduction();

    private final static int ConjOrStatementBits = Op.IMPL.bit | Op.CONJ.bit; 

    private final static int DepOrIndepBits = Op.VAR_INDEP.bit | Op.VAR_DEP.bit | Op.VAR_PATTERN.bit;

    /** sum by complexity if passes include filter */
    private static final ToIntFunction<Term> depIndepFilter = t -> {
        if (t.op().var) return 0;
        return t.hasAny(
                Op.VAR_INDEP.bit
                
                
                
        ) ? 0 : 1;
    };

    @Override
    public Pair<Term, Map<Term, Term>> apply(Term x, Random r) {
        if (x.hasAny(ConjOrStatementBits)) {
            return super.apply(x, r);
        } else
            return null;
    }

    @Override
    protected Term select(Term input, Random shuffle) {
        return Terms.nextRepeat(input, depIndepFilter, 2, shuffle);
    }

    @Nullable
    @Override
    protected Term introduce(Term input, Term selected, byte order) {

        if (selected==Imdex || selected==imInt || selected == imExt) 
            return null;

        Op inOp = input.op();
        List<ByteList> paths = $.newArrayList(1);
        int minPathLength = inOp.statement ? 2 : 0;
        input.pathsTo(selected, (path, t) ->  {
            if (path.size() >= minPathLength)
                paths.add(path.toImmutable());
            return true; 
        });
        int pSize = paths.size();
        
        if (pSize <= 1)
            return null;

        















        
        
        Term commonParent = input.commonParent(paths);
        Op commonParentOp = commonParent.op();

        
        boolean depOrIndep;
        switch (commonParentOp) {
            case CONJ:
                depOrIndep = true;
                break;
            case IMPL:
                depOrIndep = false;
                break;
            default:
                return null; 
                
        }


        ObjectByteHashMap<Term> m = new ObjectByteHashMap<>(0);
        for (int path = 0; path < pSize; path++) {
            ByteList p = paths.get(path);
            Term t = null; 
            int pathLength = p.size();
            for (int i = -1; i < pathLength-1 /* dont include the selected term itself */; i++) {
                t = (i == -1) ? input : t.sub(p.get(i));
                Op o = t.op();

                if (!depOrIndep && validIndepVarSuperterm(o)) {
                    byte inside = (byte) (1 << p.get(i + 1));
                    m.updateValue(t, inside, (previous) -> (byte) ((previous) | inside));
                } else if (depOrIndep && validDepVarSuperterm(o)) {
                    m.addToValue(t, (byte) 1);
                }
            }
        }


        if (!depOrIndep) {
            
            return (m.anySatisfy(b -> b == 0b11)) ?
                    $.v(VAR_INDEP, order) /*varIndep(order)*/ : null;

        } else {
            
            return m.anySatisfy(b -> b >= 2) ?
                    $.v(VAR_DEP, order)  /* $.varDep(order) */ : null;
        }

    }

    private static boolean validDepVarSuperterm(Op o) {
        return /*o.statement ||*/ o == CONJ;
    }

    private static boolean validIndepVarSuperterm(Op o) {
        return o.statement;
        
    }


}













