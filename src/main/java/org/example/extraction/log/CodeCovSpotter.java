package org.example.extraction.log;

import org.example.utils.Helper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class CodeCovSpotter {
    private final List<String> projects;

    private static final Pattern codeCovRegex =
            Pattern.compile("\\b(coverage|codecov)\\b", Pattern.CASE_INSENSITIVE);

    public CodeCovSpotter(String projectsCSVFile) {
        projects = Helper.getFileLinesSafe(projectsCSVFile);
    }

    public List<String> spotCovs() {
        Path dir = Path.of("sampled_workflows");
        if (!dir.toFile().exists()) throw new RuntimeException("sampled_workflow_runs folder does not exist!");
        List<String> res = new ArrayList<>();

        for (String project : projects) {
            Path sampled_workflows = Path.of("sampled_workflows", project.replace("/", "_") + ".csv");

            if (!sampled_workflows.toFile().exists()) {
                throw new RuntimeException("sampled_workflows folder does not exist for project: " + project);
            }

            List<String> workflows = Helper.getFileLinesSafe(sampled_workflows).stream().skip(1).toList();

            for (String workflow : workflows) {
                if (!codeCovRegex.matcher(workflow.split(";")[0]).find()) continue;
                res.add(project + ";" + workflow);
            }
        }

        return res;
    }
}
