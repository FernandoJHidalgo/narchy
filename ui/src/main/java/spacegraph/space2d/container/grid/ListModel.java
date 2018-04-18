package spacegraph.space2d.container.grid;

import jcog.TODO;
import org.jetbrains.annotations.Nullable;

import java.util.List;

abstract public class ListModel<X> implements GridModel<X> {

    /** orientation, dynamically changeable. true=vertical, false=horizontal. default=vertical */
    public boolean vertical = true;

    private ScrollGrid<X> surface;

    public static <X> ListModel<X> of(X... items) {
        return of(List.of(items));
    }

    public static <X> ListModel<X> of(List<X> items) {
        return new ListModel<>() {

            @Override
            public X get(int index) {
                try {
                    return items.get(index);
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.err.println("warning: " + e.getMessage());
                    return null;
                }
            }

            @Override
            public int size() {
                return items.size();
            }
        };
    }

    @Override
    public synchronized void start(ScrollGrid<X> x) {
        if (surface!=null)
            throw new TODO("support multiple observers");
        this.surface = x;
    }

    @Override
    public void stop() {
        this.surface = null;
    }

    public void onChange() {
        surface.refresh();
    }

    public void setOrientation(boolean vertical) {
        this.vertical = vertical;
    }

    abstract public X get(int index);
    abstract public int size();

    @Override
    public final int cellsX() {
        return vertical ? 1 : size();
    }

    @Override
    public final int cellsY() {
        return vertical ? size() : 1;
    }

    @Nullable
    @Override
    public final X get(int x, int y) {
        if ((vertical ? x : y) != 0)
            return null;
        return get(vertical ? y : x);
    }
}