package edu.vandy.gcdtesttask.presenter;

import edu.vandy.visfwk.model.TaskTuple;
import edu.vandy.visfwk.utils.ProgressReporter;
import edu.vandy.visfwk.view.interfaces.ViewInterface;

import java.util.concurrent.CountDownLatch;

/**
 * The class is an Adapter that uses Android's UI to visualize the
 * tests of various GCDInterface implementations using CountDownLatchs.
 */
public class GCDCountDownLatchTesterAndroidAdapter
       extends GCDCountDownLatchWorker {
    /**
     * Interface for interacting with View layer.
     */
    private final ViewInterface<GCDInterface> mViewInterface;

    /**
     * Unique ID of this Tester
     */
    private final int mUniqueID;

    /**
     * Constructor initializes the fields and displays the initial
     * mProgressStatus bar for this GCDInterface implementation.
     */
    public GCDCountDownLatchTesterAndroidAdapter(ViewInterface<GCDInterface> viewInterface,
                                                 int uniqueID,
                                                 CountDownLatch entryBarrier,
                                                 CountDownLatch exitBarrier,
                                                 TaskTuple<GCDInterface> gcdTuple,
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

