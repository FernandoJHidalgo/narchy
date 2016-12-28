package nars.bag;

import org.jetbrains.annotations.Nullable;


public interface SortedTable<V, L> extends Table<V,L> {


    @Nullable L top();

    @Nullable L bottom();

}
