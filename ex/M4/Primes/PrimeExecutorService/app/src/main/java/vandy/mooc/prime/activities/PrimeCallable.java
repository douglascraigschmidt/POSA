package vandy.mooc.prime.activities;

import java.util.concurrent.Callable;

/**
 * Uses a brute-force algorithm to determine if a given number is
 * prime or not.
 */
public class PrimeCallable
       implements Callable<PrimeCallable.PrimeResult> {
    /**
     * Debugging tag used by the Android logger.
     */
    private final String TAG =
        getClass().getSimpleName();

    /** 
     * Number to evaluate for "primality".
     */
    private final long mPrimeCandidate;

    public static class PrimeResult {
        long mPrimeCandidate;
        
        long mSmallestFactor;
        
        PrimeResult(long primeCandidate, long smallestFactor) {
            mPrimeCandidate = primeCandidate;
            mSmallestFactor = smallestFactor;
        }
    }

    /**
     * Constructor initializes the fields.
     */
    public PrimeCallable(long primeCandidate) {
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
     * Returns 0 if it is prime or the smallest factor if it is not
     * prime.
     */
    public PrimeResult call() {
        return new PrimeResult(mPrimeCandidate,
                               // Determine if mPrimeCandidate is
                               // prime or not.
                               isPrime(mPrimeCandidate,
                                       2,
                                       mPrimeCandidate / 2));
    }
}
