package jcog.event;

import jcog.list.FasterList;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**  arraylist implementation, thread safe.  creates an array copy on each update
 *   for fastest possible iteration during emitted events. */
public class ListTopic<V> extends FasterList<Consumer<V>> implements Topic<V> {

    private Consumer[] copy = EMPTY;

    public ListTopic() {
        super(8);
    }

    @Override
    public final void emit(V x) {
        final Consumer[] cc = this.copy;
        for (Consumer c : cc)
            c.accept(x);
    }

    @Override
    public void emitAsync(V x, Executor executorService) {
        final Consumer[] cc = this.copy;
        for (Consumer c : cc)
            executorService.execute(()->c.accept(x));
    }

    @Override public final synchronized void enable(Consumer<V> o) {
        if(this.add(o)) {
            commit();
        }
    }

    @Override public final synchronized void disable(Consumer<V> o) {
        if(this.remove(o)) {
            commit();
        }
    }

    private final void commit() {
        this.copy = (size == 0) ? EMPTY :
                                  toArray(new Consumer[size]);
    }

    private static final Consumer[] EMPTY = new Consumer[0];


}