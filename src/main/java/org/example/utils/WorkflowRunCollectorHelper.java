package org.example.utils;

import org.example.extraction.ci.CIWorkflowRunExtractor;
import org.kohsuke.github.GitHub;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.example.Main.logger;

public class WorkflowRunCollectorHelper {

    public static void collectFromCSV(String file, GitHub gh) {
        try {
            Path inputCsv = Paths.get(file);

            try (BufferedReader reader = Files.newBufferedReader(inputCsv)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length != 2) continue;

                    String repoName = parts[0].trim();
                    int workflowId;
                    try {
                        workflowId = Integer.parseInt(parts[1].trim());
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid workflow ID in line: {}", line);
                        continue;
                    }

                    try {
                        CIWorkflowRunExtractor extractor = new CIWorkflowRunExtractor(gh);
                        extractor.saveTestWorkflowRuns(repoName, workflowId);
                    } catch (Exception e) {
                        logger.error("Exception while saving data for {}: {}", repoName, e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Failed to initialize GitHub client or read CSV: {}", e.getMessage());
        }
    }
}
