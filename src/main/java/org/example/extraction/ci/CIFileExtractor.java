package org.example.extraction.ci;

import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHWorkflow;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static org.example.Main.logger;

public class CIFileExtractor {
    private final GHWorkflow workflow;
    private final String workflowPath;

    public CIFileExtractor(GHWorkflow workflow) {
        this.workflow = workflow;
        this.workflowPath = workflow.getPath();
    }

    public boolean isDynamic() {
        return workflowPath.split("/")[0].trim().equals("dynamic");
    }

    /**
     * Retrieves the content of the workflow file from the GitHub repository.
     * @return the content of the workflow file as a String
     * @throws IOException if an I/O error occurs while fetching the file content
     */
    public String getFileContent() throws IOException {
        if (isDynamic()) {
            logger.warn("Attempted to get contents from a dynamic workflow. Skipping...: {}", workflow.getName());
            return null;
        }

        GHContent ghContent = workflow.getRepository().getFileContent(workflowPath);
        if (ghContent == null) {
            logger.warn("No content found for workflow: {}", workflow.getName());
            return null;
        }

        InputStream is = ghContent.read();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        return reader.lines().collect(Collectors.joining("\n"));
    }
}
