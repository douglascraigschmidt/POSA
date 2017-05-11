package edu.vandy.gcdtesttask.view;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
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

import edu.vandy.gcdtesttask.R;
import edu.vandy.gcdtesttask.functionality.TestTask;
import edu.vandy.gcdtesttask.functionality.TestTaskTuple;
import edu.vandy.gcdtesttask.functionality.TestTaskTupleFactory;
import edu.vandy.tasktesterframeworklib.model.Model;
import edu.vandy.tasktesterframeworklib.model.ProgramState;
import edu.vandy.tasktesterframeworklib.model.interfaces.ModelStateInterface;
import edu.vandy.tasktesterframeworklib.presenter.PresenterLogic;
import edu.vandy.tasktesterframeworklib.presenter.interfaces.PresenterInterface;
import edu.vandy.tasktesterframeworklib.utils.Chronometer;
import edu.vandy.tasktesterframeworklib.utils.UiUtils;
import edu.vandy.tasktesterframeworklib.view.interfaces.ViewInterface;
import edu.vandy.tasktesterframeworklib.view.adapters.ListAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that acts as the View in the MVP pattern.
 */
@SuppressWarnings("unused")
public class TestTaskFragment
       extends Fragment
       implements ViewInterface<TestTaskTuple> {
    /**
     * TAG used for logging to identify statements from this class.
     */
    public final static String TAG =
        TestTaskFragment.class.getCanonicalName();

    /**
     * The AppCompatActivity that this Fragment is attached to.
     */
    private AppCompatActivity mActivity;

    /**
     * The ListView of 'Tasks' being displayed.
     */
    @SuppressWarnings("FieldCanBeLocal")
    // Should be class instance, in case interaction with list view is desired.
    private ListView mListView;

    /**
     * The Adapter that bridges between the ListView and the data
     * backing it.
     */
    public ListAdapter<TestTaskTuple> mListAdapter;

    /**
     * Stores the count entered by the user.
     */
    private EditText mCounterEditText;

    /**
     * State that's retained across runtime configuration changes.
     */
    private RetainedState mRetainedState;

    /**
     * This class stores app state that allows us to simplify handling
     * of runtime configuration changes.
     */
    static class RetainedState {
        /**
         * This is the 'Model' in the MVP Pattern.  It stores the list
         * of 'tasks' to be operated upon and displayed.
         */
        ArrayList<TestTaskTuple> mTasks;

        /**
         * Access point to the ModelState.
         */
        private ModelStateInterface<TestTaskTuple> mModelStateInterface;

        /**
         * This is the 'Presenter' in MVP pattern, which handles the
         * app 'logic'.
         */
        PresenterInterface mPresenter;

        /**
         * Constructor initializes the fields.
         */
        RetainedState(ViewInterface<TestTaskTuple> view) {
            // Create the Model layer.
            mModelStateInterface = new Model<>();

            // Create the Presenter layer.
            mPresenter = new PresenterLogic<TestTaskTuple>(view,
                                                              mModelStateInterface) {
                /**
                 * A factory method that returns an AsyncTask to perform the tests.
                 *
                 * @param viewInterface         Reference to the View layer.
                 * @param modelStateInterface   Reference to the Model layer.
                 * @param presenterLogic        Reference to the Presenter layer.
                 * @param numberOfTests         Number of tests to run.
                 * @return                      An AsyncTask to perform the tests.
                 */
                @Override
                public AsyncTask<Integer, Runnable, Void> makeTestTask(ViewInterface<TestTaskTuple> viewInterface,
                                                                       ModelStateInterface<TestTaskTuple> modelStateInterface,
                                                                       PresenterLogic<TestTaskTuple> presenterLogic,
                                                                       int numberOfTests) {
                    // Create an instance of TestTask.
                    return new TestTask(viewInterface,
                                        modelStateInterface,
                                        presenterLogic,
                                        numberOfTests);
                }
            };

            // Initialize Model layer with reference to Presenter layer.
            mModelStateInterface.initializePresenterInterface(mPresenter);

            // Create the lists of tasks to test.
            mTasks =
                new ArrayList<>(TestTaskTupleFactory.getTasksToTest(view));

            // Assign these tasks to the Model layer.
            mModelStateInterface.assignTaskTuples(mTasks);

            // Set starting state of application.
            mModelStateInterface.setState(ProgramState.NEW);
        }
    }

    /**
     * No-Op Constructor. Forced to have by FragmentManager. Best to
     * manually declare, so that it always exists. This constructor
     * will be automatically generated by Java if no other Constructor
     * is present, but best to not rely upon 'implicit' behavior like
     * that.
     *
     * Also note that it's best practice to NOT have any other
     * constructor for Fragments and to instead rely upon Setter(s)
     * for setting 'startup' values.
     */
    public TestTaskFragment() {
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
     * Called to retrieve per-instance state from an activity before
     * being killed so that the state can be restored in
     * onRestoreInstanceState().
     */
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the current value of various fields so they will be
        // available after a runtime configuration change.
        savedInstanceState.putBoolean("countVisible",
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

    /**
     * This method is called after onStart() when the activity is
     * being re-initialized from a previously saved state, given here
     * in savedInstanceState.
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // Iniitalize the super class.
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            // If count EditText is visible, then set its settings.
            if (savedInstanceState.getBoolean("countVisible")) {
                mCounterEditText.setVisibility(View.VISIBLE);
                mCounterEditText.setEnabled(savedInstanceState.getBoolean("countEditable"));
            }

            // Get all chronometer values from before hardware change.
            boolean chronoStarted =
                savedInstanceState.getBoolean("ChronoStarted", false);
            boolean chronoVisible =
                savedInstanceState.getBoolean("ChronoVisible", false);
            long chronoTime =
                savedInstanceState.getLong("ChronoTime");
            long chronoElapsed =
                savedInstanceState.getLong("ChronoElapsed");

            // If Chronometer is visible then determine its current
            // state and display that.
            if (chronoVisible) {
                getChronometer().setVisibility(View.VISIBLE);
                if (chronoStarted) {
                    // Stop chronometer in case it is started, set
                    // base, and then restart.
                    chronometerStop();
                    chronometerSetBase(chronoTime);
                    chronometerStart();
                } else {
                    // Make sure chronometer is stopped and then
                    // display the result that was ] previously
                    // calculated.
                    chronometerStop();
                    getChronometer().setBase(SystemClock.elapsedRealtime() - chronoElapsed);
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

        // TODO save chronometer time so far

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
                                         mRetainedState.mTasks);

        // Set ListView to use Adapter
        mListView.setAdapter(mListAdapter);

        return layout;
    }

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
        mRetainedState.mTasks.get(uniqueID)
                      .mProgressStatus = progress;
        mListAdapter.notifyDataSetChanged();
    }

    /**
     * Set the List of {@link TestTaskTuple} to display Progress Bars for.
     *
     * @param data List of TestTaskTuple(s) to display
     */
    @Override
    public void setData(@NonNull List<TestTaskTuple> data) {
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
     * @return List<TestTaskTuple> Tasks to test.
     */
    @Override
    public List<TestTaskTuple> getData() {
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
}
