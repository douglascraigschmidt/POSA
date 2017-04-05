package vandy.mooc.prime.utils;

import android.util.Log;

import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.function.Function;

import static vandy.mooc.prime.utils.LaunderThrowable.launderThrowable;

/**
 * This class defines a "memoizing" cache that maps a key to the value
 * produced by a function.  If a value has previously been computed it
 * is returned rather than calling the function to compute it again.
 * The Java FutureTask class is used to ensure only a single call to
 * the function is run when a key and value is first added to the
 * cache.  The ScheduledExecutorService is used to limit the amount of
 * time a key/value is retained in the cache.  This code is based on
 * an example in "Java Concurrency in Practice" by Brian Goetz et al.
 * More information on memoization is available at
 * https://en.wikipedia.org/wiki/Memoization.
 */
public class Memoizer<K, V>
       implements Function<K, V> {
    /**
     * Debugging tag used by the Android logger.
     */
    protected final String TAG =
        getClass().getSimpleName();

    /**
     * This map associates a key K with a value V that's produced by a
     * function.  A Future is used to ensure that the function is only
     * called once.
     */
    private final ConcurrentMap<K, Future<V>> mCache =
            new ConcurrentHashMap<>();

    /**
     * This function produces a value based on the key.
     */
    private final Function<K, V> mFunction;

    /**
     * The amount of time to retain a value in the cache.
     */
    private final long mTimeoutInMillisecs;

    /**
     * Executor service that executes runnable after a given timeout
     * to remove expired keys.
     */
    private ScheduledExecutorService mScheduledExecutorService = 
        Executors.newScheduledThreadPool(1);

    /**
     * Constructor initializes the fields.
     */
    public Memoizer(Function<K, V> function,
                    long timeoutInMillisecs) {
        mFunction = function; 
        mTimeoutInMillisecs = timeoutInMillisecs;
    }

    /**
     * Returns the value associated with the key in cache.  If there
     * is no value associated with the key then the function is called
     * to create the value and store it in the cache before returning
     * it.
     */
    public V apply(final K key) {
        // Try to find the key in the cache.
        Future<V> future = mCache.get(key);

        // If the key isn't present we must compute its value.
        if (future == null) {
            // Create a FutureTask whose run() method will compute
            // the value and store it in the cache.
            final FutureTask<V> futureTask =
                new FutureTask<>(() -> mFunction.apply(key));

            // Atomically add futureTask to the cache as the value
            // associated with key.
            future = mCache.putIfAbsent(key, futureTask);

            // A value of null from put() indicates the key was
            // just added (i.e., it's the "first time in"), which
            // also indicates the value hasn't been computed yet.
            if (future == null) {
                // A FutureTask "isa" Future, so this assignment
                // is fine.
                future = futureTask;

                // Run futureTask to compute the value, which is
                // (implicitly) stored in the cache when the
                // computation is finished.
                futureTask.run();

                // Don't schedule a cleanup if the futureTask was
                // interrupted or the timeout value is 0.
                if (!Thread.currentThread().isInterrupted()
                    && mTimeoutInMillisecs > 0) {
                    // Runnable that when executed will remove the
                    // futureTask from the cache when its timeout
                    // expires.
                    final Runnable cleanupCache = () -> {
                        // Only remove key if it is currently
                        // associated with futureTask.
                        if (mCache.remove(key,
                                          futureTask))
                            ;
                            /*
                            Log.d(TAG,
                                  "key " 
                                  + key 
                                  + " removed from cache upon timeout");
                            */
                        else
                            Log.d(TAG, 
                                  "key "
                                  + key 
                                  + " NOT removed from cache upon timeout"
                                  + " for futureTask "
                                  + futureTask);
                    };

                    // Schedule the cleanupCache to execute after the
                    // given timeout.
                    mScheduledExecutorService.schedule(cleanupCache,
                                                       mTimeoutInMillisecs,
                                                       TimeUnit.MILLISECONDS);
                }
            }
        } else 
            System.out.println("value " 
                               + key 
                               + " was cached");

        try {
            // Return the result of the future, which will block
            // if the futureTask hasn't finished running yet.
            return future.get();
        } catch (Exception e) {
            // Try to remove key from the cache.
            if (mCache.remove(key,
                              future))
                Log.d(TAG, "key " + key + " removed from cache upon exception");
            else
                Log.d(TAG, "key " + key + " NOT removed from cache upon exception");

            // Rethrow the exception.
            throw launderThrowable(e.getCause());
        }
    }
}
