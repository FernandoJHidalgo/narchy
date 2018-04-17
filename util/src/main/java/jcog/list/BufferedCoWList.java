package jcog.list;

import jcog.TODO;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.IntFunction;

public class BufferedCoWList<X> extends FastCoWList<X> {

    private final ConcurrentLinkedQueue<ObjectBooleanPair<X>> q;

    public BufferedCoWList(int initialCap, IntFunction<X[]> arrayBuilder) {
        super(initialCap, arrayBuilder);
        q = new ConcurrentLinkedQueue<ObjectBooleanPair<X>>();
    }

    @Override
    public boolean add(X o) {
        q.add(PrimitiveTuples.pair(o, true));
        return true;
    }

    @Override
    public boolean remove(Object o) {
        q.add(PrimitiveTuples.pair((X)o, false));
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends X> source) {
        throw new TODO();
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty();
                //&& q.isEmpty() /* to be certain */;
    }

    @Override
    public void clear() {
        synchronized (this) {
            super.clear();
            q.clear();
        }
    }

    @Override
    public void forEach(Consumer c) {
        commit();
        super.forEach(c);
    }

    @Override
    public Iterator<X> iterator() {
        commit();
        return super.iterator();
    }

    @Override
    public void reverseForEach(Procedure procedure) {
        commit();
        super.reverseForEach(procedure);
    }

    public void commit() {
        if (q.isEmpty())
            return;
        synchronized (this) {
           q.removeIf(x -> {
              if (x.getTwo()) {
                  addDirect(x.getOne());
              } else {
                  removeDirect(x.getOne());
              }
              return true;
           });
           super.commit();
        }
    }

}
