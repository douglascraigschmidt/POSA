package vandy.mooc.prime.utils;

import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * This class defines a "memoizing" cache that maps a key to the value
 * produced by a function.  If a value has previously been computed it
 * is returned rather than calling the function to compute it again.
 * The ConcurrentHashMap computeIfAbsent() method is used to ensure
 * only a single call to the function is run when a key/value pair is
 * first added to the cache.  The Java ScheduledExecutor class is used
 * to scalably limit the amount of time a key/value is retained in the
 * cache.  This code is based on an example in "Java Concurrency in
 * Practice" by Brian Goetz et al.  More information on memoization is
 * available at https://en.wikipedia.org/wiki/Memoization.
 */
public class TimedMemoizerEx<K, V>
        implements Function<K, V> {
    /**
     * Debugging tag used by the Android logger.
     */
    private final String TAG =
            getClass().getSimpleName();

    /**
     * This function produces a value based on the key.
     */
    private final Function<K, V> mFunction;

    /**
     * The amount of time to retain a value in the cache.
     */
    private final long mTimeoutInMillisecs;

    /**
     * Records the number of times a key/value is referenced
     * (if at all) within mTimeoutInMillisecs.
     */
    private class RefCountedValue {
        /**
         * Records the number of times a key is referenced
         * within mTimeoutInMillisecs.
         */
        final AtomicLong mRefCount;

        /**
         * The value that's being reference counted.
         */
        final V mValue;

        /**
         * Constructor initializes the fields.
         */
        RefCountedValue(V value, long initialCount) {
            mValue = value;
            mRefCount = new AtomicLong(initialCount);
        }

        /**
         * @return True if the ref counts are equal, else false.
         */
        @Override
        public boolean equals(Object obj) {
            if (getClass() != obj.getClass())
                return false;
            else {
                @SuppressWarnings("unchecked") final RefCountedValue t =
                        (RefCountedValue) obj;
                return mRefCount.get() == t.mRefCount.get();
            }
        }

        /**
         * Increments the ref count atomically and
         * return the current value.
         *
         * @return The current value.
         */
        V get() {
            // Increment ref count atomically.
            mRefCount.getAndIncrement();

            // Return the value;
            return mValue;
        }
    }

    /**
     * A constant whose ref count of 1 indicates its key hasn't been
     * accessed in mTimeoutInMillisecs.
     */
    private final RefCountedValue mNonAccessedValue =
            new RefCountedValue(null, 1);

    /**
     * This map associates a key K with a value V that's produced by a
     * function.  A RefCountedValue is used to keep track of how many
     * times a key/value pair is accessed.
     */
    private final Map<K, RefCountedValue> mCache =
            new ConcurrentHashMap<>();

    /**
     * Keeps track of the number of entries in mCache so mPurgeEntries
     * can be properly scheduled and cancelled.
     */
    private final ThresholdCrosser mCacheCount =
            new ThresholdCrosser(0);

    /**
     * This scheduled future is used to cancel mPurgeEntries, which
     * runs at a fixed interval to check if entries in the map have
     * become stale and should be removed.
     */
    private ScheduledFuture<?> mScheduledFuture;

    /**
     * This runnable is scheduled to run periodically by the
     * ScheduledExecutorService to purge entries in the map that
     * haven't been accessed in mTimeoutInMillisecs.
     */
    private final Runnable mPurgeEntries = () -> {
        Log.d(TAG,
                "start the purge of keys not accessed recently");

        // Iterate over all the keys in the map and purge those not
        // accessed recently.  This iterator is only called by the one
        // thread running ScheduledThreadPoolExecutor.
        mCache.forEach((key, value) -> {
            // Store the current ref count.
            long oldCount = value.mRefCount.get();

            // If the entry has not been accessed within
            // mTimeoutInMillisecs then atomically remove it.
            if (mCache.remove(key, mNonAccessedValue)) {
                Log.d(TAG,
                        "key "
                                + key
                                + " removed from cache ("
                                + mCache.size()
                                + ") since it wasn't accessed recently");

                // Decrement the count of cached entries by one, which
                // will invoke the lambda when the count drops to 0.
                mCacheCount.decrementAndCallAtN
                        (0, () -> {
                            // If there are no entries in the cache cancel
                            // mPurgeEntries from being called henceforth.
                            mScheduledFuture.cancel(true);
                            Log.d(TAG,
                                    "cancelling mPurgeEntries");
                        });
            } else {
                // Entry was accessed within mTimeoutInMillisecs,
                // so update its reference count.

                Log.d(TAG,
                        "key "
                                + key
                                + " NOT removed from cache ("
                                + mCache.size() + ") since it was accessed recently ("
                                + value.mRefCount.get()
                                + ") and ("
                                + mNonAccessedValue.mRefCount.get()
                                + ")");
                assert (mCache.get(key) != null);

                // Try to reset ref count to 1 so it won't be
                // considered as accessed (yet).  Do NOT reset it
                // to 1, however, if ref count has currently
                // increased between remove() above and here.
                value
                        .mRefCount
                        .getAndUpdate(curCount ->
                                curCount > oldCount ? curCount : 1);
            }
        });

        Log.d(TAG,
                "ending the purge of keys not accessed recently");
    };

    /**
     * This ScheduledExecutorService periodically executes
     * mPurgeEntries after a given timeout to remove expired keys.
     */
    private ScheduledExecutorService mScheduledExecutorService;

    /**
     * Constructor initializes the fields.
     */
    public TimedMemoizerEx(Function<K, V> function,
                           long timeoutInMillisecs) {
        // Store the function for subsequent use.
        mFunction = function;

        // Store the timeout for subsequent use.
        mTimeoutInMillisecs = timeoutInMillisecs;

        // Create a ScheduledThreadPoolExecutor with a single thread.
        mScheduledExecutorService =
            Executors.newScheduledThreadPool(1);

        // Set the policies to clean everything up on shutdown.
        ScheduledThreadPoolExecutor exec =
                (ScheduledThreadPoolExecutor) mScheduledExecutorService;

        // Remove mPurgeEntries on cancellation.
        exec.setRemoveOnCancelPolicy(true);

        // Disable periodic tasks at shutdown.
        exec.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);

        // Disable delayed tasks at shutdown.
        exec.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    }

    /**
     * Returns the value associated with the key in cache.  If there's
     * no value associated with the key then call a function to create
     * the value and store it in the cache before returning it.  A
     * key/value entry will be purged from the cache if it's not used
     * within the timeout passed to the constructor.
     */
    public V apply(K key) {
        // Try to find the key in the cache.  If the key isn't present
        // then call computeIfAbsent() to atomically compute the value
        // for the key and return a unique RefCountedValue.
        RefCountedValue rcValue = mCache
                .computeIfAbsent
                        (key,
                                (k) -> {
                                    // If this is the first entry added to an empty cache
                                    // then schedule mPurgeEntries to run periodically.
                                    mCacheCount.incrementAndCallAtN
                                            (1,
                                                    () -> {
                                                        // Do nothing if we've shut down.
                                                        if (mScheduledExecutorService != null) {
                                                            Log.d(TAG,
                                                                    "scheduling mPurgeEntries for key "
                                                                            + k);

                                                            // Schedule mPurgeEntries to purge keys not
                                                            // accessed within mTimeoutInMillisecs.
                                                            mScheduledFuture = mScheduledExecutorService
                                                                    .scheduleAtFixedRate
                                                                            (mPurgeEntries,
                                                                                    mTimeoutInMillisecs, // Initial timeout
                                                                                    mTimeoutInMillisecs, // Periodic timeout
                                                                                    TimeUnit.MILLISECONDS);
                                                        }
                                                    });

                                    // Apply mFunction to store/return the result.
                                    return new RefCountedValue(mFunction.apply(k),
                                            0);
                                });

        // Return the value of the rcValue, which increments its ref
        // count atomically.
        return rcValue.get();
    }

    /**
     * Shutdown the TimedMemoizer and remove all the entries from its
     * ScheduledExecutorService.
     */
    public void shutdown() {
        // Reset the count.
        mCacheCount.setInitialCount(0);

        // Shutdown the ScheduledExecutorService.
        mScheduledExecutorService.shutdownNow();
        mScheduledExecutorService = null;

        // Remove all the keys/values in the map.
        mCache.clear();
    }
}
