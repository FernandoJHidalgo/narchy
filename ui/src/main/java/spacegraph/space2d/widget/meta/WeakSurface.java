package spacegraph.space2d.widget.meta;

import spacegraph.space2d.Surface;
import spacegraph.space2d.container.EmptySurface;
import spacegraph.space2d.container.unit.AbstractUnitContainer;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

public class WeakSurface extends AbstractUnitContainer {

    private final Reference<Surface> the;

    public WeakSurface(Surface the) {
        this(the, true);
    }

    protected WeakSurface(Surface the, boolean weakOrSoft) {
        if (the == null)
            throw new NullPointerException();
        this.the = weakOrSoft ? new WeakReference<>(the) : new SoftReference<>(the);
    }

    @Override
    public boolean remove() {
        the.clear();
        return super.remove();
    }

    @Override
    public Surface the() {
        Surface s = the.get();
        if (s == null) {
            delete();
            return new EmptySurface();
        }
        return s;
    }

    protected void delete() {
        remove();
    }

}
