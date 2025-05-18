package org.example.extraction.ci;

import org.example.utils.GitHubAPIAuthHelper;
import org.kohsuke.github.GitHub;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.example.Main.logger;

public class CIExtractorMain {

    public static void main(String[] args) {
        GitHub gh = GitHubAPIAuthHelper.getGitHubAPI();
        logger.info("Starting CI workflow extraction");
        List<String> projectNames = getProjectsFromCSV();

        extractCIWorkflowsToFiles(gh, projectNames);
        saveAllWorkflowRunsFromExtracted(gh, projectNames);
    }

    private static List<String> getProjectsFromCSV() {
        File csvFile = new File("projects.csv");
        if (!csvFile.exists()) {
            logger.error("projects.csv file does not exist.");
            return List.of();
        }

        try {
            return new ArrayList<>(Files.readAllLines(csvFile.toPath()));
        } catch (IOException e) {
            logger.error("Failed to read projects from CSV: {}", e.getMessage());
            return List.of();
        }
    }

    public static void extractCIWorkflowsToFiles(GitHub gh, List<String> projectNames) {
        for (String project : projectNames) {
            CIWorkflowExtractor ciWorkflowExtractor = new CIWorkflowExtractor(gh, project);
            if(ciWorkflowExtractor.isCached()) {
                logger.info("Data for {} found. Skipping...", project);
                continue;
            }

            try {
                logger.info("Extracting {} workflows", project);
                List<CIWorkflow> workflows = ciWorkflowExtractor.getValidWorkflows();
                saveToCSV(project, workflows);
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
        if (Files.notExists(dir)) Files.createDirectory(dir);

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
                for (int workflow : workflowIds) ciWorkflowRunWriter.saveTestWorkflowRuns(project, workflow);
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
