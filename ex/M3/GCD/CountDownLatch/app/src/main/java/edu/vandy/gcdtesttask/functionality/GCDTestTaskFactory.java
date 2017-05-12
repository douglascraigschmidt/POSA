package edu.vandy.gcdtesttask.functionality;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;

import edu.vandy.fwklib.model.abstracts.AbstractTestTask;
import edu.vandy.fwklib.model.abstracts.AbstractTestTaskFactory;
import edu.vandy.fwklib.model.interfaces.ModelStateInterface;
import edu.vandy.fwklib.presenter.PresenterLogic;
import edu.vandy.fwklib.view.interfaces.ViewInterface;

/**
 * Factory class that creates the list of tests to run and the task
 * that actually runs them.
 */
public class GCDTestTaskFactory
       extends AbstractTestTaskFactory<GCDInterface> {
    @Override
    public List<SimpleImmutableEntry<String, GCDInterface>> getFuncsAndNames() {
        // Return an ArrayList containing the name and function to
        // test.
        return new ArrayList<SimpleImmutableEntry<String, GCDInterface>>() {
            { 
                add(new AbstractMap.SimpleImmutableEntry<>("BigInteger",
                                               GCDImplementations::computeGCDBigInteger));
                add(new AbstractMap.SimpleImmutableEntry<>("Binary",
                                               GCDImplementations::computeGCDBinary));
                add(new AbstractMap.SimpleImmutableEntry<>("IterativeEuclid",
                                               GCDImplementations::computeGCDIterativeEuclid));
                add(new AbstractMap.SimpleImmutableEntry<>("RecursiveEuclid",
                                               GCDImplementations::computeGCDRecursiveEuclid));
            }
        };
    }

    /**
     * Create the actual AbstractTestTask that will run the tests on Android.
     *
     * @param viewInterface       Reference to the View Layer (Created and passed in by the framework)
     * @param modelStateInterface Reference to the Model Layer (Created and passed in by the framework)
     * @param presenterLogic      Reference to the Presentation Layer (Created and passed in by the framework)
     * @param numberOfTests       Number of test iterations to run, Determined by user at run time
     * @return An AbstractTestTask that actually runs all the tests.
     */
    @Override
    public AbstractTestTask<GCDInterface> makeTestTask(ViewInterface<GCDInterface> viewInterface,
                                                       ModelStateInterface<GCDInterface> modelStateInterface,
                                                       PresenterLogic<GCDInterface> presenterLogic,
                                                       int numberOfTests) {
        return new GCDCountDownLatchTestTask(viewInterface,
                                             modelStateInterface,
                                             presenterLogic,
                                             numberOfTests);
    }
}
