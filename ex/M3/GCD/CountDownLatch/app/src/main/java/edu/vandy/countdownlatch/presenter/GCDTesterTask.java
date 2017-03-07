package edu.vandy.countdownlatch.presenter;

import android.os.AsyncTask;
import android.os.SystemClock;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.vandy.countdownlatch.R;
import edu.vandy.countdownlatch.utils.Chronometer;
import edu.vandy.countdownlatch.utils.GCDs;
import edu.vandy.countdownlatch.view.MainActivity;

public class GCDTesterTask
       extends AsyncTask<Integer, Runnable, Void>
       implements GCDCountDownLatchTester.ProgressReporter {
    /**
     * Reference to the enclosing activity.
     */
    MainActivity mActivity;

    /**
     * The executor to run this asynctask and runnables on.
     */
    ExecutorService mExecutor;

    /**
     * 
     */
    Chronometer mChronometer;

    /**
     * Constructor initializes the fields.
     */
    public GCDTesterTask(MainActivity activity,
                         Chronometer chronometer) {
        mActivity = activity;
        mExecutor = Executors.newCachedThreadPool();
        mChronometer = chronometer;
    }

    /**
     * Reset the activity after a runtime configuration change.
     * @param activity The main activity.
     */
    public void onConfigurationChange(MainActivity activity) {
        mActivity = activity;
    }

    /**
     * This factory method returns a list containing Tuples.
     */
    private List<GCDCountDownLatchTester.Tuple> makeGCDTuples() {
        // Create a new list of GCD pairs.
        List<GCDCountDownLatchTester.Tuple> list = new ArrayList<>();

        // Initialize using method references.
        list.add(new GCDCountDownLatchTester.Tuple(GCDs::computeGCDIterativeEuclid,
                                                   "GCDIterativeEuclid",
                                                   R.id.gcdProgressBar1,
                                                   R.id.gcdProgressCount1));
        list.add(new GCDCountDownLatchTester.Tuple(GCDs::computeGCDRecursiveEuclid,
                                                   "GCDRecursiveEuclid",
                                                   R.id.gcdProgressBar2,
                                                   R.id.gcdProgressCount2));
        list.add(new GCDCountDownLatchTester.Tuple(GCDs::computeGCDBigInteger,
                                                   "GCDBigInteger",
                                                   R.id.gcdProgressBar3,
                                                   R.id.gcdProgressCount3));
        list.add(new GCDCountDownLatchTester.Tuple(GCDs::computeGCDBinary,
                                                   "GCDBinary",
                                                   R.id.gcdProgressBar4,
                                                   R.id.gcdProgressCount4));
        // Return the list.
        return list;
    }

    /**
     *
     */
    public ExecutorService getExecutor() {
        return mExecutor;
    }

    /**
     * Runs in the UI thread before doInBackground() is called.
     */
    @Override
    public void onPreExecute() {
    }

    @Override
    protected Void doInBackground(Integer... iterations) {
        try {
            // Initialize the input data to use for the GCD tests.
            GCDCountDownLatchTester.initializeInputs(iterations[0]);

            // Make the list of GCD pairs.
            List<GCDCountDownLatchTester.Tuple> gcdTests
                = makeGCDTuples();

            // Create an entry barrier that ensures the threads don't
            // start until this thread lets them begin.
            CountDownLatch entryBarrier = new CountDownLatch(1);

            // Create an exit barrier that ensures this thread doesn't
            // complete until all the test threads complete.
            CountDownLatch exitBarrier = new CountDownLatch(gcdTests.size());

            // Iterate thru the tuples and call AsyncTask.execute() to
            // run GCDCountDownLatchTest for each one.
            for (GCDCountDownLatchTester.Tuple gcdTuple : gcdTests) {
                String message = "percentage complete for " 
                    + gcdTuple.mTestName;

                mExecutor.execute(new AndroidGCDCountDownLatchTester
                                  // All threads share all the entry
                                  // and exit barriers.
                                  (message,
                                   (ProgressBar) mActivity.findViewById(gcdTuple.mProgressBarResId),
                                   (TextView) mActivity.findViewById(gcdTuple.mProgressCountResId),
                                   entryBarrier,
                                   exitBarrier,
                                   gcdTuple,
                                   this));
            }

            publishProgress(new Runnable() {
                    public void run() {
                    // Initialize and start the Chronometer.
                    mChronometer.setBase(SystemClock.elapsedRealtime());
                    mChronometer.setVisibility(TextView.VISIBLE);
                    mChronometer.start();
                    }});

            System.out.println("Starting GCD tests");

            // Allow all the test threads to begin.
            entryBarrier.countDown();
            System.out.println("Waiting for results");

            // Wait until all threads are finished running.
            exitBarrier.await();
            System.out.println("All threads are done");
        } catch (Exception ex) {
            System.out.println("cancelling doInBackground() due to exception"
                               + ex);

            // Cancel ourself so that the onCancelled() hook method
            // gets called.
            cancel(true);
        }
        return null;
    }

    /**
     * Runs in the UI thread in response to publishProgress().
     */
    @Override
    public void onProgressUpdate(Runnable... runnables) {
        runnables[0].run();
    }

    /**
     * Runs in the UI thread after doInBackground() finishes running
     * successfully.
     */
    @Override
    public void onPostExecute(Void v) {
        // Indicate to the activity that we're done.
        mActivity.done();
    }

    /**
     * Runs in the UI thread after doInBackground() is cancelled. 
     */
    @Override
    public void onCancelled(Void v) {
        System.out.println("in onCancelled()");

        // Shutdown all the threads in the poll.s
        mExecutor.shutdownNow();

        // Just forward to onPostExecute();
        onPostExecute(v);
    }

    /**
     * Report progress to the UI thread.
     */
    public void updateProgress(Runnable runnable) {
        publishProgress(runnable);
    }
}
