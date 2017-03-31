package vandy.mooc.prime.utils;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.function.Function;

import static vandy.mooc.prime.utils.LaunderThrowable.launderThrowable;

/**
 * This class defines a "memoizing" cache that maps a key to the value
 * produced by a function.  FutureTask is used to ensure only a single
 * call to the function is run when a key and value is first added to
 * the cache.  This code is based on an example in "Java Concurrency
 * in Practice" by Brian Goetz et al.  More information on memoization 
 * is available at https://en.wikipedia.org/wiki/Memoization.
 */
public class Memoizer<K, V> {
    /**
     * This map associates a key K with a value V that's produced by a
     * function.  A Future is used to ensure that the function is only
     * called once.
     */
    private final ConcurrentMap<K, Future<V>> cache =
            new ConcurrentHashMap<>();

    /**
     * This function produces a value based on the key.
     */
    private final Function<K, V> mFunction;

    /**
     * Constructor initializes the function field.
     */
    public Memoizer(Function<K, V> function) {
        mFunction = function; 
    }

    /**
     * Returns the value associated with the key in cache.  If there
     * is no value associated with the key then the function is called
     * to create the value and store it in the cache before returning
     * it.
     */
    public V get(final K key) {
        // Keep looping as long as we keep getting interrupted.
        while (true) {
            // Try to find the key in the cache.
            Future<V> future = cache.get(key);

            // If the key isn't present then we'll need to compute its
            // value.
            if (future == null) {
                // Create a FutureTask whose run() method will compute
                // the value and store it in the cache.
                FutureTask<V> futureTask =
                        new FutureTask<>(() -> mFunction.apply(key));

                // Atomically add futureTask to the cache as the value
                // associated with key.
                future = cache.putIfAbsent(key, futureTask);

                // A value of null from put() indicates the key was
                // just added (i.e., it's the "first time in"), which
                // also indicates the value hasn't been computed yet.
                if (future == null) {
                    // A FutureTask "isa" Future, so this assignment
                    // is fine.
                    future = futureTask;

                    // Run futureTask to compute the value, which is
                    // implicitly stored in the cache when the
                    // computation is finished.
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
