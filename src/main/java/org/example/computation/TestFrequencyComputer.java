package org.example.computation;

import java.time.Duration;

/**
 * Calculates the frequency of test execution in a given repository based on
 * step interval length and the number of test runs.
 * Tells how many test runs are executed in a given interval, throughout a
 * duration interval.
 */
public class TestFrequencyComputer {
    private final Duration stepInterval;
    private final Duration durationInterval;

    /**
     * Constructor for TestFrequencyComputer.
     *
     * @param stepInterval      the interval length of each step
     * @param durationInterval  the total duration interval
     */
    public TestFrequencyComputer(Duration stepInterval, Duration durationInterval) {
        this.stepInterval = stepInterval;
        this.durationInterval = durationInterval;
    }

    /**
     * Computes the frequency of test execution based on the `start_time` field
     * stored in a JSON file.
     *
     * @param testRuns the number of test runs
     * @return the frequency of test execution
     */


}
