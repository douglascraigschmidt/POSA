package edu.vandy.cyclicbarrier.view;

import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import edu.vandy.cyclicbarrier.R;
import edu.vandy.cyclicbarrier.presenter.GCDTesterTask;
import edu.vandy.cyclicbarrier.utils.UiUtils;

/**
 * Main activity that shows how CyclicBarriers can be used to
 * coordinate the concurrent benchmarking of different Greatest Common
 * Divisor (GCD) implementations, which compute the largest positive
 * integer that is a divisor of two numbers.  The user can cancel
 * these computations at any point and the computations will also be
 * cancelled when the activity is destroyed.  In addition, runtime
 * configuration changes are handled gracefully, without restarting
 * the computations from the beginning.
 */
public class MainActivity 
       extends ActivityBase {
    /**
     * A list containing an async task that initiates the processing
     * of all the GCD implementations.
     */
    private List<GCDTesterTask> mTasks;

    /**
     * Number of cycles to run with the CyclicBarrier.
     */
    private static final int sCYCLES = 2;

    /**
     * Hook method called when the activity is first launched.
     */
    protected void onCreate(Bundle savedInstanceState) {
        // Call up to the super class to perform initializations.
        super.onCreate(savedInstanceState);

        // Set mTasks to the object that was stored by
        // onRetainNonConfigurationInstance().
        mTasks =
            (List<GCDTesterTask>) getLastNonConfigurationInstance();

        // This is the first time in, so allocate mTasks.
        if (mTasks == null) 
            // Create a new ArrayList.
            mTasks = new ArrayList<>();

        // There are already computations running after a runtime
        // configuration change, so keep going.
        else if (mTasks.size() != 0) {
            // Reset widgets and the activity for each async task.
            mTasks.forEach(task 
                           -> task.onConfigurationChange(this));

            // Update the start/stop FAB to display a stop icon.
            mStartOrStopFab.setImageResource(R.drawable.ic_media_stop);

            // Show the "startOrStop" FAB.
            UiUtils.showFab(mStartOrStopFab);
        }
    }

    /**
     * This hook method is called by Android as part of destroying an
     * activity due to a configuration change, when it is known that a
     * new instance will immediately be created for the new
     * configuration.
     */
    @Override
    public Object onRetainNonConfigurationInstance() {
        // Call the super class.
        super.onRetainNonConfigurationInstance();

        // Returns mTasks so that it will be saved across runtime
        // configuration changes.
        return mTasks;
    }

    /**
     * Called by the Android Activity framework when the user clicks
     * the "startOrStartComputations" button.
     *
     * @param view
     *            The view.
     */
    public void startOrStopComputations(View view) {
        if (mTasks.size() != 0)
            // The thread only exists while GCD computations are in
            // progress.
            cancelComputations();
        else 
            // Start running the computations.
            startComputations(Integer.valueOf(mCountEditText.getText().toString()));
    }

    /**
     * Start the the producer/consumer computations in the AsyncTasks.
     */
    public void startComputations(int count) {
        // Make sure there's a non-0 count.
        if (count <= 0) 
            // Inform the user there's a problem with the input.
            UiUtils.showToast(this,
                              "Please specify a count value that's > 0");
        else {
            // Create the GCDTesterTask.
            GCDTesterTask gcdTask = new GCDTesterTask(this, 
                                                      count,
                                                      mChronometer);

            // Add the new task to the list.
            mTasks.add(gcdTask);

            // Execute the async tasks.
            mTasks.forEach(task
                           -> task.executeOnExecutor(gcdTask.getExecutor(),
                                                     sCYCLES));

            // Update the start/stop FAB to display a stop icon.
            mStartOrStopFab.setImageResource(R.drawable.ic_media_stop);
        }
    }

    /**
     * Stop the producer/consumer computations.
     */
    private void cancelComputations() {
        // Stop the async task computations.
        mTasks.forEach(task -> task.cancel(true));

        UiUtils.showToast(this,
                          "Canceling the GCD computations");
    }

    /**
     * Finish up and reset the UI.
     */
    public void done() {
        // Create a command to reset the UI.
        Runnable command = () -> {
            // Clear out the async tasks to avoid later problems.
            mTasks.clear();

            // Reset the start/stop FAB to the play icon.
            mStartOrStopFab.setImageResource(android.R.drawable.ic_media_play);

            // Stop the chronometer.
            mChronometer.stop();
        };

        // Run the command on the UI thread.  This call is optimized
        // for the case where println() is called from the UI thread.
        runOnUiThread(command);
    }

    /**
     * Lifecycle hook method called when this activity is being
     * destroyed.
     */
    protected void onDestroy() {
        // Call the super class.
        super.onDestroy();

        // Only cancel the AsyncTasks when an activity is actually
        // being destroyed, but not when it's simply being rotated due
        // to a runtime configuration change.
        if (mTasks.size() != 0
            && !isChangingConfigurations()) {
            Log.d(TAG,
                  "Canceling the GCD computations");

            // Cancel the AsyncTasks since the activity is being
            // destroyed.
            mTasks.forEach(task -> task.cancel(true));
        }
    }
}
