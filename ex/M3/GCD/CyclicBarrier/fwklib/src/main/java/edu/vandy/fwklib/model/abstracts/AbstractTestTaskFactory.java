package edu.vandy.fwklib.model.abstracts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import edu.vandy.fwklib.model.TaskTuple;
import edu.vandy.fwklib.model.interfaces.ModelStateInterface;
import edu.vandy.fwklib.presenter.PresenterLogic;
import edu.vandy.fwklib.view.interfaces.ViewInterface;

import static java.util.stream.Collectors.toList;

/**
 * Super class for factory classes that create the list of tasks to
 * test and the actual AsyncTask to test them on Android.
 */
abstract public class AbstractTestTaskFactory<TestFunc> {
    /**
     * Return the list of GCDInterface tasks to test.
     */
    public final List<TaskTuple<TestFunc>> getTasksToTest() {
        // Get the functions and their names to test.
        Map<String, TestFunc> funcsToRun = getFuncsAndNames();

        // Automatically generates a unique id.
        AtomicInteger uniqueId = new AtomicInteger(0);

        // Return a List of TaskTuples containing the test tasks to run.
        return funcsToRun
            // Get the EntrySet for this map.
            .entrySet()
            
            // Convert the EntrySet into a stream.
            .stream()

            // Create a new TaskTuple for each element in the
            // EntrySet.
            .map(entry
                 -> new TaskTuple<>(entry.getValue(),
                                    entry.getKey(),
                                    uniqueId.getAndIncrement()))

            // Limit the number of TaskTuples to the number of
            // functions.
            .limit(funcsToRun.size())

            // Convert the stream to a list.
            .collect(toList());
    }

    /**
     * Returns a Map containing the functions to test (as the map's
     * values) and the names of each function ( as the map's keys).
     */
    protected abstract Map<String, TestFunc> getFuncsAndNames();

    /**
     * A factory method that returns an AbstractTestTask to perform
     * the tests.
     *
     * @param viewInterface       Reference to the View layer.
     * @param modelStateInterface Reference to the Model layer.
     * @param presenterLogic      Reference to the Presenter layer.
     * @param numberOfTests       Number of tests to run.
     * @return An AbstractTestTask to perform the tests.
     */
    abstract public AbstractTestTask<TestFunc> makeTestTask(ViewInterface<TestFunc> viewInterface,
                                                            ModelStateInterface<TestFunc> modelStateInterface,
                                                            PresenterLogic<TestFunc> presenterLogic,
                                                            int numberOfTests);
}
