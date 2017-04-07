package vandy.mooc.prime.utils;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicLong;
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
public class TimedMemoizer<K, V>
       implements Function<K, V> {
    /**
     * Debugging tag used by the Android logger.
     */
    protected final String TAG =
        getClass().getSimpleName();

    /**
     * This map associates a key K with a value V that's produced by a
     * function.  A RefCountedFutureTask is used to ensure that the
     * function is only called once.
     */
    private final ConcurrentMap<K, RefCountedFutureTask<V>> mCache =
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
     * Executor service that executes a runnable after a given timeout
     * to remove expired keys.
     */
    private ScheduledExecutorService mScheduledExecutorService = 
        Executors.newScheduledThreadPool(1);

    /**
     * Extends FutureTask to keep track of the number of times a key
     * is referenced within mTimeoutInMillisecs.
     */
    private class RefCountedFutureTask<V>
            extends FutureTask<V> {
        /**
         * Keeps track of the number of times a key is referenced
         * within mTimeoutInMillisecs.
         */
        final AtomicLong mRefCount;

        /**
         * Constructor initializes the superclass and field.
         */
        public RefCountedFutureTask(Callable<V> callable,
                                    long initialCount) {
            super(callable);
            mRefCount = new AtomicLong(initialCount);
        }

        /**
         * Returns true if the ref counts are equal, else false.
         */
        @Override
        public boolean equals(Object obj) {
            final RefCountedFutureTask<V> t = (RefCountedFutureTask<V>) obj;
            return mRefCount.get() == t.mRefCount.get();
        }

        /**
         * Waits if necessary for the computation to complete, and
         * then retrieves its result.  Also increments the ref count
         * atomically.
         */
        @Override
        public V get() throws ExecutionException, InterruptedException {
            // Call the super get(), which blocks until the value is
            // computed.
            V value = super.get();

            // Increment ref count atomically.
            mRefCount.getAndIncrement();

            // Return the value;
            return value;
        }
    }

    /**
     * Constructor initializes the fields.
     */
    public TimedMemoizer(Function<K, V> function,
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
        RefCountedFutureTask<V> future = mCache.get(key);

        // If the key isn't present we must compute its value.
        if (future == null) {
            // Create a RefCountedFutureTask whose run() method will
            // compute the value and store it in the cache.
            final RefCountedFutureTask<V> futureTask =
                new RefCountedFutureTask<>(() -> mFunction.apply(key),
                                           0);

            // Atomically add futureTask to the cache as the value
            // associated with key.
            future = mCache.putIfAbsent(key, futureTask);

            // A value of null from put() indicates the key was
            // just added (i.e., it's the "first time in"), which
            // also indicates the value hasn't been computed yet.
            if (future == null) {
                // A RefCountedFutureTask "isa" Future, so this
                // assignment is fine.
                future = futureTask;

                // Run futureTask to compute the value, which is
                // (implicitly) stored in the cache when the
                // computation is finished.
                futureTask.run();

                // Don't schedule a cleanup if the futureTask was
                // interrupted or the timeout value is 0.
                if (!Thread.currentThread().isInterrupted()
                    && mTimeoutInMillisecs > 0) {
                    // An object with ref count of 1 indicates its key
                    // hasn't been accessed in mTimeoutInMillisecs.
                    final RefCountedFutureTask<V> nonAccessedValue =
                        new RefCountedFutureTask<>(() -> mFunction.apply(key),
                                                   1);

                    /*
                      This class is needed to decouple scheduling of a runnable from
                      the actual logic invoked when the runnable is dispatched.
                     */
                    class DelegatingRunnable
                          implements Runnable {
                        private Runnable mActualRunnable;

                        /**
                         * Delegate to the underlying runnable.
                         */
                        @Override
                        public void run() {
                            mActualRunnable.run();
                        }
                    }

                    // Create a DelegatingRunnable so it can be
                    // registered with the ScheduledExecutorService.
                    final DelegatingRunnable delegatingRunnable =
                            new DelegatingRunnable();

                    // Schedule the delegatingRunnable to execute
                    // after the given timeout.  This runnable keeps
                    // executing periodically as long as the key is
                    // accessed within mTimeoutInMillisecs.
                    final ScheduledFuture<?> cancellableFuture =
                        mScheduledExecutorService.scheduleAtFixedRate
                            (delegatingRunnable,
                             mTimeoutInMillisecs,
                             mTimeoutInMillisecs,
                             TimeUnit.MILLISECONDS);

                    // Runnable that when executed will remove the
                    // futureTask from the cache when its timeout
                    // expires and it hasn't been accessed in
                    // mTimeoutInMillisecs.
                    delegatingRunnable.mActualRunnable = () -> {
                        // Remove the key only if it hasn't been
                        // accessed in mTimeoutInMillisecs.
                        if (mCache.remove(key,
                                          nonAccessedValue)) {
                            Log.d(TAG,
                                  "key "
                                  + key
                                  + " removed from cache upon timeout");
                            // Cancel the delegatingRunnable.
                            cancellableFuture.cancel(true);
                        } else {
                            Log.d(TAG,
                                  "key "
                                  + key
                                  + " NOT removed from cache upon timeout"
                                  + " since ref count "
                                  + futureTask.mRefCount.get()
                                  + " indicates recent access");
                            // Reset the key's value so it won't be
                            // considered as accessed (yet).
                            mCache.replace(key,
                                           nonAccessedValue);
                        }
                    };
                }
            }
        } else 
            System.out.println("value " 
                               + key 
                               + " was cached");

        try {
            // Get the result of the future, which will block if the
            // futureTask hasn't finished running yet.
            return future.get();
        } catch (Exception e) {
            // Unilaterally remove the key from the cache when an
            // exception occurs.
            if (mCache.remove(key) != null)
                Log.d(TAG, "key " + key + " removed from cache upon exception");
            else
                Log.d(TAG, "key " + key + " NOT removed from cache upon exception");

            // Rethrow the exception.
            throw launderThrowable(e.getCause());
        }
    }
}