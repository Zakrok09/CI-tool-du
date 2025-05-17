package org.example.extraction.ci;

import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CiFileExtractor {
    private final GitHub gh;
    private final String repoName;
    private final static Logger logger = Logger.getLogger(CiFileExtractor.class.getName());

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

        for (var workflow : repository.listWorkflows()) {
            GHContent workflowGHContent = repository.getFileContent(workflow.getPath());
            String file_content = get_file_content(workflowGHContent);

            CIContentParser parser = new CIContentParser();

            Map<String, Integer> triggers = parser.parseWorkflow(file_content);

            List<String> triggersList = triggers.entrySet()
                    .stream().filter(e -> e.getValue() > 0)
                    .map(Map.Entry::getKey)
                    .toList();

            res.add(new CIWorkflow(workflow, file_content, triggersList));
        }

        return res;
    }

    private String get_file_content(GHContent ghContent) throws IOException {
        InputStream is = ghContent.read();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        return reader.lines().collect(Collectors.joining("\n"));
    }
}
