package jcog.event;

import jcog.util.CountDownThenRun;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * arraylist implementation, thread safe.  creates an array copy on each update
 * for fastest possible iteration during emitted events.
 */
public class ListTopic<V> extends jcog.list.FastCoWList<Consumer<V>> implements Topic<V> {

    final CountDownThenRun busy = new CountDownThenRun();

    public ListTopic() {
        this(8);
    }

    public ListTopic(int capacity) {
        super(capacity, Consumer[]::new);
    }

    @Override
    public final void emit(V x) {
        final Consumer[] cc = this.copy;
        //if (cc!=null) {
        for (Consumer c : cc)
            c.accept(x);
        //}
    }

    @Override
    public void emitAsync(V x, Executor executorService) {
        final Consumer[] cc = this.copy;
        if (cc != null) {
            for (Consumer c : cc)
                executorService.execute(() -> c.accept(x));
        }
    }

    @Override
    public void emitAsyncAndWait(V x, Executor executorService) throws InterruptedException {
        final Consumer[] cc = this.copy;
        if (cc != null) {
            int n = cc.length;
            switch (n) {
                case 0:
                    return;
                default:
                    CountDownLatch l = new CountDownLatch(n);

                    for (Consumer c : cc) {
                        executorService.execute(() -> {
                            try {
                                c.accept(x);
                            } finally {
                                l.countDown();
                            }

                        });
                    }
                    l.await();
                    break;
            }
        }
    }

    @Override
    public void emitAsync(V x, Executor exe, Runnable onFinish) {
        final Consumer[] cc = this.copy;
        int n;
        if (cc != null && (n=cc.length) > 0) {
            busy.reset(n, onFinish);

            for (Consumer c : cc)
                exe.execute(busy.run(c, x));
        }
    }

// TODO
//    public void emitAsync(V x, Consumer<Iterable<Consumer<V>>> executorService) {
//        final Consumer[] cc = this.copy;
//        if (cc!=null) {
//            for (Consumer c : cc)
//                executorService.execute(() -> c.accept(x));
//        }
//    }

    @Override
    public final void enable(Consumer<V> o) {
        assert (o != null);
        add(o);
    }

    @Override
    public final void disable(Consumer<V> o) {
        assert (o != null);
        remove(o);
    }


}