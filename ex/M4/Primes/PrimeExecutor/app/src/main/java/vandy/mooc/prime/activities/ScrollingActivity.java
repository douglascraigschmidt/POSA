package vandy.mooc.prime.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import vandy.mooc.prime.R;
import vandy.mooc.prime.utils.UiUtils;

public class ScrollingActivity extends LifecycleLoggingActivity implements UICallback {
    /**
     * When set, all threads must cancel operation.
     */
    private volatile static boolean sCancelled = false;

    /**
     * Number of primes to evaluate if the user doesn't specify
     * otherwise.
     */
    private final static int sDEFAULT_COUNT = 100;

    /**
     * Maximum random number value.
     */
    private static long sMAX_VALUE = 1000000000L;

    /**
     * Maximum range of random numbers where range is
     * [sMAX_VALUE - sMAX_COUNT .. SMAX_VALUE].
     */
    private static long sMAX_COUNT = 1000;

    /**
     * EditText field for entering the desired number of iterations.
     */
    private EditText mCountEditText;

    /**
     * Keeps track of whether the edit text is visible for the user to
     * enter a count.
     */
    private boolean mIsEditTextVisible = false;

    /**
     * Reference to the "set" floating action button.
     */
    private FloatingActionButton mSetFab;

    /**
     * Reference to the "start" floating action button.
     */
    private FloatingActionButton mStartFab;

    /**
     * A TextView used to display the output.
     */
    private TextView mTextViewLog;

    /**
     * A ScrollView that contains the results of the TextView.
     */
    private ScrollView mScrollView;

    /**
     * Reference to the Executor that runs the primality computations.
     * Only allocate as many threads as their are processor cores
     * since determining primality is a CPU-bound computation.
     */
    private Executor mExecutor =
            Executors.newFixedThreadPool(Runtime
                    .getRuntime()
                    .availableProcessors());

    /**
     * Keeps track of whether the orientation of the phone has been
     * changed.
     */
    private boolean mOrientationChange = false;

    /**
     * Keeps track of the number of running tasks.
     */
    private AtomicInteger mRunningTasks =
            new AtomicInteger(0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Call up to the super class to perform initializations.
        super.onCreate(savedInstanceState);

        // Sets the content view to the xml file.
        setContentView(R.layout.activity_scrolling);

        // Initialize the views.
        initializeViews(savedInstanceState);
    }

    /**
     * Initialize the views.
     */
    private void initializeViews(Bundle savedInstanceState) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mCountEditText = findViewById(R.id.input_view);
        EditTextKt.makeClearableEditText(mCountEditText, null, () -> {
            sCancelled = true;
            println("Cancelling computations");
            return null;
        });

        // The activity is being restarted after an orientation
        // change.
        if (savedInstanceState != null) {
            mOrientationChange = true;
        }

        // Store and initialize the TextView and ScrollView.
        mTextViewLog = findViewById(R.id.text_output);
        mScrollView = findViewById(R.id.scrollview_text_output);

        // Register a listener to help display "start playing" FAB
        // when the user hits enter. This listener also sets a
        // default count value if the user enters no value.
        mCountEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                UiUtils.hideKeyboard(
                        ScrollingActivity.this,
                        mCountEditText.getWindowToken());
                if (TextUtils.isEmpty(mCountEditText.getText().toString().trim())) {
                    mCountEditText.setText(String.valueOf(sDEFAULT_COUNT));
                }

                getCountAndStartComputations();

                return true;
            } else {
                return false;
            }
        });
    }

    /**
     * Helper that extracts the user entered count value from the
     * edit text widget and calls startComputations to find prime
     * factors.
     */
    public void getCountAndStartComputations() {
        // Start running the primality computations.
        try {
            int count = Integer.valueOf(mCountEditText.getText().toString());
            if (count > sMAX_COUNT) {
                UiUtils.showToast(this,
                        "Please specify a count value in the range [1 .. " + sMAX_COUNT + "]");
            } else {
                startComputations(count);
            }
        } catch (Exception e) {
            UiUtils.showToast(this,
                    "Please specify a count value that's > 0");
        }
    }

    /**
     * Start running the primality computations.
     *
     * @param count Number of prime computations to perform.
     */
    public void startComputations(int count) {
        // Clear cancel flag.
        sCancelled = false;

        // Make sure there's a non-0 count.

        if (count <= 0)
            // Inform the user there's a problem with the input.
            UiUtils.showToast(this,
                    "Please specify a count value that's > 0");
        else {
            // Set the number of running tasks to the count.
            mRunningTasks.set(count);

            // Create "count" random values and check to see if they
            // are prime.
            new Random()
                    // Generate "count" random between sMAX_VALUE - count
                    // and sMAX_VALUE.
                    .longs(count, sMAX_VALUE - count, sMAX_VALUE)

                    // Convert each random number into a PrimeRunnable and
                    // execute it.
                    .forEach(randomNumber ->
                            mExecutor.execute(
                                    new PrimeRunnable(this, randomNumber)));

            // Print this message the first time the activity runs.
            if (!mOrientationChange)
                println("Starting primality computations");
        }
    }

    /**
     * Finish up and reset the UI.
     */
    public void done() {
        Log.d(TAG,
                "Finished in thread "
                        + Thread.currentThread());

        if (mRunningTasks.decrementAndGet() == 0) {
            // Create a command to reset the UI.
            Runnable command = () -> {
                // Append the stringToPrint and terminate it with a
                // newline.
                mTextViewLog.append("Finished primality computations\n");
                mScrollView.fullScroll(ScrollView.FOCUS_DOWN);

                // Reshow the "start" FAB.
//                UiUtils.showFab(mStartFab);
            };

            // Run the command on the UI thread.  This all is optimized
            // for the case where println() is called from the UI thread.
            runOnUiThread(command);
        }
    }

    /**
     * Append @a stringToPrint to the scrolling text view.
     */
    public void println(String stringToPrint) {
        // Create a command to print the results.
        Runnable command = () -> {
            // Append the stringToPrint and terminate it with a
            // newline.
            mTextViewLog.append(stringToPrint + "\n");
            mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
        };

        // Run the command on the UI thread, which internally
        // optimizes for the case where println() is called from the
        // UI thread.
        runOnUiThread(command);
    }

    /**
     * Lifecycle hook method called when the activity is about gain
     * focus.
     */
    @Override
    protected void onResume() {
        // Call to the super class.
        super.onResume();

        // Check to see if an orientation change occurred.  This
        // implementation is simple and doesn't take into account how
        // much progress was made before the orientation occurred.
        if (mOrientationChange) {
            // Show the "start" FAB.
//            UiUtils.showFab(mStartFab);

            // Start running the computations using the value
            // originally entered by the user.
            getCountAndStartComputations();
        }
    }

    @Override
    public boolean isCancelled() {
        return sCancelled;
    }
}
