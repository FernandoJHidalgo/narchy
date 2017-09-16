package jcog.memoize;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

import java.util.function.Function;

import static jcog.Texts.n2;

public class CaffeineMemoize<K, V> implements Memoize<K, V> {

    private final Cache<K, V> cache;
    private final Function<K, V> func;

    public CaffeineMemoize(Cache<K, V> cache, Function<K, V> compute) {
        this.cache = cache;
        this.func = compute;
    }

    public static <K, V> CaffeineMemoize<K, V> build(Function<K, V> compute, int capacity, boolean stats) {
        Caffeine<Object, Object> b = Caffeine.newBuilder();

        if (capacity < 1)
            b.softValues();
        else
            b.maximumSize(capacity);

        if (stats)
            b.recordStats();
        return new CaffeineMemoize(b.build(), compute);
    }

    @Override
    public V apply(K k) {
        return cache.get(k, func);
    }

    @Override
    public String summary() {
        CacheStats stats = cache.stats();
        String a;
        if (stats.hitCount() > 0)
            a = n2(stats.hitRate() * 100f) + "% hits, ";
        else
            a = "";
        return a + cache.estimatedSize() + " size";
    }

    @Override
    public void clear() {
        cache.invalidateAll();
        cache.cleanUp();
    }
}
