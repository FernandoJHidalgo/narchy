package jcog.data.graph;

import jcog.Util;

/**
 * immutable directed edge with cached hashcode
 */
public class ImmutableDirectedEdge<N, E> implements FromTo<Node<N,E>, E> {

    private final int hash;
    public final E id;
    public final Node<N,E> from, to;

    public ImmutableDirectedEdge(Node<N,E> from, Node<N,E> to, E id) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.hash = Util.hashCombine(id.hashCode(), from.hashCode(), to.hashCode());
    }

    @Override
    public final Node<N,E> from() {
        return from;
    }

    @Override
    public final E what() {
        return id;
    }

    @Override
    public final Node<N,E> to() {
        return to;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (hash!=obj.hashCode() || !(obj instanceof FromTo)) return false;
        ImmutableDirectedEdge ee = (ImmutableDirectedEdge) obj;
        return from == ee.from && to == ee.to && id.equals(ee.id);
    }

    public boolean isSelfLoop() {
        return from == to;
    }

    @Override
    public String toString() {
        return from + " => " + id + " => " + to;
    }


}
