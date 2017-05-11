package edu.vandy.gcdtesttask.functionality.gcd;

import edu.vandy.gcdtesttask.functionality.TestTaskTuple;
import edu.vandy.tasktesterframeworklib.view.interfaces.ViewInterface;

import java.util.concurrent.CountDownLatch;

/**
 * The class is an Adapter that uses Android's UI to visualize the
 * tests of various GCDInterface implementations using CountDownLatchs.
 */
public class GCDCountDownLatchTesterAndroidAdapter
       extends GCDCountDownLatchTester {
    /**
     * Interface for interacting with View layer.
     */
    private ViewInterface<TestTaskTuple> mViewInterface;

    /**
     * Unique ID of this Tester
     */
    private int mUniqueID;

    /**
     * Constructor initializes the fields and displays the initial
     * mProgressStatus bar for this GCDInterface implementation.
     */
    public GCDCountDownLatchTesterAndroidAdapter(ViewInterface<TestTaskTuple> viewInterface,
                                                int uniqueID,
                                                CountDownLatch entryBarrier,
                                                CountDownLatch exitBarrier,
                                                TestTaskTuple gcdTuple,
                                                ProgressReporter progressReporter) {
        super(entryBarrier,
              exitBarrier,
              gcdTuple,
              progressReporter);
        mViewInterface = viewInterface;
        mUniqueID = uniqueID;
        mViewInterface.setProgress(mUniqueID,
                                   0);
    }

    /**
     * This factory method returns a Runnable that will be displayed
     * in the UI/main thread.
     */
    protected Runnable makeReport(Integer percentageComplete) {
        return () -> {
            System.out.println(""
                               + percentageComplete
                               + "% complete for "
                               + mTestName);
            mViewInterface.setProgress(mUniqueID,
                                       percentageComplete);

        };
    }
}

