package edu.vandy.tasktesterframeworklib.presenter;

import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;

import edu.vandy.tasktesterframeworklib.R;
import edu.vandy.tasktesterframeworklib.model.ProgramState;
import edu.vandy.tasktesterframeworklib.model.abstracts.AbstractTaskTuple;
import edu.vandy.tasktesterframeworklib.model.interfaces.ModelStateInterface;
import edu.vandy.tasktesterframeworklib.presenter.interfaces.PresenterInterface;
import edu.vandy.tasktesterframeworklib.presenter.interfaces.PresenterLogicUtils;
import edu.vandy.tasktesterframeworklib.utils.UiUtils;
import edu.vandy.tasktesterframeworklib.view.interfaces.ViewInterface;

/**
 * This is the 'logic' of the app, which defines the presentation
 * layer 'core'.
 */
@SuppressWarnings("unused")
public abstract class PresenterLogic<TaskTuple extends AbstractTaskTuple>
       implements PresenterInterface {
    /**
     * TAG for logging.
     */
    private final static String TAG =
        PresenterLogic.class.getCanonicalName();

    /**
     * Interface for interacting with View layer.
     */
    private ViewInterface<TaskTuple> mViewInterface;

    /**
     * Number of cycles to run with the CyclicBarrier.
     */
    private static final int sCYCLES = 1;

    /**
     * Interface for interacting with Model layer.
     */
    private ModelStateInterface<TaskTuple> mModelStateInterface;

    /**
     * AsyncTask for testing of the tasks.
     */
    private AsyncTask<Integer, Runnable, Void> mTestTask;

    /**
     * Constructor.
     *
     * @param viewInterface       Interface for the View Layer
     * @param modelStateInterface Interface for the Model Layer
     */
    protected PresenterLogic(ViewInterface<TaskTuple> viewInterface,
                             ModelStateInterface<TaskTuple> modelStateInterface) {
        mViewInterface = viewInterface;
        mModelStateInterface = modelStateInterface;
    }

    /**
     * A factory method that returns an AsyncTask to perform the tests.
     *
     * @param viewInterface         Reference to the View layer.
     * @param modelStateInterface   Reference to the Model layer.
     * @param presenterLogic        Reference to the Presenter layer.
     * @param numberOfTests         Number of tests to run.
     * @return                      An AsyncTask to perform the tests.
     */
    protected abstract AsyncTask<Integer, Runnable, Void> makeTestTask(ViewInterface<TaskTuple> viewInterface,
                                                                       ModelStateInterface<TaskTuple> modelStateInterface,
                                                                       PresenterLogic<TaskTuple> presenterLogic,
                                                                       int numberOfTests);

    /**
     * Set Fab was pressed.
     *
     * @param view The View that was pressed.
     */
    @Override
    public void fabSetPressed(View view) {
        Log.d(TAG,
              "fabSetPressed(...)" + mModelStateInterface.getCurrentState());

        switch (mModelStateInterface.getCurrentState()) {
        case NEW:
            enableConfigUI(view);
            break;
        case ENABLED:
            fillDefaultNumber(view);
            break;
        case RUNNING:
        case CANCELLED:
        case FINISHED:
        default:
            break;
        }
    }

    /**
     * Logic of what to do when on screen button is pressed.
     *
     * @param view The View pressed.
     */
    @Override
    public void fabStartStopPressed(View view) {
        Log.d(TAG,
              "fabStartStopPressed(...)" + mModelStateInterface.getCurrentState());

        switch (mModelStateInterface.getCurrentState()) {
        case ENABLED:
            startTests(view);
            break;
        case RUNNING:
            cancelTests(view);
            break;
        case CANCELLED:
            resetUIAfterCancel(view);
            break;
        case NEW:
        case FINISHED:
        default:
            break;
        }
    }

    /**
     * @@ Mike, please document this.
     */
    private void resetButtonAfterTestCompletion(View view) {
        // @@ Mike, is there a reason why this method is a no-op?
    }

    /**
     * Enable Configuration of how many tests to run and play button.
     *
     * @param view View that was pressed that started this method.
     */
    private void enableConfigUI(View view) {
        Log.d(TAG,
              "enableConfigUI(...)");

        mViewInterface.getCountEditText()
                      .setVisibility(View.VISIBLE);
        mViewInterface.chronometerSetBase(SystemClock.elapsedRealtime());
        mModelStateInterface.setState(ProgramState.ENABLED);
    }

    /**
     * Helper method to contain code to run when Filling default
     * number of runs.
     *
     * @param view View Pressed.
     */
    private void fillDefaultNumber(View view) {
        Log.d(TAG,
              "fillDefaultNumber(...)");

        mViewInterface.getCountEditText().clearComposingText();
        mViewInterface.SetEditText(String.valueOf(mModelStateInterface.getDefaultRuns()));
    }

    /**
     * Helper method to contain code to run when starting Tests.
     *
     * @param view View Pressed.
     */
    private void startTests(View view) {
        Log.d(TAG,
              "startTests() Started");

        int numberOfTests;

        // Try to get # from on screen edit text, and handle exceptions if any.
        try {
            numberOfTests =
                Integer.valueOf(mViewInterface
                                .getCountEditText()
                                .getText()
                                .toString()
                                .trim());
        } catch (Exception ex) {
            if (ex instanceof NullPointerException) {
                Log.d(TAG,
                      "Edit Text for numbers was null, empty");
                mViewInterface.showToast("Set Number of Runs.");
            } else if (ex instanceof NumberFormatException) {
                Log.d(TAG,
                      "Edit Text for numbers was not a number");
                mViewInterface.showToast("Value entered for number of runs is empty"
                                         + " or is not a number.");
            }
            Log.d(TAG,
                  "unknown Exception occurred" + ex.getMessage());
            return;
        }

        // Do actual tests if valid testing #
        if (numberOfTests > 0) {
            // Set state to running.
            mModelStateInterface.setState(ProgramState.RUNNING);

            // Disable edit text.
            mViewInterface.getCountEditText().setEnabled(false);

            // Hide set FAB.
            UiUtils.hideFab(mViewInterface.getFABSet());

            // Change play fab to stop.
            mViewInterface.getFABStartOrStop()
                          .setImageResource(android.R.drawable.ic_delete);

            // Start running the chronometer.
            PresenterLogicUtils.resetAndStartChronometer(mViewInterface);

            // Create the test task.
            mTestTask = makeTestTask(mViewInterface,
                                     mModelStateInterface,
                                     this,
                                     numberOfTests).execute(sCYCLES);

            // Execute the test task.
            // mTestTask.execute(sCYCLES);
        }
    }

    /**
     * Reset the UI After Tests Cancelled.
     *
     * @param view View pressed that initiated this method being called.
     */
    private void resetUIAfterCancel(View view) {
        Log.d(TAG,
              "resetUIAfterCancel(...)");

        // reset the FABs, EditText, and Chronometer
        PresenterLogicUtils.resetControlUI(mViewInterface,
                                                      mModelStateInterface);

        // Reset the backend data for tracking of the Progress bars,
        // and then update the UI.
        PresenterLogicUtils.resetProgressBars(mViewInterface,
                                                         mModelStateInterface);

        // Set the Chronometer to be invisible
        mViewInterface.getChronometer()
                      .stop();
        mViewInterface.getChronometer()
                      .setVisibility(View.INVISIBLE);

        // set the application state to NEW.
        mModelStateInterface.setState(ProgramState.NEW);
        mViewInterface.notifyDataSetChanged();
    }

    /**
     * Cancel the current tests being ran.
     *
     * @param view View that was pressed that initiated this method.
     */
    private void cancelTests(View view) {
        Log.d(TAG,
              "cancelTests(...)");

        try {
            // Notify the testing AsyncTask that it should cancel.
            mTestTask.cancel(true);

            Runnable cancelTestsCommand = () -> {
                mViewInterface.showToast("CANCELLED.");
                Log.d(TAG,
                      "Tests Cancelled");

                // Set the state to CANCELLED
                mModelStateInterface.setState(ProgramState.CANCELLED);

                // Set the start/stop FAB to have 'refresh' image.
                mViewInterface.getFABStartOrStop()
                              .setImageResource(R.drawable.ic_autorenew_white_24dp);

                // Stop the Chronometer from continuing to count.
                mViewInterface.getChronometer()
                              .stop();
            };

            // Run the command to cancel the tests in the UI thread.
            mViewInterface.getFragmentActivity()
                          .runOnUiThread(cancelTestsCommand);
        } catch (Exception ex) {
            Log.e(TAG,
                  ex.getMessage());
        }
    }

    /**
     * Tell the UI to reset the Control Interface Views/FABs.
     */
    @Override
    public void resetControlUI() {
        Log.d(TAG,
              "resetControlUI()");
        PresenterLogicUtils.resetControlUI(mViewInterface,
                                                      mModelStateInterface);
    }

    /**
     * Notify the presenter layer of a state change.
     */
    @Override
    public void notifyOfStateChange() {
        ProgramState state =
            mModelStateInterface.getCurrentState();
        Log.d(TAG,
              "notifyOfStateChange(...)" + state);

        switch (state) {
        case ENABLED:
            break;
        case RUNNING:
            break;
        case CANCELLED:
            break;
        case NEW:
            break;
        case FINISHED:
            processingFinished();
            break;
        default:
            break;
        }
    }

    /**
     * Reset the UI to indicate that processing is complete.
     */
    private void processingFinished() {
        mViewInterface.getFABStartOrStop()
                      .setImageResource(android.R.drawable.ic_media_play);
        mViewInterface.getCountEditText()
                      .setEnabled(true);
    }
}
