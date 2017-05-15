package edu.vandy.gcdtesttask.view;

import android.os.Bundle;
import android.support.annotation.Nullable;

import edu.vandy.gcdtesttask.functionality.GCDTestTaskFactory;
import edu.vandy.fwklib.view.abstracts.AbstractTestTaskFragment;
import edu.vandy.fwklib.model.abstracts.AbstractTestTaskFactory;
import edu.vandy.gcdtesttask.functionality.GCDInterface;

/**
 * Fragment that acts as the View in the MVP pattern.
 */
public class TestTaskFragment
       extends AbstractTestTaskFragment<GCDInterface> {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    /**
     * Get the task factory used to create TaskTuple(s).
     *
     * @return The concrete instance of AbstractTestTaskFactory to use
     * for this app, which creates and tests various GCD
     * implementations using the CyclicBarrier barrier synchronizer.
     */
    public AbstractTestTaskFactory<GCDInterface> makeTaskFactory() {
        return new GCDTestTaskFactory();
    }
}
