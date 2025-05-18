package org.example.extraction.ci;

import org.kohsuke.github.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.example.Main.logger;

public class CIWorkflowExtractor {
    private final GitHub gh;
    private final String repoName;

    public CIWorkflowExtractor(GitHub gh, String repoName) {
        this.gh = gh;
        this.repoName = repoName;
    }

    /**
     * Retrieves a list of workflows that have more than 10 runs and are executing tests from the specified repository.
     * @return a list of CIWorkflow instances, each representing a workflow and its content from the repository
     * @throws IOException if an I/O error occurs while fetching the workflows or their contents
     */
    public List<CIWorkflow> getValidWorkflows() throws IOException {
        GHRepository repository = gh.getRepository(repoName);
        List<CIWorkflow> res = new ArrayList<>();

        List<GHWorkflow> workflows = repository.listWorkflows().toList();
        workflows = workflows.stream()
                .filter(w -> hasMoreThanNRuns(w, 10))
                .toList();

        for (var workflow : workflows) {
            try {
                CIFileExtractor workflowFile = new CIFileExtractor(workflow);
                if (workflowFile.isDynamic()) {
                    logger.info("Skipping dynamic workflow: {}", workflow.getName());
                    continue;
                }

                String file_content = workflowFile.getFileContent();
                CIContentParser parser = new CIContentParser(file_content);
                CIWorkflow ciWorkflow = parser.produceCIWorkflow(workflow);

                if (!ciWorkflow.isExecutingTests()) {
                    logger.info("Workflow {} is not executing tests. Skipping...", workflow.getName());
                    continue;
                }

                res.add(ciWorkflow);
            } catch (IOException e) {
                logger.error("Failed to read file content for workflow: {}, {}, {}", workflow.getName(), workflow.getPath(), workflow.getHtmlUrl());
            }
        }

        return res;
    }

    /**
     * Checks if the workflows have been cached in a CSV file.
     */
    public boolean isCached() {
        Path dir = Paths.get("sampled_workflows");
        if (Files.notExists(dir)) {
            logger.error("Directory {} does not exist. Cannot save workflow runs.", dir);
            return false;
        }
        Path outputFile = dir.resolve(repoName.replace("/", "_") + ".csv");
        return Files.exists(outputFile);
    }

    /**
     * Checks if the workflow has more than a specified number of runs.
     *
     * @param ghWorkflow the workflow to check
     * @param n          the number of runs to compare against
     * @return true if the workflow has more than n runs, false otherwise
     */
    private boolean hasMoreThanNRuns(GHWorkflow ghWorkflow, int n) {
        int i = 0;
        for (GHWorkflowRun ignored : ghWorkflow.listRuns()) {
            i++;
            if (i > n) return true;
        }
        return false;
    }
}
