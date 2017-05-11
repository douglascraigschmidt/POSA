package edu.vandy.gcdtesttask;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import edu.vandy.fwklib.model.TaskTuple;
import edu.vandy.fwklib.utils.ProgressReporter;
import edu.vandy.gcdtesttask.functionality.GCDCountDownLatchTester;
import edu.vandy.gcdtesttask.functionality.GCDImplementations;
import edu.vandy.gcdtesttask.functionality.GCDInterface;

/**
 * This JUnit test evaluates the GCDCountDownLatchTest class.
 */
public class GCDCountDownLatchTest 
       implements ProgressReporter {
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
     * This factory method returns a list containing tuples, where
     * each tuple contains the GCD function to run and the name of the
     * GCD function as a string.
     */
    private static List<TaskTuple<GCDInterface>> makeGCDTuples() {
        // Automatically generates a unique id.
        AtomicInteger uniqueId = new AtomicInteger(0);

        // Return a new list of GCD tuples that are each initialized
        // using method references.
        return Arrays.asList(new TaskTuple<GCDInterface>
                             (GCDImplementations::computeGCDIterativeEuclid,
                              "GCDIterativeEuclid",
                              uniqueId.getAndIncrement()),
                             new TaskTuple<GCDInterface>
                             (GCDImplementations::computeGCDRecursiveEuclid,
                              "GCDRecursiveEuclid",
                              uniqueId.getAndIncrement()),
                             new TaskTuple<GCDInterface>
                             (GCDImplementations::computeGCDBigInteger,
                              "GCDBigInteger",
                              uniqueId.getAndIncrement()),
                             new TaskTuple<GCDInterface>
                             (GCDImplementations::computeGCDBinary,
                              "GCDBinary",
                              uniqueId.getAndIncrement()));
    }

    /**
     * Main entry point that tests the GCDCountDownLatchTester class.
     */
    @Test
    public void testGCDCountDownLatchTester()
        throws BrokenBarrierException, InterruptedException {
        // Make the list of GCD tuples.
        List<TaskTuple<GCDInterface>> gcdTests = makeGCDTuples();

        // Create an entry barrier that ensures the threads don't
        // start until this thread lets them begin.
        CountDownLatch entryBarrier = new CountDownLatch(1);

        // Create an exit barrier that ensures this thread doesn't
        // complete until all the test threads complete.
        CountDownLatch exitBarrier = new CountDownLatch(gcdTests.size());

        // Iterate for each cycle.
        for (int cycle = 1; cycle <= sCYCLES; cycle++) {
            // Initialize the input arrays.
            GCDCountDownLatchTester.initializeInputs(sITERATIONS);

            // Iterate through all the GCD tuples and start a new
            // thread to run GCDCountDownLatchTest for each one.
            gcdTests.forEach(gcdTuple
                             -> new Thread(new GCDCountDownLatchTester
                                           // All threads share the
                                           // entry and exit barriers.
                                           (entryBarrier,
                                            exitBarrier,
                                            gcdTuple,
                                            this)).start());

            System.out.println("Starting GCD tests for cycle "
                               + cycle);

            // Wait until all the worker threads are ready to run.
            entryBarrier.countDown();
            System.out.println("Waiting for results from cycle "
                               + cycle);

            // Wait until all the worker threads are finished
            // running.
            exitBarrier.await();
            System.out.println("All threads are done for cycle "
                               + cycle);
        }
    }
}
