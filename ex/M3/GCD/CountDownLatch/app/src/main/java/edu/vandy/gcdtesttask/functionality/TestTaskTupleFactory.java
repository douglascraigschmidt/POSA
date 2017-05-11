package edu.vandy.gcdtesttask.functionality;

import edu.vandy.gcdtesttask.functionality.gcd.GCDImplementations;
import edu.vandy.tasktesterframeworklib.view.interfaces.ProgressBarInterface;

import java.util.Arrays;
import java.util.List;

/**
 * This factory creates the tasks to complete.
 */
public class TestTaskTupleFactory {
    /**
     * Return the list of GCDInterface tasks to test.
     */
    public static List<TestTaskTuple> getTasksToTest(ProgressBarInterface progressBarInterface) {
        return Arrays.asList(new TestTaskTuple
                             (GCDImplementations::computeGCDBigInteger,
                              "GCD BigInteger",
                              progressBarInterface,
                              0,
                              100,
                              0),

                             new TestTaskTuple
                             (GCDImplementations::computeGCDIterativeEuclid,
                              "GCD Iterative Euclid",
                              progressBarInterface,
                              0,
                              100,
                              1),

                             new TestTaskTuple
                             (GCDImplementations::computeGCDBinary,
                              "GCD Binary",
                              progressBarInterface,
                              0,
                              100,
                              2),

                             new TestTaskTuple
                             (GCDImplementations::computeGCDRecursiveEuclid,
                              "GCD Recursive Euclid",
                              progressBarInterface,
                              0,
                              100,
                              3)
                             );
    }
}
