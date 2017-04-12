package vandy.mooc.prime.utils;

import android.util.Log;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * This class defines a "memoizing" cache that maps a key to the value
 * produced by a function.  If a value has previously been computed it
 * is returned rather than calling the function to compute it again.
 * The ConcurrentHashMap computeIfAbsent() method is used to ensure
 * only a single call to the function is run when a key/value pair is
 * first added to the cache.  The Java Timer class is used to limit
 * the amount of time a key/value is retained in the cache.  This code
 * is based on an example in "Java Concurrency in Practice" by Brian
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
     * function.
     */
    private final ConcurrentMap<K, RefCountedValue<V>> mCache =
            new ConcurrentHashMap<>();

    /**
     * This function produces a value based on the key.
     */
    private final Function<K, V> mFunction;

    /**
     * Timer that executes a runnable after a given timeout to remove
     * expired keys.
     */
    private final Timer mTimer;

    /**
     * An object with ref count of 1 indicates its key hasn't been
     * accessed in mTimeoutInMillisecs.
     */
    private final RefCountedValue<?> mNonAccessedValue =
            new RefCountedValue<>(null,1);

    /**
     * Extends FutureTask to keep track of the number of times a key
     * is referenced within mTimeoutInMillisecs.
     */
    private class RefCountedValue<V> {
        /**
         * Keeps track of the number of times a key is referenced
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
         * Returns true if the ref counts are equal, else false.
         */
        @Override
        public boolean equals(Object obj) {
            if (getClass() != obj.getClass())
                return false;
            else {
                @SuppressWarnings("unchecked")
                final RefCountedValue<V> t =
                    (RefCountedValue<V>) obj;
                return mRefCount.get() == t.mRefCount.get();
            }
        }

        /**
         * Increments the ref count atomically and returns the value.
         */
        public V get() {
            // Increment ref count atomically.
            mRefCount.getAndIncrement();

            // Return the value;
            return mValue;
        }
    }

    /**
     * Constructor initializes the fields.
     */
    public TimedMemoizer(Function<K, V> function,
                         long timeoutInMillisecs) {
        // Store the function for subsequent use.
        mFunction = function; 

        // Create a timer and schedule a new timer task to purge keys
        // that have not been accessed recently.
        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {
                /**
                 * Iterate through all the keys in the map and remove
                 * those that haven't been accessed recently.
                 */
                @Override
                public void run() {
                    Log.d(TAG,
                          "start the purge of keys not accessed recently");

                    // Iterate through all the elements in the queue.
                    for (Map.Entry<K, RefCountedValue<V>> e : mCache.entrySet()) {
                        // Store the current ref count.
                        long oldCount = e.getValue().mRefCount.get();

                        // Remove the key only if it hasn't been
                        // accessed in mTimeoutInMillisecs.
                        if (mCache.remove(e.getKey(),
                                          mNonAccessedValue)) {
                            Log.d(TAG,
                                  "key "
                                  + e.getKey()
                                  + " removed from cache since it wasn't accessed recently");
                        } else {
                            Log.d(TAG,
                                  "key "
                                  + e.getKey()
                                  + " NOT removed from cache since it was accessed recently");

                            // Try to reset ref count to 1 so that it
                            // won't be considered as accessed (yet).
                            // However, if ref count has increased
                            // between the call to remove() and here
                            // don't reset it to 1.
                            e.getValue().mRefCount.getAndUpdate(curCount ->
                                                                curCount > oldCount
                                                                ? curCount
                                                                : 1);
                        }
                    }
                    Log.d(TAG,
                          "ending the purge of keys not accessed recently");
                }
            },
            timeoutInMillisecs,  // Initial timeout
            timeoutInMillisecs); // Periodic timeout 
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
        RefCountedValue<V> rcValue = mCache.get(key);

        // If the key isn't present then compute its value.
        if (rcValue == null)
            // If the key isn't present then atomically compute the
            // value associated with the key and return a unique
            // RefCountedValue associated with it.
            rcValue = mCache.computeIfAbsent
                (key,
                 (k) -> new RefCountedValue<>(mFunction.apply(k),
                                              0));
        else
            Log.d(TAG,
                  "key "
                  + key
                  + "'s value was retrieved from the cache");

        // Return the value of the rcValue.
        return rcValue.get();
    }
}
