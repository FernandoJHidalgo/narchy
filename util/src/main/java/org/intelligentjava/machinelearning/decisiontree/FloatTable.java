package org.intelligentjava.machinelearning.decisiontree;

import com.google.common.base.Joiner;
import jcog.Texts;
import jcog.list.FasterList;
import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;
import java.util.List;

/**
 * table of float[]'s of uniform length, each column labeled by a unique header (H)
 */
public class FloatTable<H> {

    public final List<float[]> rows = new FasterList();
    public final H[] cols;

    public FloatTable(@NotNull H... cols) {
        this.cols = cols;
    }

    public FloatTable<H> add(float... row) {
        assert (row.length == cols.length);
        rows.add(row);
        return this;
    }

    public FloatTable<H> print(PrintStream out) {
        System.out.println(Joiner.on("\t").join(cols));
        rows.stream().map(Texts::n4).forEach(out::println);
        return this;
    }

    public int size() {
        return rows.size();
    }
}
