package org.example.computation;

import io.github.cdimascio.dotenv.Dotenv;
import org.example.data.WorkflowRun;
import org.example.extraction.ci.KnownEvent;
import org.example.utils.Helper;
import org.kohsuke.github.GHWorkflowRun;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

import static org.example.Main.logger;

public class TriggerExecFreqComputer {
    private final List<String> projects;

    public TriggerExecFreqComputer(String projectsCSVFile) {
        projects = Helper.getFileLinesSafe(projectsCSVFile);
    }


//    PULL_REQUEST;844904
//    PUSH;180423
//    SCHEDULE;26105
//    WORKFLOW_RUN;12746
//    WORKFLOW_DISPATCH;6852
//    MERGE_GROUP;636
//    RELEASE;321
//    REPOSITORY_DISPATCH;156

    private List<String> extractTriggersFromLine(String line) {
        String[] parts = line.split(";");
        if (parts.length < 4) {
            logger.warn("Line does not contain enough parts to extract triggers: {}", line);
            return Collections.emptyList();
        }

        return List.of(parts[3].toLowerCase().split(","));
    }

    public List<String> workflows_with_workflow_dispatch() {
        List<String> res = new ArrayList<>();

        Path dir = Path.of("sampled_workflows");
        if (!dir.toFile().exists()) throw new RuntimeException("sampled_workflows folder does not exist!");

        for (File file : Objects.requireNonNull(dir.toFile().listFiles())) {
            if (file.getName().endsWith(".csv")) {
                logger.debug("Parsing file: {}", file.getName());

                try (var lines = Files.lines(file.toPath())) {
                    lines.skip(1).forEach(line -> {
                        List<String> triggers  = extractTriggersFromLine(line);

                        if (triggers.contains("workflow_dispatch")) {
                            String workflowName = file.getName().replace(".csv", "") + "-" + line.split(";")[1] + ".csv";
                            res.add(workflowName);
                        }
                    });
                } catch (IOException e) {
                    logger.error("Error reading file: {}, skipping...", file.getName(), e);
                }
            }
        }

        return res;
    }

    /**
     * Calculates the frequency of workflow runs by KnownEvent.
     *
     * @return a KnownEvent indexed array of integers, where the index is the event's enum value
     */
    public int[] frequencyOfTriggerExecutions() {
        int[] frequencies = new int[KnownEvent.values().length];
        List<WorkflowRun> allRuns = allRuns();

        allRuns.forEach(run -> {
            try {
                KnownEvent event = KnownEvent.valueOf(run.event.trim());
                frequencies[event.ordinal()]++;
            } catch (IllegalArgumentException e) {
                logger.warn("Unknown event type in workflow run: {}", run.toCSV().trim());
            }
        });

        return frequencies;
    }

    public void statistics() {
        List<String> workflowsWithDispatch = workflows_with_workflow_dispatch();
        Dotenv dotenv = Dotenv.load();
        String cutoffStr = dotenv.get("DATE_CUTOFF");
        Instant cutoff = Instant.parse(cutoffStr);

        int totalPercentage = 0;

        for (String file : workflowsWithDispatch) {
            List<WorkflowRun> runs = runsForFile(file, cutoff);

            int totalRuns = runs.size();
            int runsTriggeredByDispatch = (int) runs.stream()
                    .filter(run -> run.event.equalsIgnoreCase("workflow_dispatch"))
                    .count();

            int percentageTriggeredByDispatch = (int) ((runsTriggeredByDispatch * 100.0) / totalRuns);
            totalPercentage += percentageTriggeredByDispatch;

            System.out.println(file + ";" + totalRuns + ";" + runsTriggeredByDispatch + ";" + percentageTriggeredByDispatch + "%");
        }

        System.out.println("Total percentage of runs triggered by workflow_dispatch: " + (totalPercentage / workflowsWithDispatch.size()) + "%");
        System.out.println("Variance of percentages: " + calculateVariance(workflowsWithDispatch, cutoff));

//        List<WorkflowRun> allRuns = allRuns();
//
//        List<WorkflowRun> failedRuns = new ArrayList<>();
//        List<WorkflowRun> successRuns = new ArrayList<>();
//        List<WorkflowRun> pushRuns = new ArrayList<>();
//        List<WorkflowRun> prRuns = new ArrayList<>();
//        List<WorkflowRun> anyOtherRuns = new ArrayList<>();
//
//        List<WorkflowRun> wfDispatchRuns = new ArrayList<>();
//        List<WorkflowRun> wfDispatchRunsFailed = new ArrayList<>();
//        List<WorkflowRun> wfDispatchRunsSuccess = new ArrayList<>();
//
//        List<String> workflowsWithDispatch = workflows_with_workflow_dispatch();
//
//        for (String file : workflowsWithDispatch) {
//            List<WorkflowRun> runs = runsForFile(file);
//            wfDispatchRuns.addAll(runs);
//        }
//
//        for (WorkflowRun run : wfDispatchRuns) {
//            if (run.status.equals(GHWorkflowRun.Conclusion.SUCCESS)) {
//                successRuns.add(run);
//            } else {
//                failedRuns.add(run);
//            }
//
//            if (run.event.equalsIgnoreCase("push")) {
//                pushRuns.add(run);
//            } else if (run.event.equalsIgnoreCase("pull_request")) {
//                prRuns.add(run);
//            } else {
//                anyOtherRuns.add(run);
//            }
//        }
//
//        for (WorkflowRun run : allRuns) {
//            if (run.status.equals(GHWorkflowRun.Conclusion.SUCCESS)) {
//                successRuns.add(run);
//            } else {
//                failedRuns.add(run);
//            }
//
//            if (run.event.equalsIgnoreCase("push")) {
//                pushRuns.add(run);
//            } else if (run.event.equalsIgnoreCase("pull_request")) {
//                prRuns.add(run);
//            } else {
//                anyOtherRuns.add(run);
//            }
//        }
//
//        System.out.println("Total workflow runs: " + allRuns.size());
//        System.out.println("Total successful runs: " + successRuns.size());
//        System.out.println("Total failed runs: " + failedRuns.size());
//        System.out.println("Total push runs: " + pushRuns.size());
//        System.out.println("Total pull request runs: " + prRuns.size());
//        System.out.println("Total other runs: " + anyOtherRuns.size());
//        System.out.println("Success rate: " + (successRuns.size() * 100.0 / allRuns.size()) + "%");
//        System.out.println("Failure rate: " + (failedRuns.size() * 100.0 / allRuns.size()) + "%");
//        System.out.println("Push runs success rate: " + (pushRuns.stream().filter(run -> run.status.equals(GHWorkflowRun.Conclusion.SUCCESS)).count() * 100.0 / pushRuns.size()) + "%");
//        System.out.println("Pull request runs success rate: " + (prRuns.stream().filter(run -> run.status.equals(GHWorkflowRun.Conclusion.SUCCESS)).count() * 100.0 / prRuns.size()) + "%");
//        System.out.println("Other runs success rate: " + (anyOtherRuns.stream().filter(run -> run.status.equals(GHWorkflowRun.Conclusion.SUCCESS)).count() * 100.0 / anyOtherRuns.size()) + "%");
//        System.out.println("Percentage of runs that are push: " + (pushRuns.size() * 100.0 / allRuns.size()) + "%");
//        System.out.println("Percentage of runs that are pull request: " + (prRuns.size() * 100.0 / allRuns.size()) + "%");
//        System.out.println("Percentage of runs that are other: " + (anyOtherRuns.size() * 100.0 / allRuns.size()) + "%");
    }

