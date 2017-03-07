package edu.vandy.countdownlatch.presenter;

import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.concurrent.CountDownLatch;

import edu.vandy.countdownlatch.utils.Pair;

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

    public AndroidGCDCountDownLatchTester(String message,
                                          ProgressBar progressBar,
                                          TextView progressCount,
                                          CountDownLatch entryBarrier,
                                          CountDownLatch exitBarrier,
                                          Pair<GCD, String> gcdPair,
                                          ProgressReporter progressReporter) {
        super(entryBarrier, 
              exitBarrier,
              gcdPair,
              progressReporter);
        mPercentage = 0;
        mMessage = message; 
        mProgressBar = progressBar;
        mProgressCount = progressCount;
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
        }
    }


}

