package edu.vandy.cyclicbarrier.presenter;

import java.util.Random;
import java.util.concurrent.CyclicBarrier;

/**
 * The class tests various GCD implementations using CyclicBarrieres.
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
     * Report progress by running the Runnable in the right context
     * (e.g., the UI or main thread).
     */
    public interface ProgressReporter {
        /**
         * Report progress in the right context.
         */
        default void updateProgress(Runnable runnable) {
            runnable.run();
        }
    }

    /**
     * This data structure stores the state that's needed to visualize
     * each GCD implementation.
     */
    public static class GCDTuple {
        /**
         * Function that computes the GCD.
         */
        GCDCyclicBarrierTester.GCD mGcdFunction;

        /**
         * Name of the GCD function.
         */
        String mFuncName;

        /**
         * Resource ID of this function's progress bar.
         */
        int mProgressBarResId;

        /**
         * Resource ID of this function's progress count.
         */
        int mProgressCountResId;

        /**
         * Constructor initializes all the fields.
         */
        GCDTuple(GCDCyclicBarrierTester.GCD gcdFunction,
                 String testName,
                 int progressBarResId,
                 int progressCountResId) {
            mGcdFunction = gcdFunction;
            mFuncName = testName;
            mProgressBarResId = progressBarResId;
            mProgressCountResId = progressCountResId;
        }

        /**
         * Constructor initializes the non-GUI fields.
         */
        public GCDTuple(GCDCyclicBarrierTester.GCD gcdFunction,
                        String testName) {
            mGcdFunction = gcdFunction;
            mFuncName = testName;
            mProgressBarResId = 0;
            mProgressCountResId = 0;
        }
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
    protected final String mTestName;

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
     * A reference to the ProgressReporter.
     */
    protected ProgressReporter mProgressReporter;

    /**
     * Constructor initializes the fields.
     */
    public GCDCyclicBarrierTester(CyclicBarrier entryBarrier,
                                  CyclicBarrier exitBarrier,
                                  GCDTuple gcdTuple,
                                  ProgressReporter progressReporter) {
        mEntryBarrier = entryBarrier;
        mExitBarrier = exitBarrier;
        mGcdFunction = gcdTuple.mGcdFunction;
        mTestName = gcdTuple.mFuncName;
        mProgressReporter = progressReporter;
    }

    /**
     * Initialize the input arrays so that all the GCD functions
     * operate on the same randomly generated data.
     */
    public static void initializeInputs(int iterations) {
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
    private void runTest() {
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
            if (Thread.interrupted()) {
                System.out.println("Interrupt request received in runTest() for "
                                   + mTestName
                                   + " in thread "
                                   + Thread.currentThread());
                return;
            }

            // Get the next two random numbers.
            int number1 = mInputA[i];
            int number2 = mInputB[i];

            // Compute the GCD of these two numbers.
            int result = mGcdFunction.compute(number1, number2);

            // Publish the progress every 10%.
            if (((i + 1) % (iterations / 10)) == 0) {
                /*
                System.out.println("In runTest() on iteration "
                                   + i
                                   + " the GCD of "
                                   + number1
                                   + " and "
                                   + number2
                                   + " is "
                                   + result);
                */
                // Convert to a percentage of 100.
                Double percentage =
                    ((double) (i + 1) / (double) iterations) * 100.00;
                
                // Publish progress as a percentage of total
                // completion.
                mProgressReporter.updateProgress(makeReport(percentage.intValue()));
            }
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
     * This factory method returns a Runnable that will be displayed
     * in the UI/main thread.
     */
    protected Runnable makeReport(Integer percentageComplete) {
        return new Runnable () {
            public void run() {
                System.out.println(""
                                   + percentageComplete
                                   + "% complete for "
                                   + mTestName);
            }};
    }

    /**
     * Main entry point into the GCD test.
     */
    public void run() {
        try {
            // Wait for all threads to arrive at the entry barrier and
            // then start the test.
            mEntryBarrier.await();

            // Run the test.
            runTest();

            // Wait for all threads to arrive at the exit barrier and
            // then exit the test.
            mExitBarrier.await();
        } catch (Exception ex) {
            System.out.println("exception "
                               + ex
                               + " received in run() for thread "
                               + Thread.currentThread());
        }
    }
}

