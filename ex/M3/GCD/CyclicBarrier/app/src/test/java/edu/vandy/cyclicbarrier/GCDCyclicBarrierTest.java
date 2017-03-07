package edu.vandy.cyclicbarrier;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import edu.vandy.cyclicbarrier.presenter.GCDCyclicBarrierTester;
import edu.vandy.cyclicbarrier.utils.GCDs;
import edu.vandy.cyclicbarrier.utils.Pair;

/**
 * This JUnit test evaluates the GCDCyclicBarrierTest class.
 */
public class GCDCyclicBarrierTest {
    /**
     * Number of times to iterate, which is 100 million to ensure the
     * program runs for a while.
     */
    private static final int sITERATIONS = 100000000;

    /**
     * Number of cycles to run with the CyclicBarrier.
     */
    private static final int sCYCLES = 2;

    /**
     * This factory method returns a list containing pairs, where each
     * pair contains the GCD function to run and the name of the GCD
     * function as a string.
     */
    private static List<Pair<GCDCyclicBarrierTester.GCD, String>> makeGCDpairs() {
        // Create a new list of GCD pairs.
        List<Pair<GCDCyclicBarrierTester.GCD, String>> list = new ArrayList<>();

        // Initialize using method references.
        list.add(Pair.create(GCDs::computeGCDIterativeEuclid, "GCDIterativeEuclid"));
        list.add(Pair.create(GCDs::computeGCDRecursiveEuclid, "GCDRecursiveEuclid"));
        list.add(Pair.create(GCDs::computeGCDBigInteger, "GCDBigInteger"));
        list.add(Pair.create(GCDs::computeGCDBinary, "GCDBinary"));

        // Return the list.
        return list;
    }

    /**
     * Main entry point that tests the GCDCyclicBarrierTester class.
     */
    @Test
    public void testGCDCyclicBarrierQueue() throws BrokenBarrierException, InterruptedException {
        // Make the list of GCD pairs.
        List<Pair<GCDCyclicBarrierTester.GCD, String>> gcdTests 
            = makeGCDpairs();

        // Create an entry barrier that ensures all threads start at
        // the same time.  We add a "+ 1" for the thread that
        // initializes the tests.
        CyclicBarrier entryBarrier =
                new CyclicBarrier(gcdTests.size() + 1,
                                  // Barrier action (re)initializes the test data.
                                  () -> GCDCyclicBarrierTester.initializeInputs(sITERATIONS));

        // Create an exit barrier that ensures all threads end at the
        // same time.  We add a "+ 1" for the thread that waits for
        // the tests to complete.
        CyclicBarrier exitBarrier =
                new CyclicBarrier(gcdTests.size() + 1);

        // Iterate for each cycle.
        for (int cycle = 1; cycle <= sCYCLES; cycle++) {

            // Iterate through all the GCD pairs and start a new
            // thread to run GCDCyclicBarrierTest for each one.
            for (Pair<GCDCyclicBarrierTester.GCD, String> gcdpair : gcdTests)
                new Thread(new GCDCyclicBarrierTester
                           // All threads share all the entry and exit
                           // barriers.
                           (entryBarrier,
                            exitBarrier,
                            gcdpair)).start();

            System.out.println("Starting GCD tests for cycle "
                               + cycle);

            // Wait until all threads are ready to run.
            entryBarrier.await();     
            System.out.println("Waiting for results from cycle "
                               + cycle);

            // Wait until all threads are finished running.
            exitBarrier.await();
            System.out.println("All threads are done for cycle "
                               + cycle);
        }
    }
}
