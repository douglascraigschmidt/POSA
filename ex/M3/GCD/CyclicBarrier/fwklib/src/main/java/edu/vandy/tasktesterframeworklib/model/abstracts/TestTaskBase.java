package edu.vandy.tasktesterframeworklib.model.abstracts;

import android.os.AsyncTask;
import android.util.Log;

import edu.vandy.tasktesterframeworklib.R;
import edu.vandy.tasktesterframeworklib.model.ProgramState;
import edu.vandy.tasktesterframeworklib.model.interfaces.ModelStateInterface;
import edu.vandy.tasktesterframeworklib.presenter.interfaces.PresenterInterface;
import edu.vandy.tasktesterframeworklib.view.interfaces.ViewInterface;

/**
 * Abstract base class for all test tasks. 
 */
@SuppressWarnings("WeakerAccess")
public abstract class TestTaskBase<TaskTuple extends AbstractTaskTuple>
       extends AsyncTask<Integer, Runnable, Void> {
    /**
     * TAG used for logging.
     */
    private final static String TAG =
        TestTaskBase.class.getCanonicalName();

    /**
     * Interface for interacting with View Layer.
     */
    final protected ViewInterface<TaskTuple> mViewInterface;

    /**
     * Interface for interacting with Model Layer.
     */
    final protected ModelStateInterface<TaskTuple> mModelStateInterface;

    /**
     * Interface for interacting with Presenter Layer.
     */
    final protected PresenterInterface mPresenterInterface;

    /**
     * boolean flag to signal that processing should be cancelled.
     */
    protected boolean mFlaggedToCancel = false;

    /**
     * Default Constructor.
     *
     * @param viewInterface
     * 	Interface for interacting with View Layer.
     * @param modelStateInterface
     * 	Interface for interacting with Model Layer.
     * @param presenterInterface
     * 	Interface for interacting with Presenter Layer.
     */
    public TestTaskBase(final ViewInterface<TaskTuple> viewInterface,
                        final ModelStateInterface<TaskTuple> modelStateInterface,
                        final PresenterInterface presenterInterface) {
        mViewInterface = viewInterface;
        mModelStateInterface = modelStateInterface;
        mPresenterInterface = presenterInterface;
    }

    /**
     * <p>Runs on the UI thread after {@link #doInBackground}. The
     * specified result is the value returned by {@link #doInBackground}.</p>
     * <p>
     * <p>This method won't be invoked if the task was cancelled.</p>
     *
     * @param result
     * 	The result of the operation computed by {@link #doInBackground}.
     *
     * @see #onPreExecute
     * @see #doInBackground
     * @see #onCancelled(Object)
     */
    @Override
    protected void onPostExecute(Void result) {
        Log.d(TAG,
              "onPostExecute(...)");
        mModelStateInterface.setState(ProgramState.FINISHED);
        mPresenterInterface.resetControlUI();
    }

    /**
     * Runs on the UI thread after {@link #publishProgress} is invoked.
     * The specified values are the values passed to {@link #publishProgress}.
     *
     * @param values
     * 	The values indicating progress.
     *
     * @see #publishProgress
     * @see #doInBackground
     */
    @Override
    protected void onProgressUpdate(Runnable... values) {
        Log.d(TAG,
              "onProgressUpdate(...)");
        for (Runnable runnable : values) 
            mViewInterface.getFragmentActivity()
                          .runOnUiThread(runnable);
    }

    /**
     * <p>Runs on the UI thread after {@link #cancel(boolean)} is invoked and
     * {@link #doInBackground(Object[])} has finished.</p>
     * <p>
     * <p>The default implementation simply invokes {@link #onCancelled()} and
     * ignores the result. If you write your own implementation, do not call
     * <code>super.onCancelled(result)</code>.</p>
     *
     * @param result
     * 	The result, if any, computed in {@link #doInBackground(Object[])}, can be null
     *
     * @see #cancel(boolean)
     * @see #isCancelled()
     */
    @Override
    protected void onCancelled(Void result) {
        Log.d(TAG,
              "onCancelled(...)");
        mFlaggedToCancel = true;

        // Create a command to stop running the program.
        Runnable stopRunningCommand = () -> {
            mViewInterface.getFABStartOrStop()
                          .setImageResource(R.drawable.ic_autorenew_white_24dp);
            mModelStateInterface.setState(ProgramState.CANCELLED);
            mViewInterface.getChronometer()
                          .stop();
        };

        // Send the command to the UI thread.
        mViewInterface.getFragmentActivity()
                      .runOnUiThread(stopRunningCommand);
    }
}
