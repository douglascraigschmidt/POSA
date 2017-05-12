package edu.vandy.gcdtesttask.functionality;

import java.util.HashMap;
import java.util.Map;

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
    /**
     * Returns a Map containing the functions to test (as the map's
     * values) and the names of each function (as the map's keys).
     */
    @Override
    public Map<String, GCDInterface> getFuncsAndNames() {
        // Return a HashMap containing the name and function to test.
        return new HashMap<String, GCDInterface>() {
                { 
                    put("GCD BigInteger",
                        GCDImplementations::computeGCDBigInteger);
                    put("GCD Iterative Euclid",
                        GCDImplementations::computeGCDIterativeEuclid);
                    put("GCD Binary",
                        GCDImplementations::computeGCDBinary);
                    put("GCD Recursive Euclid",
                        GCDImplementations::computeGCDRecursiveEuclid);
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
        return new GCDCyclicBarrierTestTask(viewInterface,
                               modelStateInterface,
                               presenterLogic,
                               numberOfTests);
    }
}
