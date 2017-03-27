package vandy.mooc.prime.activities;

import android.util.Log;

/**
 * Uses a brute-force algorithm to determine if a given number is
 * prime or not.
 */
public class PrimeRunnable
       implements Runnable {
    /**
     * Debugging tag used by the Android logger.
     */
    private final String TAG =
        getClass().getSimpleName();

    /**
     * A reference to the MainActivity. 
     */
    private final MainActivity mActivity;

    /** 
     * Number to evaluate for "primality".
     */
    private final long mPrimeCandidate;

    /**
     * Constructor initializes the fields.
     */
    public PrimeRunnable(MainActivity activity,
                         long primeCandidate) {
        mActivity = activity;
        mPrimeCandidate = primeCandidate;
    }
    
    /**
     * This method provides a brute-force determination of whether
     * number @a n is prime.  Returns 0 if it is prime, or the
     * smallest factor if it is not prime. @a minFactor and @a
     * maxFactor can be used to partition the work among threads. For
     * just one thread, typical values are 2 and n/2.
     */
    private long isPrime(long n,
                         long minFactor,
                         long maxFactor) {
        if (n > 3)
            for (long factor = minFactor;
                 factor <= maxFactor;
                 ++factor) 
                if (n / factor * factor == n)
                    return factor;

        return 0;
    }

    /**
     * Hook method that determines if a given number is prime.
     */
    public void run() {
        // Determine if mPrimeCandidate is prime or not.
        long smallestFactor  = isPrime(mPrimeCandidate,
                                       2,
                                       mPrimeCandidate / 2);

        if (smallestFactor != 0)
            mActivity.println(""
                              + mPrimeCandidate
                              + " is not prime with smallest factor "
                              + smallestFactor);
        else
            mActivity.println(""
                              + mPrimeCandidate
                              + " is prime");

        // Tell the activity we're done.
        mActivity.done();
    }
}
