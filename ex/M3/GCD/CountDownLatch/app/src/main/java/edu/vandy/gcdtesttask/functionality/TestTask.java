package edu.vandy.gcdtesttask.functionality;

import android.os.SystemClock;
import android.util.Log;

import edu.vandy.tasktesterframeworklib.model.abstracts.TestTaskBase;
import edu.vandy.tasktesterframeworklib.model.interfaces.ModelStateInterface;
import edu.vandy.tasktesterframeworklib.presenter.interfaces.PresenterInterface;
import edu.vandy.gcdtesttask.functionality.gcd.GCDCyclicBarrierTester;
import edu.vandy.gcdtesttask.functionality.gcd.GCDCyclicBarrierTesterAndroidAdapter;
import edu.vandy.tasktesterframeworklib.view.interfaces.ViewInterface;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.stream.Collectors.toList;

/**
 * This class tests various GCDInterface implementations using CyclicBarriers
 * on Android.
 */
public class TestTask
	extends TestTaskBase<TestTaskTuple>
	implements GCDCyclicBarrierTester.ProgressReporter {
    /**
     * String Tag for logging.
     */
    private final static String TAG =
        TestTask.class.getCanonicalName();

    /**
     * The executor to run this AsyncTask and Runnable(s) on.
     */
    private ExecutorService mExecutor;

    /**
     * This list of AndroidGCDCountDownLatchTesters keeps track of the
     * objects to update after a runtime configuration change.
     */
    private List<GCDCyclicBarrierTesterAndroidAdapter> mGcdTesters;

    /**
     * Number of iterations to run the GCDInterface tests.
     */
    private int mIterations;

    /**
     * This entry barrier ensures the threads don't start until the
     * coordinator thread lets them begin.
     */
    private CountDownLatch mEntryBarrier;

    /**
     * This exit barrier ensures the coordinator thread doesn't
     * complete until all the test threads complete.
     */
    private CountDownLatch mExitBarrier;

    /**
     * Constructor initializes the fields.
     */
    public TestTask(ViewInterface<TestTaskTuple> viewInterface,
                    ModelStateInterface<TestTaskTuple> modelStateInterface,
                    PresenterInterface presenterInterface,
                    int iterations) {
        super(viewInterface,
              modelStateInterface,
              presenterInterface);

        // Set the number of times to run the tests.
        mIterations = iterations;

        // Create a new cached thread pool executor.
        mExecutor = Executors.newCachedThreadPool();
    }

    /**
     * Hook method called in the UI thread prior to execution of the
     * asynctask.
     */
    protected void onPreExecute() {
        // This list of GCDTuples keeps track of the data needed to
        // run each GCDInterface implementation.
        List<TestTaskTuple> gcdTaskTuples =
            mModelStateInterface.getAllTasks();

        Log.d(TAG,
              "onPreExecute()");

        // Create an entry barrier that ensures the threads don't
        // start until this thread lets them begin.
        mEntryBarrier = new CountDownLatch(1);

        // Create an exit barrier that ensures this thread doesn't
        // complete until all the test threads complete.
        mExitBarrier = new CountDownLatch(mGcdTuples.size());

        // Create a list of GCDInterface testers.
        mGcdTesters = gcdTaskTuples
            // Covert the GCDInterface tuples into a stream.
            .stream()

            // Map each GCDInterface tuple into a GCDInterface tester.
            .map(gcdTaskTuple ->
                 new GCDCyclicBarrierTesterAndroidAdapter
                 // All threads share the entry and exit barriers.
                 (mViewInterface,
                  gcdTaskTuple.mTaskUniqueId,
                  mEntryBarrier,
                  mExitBarrier,
                  gcdTaskTuple,
                  this))

            // Collect into a list.
            .collect(toList());
    }

    /**
     * Runs in a background thread to initiate all the GCDInterface tests and
     * wait for them to complete.
     */
    @Override
    protected Void doInBackground(Integer... cycles) {
        Log.d(TAG,
              "doInBackground()" + cycles[0]);

        // Iterate for each cycle.
        for (int cycle = 1;
             cycle <= cycles[0];
             cycle++) {
            try {
                // Iterate for each GCDInterface tester.
                for (GCDCyclicBarrierTesterAndroidAdapter gcdTester : mGcdTesters)
                    // Execute the GCDInterface tester in the executor service
                    // thread pool.
                    mExecutor.execute(gcdTester);
            
                try {
                    // Create a runnable on the UI thread to
                    // initialize the chronometer.
                    publishProgress((Runnable) () -> {
                        // Log.d(TAG,
                        //       "publish progress from inside doInBackground.");

                        // Initialize and start the Chronometer.
                        mViewInterface.chronometerStop();
                        mViewInterface.chronometerSetBase(SystemClock.elapsedRealtime());
                        mViewInterface.chronometerSetVisibility(true);
                        mViewInterface.chronometerStart();
                    });

                    System.out.println("Starting GCDInterface tests for cycle "
                                       + cycle);

                    // Allow all the test threads to begin.
                    entryBarrier.countDown();

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

                    // Cancel ourselves so the onCancelled() hook
                    // method gets called.
                    cancel(true);
                    return null;
                }
            } catch (Exception ex) {
                Log.d(TAG,
                      "Exception: " + ex.getMessage());
            }
        }
        return null;
    }

    /**
     * Runs in the UI thread after doInBackground() finishes running
     * successfully.
     */
    @Override
    public void onPostExecute(Void v) {
        Runnable command = () -> {
            // Stop the chronometer.
            mViewInterface.getChronometer()
                          .stop();

            Log.d(TAG,
                  "onPostExecute()");
        };

        // Run the command on the UI thread.  This call is optimized
        // for the case where println() is called from the UI thread.
        mViewInterface.getFragmentActivity()
                      .runOnUiThread(command);

        // Call to the super class.
        super.onPostExecute(v);
    }

    /**
     * Runs in the UI thread after doInBackground() is cancelled.
     */
    @Override
    public void onCancelled(Void v) {
        System.out.println("in onCancelled()");

        // Shutdown all the threads in the polls.
        mExecutor.shutdownNow();

        // Call to the super class.
        super.onCancelled(v);
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
