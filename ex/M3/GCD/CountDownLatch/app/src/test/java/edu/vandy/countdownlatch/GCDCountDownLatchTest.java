package edu.vandy.countdownlatch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import edu.vandy.countdownlatch.presenter.GCDCountDownLatchTester;
import edu.vandy.countdownlatch.utils.GCDs;
import edu.vandy.countdownlatch.utils.Pair;

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
     * This factory method returns a list containing pairs, where each
     * pair contains the GCD function to run and the name of the GCD
     * function as a string.
     */
    private static List<Pair<GCDCountDownLatchTester.GCD, String>> makeGCDpairs() {
        // Create a new list of GCD pairs.
        List<Pair<GCDCountDownLatchTester.GCD, String>> list = new ArrayList<>();

        // Initialize using method references.
        list.add(Pair.create(GCDs::computeGCDIterativeEuclid, "GCDIterativeEuclid"));
        list.add(Pair.create(GCDs::computeGCDRecursiveEuclid, "GCDRecursiveEuclid"));
        list.add(Pair.create(GCDs::computeGCDBigInteger, "GCDBigInteger"));
        list.add(Pair.create(GCDs::computeGCDBinary, "GCDBinary"));

        // Return the list.
        return list;
    }

    /**
     * Main entry point that tests the GCDCountDownLatchTester class.
     */
    @Test
    public void testGCDCountDownLatchQueue() throws BrokenBarrierException, InterruptedException {
        // Initialize the input data to use for the GCD tests.
        GCDCountDownLatchTester.initializeInputs(sITERATIONS);

        // Make the list of GCD pairs.
        List<Pair<GCDCountDownLatchTester.GCD, String>> gcdTests 
            = makeGCDpairs();

        // Create an entry barrier that ensures the threads don't
        // start until the initializer threads lets them begin.
        CountDownLatch entryBarrier =
            new CountDownLatch(1);

        // Create an exit barrier that the initializer thread doesn't
        // complete until all the test threads complete.
        CountDownLatch exitBarrier =
            new CountDownLatch(gcdTests.size());

        // Iterate through all the GCD pairs and start a new thread to
        // run GCDCountDownLatchTest for each one.
        for (Pair<GCDCountDownLatchTester.GCD, String> gcdpair : gcdTests)
            new Thread(new GCDCountDownLatchTester
                       // All threads share all the entry and exit
                       // barriers.
                       (entryBarrier,
                        exitBarrier,
                        gcdpair,
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
