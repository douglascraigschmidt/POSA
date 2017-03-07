package edu.vandy.countdownlatch.presenter;

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.vandy.countdownlatch.utils.GCDs;
import edu.vandy.countdownlatch.utils.Pair;
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
     * Constructor initializes the fields.
     */
    public GCDTesterTask(MainActivity activity) {
        mActivity = activity;
        mExecutor = Executors.newCachedThreadPool();
    }

    /**
     * Reset the activity after a runtime configuration change.
     * @param activity The main activity.
     */
    public void onConfigurationChange(MainActivity activity) {
        mActivity = activity;
    }

    /**
     * This factory method returns a list containing pairs, where each
     * pair contains the GCD function to run and the name of the GCD
     * function as a string.
     */
    private List<Pair<GCDCountDownLatchTester.GCD, String>> makeGCDPairs() {
        // Create a new list of GCD pairs.
        List<Pair<GCDCountDownLatchTester.GCD, String>> list = new ArrayList<>();

        // Initialize using method references.
        list.add(Pair.create(GCDs::computeGCDIterativeEuclid, "GCDIterativeEuclid"));
        list.add(Pair.create(GCDs::computeGCDRecursiveEuclid, "GCDRecursiveEuclid"));
        list.add(Pair.create(GCDs::computeGCDBigInteger, "GCDBigInteger"));
        list.add(Pair.create(GCDs::computeGCDBinary, "GCDBinary"));

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
            List<Pair<GCDCountDownLatchTester.GCD, String>> gcdTests 
                = makeGCDPairs();

            // Create an entry barrier that ensures the threads don't
            // start until this thread lets them begin.
            CountDownLatch entryBarrier = new CountDownLatch(1);

            // Create an exit barrier that ensures this thread doesn't
            // complete until all the test threads complete.
            CountDownLatch exitBarrier = new CountDownLatch(gcdTests.size());

            // Iterate thru the GCD pairs and call AsyncTask.execute()
            // to run GCDCountDownLatchTest for each one.
            for (Pair<GCDCountDownLatchTester.GCD, String> gcdPair : gcdTests) {
                String message = "percentage complete for " + gcdPair.second;

                mExecutor.execute(new AndroidGCDCountDownLatchTester
                        // All threads share all the entry and exit
                        // barriers.
                        (message,
                                0,
                                0,
                                entryBarrier,
                                exitBarrier,
                                gcdPair,
                                this));
            }

            System.out.println("Starting GCD tests");

            // Allow all the test threads to begin.
            entryBarrier.countDown();
            System.out.println("Waiting for results");

            // Wait until all threads are finished running.
            exitBarrier.await();
            System.out.println("All threads are done");
        } catch (Exception e) {
            System.out.println("cancelling doInBackground()");

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
