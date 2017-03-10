package edu.vandy.countdownlatch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import edu.vandy.countdownlatch.presenter.GCDCountDownLatchTester;
import edu.vandy.countdownlatch.utils.GCDs;

/**
 * This JUnit test evaluates the GCDCountDownLatchTest class.
 */
public class GCDCountDownLatchTest 
       implements GCDCountDownLatchTester.ProgressReporter {
    /**
     * Number of times to iterate, which is 100 million to ensure the
     * program runs for a while.
     */
    private static final int sITERATIONS = 100000000;

    /**
     * This factory method returns a list containing tuples, where
     * each tuple contains the GCD function to run and the name of the
     * GCD function as a string.
     */
    private static List<GCDCountDownLatchTester.GCDTuple> makeTests() {
        // Create a new list of GCD tuples.
        List<GCDCountDownLatchTester.GCDTuple> list = new ArrayList<>();

        // Initialize using method references.
        list.add(new GCDCountDownLatchTester.GCDTuple(GCDs::computeGCDIterativeEuclid,
                                                   "GCDIterativeEuclid"));
        list.add(new GCDCountDownLatchTester.GCDTuple(GCDs::computeGCDRecursiveEuclid,
                                                   "GCDRecursiveEuclid"));
        list.add(new GCDCountDownLatchTester.GCDTuple(GCDs::computeGCDBigInteger,
                                                   "GCDBigInteger"));
        list.add(new GCDCountDownLatchTester.GCDTuple(GCDs::computeGCDBinary,
                                                   "GCDBinary"));
        // Return the list.
        return list;
    }

    /**
     * Main entry point that tests the GCDCountDownLatchTester class.
     */
    @Test
    public void testGCDCountDownLatchQueue()
            throws BrokenBarrierException, InterruptedException {
        // Initialize the input data to use for the GCD tests.
        GCDCountDownLatchTester.initializeInputs(sITERATIONS);

        // Make the list of GCD pairs.
        List<GCDCountDownLatchTester.GCDTuple> gcdTests
            = makeTests();

        // Create an entry barrier that ensures the threads don't
        // start until the initializer threads lets them begin.
        CountDownLatch entryBarrier =
            new CountDownLatch(1);

        // Create an exit barrier that the initializer thread doesn't
        // complete until all the test threads complete.
        CountDownLatch exitBarrier =
            new CountDownLatch(gcdTests.size());

        // Iterate through all the GCD tuples and start a new thread to
        // run GCDCountDownLatchTest for each one.
        for (GCDCountDownLatchTester.GCDTuple gcdTuple : gcdTests)
            new Thread(new GCDCountDownLatchTester
                       // All threads share all the entry and exit
                       // barriers.
                       (entryBarrier,
                        exitBarrier,
                        gcdTuple,
                        this) {

                }).start();

        System.out.println("Starting GCD tests");

        // Allow all the test threads to begin.
        entryBarrier.countDown();     
        System.out.println("Waiting for results");

        // Wait until all threads are finished running.
        exitBarrier.await();
        System.out.println("All threads are done");
    }
}
