package edu.vandy.tasktesterframeworklib.model.abstracts;

import edu.vandy.tasktesterframeworklib.view.interfaces.ProgressBarInterface;

/**
 * Tuple for tasks.
 */
public abstract class AbstractTaskTuple {
    /**
     * Name of the task function.
     */
    public String mFuncName;

    /**
     * Interface for updating the UI's Progress Bar for this 'Task'.
     */
    public ProgressBarInterface mProgressBar;

    /**
     * Current status of progress
     */
    public int mProgressStatus;

    /**
     * Unique ID of this task, currently is the array index of the task.
     */
    public int mTaskUniqueId;

    /**
     * Value to increment by for each processing.
     */
    public int mIncrement = 1;

    /**
     * Set the increment.
     */
    public void setIncrement(int newIncrement) {
        mIncrement = newIncrement;
    }

    /**
     * Update the progress bar by the increment.
     */
    private void incrementProgress(int increment) {
        mProgressBar.setProgress(mTaskUniqueId,
                                 (mProgressStatus
                                  + increment));
    }
}
