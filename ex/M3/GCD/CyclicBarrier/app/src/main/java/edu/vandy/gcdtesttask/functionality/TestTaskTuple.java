package edu.vandy.gcdtesttask.functionality;

import edu.vandy.tasktesterframeworklib.model.abstracts.AbstractTaskTuple;
import edu.vandy.gcdtesttask.functionality.gcd.GCDInterface;
import edu.vandy.tasktesterframeworklib.view.interfaces.ProgressBarInterface;

/**
 * This data structure stores the state that's needed to visualize
 * each GCDInterface implementation.
 */
//@SuppressWarnings("unused")
public class TestTaskTuple
       extends AbstractTaskTuple {
    /**
     * Function that computes the GCDInterface.
     */
    public GCDInterface mGcdFunction;

    /**
     * Constructor initializes all the fields.
     */
    public TestTaskTuple(GCDInterface gcdFunction,
                         String testName,
                         ProgressBarInterface progressBar,
                         int startingProgress,
                         int maxProgress,
                         int taskUniqueId) {
        mGcdFunction = gcdFunction;
        mFuncName = testName;
        mProgressBar = progressBar;
        mProgressStatus = startingProgress;
        mTaskUniqueId = taskUniqueId;
    }

    /**
     * Constructor initializes the non-GUI fields.
     */
    public TestTaskTuple(GCDInterface gcdFunction,
                         String testName) {
        mGcdFunction = gcdFunction;
        mFuncName = testName;
        mProgressBar = null;
        mProgressStatus = 0;
        mTaskUniqueId = -1;
    }
}
