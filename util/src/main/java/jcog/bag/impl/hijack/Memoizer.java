package jcog.bag.impl.hijack;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Agnostic Cache for Method Invokation using Reflection
 * https://raw.githubusercontent.com/ggrandes/memoizer/master/src/main/java/org/javastack/memoizer/Memoizer.java
 * <p>
 * TODO integrate this with HijackMemoize
 */
class Memoizer implements InvocationHandler {
    /**
     * Default: 1024 elements
     */
    public static final int DEFAULT_CACHE_MAX_ELEMENTS = 1024;
    /**
     * Default: 1000millis
     */
    public static final long DEFAULT_CACHE_EXPIRE_MILLIS = 1000L; // 1 second

    private final Object object;
    private final Map<CacheKey, CacheValue> cache;
    private final long expireMillis;

    /**
     * Memoize object using default maxElement and default expireMillis
     *
     * @param origin object to speedup
     * @return proxied object
     * @see #memoize(Object, int, long)
     */
    public static Object memoize(final Object origin) //
    {
        return memoize(origin, DEFAULT_CACHE_MAX_ELEMENTS, DEFAULT_CACHE_EXPIRE_MILLIS);
    }

    /**
     * Memoize object
     *
     * @param origin       object to speedup
     * @param maxElements  limit elements to cache
     * @param expireMillis expiration time in millis
     * @return proxied object
     */
    public static Object memoize(final Object origin, //
                                 final int maxElements, final long expireMillis) //
    {
        final Class<?> clazz = origin.getClass();
        final Memoizer memoizer = new Memoizer(origin, maxElements, expireMillis);
        return Proxy.newProxyInstance(clazz.getClassLoader(), //
                clazz.getInterfaces(), memoizer);
    }

    private Memoizer(final Object object, final int size, final long expireMillis) {
        this.object = object;
        this.expireMillis = expireMillis;
        this.cache = allocCache(size);
    }

    private static final Map<CacheKey, CacheValue> allocCache(final int maxSize) {
        return Collections.synchronizedMap(new LinkedHashMap<CacheKey, CacheValue>() {
            private static final long serialVersionUID = 42L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<CacheKey, CacheValue> eldest) {
                return size() > maxSize;
            }
        });
    }

    /**
     * Internal method
     */
    @Override
    public Object invoke(final Object proxy, //
                         final Method method, //
                         final Object[] args) throws Throwable {
        if (method.getReturnType().equals(Void.TYPE)) {
            // Don't cache void methods
            return invoke(method, args);
        } else {
            final CacheKey key = new CacheKey(method, Arrays.asList(args));
            CacheValue cacheValue = cache.get(key);
            if (cacheValue != null) {
                final Object ret = cacheValue.getValueIfNotExpired();
                if (ret != CacheValue.EXPIRED) {
                    return ret;
                }
            }
            final Object ret = invoke(method, args);
            cache.put(key, new CacheValue(ret, expireMillis));
            return ret;
        }
    }

    private Object invoke(final Method method, final Object[] args) //
            throws Throwable {
        try {
            return method.invoke(object, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    private static final class CacheKey {
        private final Method method;
        private final List<Object> params;

        public CacheKey(final Method method, final List<Object> params) {
            this.method = method;
            this.params = params;
        }

        @Override
        public int hashCode() {
            return (method.hashCode() ^ params.hashCode());
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof CacheKey) {
                final CacheKey o = (CacheKey) obj;
                return o.method.equals(this.method) && //
                        o.params.equals(this.params);
            }
            return false;
        }
    }

    private static final class CacheValue {
        private static final Object EXPIRED = new Object();
        private final Object value;
        private final long expire;

        public CacheValue(final Object value, final long expire) {
            this.value = value;
            this.expire = System.currentTimeMillis() + expire;
        }

        public Object getValueIfNotExpired() {
            if (expire > System.currentTimeMillis()) {
                return value;
            }
            return EXPIRED;
        }
    }

        /**
     * https://github.com/ggrandes/memoizer/blob/master/src/main/java/org/javastack/memoizer/example/Example.java
     */
    static class Example {
        /**
         * Sample Interface to Memoize
         */
        public interface SampleInterface {
            String hash(final String in) throws NoSuchAlgorithmException;
        }

        /**
         * Sample Slow Implementation (MessageDigest with SHA-512)
         */
        public static class SampleSlowImpl implements SampleInterface {
            private static final Charset UTF8 = Charset.forName("UTF-8");

            @Override
            public String hash(final String in) throws NoSuchAlgorithmException {
                final MessageDigest md = MessageDigest.getInstance("SHA-512");
                final byte[] buf = md.digest(in.getBytes(UTF8));
                return Base64.getEncoder().encodeToString(buf);
            }
        }

        private static final String getHeader(final Class<?> b1, //
                                              final Class<?> b2) {
            final String s1 = b1.getSimpleName();
            final String s2 = b2.getSimpleName();
            if (s1.equals(s2))
                return s1 + ":direct";
            return s1 + ":memoize";
        }

        /**
         * Simple Test / Benchmark
         */
        public static void main(final String[] args) throws NoSuchAlgorithmException {
            final int TOTAL = (int) 1e6;
            final String TEST_TEXT = "hello world";
            final int cacheElements = 1024;
            final long cacheMillis = 1000; // 1 second
            final SampleInterface[] samples = {
                    new SampleSlowImpl(), //
                    (SampleInterface) Memoizer.memoize(new SampleSlowImpl(), cacheElements, cacheMillis)
            };
            //
            for (int k = 0; k < samples.length; k++) {
                final SampleInterface base = samples[k & ~1];
                final SampleInterface test = samples[k];
                final String hdr = getHeader(base.getClass(), test.getClass());
                long ts = System.currentTimeMillis();
                for (int i = 0; i < TOTAL; i++) {
                    test.hash(TEST_TEXT);
                }
                long diff = System.currentTimeMillis() - ts;
                System.out.println(hdr + '\t' + "diff=" + diff + "ms" + '\t' + //
                        test.hash(TEST_TEXT));
            }
        }
    }


}
