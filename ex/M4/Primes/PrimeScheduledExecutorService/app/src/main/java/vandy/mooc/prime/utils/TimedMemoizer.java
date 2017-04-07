package vandy.mooc.prime.utils;

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
 * cache.  The Java ScheduledExecutorService is used to limit the
 * amount of time a key/value is retained in the cache.  This code is
 * based on an example in "Java Concurrency in Practice" by Brian
 * Goetz et al.  More information on memoization is available at
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
        RefCountedFutureTask(Callable<V> callable,
                                    long initialCount) {
            super(callable);
            mRefCount = new AtomicLong(initialCount);
        }

        /**
         * Returns true if the ref counts are equal, else false.
         */
        @Override
        public boolean equals(Object obj) {
            if (getClass() != obj.getClass())
                return false;
            else {
                @SuppressWarnings("unchecked")
                final RefCountedFutureTask<V> t = (RefCountedFutureTask<V>) obj;
                return mRefCount.get() == t.mRefCount.get();
            }
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
     * it.  A key/value entry will be purged from the cache if it's
     * not used within the timeout passed to the constructor.
     */
    public V apply(K key) {
        // Try to find the key in the cache.
        RefCountedFutureTask<V> future = mCache.get(key);

        // If the key isn't present then compute its value.
        if (future == null)
            future = computeValue(key);

        // Return the value of the future, blocking until it's
        // computed.
        return getFutureValue(key, future);
    }

    /**
     * Compute the value associated with the key and return a
     * unique RefCountedFutureTask associated with it.
     */
    private RefCountedFutureTask<V> computeValue(K key) {
        // Create a RefCountedFutureTask whose run() method will
        // compute the value and store it in the cache.
        final RefCountedFutureTask<V> futureTask =
            new RefCountedFutureTask<>(() -> mFunction.apply(key),
                                       0);

        // Atomically try to add futureTask to the cache as the value
        // associated with key.
        RefCountedFutureTask<V> future =
            mCache.putIfAbsent(key, futureTask);

        // If future != null the value was already in the cache, so
        // just return it.
        if (future != null) {
            Log.d(TAG, 
                  "key " 
                  + key 
                  + "'s value was retrieved from the cache");
            return future;
        }

        // A value of null from put() indicates the key was just added
        // (i.e., it's the "first time in"), which indicates the value
        // hasn't been computed yet.
        else {
            // Run futureTask to compute the value, which is
            // (implicitly) stored in the cache when the computation
            // is finished.
            futureTask.run();

            // Use the ScheduledExecutorService to remove @a key from
            // the cache if its timeout expires and it hasn't been
            // accessed in mTimeoutInMillisecs.
            scheduleTimeout(key);

            // Return the future.
            return futureTask;
        }
    }

    /**
     * Use the ScheduledExecutorService to remove @a key from the
     * cache if its timeout expires and it hasn't been accessed in
     * mTimeoutInMillisecs.
     */
    private void scheduleTimeout(K key) {
        // Don't schedule a cleanup if the futureTask was interrupted
        // or the timeout value is 0.
        if (!Thread.currentThread().isInterrupted()
            && mTimeoutInMillisecs > 0) {
            // An object with ref count of 1 indicates its key hasn't
            // been accessed in mTimeoutInMillisecs.
            final RefCountedFutureTask<V> nonAccessedValue =
                new RefCountedFutureTask<>(() -> mFunction.apply(key),
                                           1);

            /*
              Decouples scheduling of a runnable from the logic
              invoked when the runnable is dispatched.
            */
            class DelegatingRunnable
                implements Runnable {
                /**
                 * The actual runnable that is delegated to by
                 * the run() hook method below.
                 */
                Runnable mActualRunnable;

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

            // Schedule the delegatingRunnable to execute after the
            // given timeout.  This runnable keeps executing
            // periodically as long as the key is accessed within
            // mTimeoutInMillisecs.
            final ScheduledFuture<?> cancellableFuture =
                mScheduledExecutorService.scheduleAtFixedRate
                (delegatingRunnable,
                 mTimeoutInMillisecs, // Initial timeout
                 mTimeoutInMillisecs, // Periodic timeout
                 TimeUnit.MILLISECONDS);

            // Runnable that when executed will remove the key and
            // futureTask from the cache if its timeout expires and it
            // hasn't been accessed in mTimeoutInMillisecs.
            delegatingRunnable.mActualRunnable = () -> {
                // Remove the key only if it hasn't been accessed in
                // mTimeoutInMillisecs.
                if (mCache.remove(key,
                                  nonAccessedValue)) {
                    Log.d(TAG,
                          "key "
                          + key
                          + " removed from cache since it hasn't been accessed recently");
                    // Stop the ScheduledExecutorService from
                    // dispatching the delegatingRunnable.
                    cancellableFuture.cancel(true);
                } else {
                    Log.d(TAG,
                          "key "
                          + key
                          + " NOT removed from cache since its been accessed recently");
                    // Reset the key's value so it won't be considered
                    // as accessed (yet).
                    mCache.replace(key,
                                   nonAccessedValue);
                }
            };
        }
    }

    /**
     * Return the value of the future, blocking until it's computed.
     */
    private V getFutureValue(K key,
                             RefCountedFutureTask<V> future) {
        try {
            // Get the result of the future, which will block if the
            // futureTask hasn't finished running yet.
            return future.get();
        } catch (Exception e) {
            // Unilaterally remove the key from the cache when an
            // exception occurs.
            if (mCache.remove(key) != null)
                Log.d(TAG,
                      "key "
                      + key 
                      + " removed from cache upon exception");
            else
                Log.d(TAG,
                      "key "
                      + key 
                      + " NOT removed from cache upon exception");

            // Rethrow the exception.
            throw launderThrowable(e.getCause());
        }
    }
}
