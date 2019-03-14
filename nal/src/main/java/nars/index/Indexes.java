package nars.index;

import javassist.scopedpool.SoftValueHashMap;
import nars.nar.util.DefaultConceptBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * yes i know plural is 'indices' but Indexes seems better
 */
public enum Indexes {
    ;


    //    public static class DefaultTermIndex extends MapIndex2 {
    //
    //        public DefaultTermIndex(int capacity, @NotNull Random random) {
    //            super(new HashMap(capacity, 0.9f),
    //                    new DefaultConceptBuilder(random, 8, 24));
    //
    //        }
    //    }
        public static class DefaultTermIndex extends MapIndex {

            public DefaultTermIndex(int capacity, @NotNull Random random) {
                super(
                        new DefaultConceptBuilder(random),
                        new ConcurrentHashMap<>(capacity),
                        new ConcurrentHashMap<>(capacity)
                        //new ConcurrentHashMapUnsafe(capacity)
                );
            }
        }

//    public static class WeakTermIndex2 extends GroupedMapIndex {
//
//            public WeakTermIndex2(int capacity, @NotNull Random random) {
//                super(new WeakHashMap<>(capacity),
//                        new DefaultConceptBuilder(random));
//
//            }
//        }

    public static class WeakTermIndex extends MapIndex {

            public WeakTermIndex(int capacity, @NotNull Random random) {
                super(
                        new DefaultConceptBuilder(random),
                        //new SoftValueHashMap(capacity)
                        new WeakHashMap<>(capacity),
                        new WeakHashMap<>(capacity)
                );

            }
        }

    public static class SoftTermIndex extends MapIndex {

            public SoftTermIndex(int capacity, @NotNull Random random) {
                super(
                        new DefaultConceptBuilder(random),
                        new SoftValueHashMap(capacity),
                        new WeakHashMap(capacity)
                        //new WeakHashMap<>(capacity)
                );

            }
        }
}