package edu.vandy.countdownlatch.presenter;

import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.concurrent.CountDownLatch;

/**
 *
 */
public class AndroidGCDCountDownLatchTester
       extends GCDCountDownLatchTester {
    /**
     * The progress bar that's used to show how the computations are
     * proceeding.
     */
    ProgressBar mProgressBar;

    /**
     * Stores the current message to print above the progress bar.
     */
    final String mMessage;

    /**
     * Stores the current percentage in the progress bar.
     */
    int mPercentage;

    /**
     * Displays the current message and percentage above the progress
     * bar.
     */
    TextView mProgressCount;

    /**
     *
     */
    public AndroidGCDCountDownLatchTester(String message,
                                          ProgressBar progressBar,
                                          TextView progressCount,
                                          CountDownLatch entryBarrier,
                                          CountDownLatch exitBarrier,
                                          Tuple gcdTuple,
                                          ProgressReporter progressReporter) {
        super(entryBarrier, 
              exitBarrier,
              gcdTuple,
              progressReporter);
        mPercentage = 0;
        mMessage = message; 
        mProgressBar = progressBar;
        mProgressCount = progressCount;
        mProgressBar.setProgress(0);
        mProgressBar.setVisibility(ProgressBar.VISIBLE);
        mProgressCount.setText(mMessage + mPercentage);
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
            mProgressBar.setProgress(percentageComplete);
            mPercentage = percentageComplete;
            mProgressCount.setText(mMessage + mPercentage);
        };
    }


}

