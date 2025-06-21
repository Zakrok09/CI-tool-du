package org.example.extraction.log;

import io.github.cdimascio.dotenv.Dotenv;
import org.example.utils.GitHubAPIAuthHelper;
import org.example.utils.Helper;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static org.example.Main.logger;

public class CodeCovMain {

    public static void main(String[] args) {
        CoverageAndTestExtractor cate = new CoverageAndTestExtractor();

        for (String project : CoverageAndTestExtractor.cov_location.keySet()) {
            cate.extractCovAndTest(project);
        }
    }

//    public static void main(String[] args) throws IOException {
//        GitHub gh = GitHubAPIAuthHelper.getGitHubAPI();
//
//        Dotenv dotenv = Dotenv.load();
//        Instant cutoff = Instant.parse(dotenv.get("COV_CUTOFF"));
//
//        List<String> projectLines = Helper.getFileLinesSafe("code-cov-workflows.csv");
//
//        for (String projectLine : projectLines) {
//            try {
//                String project = projectLine.split(";")[0];
//                long id = Long.parseLong(projectLine.split(";")[2].trim());
//                String path_to_runs =
//                        "sampled_workflow_runs/" + project.replace("/", "_") + "-" + id + ".csv";
//
//                LogExtractor le = new LogExtractor(gh, project);
//
//                le.extractLogs(path_to_runs, cutoff);
//            } catch (NumberFormatException e) {
//                logger.error("Failed to parse id {}, line {}", projectLine.split(";")[2].trim(), projectLine);
//            }
//        }
//    }

}
