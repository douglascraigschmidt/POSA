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
     * An object with ref count of 1 indicates its key hasn't been
     * accessed in mTimeoutInMillisecs.
     */
    private final RefCountedFutureTask<?> mNonAccessedValue =
            new RefCountedFutureTask<>(() -> null, 1);

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
         * A scheduled future that can be used to cancel a runnable
         * that has been scheduled to run at a fixed inteval to check
         * if this future task has becom stale and should be removed
         * from the cache (see scheduleAtFixedRateTimeout()).
         */
        ScheduledFuture<?> mScheduledFuture;

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

        /**
         * Use the ScheduledExecutorService to remove @a key from the
         * cache if its timeout expires and it hasn't been accessed in
         * mTimeoutInMillisecs.
         */
        public void schedule(K key) {
            // Create a runnable that will check if the cached entry
            // has become stale (i.e., not accessed within
            // mTimeoutInMillisecs) and if so will remove that entry.
            final Runnable removeIfStale = new Runnable() {
                    @Override
                    public void run() {
                        // Remove the key only if it hasn't been
                        // accessed in mTimeoutInMillisecs.
                        if (mCache.remove(key,
                                          mNonAccessedValue)) {
                            Log.d(TAG,
                                  "key "
                                  + key
                                  + " removed from cache since it wasn't accessed recently");
                        } else {
                            Log.d(TAG,
                                  "key "
                                  + key
                                  + " NOT removed from cache since it was accessed recently");
                            // Reset reference count so that it won't be
                            // considered as accessed (yet).
                            mRefCount.set(1);

                            // Reschedule this runnable to be run in
                            // mTimeoutInMillisecs.
                            mScheduledExecutorService.schedule
                                (this,
                                 mTimeoutInMillisecs,
                                 TimeUnit.MILLISECONDS);
                        }
                    }
                };

            // Schedule runnable to execute after mTimeoutInMillisecs.
            mScheduledExecutorService.schedule
                (removeIfStale,
                 mTimeoutInMillisecs,
                 TimeUnit.MILLISECONDS);
        }

       /**
         * Use the ScheduledExecutorService to remove @a key from the
         * cache if its timeout expires and it hasn't been accessed in
         * mTimeoutInMillisecs.
         */
        void scheduleAtFixedRateTimeout(K key) {
            Runnable cancelRunnable = () -> {
                // Remove the key only if it hasn't been accessed in
                // mTimeoutInMillisecs.
                if (mCache.remove(key, mNonAccessedValue)) {
                    Log.d(TAG,
                          "key "
                          + key
                          + " removed from cache since it wasn't accessed recently");
                    // Stop the ScheduledExecutorService from
                    // re-dispatching this runnable.
                    mScheduledFuture.cancel(true);
                } else {
                    Log.d(TAG,
                            "key "
                                    + key
                                    + " NOT removed from cache since it was accessed recently");

                    // Reset reference count so that it won't be
                    // considered as accessed (yet).
                    mRefCount.set(1);
                }
            };

            // Schedule runnable to execute repeatedly every
            // mTimeoutInMillisecs.
            mScheduledFuture =
                mScheduledExecutorService.scheduleAtFixedRate
                (cancelRunnable,
                 mTimeoutInMillisecs, // Initial timeout.
                 mTimeoutInMillisecs, // Periodic timeout.
                 TimeUnit.MILLISECONDS);
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
        else
            Log.d(TAG,
                  "key "
                  + key
                  + "'s value was retrieved from the cache");

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
                  + "'s value was added to the cache");
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

            if (!Thread.currentThread().isInterrupted()
                && mTimeoutInMillisecs > 0) 
                // Schedule removal of @a key from the cache if its
                // timeout expires and it hasn't been accessed in
                // mTimeoutInMillisecs.  Returns the future task
                futureTask.scheduleAtFixedRateTimeout(key);
                // Change this to future.Task.schedule(key) to 
                // try the alternative implementation.

            // Return the future.
            return futureTask;
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
