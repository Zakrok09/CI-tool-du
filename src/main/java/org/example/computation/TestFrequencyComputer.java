package org.example.computation;

import org.example.data.WorkflowRun;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.example.Main.logger;

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
     * @param stepInterval     the interval length of each step
     * @param durationInterval the total duration interval
     */
    public TestFrequencyComputer(Duration stepInterval, Duration durationInterval) {
        this.stepInterval = stepInterval;
        this.durationInterval = durationInterval;
    }

    /**
     * Calculates the frequency of test execution based on duration and step intervals.
     * A test run appears in a step if it started between the start and end of the step.
     *
     * @return a map of integers showing test executions for that step.
     * The length of the list is equal to the number of steps in the duration interval, so duration/stepInterval.
     */
    public Map<String, List<Integer>> calculateFrequency() {
        Map<String, List<WorkflowRun>> workflowToRuns = parseFiles();

        // Find the earliest start time across all runs
        long intervalStartMillis = workflowToRuns.values().stream()
                .flatMap(List::stream)
                .mapToLong(run -> run.start_time.toEpochMilli())
                .min()
                .orElse(System.currentTimeMillis());

        int numberOfSteps = (int) (durationInterval.toMillis() / stepInterval.toMillis());
        Map<String, List<Integer>> frequencies = new HashMap<>();

        for (String repoName : workflowToRuns.keySet()) {
            frequencies.put(repoName, new ArrayList<>(Collections.nCopies(numberOfSteps, 0)));
        }

        for (String repoName : workflowToRuns.keySet()) {
            List<WorkflowRun> runs = workflowToRuns.get(repoName);
            for (WorkflowRun run : runs) {
                long stepIndex = (run.start_time.toEpochMilli() - intervalStartMillis) / stepInterval.toMillis();
                if (stepIndex >= 0 && stepIndex < numberOfSteps) {
                    frequencies.get(repoName).set((int) stepIndex, frequencies.get(repoName).get((int) stepIndex) + 1);
                }
            }
        }

        return frequencies;
    }

    /**
     * Parses the sampled workflow runs from CSV files in the specified directory.
     *
     * @return a map of repository names to lists of WorkflowRun objects
     */
    public Map<String, List<WorkflowRun>> parseFiles() {
        Path dir = Path.of("sampled_workflow_runs");
        if (!dir.toFile().exists()) throw new RuntimeException("sampled_workflow_runs folder does not exist!");

        Map<String, List<WorkflowRun>> workflowRuns = new HashMap<>();

        for (File file : Objects.requireNonNull(dir.toFile().listFiles())) {
            if (file.getName().endsWith(".csv")) {
                logger.debug("Parsing file: {}", file.getName());
                String repoName = getRepoName(file.getName());

                try (var lines = Files.lines(file.toPath())) {
                    List<WorkflowRun> runs = parseLines(lines);
                    addToMapOrAppend(workflowRuns, repoName, runs);
                    workflowRuns.put(repoName, runs);

                    logger.debug("Parsed {} runs for repository: {}", runs.size(), repoName);
                } catch (IOException e) {
                    logger.error("Error reading file: {}, skipping...", file.getName(), e);
                }
            }
        }

        return workflowRuns;
    }

    private List<WorkflowRun> parseLines(Stream<String> lines) throws IOException {
        return lines.skip(1)
                .map(WorkflowRun::fromCSV)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private void addToMapOrAppend(Map<String, List<WorkflowRun>> map, String key, List<WorkflowRun> runs) {
        if (map.containsKey(key)) {
            map.get(key).addAll(runs);
        } else {
            map.put(key, runs);
        }
    }

    private String getRepoName(String fileName) {
        String[] repoArray = fileName.replace(".csv", "").split("-");

        StringBuilder repoNameBuilder = new StringBuilder();
        for (int i = 0; i < repoArray.length - 1; i++) {
            repoNameBuilder.append(repoArray[i]);
        }
        return repoNameBuilder.toString();
    }

}
