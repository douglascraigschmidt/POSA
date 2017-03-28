package vandy.mooc.prime.activities;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import vandy.mooc.prime.R;
import vandy.mooc.prime.utils.UiUtils;

import static java.util.stream.Collectors.toList;

/**
 * Main activity for an app that shows how to use the Java
 * ExecutorService interface and a fixed-size thread pool to determine
 * if n random numbers are prime or not.  The user can interrupt the
 * thread performing this computation at any point and the thread will
 * also be interrupted when the activity is destroyed.  In addition,
 * runtime configuration changes are handled gracefully.
 */
public class MainActivity 
       extends LifecycleLoggingActivity {
    /**
     * EditText field for entering the desired number of iterations.
     */
    private EditText mCountEditText;

    /**
     * Number of primes to evaluate if the user doesn't specify
     * otherwise.
     */
    private final static int sDEFAULT_COUNT = 50;

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
     * Reference to the ExecutorService that runs the primality
     * computations.
     */
    private ExecutorService mExecutorService;

    /**
     * Keeps track of whether the orientation of the phone has been
     * changed.
     */
    private boolean mOrientationChange = false;

    /**
     * Keeps track of whether we're debugging or not.
     */
    private boolean mDebugging;

    /**
     * Hook method called when the activity is first launched.
     */
    protected void onCreate(Bundle savedInstanceState) {
        // Call up to the super class to perform initializations.
        super.onCreate(savedInstanceState);

        // Sets the content view to the xml file.
        setContentView(R.layout.main_activity);

        // Initialize the views.
        initializeViews(savedInstanceState);
    }

    /**
     * Initialize the views.
     */
    private void initializeViews(Bundle savedInstanceState) {
        // Set the EditText that holds the count entered by the user
        // (if any).
        mCountEditText = (EditText) findViewById(R.id.count);

        // Store floating action button that sets the count.
        mSetFab = (FloatingActionButton) findViewById(R.id.set_fab);

        // Store floating action button that starts playing ping/pong.
        mStartFab = (FloatingActionButton) findViewById(R.id.play_fab);

        // Make the count button invisible for animation purposes.
        mStartFab.setVisibility(View.INVISIBLE);

        if (TextUtils.isEmpty(mCountEditText.getText().toString().trim()))
            // Make the EditText invisible for animation purposes.
            mCountEditText.setVisibility(View.INVISIBLE);

        // The activity is being restarted after an orientation
        // change.
        if (savedInstanceState != null) 
            mOrientationChange = true;

        // Store and initialize the TextView and ScrollView.
        mTextViewLog =
            (TextView) findViewById(R.id.text_output);
        mScrollView =
            (ScrollView) findViewById(R.id.scrollview_text_output);

        // Register a listener to help display "start playing" FAB
        // when the user hits enter.  This listener also sets a
        // default count value if the user enters no value.
        mCountEditText.setOnEditorActionListener
            ((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    UiUtils.hideKeyboard(MainActivity.this,
                                         mCountEditText.getWindowToken());
                    if (TextUtils.isEmpty
                        (mCountEditText.getText().toString().trim())) 
                        mCountEditText.setText(String.valueOf(sDEFAULT_COUNT));

                    // Show the "start" FAB.
                    UiUtils.showFab(mStartFab);
                    return true;
                } else {
                    return false;
                }
            });
    }

    /**
     * Called by the Android Activity framework when the user clicks
     * the '+' floating action button.
     *
     * @param view The view
     */
    public void setCount(View view) {
        // Check whether the EditText is visible to determine
        // the kind of animations to use.
        if (mIsEditTextVisible) {
            // Hide the EditText using circular reveal animation
            // and set boolean to false.
            UiUtils.hideEditText(mCountEditText);
            mIsEditTextVisible = false;

            // Rotate the FAB from 'X' to '+'.
            int animRedId = R.anim.fab_rotate_backward;

            // Load and start the animation.
            mSetFab.startAnimation
                (AnimationUtils.loadAnimation(this,
                                              animRedId));
            // Hides the start FAB.
            UiUtils.hideFab(mStartFab);
        } else {
            // Reveal the EditText using circular reveal animation and
            // set boolean to true.
            UiUtils.revealEditText(mCountEditText);
            mIsEditTextVisible = true;
            mCountEditText.requestFocus();

            // Rotate the FAB from '+' to 'X'.
            int animRedId = R.anim.fab_rotate_forward;

            // Load and start the animation.
            mSetFab.startAnimation(AnimationUtils.loadAnimation(this,
                                                                animRedId));
        }
    }

    /**
     * Called by the Android Activity framework when the user clicks
     * the "startComputations" button.
     *
     * @param view
     *            The view.
     */
    public void handleStartButton(View view) {
        // Start running the primality computations.
        startComputations(Integer.valueOf(mCountEditText.getText().toString()));
    }

    /**
     * Start running the primality computations.
     *
     * @param count Number of prime computations to perform.
     */
    public void startComputations(int count) {
        // Make sure there's a non-0 count.
        if (count <= 0) 
            // Inform the user there's a problem with the input.
            UiUtils.showToast(this,
                              "Please specify a count value that's > 0");
        else {
            // Hides the start FAB.
            UiUtils.hideFab(mStartFab);

            // Only allocate as many threads as their are processor
            // cores since determining primality is a CPU-bound
            // computation.
            mExecutorService =
                Executors.newFixedThreadPool(Runtime.getRuntime()
                                             .availableProcessors());

            // Create a list of futures that will contain the results
            // of concurrently checking the primality of "count"
            // random numbers.
            final List<Future<PrimeCallable.PrimeResult>> futures = new Random()
                // Generate "count" random between 0 and MAX_VALUE.
                .longs(count, 0, Integer.MAX_VALUE)

                // Convert each random number into a PrimeCallable.
                .mapToObj(PrimeCallable::new)

                // Submit each PrimeCallable to the ExecutorService.
                .map(mExecutorService::submit)

                // Collect the results into a list of futures.
                .collect(toList());

            // Execute the lambda expression that gets all the results
            // in the background so it doesn't block the UI thread.
            mExecutorService.execute(() -> {

                // Iterate through all the futures to get the results.
                for (Future<PrimeCallable.PrimeResult> f : futures) {
                    try {
                        // This call will block until the future is triggered.
                        PrimeCallable.PrimeResult result = f.get();

                        if (result.mSmallestFactor != 0)
                            MainActivity.this.println(""
                                                      + result.mPrimeCandidate
                                                      + " is not prime with smallest factor "
                                                      + result.mSmallestFactor);
                        else
                            MainActivity.this.println(""
                                                      + result.mPrimeCandidate
                                                      + " is prime");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // Tell the activity we're done.
                MainActivity.this.done();
            });
        }

        // Only print this message the first time the activity runs.
        if (!mOrientationChange)
            println("Starting primality computations");
    }

    /**
     * Finish up and reset the UI.
     */
    public void done() {
        // Create a command to reset the UI.
        Runnable command = () -> {
            // Append the stringToPrint and terminate it with a
            // newline.
            mTextViewLog.append("Finished primality computations\n");
            mScrollView.fullScroll(ScrollView.FOCUS_DOWN);

            // Reshow the "start" FAB.
            UiUtils.showFab(mStartFab);
        };

        // Run the command on the UI thread.  This all is optimized
        // for the case where println() is called from the UI thread.
        runOnUiThread(command);
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
            UiUtils.showFab(mStartFab);

            // Start running the computations using the value
            // originally entered by the user.
            startComputations(Integer.valueOf(mCountEditText.getText().toString()));
        }
    }
}
