package edu.vandy.fwklib.model;

import edu.vandy.fwklib.model.interfaces.ModelStateInterface;

import java.util.List;

/**
 * Class that stores any task-independent model data.
 */
public class Model<TestFunc>
       implements ModelStateInterface<TestFunc> {
    /**
     * Time threads started.
     */
    private long mBaseTime;

    /**
     * Time Elapsed so far for processing.
     */
    private long mTimeElapsed;

    /**
     * Default number of runs to fill EditText when button pressed if
     * the user doesn't specify otherwise.
     */
    private long mDefaultRuns = 10000000;

    /**
     * List of Task(s) stored as {@link TaskTuple}(s)
     */
    private List<TaskTuple<TestFunc>> mTasks;

    /**
     * Get the BaseTime to set the chronometer to.
     *
     * @param baseTime unix timestamp to set base time to.
     */
    @Override
    public void setBaseTime(long baseTime) {
        mBaseTime = baseTime;
    }

    /**
     * Get the base time (Unix Time) that the Chronometer should use.
     *
     * @return Long Base time Chronometer should use.
     */
    @Override
    public long getBaseTime() {
        return mBaseTime;
    }

    /**
     * Set the Time Elapsed on current test.
     *
     * @param timeElapsed long Time (Unix Time) Elapsed so far.
     */
    @Override
    public void setTimeElapsed(long timeElapsed) {
        mTimeElapsed = timeElapsed;
    }

    /**
     * Get Time (Unix Time) Elapsed in current test.
     *
     * @return long Time Elapsed.
     */
    @Override
    public long getTimeElapsed() {
        return mTimeElapsed;
    }

    /**
     * Get the number to use as a default number of test runs.
     *
     * @return long Default for the number of runs to test.
     */
    @Override
    public long getDefaultRuns() {
        return mDefaultRuns;
    }

    /**
     * Set value to be used when filling UI with default number of
     * tests to run.
     *
     * @param defaultRuns long value to use when requesting default
     * test number.
     */
    public void setDefaultRuns(long defaultRuns) {
        mDefaultRuns = defaultRuns;
    }

    /**
     * Get the TaskTuple at @a position.
     */
    @Override
    public TaskTuple<TestFunc> getTaskTuple(int position) {
        return mTasks.get(position);
    }

    /**
     *
     * @param tasks ArrayList of {@link TaskTuple} to be tested.
     */
    @Override
    public void setTaskTuples(List<TaskTuple<TestFunc>> tasks) {
        mTasks = tasks;
    }

    /**
     * Return a count of the number of tasks.
     */
    @Override
    public int getTaskTuplesCount() {
        return mTasks.size();
    }

    /**
     * Return a list of all the tasks.
     */
    @Override
    public List<TaskTuple<TestFunc>> getTestTasks() {
        return mTasks;
    }
}
