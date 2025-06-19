package org.example.computation;

import io.github.cdimascio.dotenv.Dotenv;
import org.example.data.WorkflowRun;
import org.example.extraction.ci.KnownEvent;
import org.example.utils.Helper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
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
    private final String file;

    /**
     * Constructor for TestFrequencyComputer.
     *
     * @param stepInterval     the interval length of each step
     * @param durationInterval the total duration interval
     * @param file path from route to the csv with all projects to take
     */
    public TestFrequencyComputer(Duration stepInterval, Duration durationInterval, String file) {
        this.stepInterval = stepInterval;
        this.durationInterval = durationInterval;
        this.file = file;
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

        Dotenv dotenv = Dotenv.load();
        String cutoffStr = dotenv.get("DATE_CUTOFF");
        if (cutoffStr == null) throw new RuntimeException("DATE_CUTOFF env variable not set!");
        Instant start = Instant.parse(cutoffStr);

        int numberOfSteps = (int) (durationInterval.toMillis() / stepInterval.toMillis());
        Map<String, List<Integer>> frequencies = new HashMap<>();

        for (String repoName : workflowToRuns.keySet()) {
            frequencies.put(repoName, new ArrayList<>(Collections.nCopies(numberOfSteps, 0)));
        }

        for (String repoName : workflowToRuns.keySet()) {
            List<WorkflowRun> runs = workflowToRuns.get(repoName);

            for (WorkflowRun run : runs) {
                long millisSinceCutoff = run.start_time.toEpochMilli() - start.toEpochMilli();
                int stepIndex = (int) (millisSinceCutoff / stepInterval.toMillis());

                // Only count runs that fall within our analysis window
                if (stepIndex >= 0 && stepIndex < numberOfSteps) {
                    frequencies.get(repoName).set(stepIndex, frequencies.get(repoName).get(stepIndex) + 1);
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

        Dotenv dotenv = Dotenv.load();
        String cutoffStr = dotenv.get("DATE_CUTOFF");
        if (cutoffStr == null) throw new RuntimeException("DATE_CUTOFF env variable not set!");
        Instant start = Instant.parse(cutoffStr);

        List<String> projects = Helper.getFileLinesSafe(file);

        Map<String, List<WorkflowRun>> workflowRuns = new HashMap<>();

        for (String project : projects) {
            Path sampled_workflows = Path.of("sampled_workflows", project.replace("/", "_") + ".csv");

            if (!sampled_workflows.toFile().exists()) {
                throw new RuntimeException("sampled_workflows folder does not exist for project: " + project);
            }

            List<String> workflows = Helper.getFileLinesSafe(sampled_workflows).stream().skip(1).toList();
            for (String workflow : workflows) {
                String[] parts = workflow.split(";");

                if (parts.length < 4) {
                    logger.warn("Skipping malformed workflow line: {}", workflow);
                    continue;
                }

                String workflowRunsFile = project.replace("/", "_") + "-" + parts[1] + ".csv";
                Path workflowRunsPath = Path.of("sampled_workflow_runs", workflowRunsFile);

                for (String line : Helper.getFileLinesSafe(workflowRunsPath).stream().skip(1).toList()) {
                    if (line.trim().isEmpty()) continue;

                    WorkflowRun run = WorkflowRun.fromCSV(line);

                    if(run == null) continue;

                    if (run.start_time.isBefore(start)) continue;
                    workflowRuns.putIfAbsent(project, new ArrayList<>());
                    workflowRuns.get(project).add(run);
                }
            }

            System.out.println("Total workflow runs for project " + project + ": " + workflowRuns.getOrDefault(project, Collections.emptyList()).size());
        }

        return workflowRuns;
    }

}