    private double calculateVariance(List<String> workflowsWithDispatch, Instant cutoff) {
        List<WorkflowRun> runs = new ArrayList<>();
        for (String file : workflowsWithDispatch) {
            runs.addAll(runsForFile(file, cutoff));
        }

        double mean = runs.stream()
                .mapToInt(run -> run.event.equalsIgnoreCase("workflow_dispatch") ? 1 : 0)
                .average()
                .orElse(0.0);

        return runs.stream()
                .mapToDouble(run -> Math.pow((run.event.equalsIgnoreCase("workflow_dispatch") ? 1 : 0) - mean, 2))
                .average()
                .orElse(0.0);
    }


    private List<WorkflowRun> allRuns() {
        List<WorkflowRun> res = new ArrayList<>();
        Dotenv dotenv = Dotenv.load();
        String cutoffStr = dotenv.get("DATE_CUTOFF");
        Instant cutoff = Instant.parse(cutoffStr);

        for (String project : projects) res.addAll(runsForProject(project, cutoff));

        return res;
    }

    private static List<WorkflowRun> runsForProject(String project, Instant cutoff) {
        List<WorkflowRun> res = new ArrayList<>();
        Path sampled_workflows = Path.of("sampled_workflows", project.replace("/", "_") + ".csv");

        if (!sampled_workflows.toFile().exists()) {
            throw new RuntimeException("sampled_workflows folder does not exist for project: " + project);
        }

        List<String> workflows = Helper.getFileLinesSafe(sampled_workflows).stream().skip(1).toList();
        for (String workflow : workflows) {
            // name;id;path;triggers
            String[] parts = workflow.split(";");

            if (parts.length < 4) {
                logger.warn("Skipping malformed workflow line: {}", workflow);
                continue;
            }

            String workflowRunsFile = project.replace("/", "_") + "-" + parts[1] + ".csv";
            Path workflowRunsPath = Path.of("sampled_workflow_runs", workflowRunsFile);

            try (var lines = Files.lines(workflowRunsPath)) {
                List<WorkflowRun> runs = lines.skip(1)
                        .map(WorkflowRun::fromCSV)
                        .filter(run -> run.createdAt.isAfter(cutoff))
                        .toList();

                res.addAll(runs);
            } catch (IOException e) {
                logger.error("Error reading workflow runs file: {}, skipping...", workflowRunsPath, e);
            }
        }

        return res;
    }

    private List<WorkflowRun> runsForFile(String file, Instant cutoff) {
        List<WorkflowRun> res = new ArrayList<>();
        Path workflowRunsPath = Path.of("sampled_workflow_runs", file);

        try (var lines = Files.lines(workflowRunsPath)) {
            res = lines.skip(1)
                    .map(WorkflowRun::fromCSV)
                    .filter(run -> run.createdAt.isAfter(cutoff))
                    .toList();
        } catch (IOException e) {
            logger.error("Error reading workflow runs file: {}, skipping...", workflowRunsPath, e);
        }

        return res;
    }
}
