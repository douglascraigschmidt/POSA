package vandy.mooc.prime.activities;

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
    private final UICallback mUiCallback;

    /**
     * Number to evaluate for "primality".
     */
    private final long mPrimeCandidate;

    /**
     * Constructor initializes the fields.
     */
    public PrimeRunnable(UICallback uiCallback,
                         long primeCandidate) {
        mUiCallback = uiCallback;
        mPrimeCandidate = primeCandidate;
    }

    /**
     * This method provides a brute-force determination of whether
     * number @a n is prime.  Returns 0 if it is prime, -1 if operation
     * has been cancelled, or the smallest factor if it is not prime.
     */
    private long isPrime(long n) {
        if (n > 3)
            for (long factor = 2; factor <= n / 2; ++factor) {
                if (n / factor * factor == n) {
                    return factor;
                }

                if (mUiCallback.isCancelled()) {
                    return -1;
                }
            }

        return 0;
    }

    /**
     * Hook method that determines if a given number is prime.
     */
    public void run() {
        // Determine if mPrimeCandidate is prime or not.
        long smallestFactor = isPrime(mPrimeCandidate);
        if (smallestFactor > 0) {
            mUiCallback.println(""
                    + mPrimeCandidate
                    + " is not prime with smallest factor "
                    + smallestFactor);
        } else if (smallestFactor == 0) {
            mUiCallback.println(""
                    + mPrimeCandidate
                    + " is prime");
        }

        // Tell the activity we're done.
        mUiCallback.done();
    }
}
