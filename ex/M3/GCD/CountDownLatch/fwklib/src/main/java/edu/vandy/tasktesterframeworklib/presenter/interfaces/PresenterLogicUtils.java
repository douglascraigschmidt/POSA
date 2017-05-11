package edu.vandy.tasktesterframeworklib.presenter.interfaces;

import android.os.SystemClock;
import android.util.Log;
import android.view.View;

import edu.vandy.tasktesterframeworklib.model.ProgramState;
import edu.vandy.tasktesterframeworklib.model.abstracts.AbstractTaskTuple;
import edu.vandy.tasktesterframeworklib.model.interfaces.ModelStateInterface;
import edu.vandy.tasktesterframeworklib.utils.UiUtils;
import edu.vandy.tasktesterframeworklib.view.interfaces.ViewInterface;

/**
 * A utility class that defines static helper methods used by the
 * PresenterLogic class.
 */
@SuppressWarnings("UnusedParameters")
public class PresenterLogicUtils {
    /**
     * TAG for logging.
     */
    private final static String TAG =
        PresenterLogicUtils.class.getCanonicalName();

    /**
     * Ensure this class is only used as a utility.
     */
    private PresenterLogicUtils() {
        throw new AssertionError();
    }

    /**
     * Reset and Start the Chronometer.
     *
     * @param viewInterface
     * 	the interface instance for interacting with the View Layer.
     */
    public static <TaskTuple extends AbstractTaskTuple> void resetAndStartChronometer(ViewInterface<TaskTuple> viewInterface) {
        Log.d(TAG,
              "resetAndStartChronometer(....)");

        // Run a new Runnable on the UI thread to reset and start the
        // Chronometer.
        viewInterface
            .getFragmentActivity()
            .runOnUiThread(() -> {
                    viewInterface.chronometerStop();
                    viewInterface.chronometerSetBase(SystemClock.elapsedRealtime());
                    viewInterface.chronometerSetVisibility(true);
                    viewInterface.chronometerStart();
                });
    }

    /**
     * Reset the state and then display of the progress bars for each
     * test.
     *
     * @param viewInterface
     * 	the interface instance for interacting with the View Layer.
     * @param modelStateInterface
     * 	the interface instance for interacting with the Model Layer.
     */
    public static <TaskTuple extends AbstractTaskTuple> void resetProgressBars(ViewInterface<TaskTuple> viewInterface,
                                                                               ModelStateInterface<TaskTuple> modelStateInterface) {
        Log.d(TAG,
              "resetProgressBars(....)");

        // For each of the underlying AbstractTaskTuple(s) do the following:
        for (int counter = 0;
             counter < modelStateInterface.getTaskTuplesCount();
             counter++) {
            // set the progress to 0.
            modelStateInterface.getTaskTuple(counter).mProgressStatus = 0;

            // @@ Mike, can this code be removed?!
            // @@ Doug, I think this can be deleted, but emulator is freaking out on me right now
            // Leaving this here so I can finish rest of documentation, etc.

            //			// get the uniqueID
            //			int uniqueID =
            //				modelStateInterface.getTaskTuple(counter)
            //					.mTaskUniqueId;
            //			// Once you have the uniqueID actually reset the progress counter.
            //			modelStateInterface.getTaskTuple(counter)
            //				.mProgressBar
            //				.setProgress(uniqueID,
            //								 0);
        }
    }

    /**
     * Helper method to reset Control UI elements.
     *
     * @param viewInterface
     * 	Interface for interacting with View layer.
     * @param modelStateInterface
     * 	Interface for interacting with Model layer.
     */
    public static <TaskTuple extends AbstractTaskTuple> void resetControlUI(ViewInterface<TaskTuple> viewInterface,
                                                                            ModelStateInterface<TaskTuple> modelStateInterface) {
        Log.d(TAG,
              "resetControlUI(....)");

        // Runnable to be ran on the UI thread for actually resetting
        // the UI's Views and state.
        Runnable resetUIRunnableCommand = () -> {
            // Enable Edit Text and clear number of runs.
            viewInterface.getCountEditText()
                         .setEnabled(true);
            viewInterface.getCountEditText()
                         .getText()
                         .clear();
            viewInterface.getCountEditText()
                         .setVisibility(View.INVISIBLE);

            // reset start/stop FAB
            UiUtils.hideFab(viewInterface.getFABStartOrStop());
            viewInterface.getFABStartOrStop()
                         .setImageResource(android.R.drawable.ic_media_play);
            viewInterface.getFABStartOrStop()
                         .setVisibility(View.INVISIBLE);

            // reset set FAB
            UiUtils.showFab(viewInterface.getFABSet());

            // stop Chronometer
            viewInterface.getChronometer().stop();

            // set application state.
            modelStateInterface.setState(ProgramState.NEW);
        };

        // Run the command on the UI thread.
        viewInterface.getFragmentActivity()
                     .runOnUiThread(resetUIRunnableCommand);
    }
}
