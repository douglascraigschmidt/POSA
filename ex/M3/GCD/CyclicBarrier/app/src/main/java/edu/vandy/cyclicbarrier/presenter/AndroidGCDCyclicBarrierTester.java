package edu.vandy.cyclicbarrier.presenter;

import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.concurrent.CyclicBarrier;

/**
 * The class visualizes the tests of various GCD implementations using
 * CyclicBarriers.
 */
class AndroidGCDCyclicBarrierTester
       extends GCDCyclicBarrierTester {
    /**
     * The progress bar that's used to show how the computations are
     * proceeding.
     */
    private ProgressBar mProgressBar;

    /**
     * Stores the current message to print above the progress bar.
     */
    private final String mMessage;

    /**
     * Stores the current percentage in the progress bar.
     */
    private int mPercentage;

    /**
     * Displays the current message and percentage above the progress
     * bar.
     */
    private TextView mProgressCount;

    /**
     * Constructor initializes the fields and displays the initial
     * progress bar for this GCD implementation.
     */
    AndroidGCDCyclicBarrierTester(String message,
                                         ProgressBar progressBar,
                                         TextView progressCount,
                                         CyclicBarrier entryBarrier,
                                         CyclicBarrier exitBarrier,
                                         GCDTuple gcdTuple,
                                         ProgressReporter progressReporter) {
        super(entryBarrier, 
              exitBarrier,
              gcdTuple,
              progressReporter);
        mPercentage = 0;
        mMessage = message; 
        mProgressBar = progressBar;
        mProgressCount = progressCount;

        // Display the initial progress bar for this GCD implementation.
        mProgressReporter.updateProgress(new Runnable() {
                public void run() {
                    mProgressBar.setProgress(0);
                    mProgressBar.setVisibility(ProgressBar.VISIBLE);
                    mProgressCount.setText(mPercentage + mMessage);
                }
            });
    }

    /**
     * Reset various fields after a runtime configuration change.
     */
    void onConfigurationChange(ProgressBar progressBar,
                               TextView progressCount) {
        mProgressBar = progressBar;
        mProgressCount = progressCount;

        // Redisplay the progress bar and count for this GCD implementation.
        mProgressReporter.updateProgress(() -> {
                mProgressBar.setProgress(mPercentage);
                mProgressBar.setVisibility(ProgressBar.VISIBLE);
                mProgressCount.setText(mPercentage + mMessage);
            });
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
                mProgressBar.setProgress(percentageComplete);
                mPercentage = percentageComplete;
                mProgressCount.setText(mPercentage + mMessage);
            }};
    }
}

