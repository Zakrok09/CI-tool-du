package org.example.extraction.ci;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.data.WorkflowRun;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHWorkflowRun;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;

import static org.example.Main.logger;

public class CIWorkflowRunWriter {
    private final GitHub gh;

    public CIWorkflowRunWriter(GitHub gh) {
        this.gh = gh;
    }

    /**
     * Save all workflow runs for a given repo and workflow ID.
     */
    public void saveTestWorkflowRuns(String repoName, int workflowId) {
        Path dir = Paths.get("sampled_workflow_runs");
        Path tempFile = dir.resolve(repoName.replace("/", "_") + "-" + workflowId + ".tmp.json");
        Path outputFile = dir.resolve(repoName.replace("/", "_") + "-" + workflowId + ".json");

        if (Files.exists(outputFile)) {
            logger.info("Data for {} found. Skipping...", repoName);
            return;
        }

        try {
            if (Files.notExists(dir)) Files.createDirectory(dir);

            ObjectMapper mapper = getObjectMapper();

            Instant cutoff = Instant.parse("2024-05-08T12:00:00Z");
            GHRepository repo = gh.getRepository(repoName);
            PagedIterable<GHWorkflowRun> runs = repo.getWorkflow(workflowId).listRuns();

            writeRunsToFileWithTemp(tempFile, runs, cutoff, mapper);

            Files.move(tempFile, outputFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            System.out.println("Saved " + repoName + " with workflow runs.");
        } catch (IOException e) {
            System.err.println("Failed to save workflow runs for " + repoName + ": " + e.getMessage());
        }
    }

    private static ObjectMapper getObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    private static void writeRunsToFileWithTemp(Path tempFile, PagedIterable<GHWorkflowRun> runs, Instant cutoff, ObjectMapper mapper) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardOpenOption.CREATE)) {
            writer.write("[\n");

            boolean first = true;
            for (GHWorkflowRun run : runs) {
                if (run.getCreatedAt().isBefore(cutoff)) break;
                System.out.println(run.getRunStartedAt() + " " + run.getWorkflowId() + " " + run.getName());
                try {
                    WorkflowRun runData = new WorkflowRun(run);
                    if (!first) writer.write(",\n");
                    writer.write(mapper.writeValueAsString(runData));
                    first = false;
                    writer.write(mapper.writeValueAsString(runData));
                    writer.newLine();
                } catch (Exception e) {
                    System.err.println("Error processing run " + run.getId() + ": " + e.getMessage());
                }
            }

            writer.write("\n]");
            writer.flush();
        }
    }
}
