package nars.index.task;

import jcog.data.byt.ByteSeq;
import jcog.radixtree.MyConcurrentRadixTree;
import nars.Task;
import nars.index.term.tree.TermKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Created by me on 10/14/16.
 */
public class TreeTaskIndex extends TaskIndex {


    public final MyConcurrentRadixTree<Task> tasks = new MyConcurrentRadixTree<>();

    @Override
    public boolean contains(Task t) {
        return tasks.getValueForExactKey(key(t))!=null;
    }

    @Override
    public @Nullable final Task addIfAbsent(@NotNull Task x) {

        Task y = tasks.putIfAbsent(key(x), x);
        //return y == x ? null : y;
        if (y == x)
            return null;
        else {
//            if (!y.equals(x)) {
//                System.err.println("serialization inconsisency:\n" + x + "\t" + key(x) + "\n" + y + "\t" + key(y) );
//                System.out.println("\tarray equality=" + Arrays.equals(key(x).array(), key(y).array()));
//                Task z = tasks.putIfAbsent(key(x), x);
//            }
            return y;
        }
    }

    @Override
    protected final void removeInternal(@NotNull Task tt) {
        tasks.remove(key(tt));
    }

    @NotNull
    static ByteSeq key(@NotNull Task x) {
        return new TermKey(x);
    }


    @Override
    public void clear() {
        tasks.clear();
    }

    @Override
    public void forEach(@NotNull Consumer<Task> each) {
        tasks.forEach(each);
    }
}
