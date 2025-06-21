package org.example.extraction.log;

import io.github.cdimascio.dotenv.Dotenv;
import org.example.utils.Helper;
import org.jetbrains.annotations.NotNull;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.example.Main.logger;

public class LogExtractor {
    private final GHRepository repository;

    public LogExtractor(GitHub gh, String repo) throws IOException {
        this.repository = gh.getRepository(repo);
    }

    public void extractLogs(String pathToRunsFile, Instant cutoff) throws IOException {
        List<String> runs = Helper.getFileLinesSafe(pathToRunsFile).stream().skip(1)
                .map(String::trim).filter(s -> !s.isEmpty())
                .filter(line -> {
                    try {
                        Instant start_time = Instant.parse(line.split(";")[4].trim());
                        return start_time.isAfter(cutoff) &&
                                start_time.isBefore(Instant.parse("2025-06-15T00:00:00Z")) &&
                                line.split(";")[7].equals("success");
                    } catch (ArrayIndexOutOfBoundsException e) {
                        logger.error("Mistyped line {}", line);
                        return false;
                    }
        }).toList();

        Instant walk = Instant.parse("2025-06-16T00:00:00Z");

        for (String run : runs) {
            String ran_date = getRanDate(run);
            Instant run_instant = getRanInstant(run);

            if (run_instant.isBefore(walk)) {
                long id = getId(run);
                String fileName = getFileName(ran_date);
                boolean success = downloadLogs(fileName, repository.getWorkflowRun(id).getLogsUrl());

                if (success)
                    walk = walk.minusSeconds(60 * 60 * 24 * 7);
            }
        }
    }

    private @NotNull String getFileName(String ran_date) {
        return "cov-logs/" +
                repository.getFullName().replace("/", "_") +
                "/" +
                ran_date.substring(0, 10);
    }

    private static String getRanDate(String run) {
        return run.split(";")[4];
    }

    private static Instant getRanInstant(String run) {
        return Instant.parse(run.split(";")[4]);
    }

    private static long getId(String run) {
        return Long.parseLong(run.split(";")[0].trim());
    }

    public static boolean downloadLogs(String fileName, URL logsUrl) throws IOException {
        String token = Dotenv.load().get("GITHUB_OAUTH");
        if (token == null) throw new IllegalStateException("GITHUB_OAUTH not set");

        HttpURLConnection connection = (HttpURLConnection) logsUrl.openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("Authorization", "Bearer " + token);
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");

        Path targetDir = Path.of(fileName).toAbsolutePath();
        try (ZipInputStream zipIn = new ZipInputStream(connection.getInputStream())) {
            for (ZipEntry ze; (ze = zipIn.getNextEntry()) != null;) {
                try {
                    Path resolvedPath = targetDir.resolve(ze.getName()).normalize();
                    if (ze.isDirectory()) {
                        Files.createDirectories(resolvedPath);
                    } else {
                        Files.createDirectories(resolvedPath.getParent());
                        Files.copy(zipIn, resolvedPath);
                    }
                } catch (FileAlreadyExistsException e) {
                    logger.warn("File {} already exists. Skipping...", ze.toString());
                }
            }
            logger.info("Logs for {}downloaded", fileName);
        } catch (FileNotFoundException e) {
            logger.warn("Failed downloading {}. Skipping to next one in month...", fileName);
            return false;
        }
        return true;
    }
}
