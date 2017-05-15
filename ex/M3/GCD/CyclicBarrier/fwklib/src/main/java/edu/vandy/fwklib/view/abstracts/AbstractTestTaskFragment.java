package edu.vandy.fwklib.view.abstracts;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import edu.vandy.fwklib.R;
import edu.vandy.fwklib.model.Model;
import edu.vandy.fwklib.model.ProgramState;
import edu.vandy.fwklib.model.TaskTuple;
import edu.vandy.fwklib.model.abstracts.AbstractTestTask;
import edu.vandy.fwklib.model.abstracts.AbstractTestTaskFactory;
import edu.vandy.fwklib.model.interfaces.ModelStateInterface;
import edu.vandy.fwklib.presenter.PresenterLogic;
import edu.vandy.fwklib.presenter.interfaces.PresenterInterface;
import edu.vandy.fwklib.utils.Chronometer;
import edu.vandy.fwklib.utils.UiUtils;
import edu.vandy.fwklib.view.adapters.ListAdapter;
import edu.vandy.fwklib.view.interfaces.ViewInterface;

/**
 * Abstract Base Fragment Class that holds and displays {@link
 * AbstractTestTask} tests upon all
 * {@link TaskTuple} tuples of data.
 */
public abstract class AbstractTestTaskFragment<TestFunc>
        extends Fragment
        implements ViewInterface<TestFunc> {
    /**
     * TAG used for logging to identify statements from this class.
     */
    public final static String TAG =
            AbstractTestTaskFragment.class.getCanonicalName();

    /**
     * The AppCompatActivity that this Fragment is attached to.
     */
    protected AppCompatActivity mActivity;

    /**
     * The ListView of 'Tasks' being displayed.
     */
    @SuppressWarnings("FieldCanBeLocal")
    // Should be class instance, in case interaction with list view is desired.
    protected ListView mListView;

    /**
     * The Adapter that bridges between the ListView and the data
     * backing it.
     */
    public ListAdapter<TestFunc> mListAdapter;

    /**
     * Stores the count entered by the user.
     */
    protected EditText mCounterEditText;

    /**
     * State that's retained across runtime configuration changes.
     */
    protected RetainedState mRetainedState;

    /**
     * Set the List of Tasks to display Progress Bars for.
     *
     * @param data List of TestTaskTuple(s) to display
     */
    @Override
    public void setData(@NonNull List<TaskTuple<TestFunc>> data) {
        Log.d(TAG,
                "setData() : size: " + data.size());
        mRetainedState.mTasks
                .addAll(data);
        mListAdapter.notifyDataSetChanged();
    }

    /**
     * Get the data from the 'model' (stored in View Layer b/c of how
     * Android's Adapters work).
     *
     * @return List<TaskTuple> Tasks to test.
     */
    @Override
    public List<TaskTuple<TestFunc>> getData() {
        return mRetainedState.mTasks;
    }

    /**
     * Get reference to FragmentActivity containing this fragment, for
     * purposes of running Runnable(s) on UI thread.
     *
     * @return FragmentActivity containing this Fragment.
     */
    public FragmentActivity getFragmentActivity() {
        return getActivity();
    }

    /**
     * Make a toast on the current Activity's screen.
     *
     * @param stringValue Text to display in screen toast message.
     */
    @Override
    public void showToast(String stringValue) {
        // Forward to the UiUtils showToast() helper method.
        UiUtils.showToast(getActivity(),
                stringValue);
    }

    /**
     * A factory method that gets the task factory used to create
     * TaskTuple(s).
     *
     * @return TaskFactory {@link AbstractTestTaskFactory} Implementation for this app.
     */
    abstract public AbstractTestTaskFactory<TestFunc> makeTaskFactory();

    /**
     * Called when the Fragment is no longer resumed.  This is generally
     * tied to {@link Activity#onPause() Activity.onPause} of the containing
     * Activity's lifecycle.
     */
    @Override
    public void onPause() {
        super.onPause();
        mRetainedState.mModelStateInterface
                .setTimeElapsed(getChronometer().getTimeElapsed());
    }

    /**
     * Called when a fragment is first attached to its context.
     * {@link #onCreate(Bundle)} will be called after this.
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof AppCompatActivity)
            mActivity = (AppCompatActivity) context;
        else
            throw new ClassCastException
                    (context.toString()
                            + " must implement MyListFragment.OnItemSelectedListener");
    }

    /**
     * Notify the fragment (View layer) that the dataset (Model layer)
     * has changed.
     */
    @Override
    public void notifyDataSetChanged() {
        mListAdapter.notifyDataSetChanged();
    }

    /**
     * Set the progress of a Test Task and then notify the ListView and
     * its adapter of the change.
     */
    public void setProgress(int uniqueID,
                            int progress) {

        // if progress is 100% then store timestamp.
        if (progress == 100) {
            mRetainedState.mTasks
                    .get(uniqueID)
                    .setTimeCompletedString(
                            mChronometerRef.get(0).get().getText().toString()
                    );
        }

        mRetainedState.mTasks
                .get(uniqueID)
                .setProgressStatus(progress);

        mListAdapter.notifyDataSetChanged();
    }

    /**
     * Create the View that the Fragment will use as its base.
     *
     * @param inflater           LayoutInflater that is being used to inflate(create) the View.
     * @param container          ViewGroup the view is being placed into.
     * @param savedInstanceState Bundle of any savedInstanceState that might exist from
     *                           previous running of this Fragment.
     * @return The View of this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View layout = inflater.inflate(R.layout.tester_task_fragment,
                container,
                false);

        // Initialize chronometer to use ChronometerUpdateInterface
        // default methods.
        initializeChronometer((Chronometer) layout.findViewById(R.id.testerTaskChronometer));

        // Initialize count editText to use CountUpdateInterface
        // default methods.
        mCounterEditText = (EditText)
                layout.findViewById(R.id.testerTaskCount);
        initializeCounter(mCounterEditText, this);

        // Get the reference to the floating action button that sets
        // the count.
        FloatingActionButton setFab =
                (FloatingActionButton) layout.findViewById(R.id.testerTaskSet_fab);

        // Set OnClickListener to notify Presenter Layer through
        // NotifyOfGUIActionsInterface of FAB press.
        setFab.setOnClickListener(view
                -> mRetainedState.mPresenter.fabSetPressed(view));

        // Initialize fab with interface Defaults.
        initializeFABSet(setFab);

        // Get the reference to the floating action button that
        // starts/stops processing.
        FloatingActionButton startOrStopFab =
                (FloatingActionButton) layout.findViewById(R.id.testerTaskPlay_fab);

        // Set OnClickListener to notify Presenter Layer through
        // NotifyOfGUIActionsInterface of FAB press.
        startOrStopFab.setOnClickListener(view
                -> mRetainedState.mPresenter.fabStartStopPressed(view));

        // Initialize fab with interface Defaults
        initializeFABStartStop(startOrStopFab);

        // Get reference to actual listview
        mListView = (ListView) layout.findViewById(R.id.testerTaskListView);

        // Create an instance of the adapter (Adapts backend data & UI to work together)
        mListAdapter = new ListAdapter<>(getActivity(),
                R.layout.tester_task_display_row,
                (ArrayList<TaskTuple<TestFunc>>) mRetainedState.mTasks);

        // Set ListView to use Adapter
        mListView.setAdapter(mListAdapter);

        return layout;
    }


    /**
     * This class stores app state that allows us to simplify handling
     * of runtime configuration changes.
     */
    public class RetainedState {
        /**
         * This is the 'Model' in the MVP Pattern.  It stores the list
         * of 'tasks' to be operated upon and displayed.
         */
        List<TaskTuple<TestFunc>> mTasks;

        /**
         * Access point to the state of the Model.
         */
        ModelStateInterface<TestFunc> mModelStateInterface;

        /**
         * This is the 'Presenter' in MVP pattern, which handles the
         * app 'logic'.
         */
        PresenterInterface mPresenter;

        /**
         * Constructor initializes the fields.
         */
        RetainedState(ViewInterface<TestFunc> view) {
            // Create the Model layer.
            mModelStateInterface = new Model<>();


            // Create the task factory.
            final AbstractTestTaskFactory<TestFunc> testTaskFactory =
                    makeTaskFactory();

            // Create the Presenter layer.
            mPresenter = new PresenterLogic<TestFunc>(view,
                    mModelStateInterface) {
                /**
                 * A factory method that returns an
                 * AbstractTestTask to perform the tests.
                 *
                 * @param viewInterface         Reference to the View layer.
                 * @param modelStateInterface   Reference to the Model layer.
                 * @param presenterLogic        Reference to the Presenter layer.
                 * @param numberOfTests         Number of tests to run.
                 * @return A AbstractStateInterface to perform the tests.
                 */
                @Override
                public AbstractTestTask<TestFunc> makeTestTask(ViewInterface<TestFunc> viewInterface,
                                                               ModelStateInterface<TestFunc> modelStateInterface,
                                                               PresenterLogic<TestFunc> presenterLogic,
                                                               int numberOfTests) {
                    return testTaskFactory.makeTestTask(viewInterface,
                            modelStateInterface,
                            presenterLogic,
                            numberOfTests);
                }
            };

            // Set the number to use for Default Runs.
            mModelStateInterface.setDefaultRuns(testTaskFactory.setDefaultRuns());

            // Initialize Model layer with reference to Presenter layer.
            mModelStateInterface.initializePresenterInterface(mPresenter);

            // Use the task factory to create all the tasks to test.
            mTasks = testTaskFactory.getTasksToTest();

            // Assign these tasks to the Model layer.
            mModelStateInterface.setTaskTuples(mTasks);

            // Set starting state of application.
            mModelStateInterface.setState(ProgramState.NEW);
        }

        ViewState mViews = new ViewState();

        class ViewState {

            public boolean countVisible;
            public boolean countEditable;
            public boolean ChronoStarted;
            public boolean ChronoVisible;
            public long ChronoTime;
            public long ChronoElapsed;


        }

    }

    /**
     * This Fragment lifecycle method is called after onAttach() and
     * before onCreateView().
     *
     * @param savedInstanceState Bundle that can store instance state.
     */
    @Override
    public void onCreate(@Nullable
                                 Bundle savedInstanceState) {
        // Initialize super class.
        super.onCreate(savedInstanceState);

        // Only initialize mRetainedState if it's the first time in.
        if (mRetainedState == null)
            mRetainedState = new RetainedState(this);
    }

    /**
     * This method is called after onStart() when the activity is
     * being re-initialized from a previously saved state, given here
     * in savedInstanceState.
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // Initialize the super class.
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            // If count EditText is visible, then set its settings.
            if (mRetainedState.mViews.countVisible) {
                mCounterEditText.setVisibility(View.VISIBLE);
                mCounterEditText.setEnabled(mRetainedState.mViews.countEditable);
            }

            // If Chronometer is visible then determine its current
            // state and display that.
            if (mRetainedState.mViews.ChronoVisible) {
                getChronometer().setVisibility(View.VISIBLE);
                if (mRetainedState.mViews.ChronoStarted) {
                    // Stop chronometer in case it is started, set
                    // base, and then restart.
                    chronometerStop();
                    chronometerSetBase(mRetainedState.mViews.ChronoTime);
                    chronometerStart();
                } else {
                    // Make sure chronometer is stopped and then
                    // display the result that was previously
                    // calculated.
                    chronometerStop();
                    getChronometer().setBase(SystemClock.elapsedRealtime()
                            - mRetainedState.mViews.ChronoElapsed);
                }
            }

            // Set the Play/Stop FAB's appropriate image based on
            // application state.
            switch (mRetainedState.mModelStateInterface.getCurrentState()) {
                case CANCELLED:
                    getFABStartOrStop().setImageResource(R.drawable.ic_autorenew_white_24dp);
                    break;
                case RUNNING:
                    // change play fab to stop
                    getFABStartOrStop().setImageResource(android.R.drawable.ic_delete);
                    break;
                default:
                    getFABStartOrStop().setImageResource(android.R.drawable.ic_media_play);
            }
        }
    }

    /**
     * Called to retrieve per-instance state from an activity before
     * being killed so that the state can be restored in
     * onRestoreInstanceState().
     */
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // @@ Mike, can any of this code be moved into the RetainedState?


        // Save the current value of various GUI fields so they will be
        // available after a runtime configuration change.
        savedInstanceState.putBoolean("",
                mCounterEditText.getVisibility() == View.VISIBLE);
        savedInstanceState.putBoolean("countEditable",
                mCounterEditText.isEnabled());
        savedInstanceState.putLong("ChronoTime",
                getChronometer().getBase());
        savedInstanceState.putBoolean("ChronoVisible",
                getChronometer().getVisibility() == View.VISIBLE);
        savedInstanceState.putBoolean("ChronoStarted",
                getChronometer().getStarted());
        savedInstanceState.putLong("ChronoElapsed",
                getChronometer().getTimeElapsed());

        // Call up to the super class.
        super.onSaveInstanceState(savedInstanceState);
    }
}
