package org.example.extraction.ci;

import org.apache.commons.lang3.tuple.Pair;
import org.example.Main;
import org.example.computation.TestTriggerComputer;
import org.example.fetching.CachedDataRepoFetcher;
import org.example.fetching.CachedGitCloner;
import org.example.utils.GitHubAPIAuthHelper;
import org.kohsuke.github.GitHub;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.example.Main.logger;

public class CIExtractorMain {

    // https://open.spotify.com/track/4RvWPyQ5RL0ao9LPZeSouE?si=f4e83e3f85714521
    public static void main(String[] args) {
        logger.info("Starting CI workflow extraction");
        List<String> projectNames = getProjectsFromCSV("intake/pr-wf.csv");

        int totalTokens = 3;
        int totalProjects = projectNames.size();
        int batchSize = totalProjects / totalTokens;
        int remainder = totalProjects % totalTokens;

        List<Pair<Integer, List<String>>> searchPairs = new ArrayList<>();
        int currentIndex = 0;

        for (int i = 0; i < totalTokens; ++i) {
            int currentBatchSize = batchSize + (i < remainder ? 1 : 0);
            List<String> repos = new ArrayList<>();

            for (int k = 0; k < currentBatchSize && currentIndex < totalProjects; ++k) {
                repos.add(projectNames.get(currentIndex));
                currentIndex++;
            }

            searchPairs.add(Pair.of(i, repos));
        }

        long start = System.nanoTime();
        GitHubAPIAuthHelper ghHelper = new GitHubAPIAuthHelper();
        searchPairs.parallelStream().forEach(pair -> {
            int index = pair.getKey();
            List<String> repos = pair.getValue();

            int startIndex = index * batchSize;
            int endIndex = (index != totalTokens - 1) ? startIndex + batchSize : projectNames.size();

            GitHub gh = ghHelper.getNextGH();

            logger.info("Starting thread for token index {} with start {} and end {}",
                    index, startIndex, endIndex);
             extractCIWorkflowsToFiles(gh, repos);
//             saveAllWorkflowRunsFromExtracted(gh, repos);
        });

        long end = System.nanoTime();
        long elapsedSeconds = (end - start) / 1_000_000_000;
        long minutes = elapsedSeconds / 60;
        long seconds = elapsedSeconds
                % 60;
        Main.logger
                .info(String.format("Downloaded %d projects in %dmin%02dsec.", projectNames.size(), minutes, seconds));

        // Original extraction
        // extractCIWorkflowsToFiles(gh, projectNames);
        // saveAllWorkflowRunsFromExtracted(gh, projectNames);
    }

    public static List<String> getProjectsFromCSV(String fileName) {
        File csvFile = new File(fileName);
        if (!csvFile.exists()) {
            logger.error("{} file does not exist.", fileName);
            return List.of();
        }

        try {
            return new ArrayList<>(Files.readAllLines(csvFile.toPath()));
        } catch (IOException e) {
            logger.error("Failed to read projects from CSV: {}", e.getMessage());
            return List.of();
        }
    }

    public static List<String> getProjectsFromCSV() {
        return getProjectsFromCSV("projects.csv");
    }

    public static void extractCIWorkflowsToFiles(GitHub gh, List<String> projectNames) {
        for (String project : projectNames) {
            CIWorkflowExtractor ciWorkflowExtractor = new CIWorkflowExtractor(gh, project);
            if (ciWorkflowExtractor.isCached()) {
                logger.info("Data for {} found. Skipping...", project);
                continue;
            }

            try {
                logger.info("Extracting {} workflows", project);
                List<CIWorkflow> workflows = ciWorkflowExtractor.getValidWorkflows();

                if (workflows.isEmpty()) {
                    saveExcludedToCSV(project);
                } else {
                    saveToCSV(project, workflows);
                }
            } catch (Exception e) {
                logger.error("Failed to extract workflows for {}: {}", project, e.getMessage());
            }
        }
    }

    /**
     * Saves the workflows to a CSV file.
     */
    private static void saveToCSV(String repoName, List<CIWorkflow> ciWorkflows) throws IOException {
        Path dir = Paths.get("sampled_workflows");
        if (Files.notExists(dir))
            Files.createDirectory(dir);

        Path outputFile = dir.resolve(repoName.replace("/", "_") + ".csv");
        if (Files.exists(outputFile)) {
            logger.warn("File with data for {} found. Not overwriting...", repoName);
            return;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
            writer.write("name;id;path;triggers\n");
            for (CIWorkflow workflow : ciWorkflows) {
                writer.write(workflow.toCSV() + "\n");
            }
        } catch (IOException e) {
            logger.error("Failed to save workflows to CSV: {}", e.getMessage());
        }
    }

    private static void saveExcludedToCSV(String project) {
        try (FileWriter fw = new FileWriter("excluded_by_workflow.csv", true);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter out = new PrintWriter(bw)) {
            out.println(project);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves all workflow runs from the extracted workflows.
     */
    public static void saveAllWorkflowRunsFromExtracted(GitHub gh, List<String> projectNames) {
        Path dir = Paths.get("sampled_workflows");
        if (Files.notExists(dir)) {
            logger.error("Directory {} does not exist. Cannot save workflow runs.", dir);
            return;
        }

        for (String project : projectNames) {
            Path filePath = dir.resolve(project.replace("/", "_") + ".csv");
            if (!Files.exists(filePath)) {
                logger.error("File {} does not exist. Cannot save workflow runs.", filePath);
                continue;
            }

            try {
                List<Integer> workflowIds = readIdsFromCSV(filePath);
                CIWorkflowRunExtractor ciWorkflowRunWriter = new CIWorkflowRunExtractor(gh);
                for (int workflow : workflowIds)
                    ciWorkflowRunWriter.saveTestWorkflowRuns(project, workflow);
            } catch (Exception e) {
                logger.error("Failed to save workflow runs for {}: {}", project, e.getMessage());
            }
        }
    }

    public void calculateTestFrequency() {

    }

    private static List<Integer> readIdsFromCSV(Path filePath) throws IOException {
        return Files.readAllLines(filePath)
                .stream()
                .map(line -> line.split(";"))
                .skip(1)
                .filter(parts -> parts.length == 4)
                .map(parts -> parts[1].trim())
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }
}
