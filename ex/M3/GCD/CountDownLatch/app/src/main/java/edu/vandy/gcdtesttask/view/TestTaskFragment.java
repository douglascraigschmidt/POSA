package edu.vandy.gcdtesttask.view;

import edu.vandy.gcdtesttask.functionality.GCDTestTaskFactory;
import edu.vandy.fwklib.view.abstracts.AbstractTestTaskFragment;
import edu.vandy.fwklib.model.abstracts.AbstractTestTaskFactory;
import edu.vandy.gcdtesttask.functionality.GCDInterface;

/**
 * Fragment that acts as the View in the MVP pattern.
 */
public class TestTaskFragment
       extends AbstractTestTaskFragment<GCDInterface> {
    /**
     * Get the task factory used to create TaskTuple(s).
     *
     * @return The concrete instance of AbstractTestTaskFactory to use
     * for this app, which creates and tests various GCD
     * implementations using the CountDownLatch barrier synchronizer.
     */
    public AbstractTestTaskFactory<GCDInterface> makeTaskFactory() {
        return new GCDTestTaskFactory();
    }
}
