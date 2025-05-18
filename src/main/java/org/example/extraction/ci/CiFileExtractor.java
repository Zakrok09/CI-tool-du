package org.example.extraction.ci;

import org.kohsuke.github.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CiFileExtractor {
    private final GitHub gh;
    private final String repoName;

    public CiFileExtractor(GitHub gh, String repoName) {
        this.gh = gh;
        this.repoName = repoName;
    }

    /**
     * Retrieves a list of workflows from the specified repository.
     *
     * @return a list of CIWorkflow instances, each representing a workflow and its content from the repository
     * @throws IOException if an I/O error occurs while fetching the workflows or their contents
     */
    public List<CIWorkflow> getWorkflows() throws IOException {
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
