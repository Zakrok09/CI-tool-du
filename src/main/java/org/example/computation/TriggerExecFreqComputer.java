package org.example.computation;

import io.github.cdimascio.dotenv.Dotenv;
import org.example.data.WorkflowRun;
import org.example.extraction.ci.CIWorkflow;
import org.example.extraction.ci.KnownEvent;
import org.example.utils.GitHubAPIAuthHelper;
import org.example.utils.Helper;
import org.kohsuke.github.GitHub;

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

    /**
     * Calculates the frequency of workflow runs by KnownEvent.
     * @return a KnownEvent indexed array of integers, where the index is the event's enum value
     */
    public int[] frequencyOfTriggerExecutions() {
        int[] frequencies = new int[KnownEvent.values().length];
        GitHub gh = GitHubAPIAuthHelper.getGitHubAPI();
        Dotenv dotenv = Dotenv.load();
        String cutoffStr = dotenv.get("DATE_CUTOFF");
        Instant cutoff = Instant.parse(cutoffStr);

        for (String project : projects) {
            Path sampled_workflows = Path.of("sampled_workflows", project.replace("/", "_") + ".csv");

            if (!sampled_workflows.toFile().exists()) {
                throw new RuntimeException("sampled_workflows folder does not exist for project: " + project);
            }

            List<String> workflows = Helper.getFileLinesSafe(sampled_workflows).stream().skip(1).toList();
            int workflowRunCount = 0;
            for (String workflow : workflows) {
                // name;id;path;triggers
                String[] parts = workflow.split(";");

                if (parts.length < 4) {
                    logger.warn("Skipping malformed workflow line: {}", workflow);
                    continue;
                }

                String workflowRunsFile = project.replace("/", "_") + "-" + parts[1] + ".csv";
                KnownEvent[] workflowEvents = Arrays.stream(parts[3].split(","))
                        .map(String::trim).map(String::toUpperCase)
                        .map(KnownEvent::valueOf)
                        .toArray(KnownEvent[]::new);

                Path workflowRunsPath = Path.of("sampled_workflow_runs", workflowRunsFile);

                try (var lines = Files.lines(workflowRunsPath)) {
                    List<WorkflowRun> runs = lines.skip(1)
                            .map(WorkflowRun::fromCSV)
                            .filter(run -> run.createdAt.isAfter(cutoff))
                            .toList();
                    int workflow_runs = runs.size();

                    for (KnownEvent event : workflowEvents) {
                        frequencies[event.ordinal()] += workflow_runs;
                    }

                    workflowRunCount += workflow_runs;

//                            .map(WorkflowRun::fromCSV)
//                            .forEach(
//                                    run -> {
//                                        try {
//                                            GHWorkflowRun a = gh.getRepository(project).getWorkflowRun(run.id);
//                                            System.out.println(a.getEvent());
//                                        } catch (IOException e) {
//                                            logger.error("Error fetching workflow run for project: {}, id: {}", project, parts[1]);
//                                        }
//                                    }
//                            );

                } catch (IOException e) {
                    logger.error("Error reading workflow runs file: {}, skipping...", workflowRunsPath, e);
                }
            }

            System.out.println("Total workflow runs for project " + project + ": " + workflowRunCount);
        }

        return frequencies;
    }

//
//    public Map<String, Integer> frequencyOfTriggerExecutionsMAP() {
//        Map<String, Integer> res = new HashMap<>();
//        GitHub gh = GitHubAPIAuthHelper.getGitHubAPI();
//
//        for (String project : projects) {
//            Path sampled_workflows = Path.of("sampled_workflows", project.replace("/", "_") + ".csv");
//
//            if (!sampled_workflows.toFile().exists()) {
//                throw new RuntimeException("sampled_workflows folder does not exist for project: " + project);
//            }
//
//            List<String> workflows = getFileLinesSafe(sampled_workflows).stream().skip(1).toList();
//
//            for (String workflow : workflows) {
//                // name;id;path;triggers
//                String[] parts = workflow.split(";");
//
//                if (parts.length < 4) {
//                    logger.warn("Skipping malformed workflow line: {}", workflow);
//                    continue;
//                }
//
//                String workflowRunsFile = project.replace("/", "_") + "-" + parts[1] + ".csv";
//                KnownEvent[] workflowEvents = Arrays.stream(parts[3].split(","))
//                        .map(String::trim).map(String::toUpperCase)
//                        .map(KnownEvent::valueOf)
//                        .toArray(KnownEvent[]::new);
//
//                Path workflowRunsPath = Path.of("sampled_workflow_runs", workflowRunsFile);
//
//                String key = Arrays.stream(workflowEvents)
//                        .sorted()
//                        .map(Objects::toString)
//                        .collect(Collectors.joining(","));
//
//                try (var lines = Files.lines(workflowRunsPath)) {
//                    int workflow_runs = (int) lines.skip(1).count();
//
//                    if (res.containsKey(key)) {
//                        workflow_runs += res.get(key);
//                    }
//
//                    res.put(key, workflow_runs);
//
//
//                } catch (IOException e) {
//                    logger.error("Error reading workflow runs file: {}, skipping...", workflowRunsPath, e);
//                }
//            }
//        }
//
//        return res;
//    }


}
