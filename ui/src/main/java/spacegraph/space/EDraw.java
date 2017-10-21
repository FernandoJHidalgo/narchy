package spacegraph.space;


import jcog.Util;
import jcog.pri.PLink;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.tuple.Tuples;
import spacegraph.SimpleSpatial;

/**
 * Drawn edge, lightweight
 */
public class EDraw<Y extends SimpleSpatial> extends PLink<Twin<Y>> {

    private final int hash;
    //TODO use pri as 'width' or 'a'
    public float width, r, g, b, a;

    /** additional attraction force multiplier */
    public float attraction = 1f;

    /** proportional to radius */
    public float attractionDist = 1f;

    public EDraw(Y src, Y target, float pri) {
        super(Tuples.twin(src,target), pri);
        this.hash = Util.hashCombine(src.id.hashCode(), target.id.hashCode());
    }

    public Y src() {
        return id.getOne();
    }
    public Y tgt() {
        return id.getTwo();
    }

    @Override
    public final int hashCode() {
        return hash;
    }
}
