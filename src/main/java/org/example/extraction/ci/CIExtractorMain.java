package org.example.extraction.ci;

import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.example.Main.logger;

public class CIExtractorMain {
    private final List<String> projects;
    private final GitHub gh;

    public CIExtractorMain(GitHub gh, List<String> projects) {
        this.projects = projects;
        this.gh = gh;
    }

    public void extractCIWorkflowsToFiles() {
        for (String project : projects) {
            CIWorkflowExtractor ciWorkflowExtractor = new CIWorkflowExtractor(gh, project);

            try {
                logger.info("Extracting {} test frequency", project);
                List<CIWorkflow> workflows = ciWorkflowExtractor.getValidWorkflows();
                ciWorkflowExtractor.saveToCSV(workflows);
            } catch (Exception e) {
                logger.error("Failed to extract test frequency for {}: {}", project, e.getMessage());
            }
        }
    }

    public void saveAllWorkflowRunsFromExtracted() {
        Path dir = Paths.get("sampled_workflows");
        if (Files.notExists(dir)) {
            logger.error("Directory {} does not exist. Cannot save workflow runs.", dir);
            return;
        }

        for (String project : projects) {
            Path filePath = dir.resolve(project.replace("/", "_") + ".csv");
            if (!Files.exists(filePath)) {
                logger.error("File {} does not exist. Cannot save workflow runs.", filePath);
                continue;
            }

            try {
                List<Integer> workflowIds = readIdsFromCSV(filePath);
                CIWorkflowRunWriter ciWorkflowRunWriter = new CIWorkflowRunWriter(gh);
                for (int workflow : workflowIds) ciWorkflowRunWriter.saveTestWorkflowRuns(project, workflow);
            } catch (Exception e) {
                logger.error("Failed to save workflow runs for {}: {}", project, e.getMessage());
            }
        }
    }

    public void calculateTestFrequency() {

    }

    private List<Integer> readIdsFromCSV(Path filePath) throws IOException {
        return Files.readAllLines(filePath)
                .stream()
                .map(line -> line.split(","))
                .filter(parts -> parts.length == 2)
                .map(parts -> parts[1].trim())
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

}
