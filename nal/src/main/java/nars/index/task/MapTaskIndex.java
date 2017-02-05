package nars.index.task;

import nars.Task;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Created by me on 8/14/16.
 */
public final class MapTaskIndex implements TaskIndex {

    final static Logger logger = LoggerFactory.getLogger(MapTaskIndex.class);

    @NotNull
    protected final Map<Task, Task> tasks;

    public MapTaskIndex(boolean concurrent) {
        this(concurrent, 1);
    }

    public MapTaskIndex(boolean concurrent, int initialCapacity) {
        this.tasks =
                concurrent ?
                    new ConcurrentHashMap<>(initialCapacity/* estimate TODO */):
                        //new ConcurrentHashMapUnsafe(128 * 1024 /* estimate TODO */);
                    new HashMap<>(initialCapacity);

        //Caffeine.newBuilder()

//                .removalListener((k,v,cause) -> {
//                    if (cause != RemovalCause.EXPLICIT)
//                        logger.error("{} removal: {},{}", cause, k, v);
//                })

//                .build();
//        tasks.cleanUp();
//
//        this.tasksMap = tasks.asMap();
    }

    @Override
    public final boolean contains(Task t) {
        return tasks.containsKey(t);
    }

    public void removeDeleted() {
        Iterator<Task> ii = tasks.values().iterator();
        while (ii.hasNext()) {
            Task x = ii.next();
            if (x.isDeleted()) {
                logger.error("lingering deleted task: {}", x);
                ii.remove();
            }
        }
    }


    @Override
    public Task addIfAbsent(@NotNull Task x) {
        return tasks.putIfAbsent(x,x);
    }


    @Override
    public final void removeInternal(@NotNull Task tt) {
        tasks.remove(tt);
    }

    @Override
    public void clear() {
        tasks.clear();
    }

    @Override
    public void forEach(@NotNull Consumer<Task> each) {
        tasks.forEach((k,v)->each.accept(v));
    }
}
