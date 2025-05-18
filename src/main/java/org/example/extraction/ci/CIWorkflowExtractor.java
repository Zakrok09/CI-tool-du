package org.example.extraction.ci;

import org.kohsuke.github.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            String file_content = getFileContent(repository.getFileContent(workflow.getPath()));

            CIContentParser parser = new CIContentParser(file_content);
            if (!parser.isExecutingTests()) continue;

            Map<String, Integer> triggers = parser.parseWorkflow();
            List<String> triggersList = triggers.entrySet()
                    .stream().filter(e -> e.getValue() > 0)
                    .map(Map.Entry::getKey)
                    .toList();

            res.add(new CIWorkflow(workflow, file_content, triggersList));
        }

        return res;
    }

    public void saveToCSV(List<CIWorkflow> ciWorkflows) throws IOException {
        Path dir = Paths.get("sampled_workflows");
        if (Files.notExists(dir)) Files.createDirectory(dir);

        Path outputFile = dir.resolve(repoName.replace("/", "_") + ".csv");
        if (Files.exists(outputFile)) {
            System.out.println("Data for " + repoName + " found. Skipping...");
            return;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
            writer.write("name;id;path;triggers\n");
            for (CIWorkflow workflow : ciWorkflows) {
                writer.write(workflow.toCSV() + "\n");
            }
        }
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

    private String getFileContent(GHContent ghContent) throws IOException {
        InputStream is = ghContent.read();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        return reader.lines().collect(Collectors.joining("\n"));
    }
}
