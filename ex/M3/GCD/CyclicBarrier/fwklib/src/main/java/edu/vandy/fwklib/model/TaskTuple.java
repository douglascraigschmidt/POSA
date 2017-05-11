package edu.vandy.fwklib.model;

import edu.vandy.fwklib.view.interfaces.ProgressBarInterface;

/**
 * Common tuple that contains the state of each task.
 */
@SuppressWarnings({"FieldCanBeLocal", "unused", "WeakerAccess"})
public class TaskTuple<TestFunc> {
    /**
     * The test function.
     */
    private final TestFunc mTestFunc;

    /**
     * Name of the test.
     */
    private final String mTestName;

    /**
     * Current status of progress.
     */
    private int mProgressStatus = 0;

    /**
     * Unique ID of this task, currently is the array index of the task.
     */
    private int mTaskUniqueId;

    /**
     * Interface for updating the UI's ProgressBar for this TaskTuple.
     */
    private ProgressBarInterface mProgressBar;

    /**
     * Constructor initializes the fields.
     */
    public TaskTuple(TestFunc testFunc,
                     String testName,
                     int taskUniqueId) {
        mTestFunc = testFunc;
        mTestName = testName;
        mTaskUniqueId = taskUniqueId;
    }

    /**
     * Returns the function that performs the test.
     */
    public TestFunc getTestFunc() {
        return mTestFunc;
    }

    /**
     * Returns the name of the test.
     */
    public String getTestName() {
        return mTestName;
    }

    /**
     * Returns the unique id of the task.
     */
    public int getTaskUniqueId() {
        return mTaskUniqueId;
    }

    /**
     * Return the current status of progress.
     */
    public int getProcessStatus() {
        return mProgressStatus;
    }

    /**
     * Set the Unique ID for this Task.
     *
     * @param taskUniqueId The Unique Task ID
     */
    public void setTaskUniqueId(final int taskUniqueId) {
        mTaskUniqueId = taskUniqueId;
    }

    /**
     * Update the progress bar by the increment.
     */
    private void incrementProgress(int increment) {
        mProgressBar.setProgress(mTaskUniqueId,
                                 mProgressStatus
                                 + increment);
    }

    /**
     * Set the progress status.
     */
    public void setProgressStatus(int progressStatus) {
        mProgressStatus = progressStatus;
    }

    /**
     * Returns the current progress status.
     */
    public int getProgressStatus() {
        return mProgressStatus;
    }
}
