package edu.vandy.cyclicbarrier.presenter;

import java.util.Random;
import java.util.concurrent.CyclicBarrier;
import java.util.function.BiFunction;

import edu.vandy.cyclicbarrier.utils.Pair;

/**
 * The class tests various GCD implementations using CyclicBarriers.
 */
public class GCDCyclicBarrierTester 
       implements Runnable {
    /**
     * This functional interface matches the signature of all the GCD
     * methods.
     */
    @FunctionalInterface
    public interface GCD {
        /**
         * Compute and return the GCD for parameters @a a and @a b.
         */
        int compute(int a, int b);
    }

    /**
     * This entry barrier is used to synchronize the entry of all
     * threads to the start of the GCD tests.
     */
    private final CyclicBarrier mEntryBarrier;

    /**
     * This exit barrier is used to synchronize the exit of all
     * threads at the end of the GCD tests.
     */
    private final CyclicBarrier mExitBarrier;

    /**
     * This lambda contains the GCD function to test.
     */
    private final GCD mGcdFunction;

    /**
     * Contains the name of the GCD function being tested.
     */
    private final String mTestName;

    /**
     * An array of randomly generated input to use as the first
     * parameter to the GCD function.
     */
    private static int[] mInputA;

    /**
     * An array of randomly generated input to use as the second
     * parameter to the GCD function.
     */
    private static int[] mInputB;

    /**
     * Constructor initializes the fields.
     */
    public GCDCyclicBarrierTester(CyclicBarrier entryBarrier,
                                  CyclicBarrier exitBarrier,
                                  Pair<GCD, String> gcdPair) {
        mEntryBarrier = entryBarrier;
        mExitBarrier = exitBarrier;
        mGcdFunction = gcdPair.first;
        mTestName = gcdPair.second;
    }

    /**
     * Initialize the input arrays so that all the GCD functions
     * operate on the same randomly generated data.
     */
    public static void initializeInputs(int iterations) {
        System.out.println("calling initialize inputs!!!!!");
        // Create a new Random number generator.  
        Random random = new Random();

        // Generate "iterations" random ints between 0 and MAX_VALUE.
        mInputA =
            random.ints(iterations, 0, Integer.MAX_VALUE).toArray();

        // Generate "iterations" random ints between 0 and MAX_VALUE.
        mInputB =
            random.ints(iterations, 0, Integer.MAX_VALUE).toArray();
    }

    /**
     * Run the GCD test.
     */
    void runTest() {
        System.out.println("Starting test of "
                           + mTestName
                           + " in thread "
                           + Thread.currentThread());

        // Size of the array(s) of random numbers indicates how many
        // iterations to perform.
        int iterations = mInputA.length;

        // Note the start time.
        long startTime = System.nanoTime();

        // Iterate for the given # of iterations.
        for (int i = 0; i < iterations; ++i) {
            // Get the next two random numbers.
            int number1 = mInputA[i];
            int number2 = mInputB[i];

            // Compute the GCD of these two numbers.
            int result = mGcdFunction.compute(number1, number2);

            // Print results every 10 million iterations.
            /*
            if ((i % (iterations / 10)) == 0)
                System.out.println("In runTest() on iteration "
                                   + i
                                   + " the GCD of "
                                   + number1
                                   + " and "
                                   + number2
                                   + " is "
                                   + result);
            */
        }

        // Stop timing the tests. 
        long stopTime = System.nanoTime();

        // Print the results.
        System.out.println(""
                           + (double)(stopTime - startTime) / 1000000.0
                           + " millisecond run time for "
                           + mTestName
                           + " in thread "
                           + Thread.currentThread());
    }

    /**
     * Main entry point into the GCD test.
     */
    public void run() {
        try {
            // Wait for all threads to be ready to run.
            mEntryBarrier.await();

            // Run the test.
            runTest();

            // Wait for all the threads to finish running the test.
            mExitBarrier.await();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

