package nars.concept.table;

import nars.NAR;
import nars.task.Task;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * Created by me on 5/7/16.
 */
public interface TemporalBeliefTable extends TaskTable {

    @Nullable Task strongest(long when, long now, @Nullable Task against);

    @Nullable Truth truth(long when, long now, EternalTable eternal);

    Task add(@NotNull Task input, EternalTable eternal, List<Task> displ, @NotNull NAR nar);

    boolean removeIf(@NotNull Predicate<? super Task> o, List<Task> displ);

    long minTime();
    long maxTime();

//    void minTime(long minT);
//    void maxTime(long maxT);

    void capacity(int c, List<Task> displaced);

    boolean isFull();
}