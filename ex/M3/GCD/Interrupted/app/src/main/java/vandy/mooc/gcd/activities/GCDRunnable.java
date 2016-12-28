package vandy.mooc.gcd.activities;

import android.util.Log;

import java.util.Random;

/**
 * Computes the greatest common divisor (GCD) of two numbers, which is
 * the largest positive integer that divides two integers without a
 * remainder.  This implementation extends Random and implements the
 * Runnable interface's run() hook method.
 */
public class GCDRunnable
       extends Random // Inherits random number generation capabilities.
       implements Runnable {
    /**
     * Debugging tag used by the Android logger.
     */
    protected final String TAG =
        getClass().getSimpleName();

    /**
     * A reference to the MainActivity. 
     */
    private final MainActivity mActivity;

    /** 
     * Number of times to iterate.
     */
    private final int mMaxIterations;

    /**
     * Constructor initializes the fields.
     */
    public GCDRunnable(MainActivity activity,
                       int maxIterations) {
        mActivity = activity;
        mMaxIterations = maxIterations;
    }
    
    /**
     * Provides a recursive implementation of Euclid's algorithm to
     * compute the "greatest common divisor" (GCD).
     */
    private int computeGCD(int number1,
                           int number2) {
        // Basis case.
        if (number2 == 0)
            return number1;
        // Recursive call.
        return computeGCD(number2,
                          number1 % number2);
    }

    /**
     * Hook method that runs for MAX_ITERATIONs computing the GCD of
     * randomly generated numbers.
     */
    public void run() {
        final String threadString = 
            " with thread id " 
            + Thread.currentThread();

        mActivity.println("Entering run()"
                          + threadString);

        // Number of times to print the results.
        int maxPrintIterations = mMaxIterations / 10;

        // Generate random numbers and compute their GCDs.

        for (int i = 0; i < mMaxIterations; ++i) {
            // Generate two random numbers.
            int number1 = nextInt(); 
            int number2 = nextInt();
                
            if (Thread.interrupted()) {
                Log.d(TAG,
                      "thread "
                      + Thread.currentThread().getName()
                      + " interrupted");
                break;
            }

            // Print results.
            else if ((i % maxPrintIterations) == 0)
                mActivity.println("In run()"
                                  + threadString 
                                  + " the GCD of " 
                                  + number1
                                  + " and "
                                  + number2
                                  + " is "
                                  + computeGCD(number1,
                                               number2));
        }

        mActivity.println("Leaving run() "
                          + threadString);

        // 
        mActivity.done();
    }
}
