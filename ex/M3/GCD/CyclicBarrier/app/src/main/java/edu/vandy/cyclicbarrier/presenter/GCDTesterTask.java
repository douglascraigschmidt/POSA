package edu.vandy.cyclicbarrier.presenter;

import android.os.AsyncTask;
import android.os.SystemClock;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.vandy.cyclicbarrier.R;
import edu.vandy.cyclicbarrier.utils.Chronometer;
import edu.vandy.cyclicbarrier.utils.GCDs;
import edu.vandy.cyclicbarrier.view.MainActivity;

/**
 * This class provides a driver that tests the various GCD implementations.
 */
public class GCDTesterTask
       extends AsyncTask<Integer, Runnable, Void>
       implements GCDCyclicBarrierTester.ProgressReporter {
    /**
     * Reference to the enclosing activity.
     */
    private MainActivity mActivity;

    /**
     * The executor to run this asynctask and runnables on.
     */
    private ExecutorService mExecutor;

    /**
     * A variant of the Android chronometer widget revised to support
     * millisecond resolution.
     */
    private Chronometer mChronometer;

    /**
     * This list of GCDTuples keeps track of the data needed to run
     * each GCD implementation.
     */
    private List<GCDCyclicBarrierTester.GCDTuple> mGcdTuples;

    /**
     * This list of AndroidGCDCountDownLatchTesters keeps track of the
     * objects to update after a runtime configuration change.
     */
    private List<AndroidGCDCyclicBarrierTester> mGcdTesters;

    /**
     * Number of iterations to run the GCD tests.
     */
    private int mIterations;
    
    /**
     * This entry barrier ensures all threads start at the same time.
     */
    private CyclicBarrier mEntryBarrier;

    /**
     * This exit barrier ensures all threads start at the same time.
     */
    private CyclicBarrier mExitBarrier;

    /**
     * Constructor initializes the fields.
     */
    public GCDTesterTask(MainActivity activity,
                         int iterations,
                         Chronometer chronometer) {
        mActivity = activity;
        mIterations = iterations;
        mExecutor = Executors.newCachedThreadPool();
        mChronometer = chronometer;
    }

    /**
     * Reset various fields after a runtime configuration change.
     *
     * @param activity The main activity.
     */
    public void onConfigurationChange(MainActivity activity) {
        mActivity = activity;

        for (int i = 0; i < mGcdTuples.size(); ++i) {
            mGcdTesters.get(i).onConfigurationChange
                ((ProgressBar) activity.findViewById(mGcdTuples.get(i).mProgressBarResId),
                 (TextView) activity.findViewById(mGcdTuples.get(i).mProgressCountResId));
        }
    }

    /**
     * This factory method returns a list containing Tuples.
     */
    private List<GCDCyclicBarrierTester.GCDTuple> makeGCDTuples() {
        // Create a new list of GCD pairs.
        List<GCDCyclicBarrierTester.GCDTuple> list = new ArrayList<>();

        // Initialize the list using method references to various GCD
        // implementations.
        list.add(new GCDCyclicBarrierTester.GCDTuple(GCDs::computeGCDBigInteger,
                                                     "GCDBigInteger",
                                                     R.id.gcdProgressBar1,
                                                     R.id.gcdProgressCount1));
        list.add(new GCDCyclicBarrierTester.GCDTuple(GCDs::computeGCDIterativeEuclid,
                                                     "GCDIterativeEuclid",
                                                     R.id.gcdProgressBar2,
                                                     R.id.gcdProgressCount2));
        list.add(new GCDCyclicBarrierTester.GCDTuple(GCDs::computeGCDRecursiveEuclid,
                                                     "GCDRecursiveEuclid",
                                                     R.id.gcdProgressBar3,
                                                     R.id.gcdProgressCount3));
        list.add(new GCDCyclicBarrierTester.GCDTuple(GCDs::computeGCDBinary,
                                                     "GCDBinary",
                                                     R.id.gcdProgressBar4,
                                                     R.id.gcdProgressCount4));
        // Return the list.
        return list;
    }

    /**
     *
     */
    protected void onPreExecute() {
        // Make the list of GCD pairs.
        mGcdTuples = makeGCDTuples();

        // Create an empty list to hold the GCD testers.
        mGcdTesters = new ArrayList<>(mGcdTuples.size());

        // Create an entry barrier that ensures all threads start at
        // the same time.  We add a "+ 1" for the thread that
        // initializes the tests.
        mEntryBarrier =
            new CyclicBarrier(mGcdTuples.size() + 1,
                              // Barrier action (re)initializes the test data.
                              () -> GCDCyclicBarrierTester.initializeInputs(mIterations));

        // Create an exit barrier that ensures all threads end at the
        // same time.  We add a "+ 1" for the thread that waits for
        // the tests to complete.
        mExitBarrier =
            new CyclicBarrier(mGcdTuples.size() + 1);

        // Iterate thru the tuples and call AsyncTask.execute() to run
        // GCDCycliceBarrierTest for each one.
        for (GCDCyclicBarrierTester.GCDTuple gcdTuple : mGcdTuples) {
            String message = "% complete for " + gcdTuple.mFuncName;

            // Create a runnable that will run a GCD implementation.
            AndroidGCDCyclicBarrierTester gcdTester = new AndroidGCDCyclicBarrierTester
                // All threads share all the entry
                // and exit barriers.
                (message,
                 (ProgressBar) mActivity.findViewById(gcdTuple.mProgressBarResId),
                 (TextView) mActivity.findViewById(gcdTuple.mProgressCountResId),
                 mEntryBarrier,
                 mExitBarrier,
                 gcdTuple,
                 this);

            // Add to the list of Gcd testers.
            mGcdTesters.add(gcdTester);
        }
    }

    /**
     * Runs in a background thread to initiate all the GCD tests and
     * wait for them to complete.
     */
    @Override
        protected Void doInBackground(Integer... cycles) {
        // Iterate for each cycle.
        for (int cycle = 1; cycle <= cycles[0]; cycle++) {

            // Iterate for each GCD tester.
            for (AndroidGCDCyclicBarrierTester gcdTester : mGcdTesters) 
                // Execute the GCD tester in the executor service
                // thread pool.
                mExecutor.execute(gcdTester);

            try {
                // Create a runnable on the UI thread to initialize
                // the chronometer.
                publishProgress(new Runnable() {
                        public void run() {
                            // Initialize and start the Chronometer.
                            mChronometer.setBase(SystemClock.elapsedRealtime());
                            mChronometer.setVisibility(TextView.VISIBLE);
                            mChronometer.start();
                        }
                    });

                System.out.println("Starting GCD tests for cycle "
                                   + cycle);

                // Wait until all threads are ready to run.
                mEntryBarrier.await();     
                System.out.println("Waiting for results from cycle "
                                   + cycle);

                // Wait until all threads are finished running.
                mExitBarrier.await();
                System.out.println("All threads are done for cycle "
                                   + cycle);
            } catch (Exception ex) {
                System.out.println("cancelling doInBackground() due to exception"
                                   + ex);
                    
                // Cancel ourself so that the onCancelled() hook method
                // gets called.
                cancel(true);
                return null;
            }
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
        // Publish the runnable on the UI thread.
        publishProgress(runnable);
    }

    /**
     * Return the ExecutorService.
     */
    public Executor getExecutor() {
        return mExecutor;
    }
}
