package vandy.mooc.prime.utils;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.function.Function;

import static vandy.mooc.prime.utils.LaunderThrowable.launderThrowable;

/**
 * This class is based on an example in "Java Concurrency in Practice"
 * by Brian Goetz et al.  It shows how to create a "memoizing" cache
 * that uses a FutureTask to ensure only a single computation is run if an 
 */
public class Memoizer<K, V>
        implements Function<K, V> {
    /**
     *
     */
    private final ConcurrentMap<K, Future<V>> cache =
            new ConcurrentHashMap<>();

    /**
     *
     */
    private final Function<K, V> mFunction;

    /**
     * Constructor initializes the field.
     */
    public Memoizer(Function<K, V> function) {
        mFunction = function; 
    }

    public V apply(final K key) {
        // Keep looping as long as we keep getting interrupted.
        while (true) {
            // Try to find the key in the cache.
            Future<V> future = cache.get(key);

            // If the key isn't present then we'll need
            // to compute its value.
            if (future == null) {
                // Create a FutureTask whose run() method will compute
                // the value.
                FutureTask<V> futureTask =
                        new FutureTask<>(() -> mFunction.apply(key));

                // Atomically add futureTask to the cache
                // as the value associated with key.  
                future = cache.putIfAbsent(key, futureTask);

                // A value of null from put() indicates the key was
                // just added (i.e., it's the "first time in"), which
                // also indicates the value hasn't been computed yet.
                if (future == null) {
                    future = futureTask;

                    // Run futureTask to compute the value.
                    futureTask.run();
                }
            } else {
                System.out.println("value " 
                                   + key 
                                   + " was cached");
            }

            try {
                // Return the result of the future, which will block
                // if the futureTask hasn't finished running yet.
                return future.get();
            } catch (CancellationException e) {
                // Remove key from the cache and retry the while loop
                // from the beginning.
                cache.remove(key, future);
            } catch (Exception e) {
                throw launderThrowable(e.getCause());
            }
        }
    }
}
