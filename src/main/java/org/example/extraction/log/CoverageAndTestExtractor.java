package org.example.extraction.log;

import kotlin.Pair;
import org.example.utils.Helper;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.example.Main.logger;

public class CoverageAndTestExtractor {

    public static final
    Map<String, String> cov_location = Map.of(
        "aws_aws-cdk", "0_Collect Coverage.txt",
        "bloomberg_memray", "0_coverage.txt",
        "bridgecrewio_checkov", "0_update-coverage.txt",
        "logto-io_logto", "0_report-coverage.txt",
        "microsoft_semantic-kernel", "0_python-tests-coverage.txt",
        "nodejs_node", "0_coverage-linux.txt",
        "simonw_datasette", "0_test.txt"
    );

    private static final
    Map<String, Pattern> pattern = Map.of(
            "aws_aws-cdk", Pattern.compile("Lines\\s+:\\s+(\\d+\\.\\d+)%"),
            "bloomberg_memray", Pattern.compile("lines\\.*: (\\d+\\.\\d+)%"),
            "bridgecrewio_checkov", Pattern.compile("TOTAL\\s+\\d+\\s+\\d+\\s+\\d+\\s+\\d+\\s+(\\d+)%"),
            "logto-io_logto", Pattern.compile("Lines\\s+:\\s+(\\d+\\.\\d+)%"),
            "microsoft_semantic-kernel", Pattern.compile("TOTAL\\s+\\d+\\s+\\d+\\s+(\\d+)%"),
            "nodejs_node", Pattern.compile("All files\\s*\\|\\s*(\\d+\\.\\d+)"),
            "simonw_datasette", Pattern.compile("TOTAL\\s+\\d+\\s+\\d+\\s+(\\d+)%")
    );


    public void extractCovAndTest(String project) {
        project = project.replace("/", "_");

        Path dir = Path.of("cov-logs/" + project);
        File project_cov_dir = dir.toFile();

        if (!project_cov_dir.isDirectory())
            logger.fatal("Something terribly wrong: {} is a file and not a directory", dir.toString());

        for (File date_folder : Objects.requireNonNull(project_cov_dir.listFiles())) {
            String date = date_folder.getName();

            Path path_to_parse = date_folder.toPath().resolve(cov_location.get(project));

            String coverage = get_coverage(project, path_to_parse);

            System.out.println(project + "," + date + "," + coverage);
        }
    }

    private static @NotNull String get_coverage(String project, Path path_to_parse) {
        String coverage = null;
        for (String line : Helper.getFileLinesSafe(path_to_parse)) {
            Matcher matcher = pattern.get(project).matcher(line);
            if (matcher.find() && !matcher.group(1).isEmpty()) {
                coverage = matcher.group(1);
                break;
            }
        }

        if (coverage == null) throw new IllegalStateException("Did not find coverage");
        return coverage;
    }
}
