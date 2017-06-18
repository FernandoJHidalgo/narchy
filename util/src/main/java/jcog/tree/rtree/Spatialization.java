package jcog.tree.rtree;

import jcog.tree.rtree.split.AxialSplitLeaf;
import jcog.tree.rtree.split.LinearSplitLeaf;
import jcog.tree.rtree.split.QuadraticSplitLeaf;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class Spatialization<T> {

    public final Split<T> split;
    public final Function<T, HyperRegion> bounds;
    public final short max;       // max entries per node
    public final short min;       // least number of entries per node

    public Spatialization(@Deprecated final Function<T, HyperRegion> bounds, DefaultSplits split, final int min, final int max) {
        this(bounds, split.get(), min, max);
    }

    public Spatialization(@Deprecated final Function<T, HyperRegion> bounds, final Split<T> split, final int min, final int max) {
        this.max = (short) max;
        this.min = (short) min;
        this.bounds = bounds;
        this.split = split;
    }

    public HyperRegion region(@NotNull T t) {
        return bounds.apply(t);
    }

    public Node<T, T> newLeaf() {
        return new Leaf(max);
    }

    public Branch<T> newBranch() {
        return new Branch<>(max);
    }

    public Node<T, ?> split(T t, Leaf<T> leaf) {
        return split.split(t, leaf, this);
    }

    public double perimeter(T c) {
        return region(c).perimeter();
    }

    /**
     * Different methods for splitting nodes in an RTree.
     */
    public enum DefaultSplits {
        AXIAL {
            @Override
            public <T> Split<T> get() {
                return new AxialSplitLeaf<>();
            }
        },
        LINEAR {
            @Override
            public <T> Split<T> get() {
                return new LinearSplitLeaf<>();
            }
        },
        QUADRATIC {
            @Override
            public <T> Split<T> get() {
                return new QuadraticSplitLeaf<>();
            }
        };

        abstract public <T> Split<T> get();

    }
}
