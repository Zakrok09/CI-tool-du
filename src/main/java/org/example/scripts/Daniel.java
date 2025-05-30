package org.example.scripts;

import static org.example.Main.logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.Main;
import org.example.computation.DataComputor;
import org.example.computation.DataSaver;
import org.example.data.Repository;
import org.example.extraction.JGitCommitSampler;
import org.example.extraction.RepoRetrospect;
import org.example.fetching.CachedDataRepoFetcher;
import org.example.fetching.CachedGitCloner;
import org.example.utils.GitHubAPIAuthHelper;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.FileWriter;
import java.io.File;
import java.util.concurrent.CompletableFuture;

import org.example.extraction.RepoRetrospect.CommitPair;

public class Daniel {

    public static void danielLoad(String group) throws IOException {
        String path = "intake/" + group + ".json";
        String json = Files.readString(Paths.get(path));

        ObjectMapper mapper = new ObjectMapper();
        List<Object> rawItems = mapper.readValue(json, new TypeReference<>() {
        });

        List<String> items = rawItems.stream()
                .map(inner -> (String) inner)
                .limit(50)
                .toList();

        GitHubAPIAuthHelper ghHelper = new GitHubAPIAuthHelper();

        long start = System.nanoTime();
        try (ForkJoinPool customPool = new ForkJoinPool(1)) {
            customPool.submit(() -> {
                items.parallelStream().forEach(project -> {
                    try {
                        CachedGitCloner.getGit(project);
                        CachedDataRepoFetcher.getRepoData(ghHelper.getNextGH(), project);
                        Main.logger.info("{} downloaded successfully.", project);
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                });
            }).join();
        }
        long end = System.nanoTime();
        long elapsedSeconds = (end - start) / 1_000_000_000;
        long minutes = elapsedSeconds / 60;
        long seconds = elapsedSeconds % 60;
        Main.logger.info(String.format("Downloaded 80 projects in %dmin%02dsec.", minutes, seconds));
    }

    public static void danielKPI(String group) throws IOException {
        String path = "intake/" + group + ".json";
        String json = Files.readString(Paths.get(path));
        ObjectMapper mapper = new ObjectMapper();

        GitHubAPIAuthHelper ghHelper = new GitHubAPIAuthHelper();

        List<String> items = mapper.readValue(json, new TypeReference<>() {
        });
        List<Repository> repos = items.stream().map(project -> {
            try {
                return CachedDataRepoFetcher.getRepoData(ghHelper.getNextGH(), project);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).toList();

        Instant instant = LocalDate.of(2024, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Duration duration = Duration.ofDays(30);
        int count = 12;

        DataSaver.saveData(group + "-df", instant, duration, count, repos, DataComputor::computeDeliveryFrequency);
        DataSaver.saveData(group + "-clt", instant, duration, count, repos, DataComputor::computeCLT);
        DataSaver.saveData(group + "-ds", instant, duration, count, repos, DataComputor::computeDeliverySize);
        DataSaver.saveData(group + "-mttr", instant, duration, count, repos, DataComputor::computeMTTR);
        DataSaver.saveData(group + "-dc", instant, duration, count, repos, DataComputor::computeDefectCount);
    }

    public static void danielComments(String group) throws IOException {
        String path = "intake/" + group + ".json";
        String json = Files.readString(Paths.get(path));
        ObjectMapper mapper = new ObjectMapper();

        List<String> items = mapper.readValue(json, new TypeReference<>() {
        });

        Duration limit = Duration.ofDays(360 * 5);

        try (ForkJoinPool customPool = new ForkJoinPool(1)) {
            List<CompletableFuture<Void>> futures = items.stream()
                    .map(project -> CompletableFuture.runAsync(() -> {
                        try {
                            Git repoGit = CachedGitCloner.getGit(project);
                            JGitCommitSampler sampler = new JGitCommitSampler(repoGit.getRepository());
                            sampler.sampleCommitsWithDuration(Duration.ofDays(7), limit);
                            List<RevCommit> sampledCommits = sampler.getSampledCommits();

                            RepoRetrospect repoRetrospect = new RepoRetrospect(repoGit);

                            saveCommentData(project, repoRetrospect.commentPecentageWalk(sampledCommits));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }, customPool))
                    .toList();

            // Wait for all tasks to complete
            futures.forEach(CompletableFuture::join);
        }
    }

    private static void saveCommentData(String repoName, List<RepoRetrospect.CommitPair<Double>> pairs) {
        logger.info("Saving comment data for {}", repoName);

        String fileName = repoName.replace('/', '_') + "_comments" + ".csv";
        File output = new File("repos", fileName);

        try (FileWriter csvWriter = new FileWriter(output)) {
            csvWriter.append("commitSHA,commitDate,commentPercentage");
            csvWriter.append("\n");

            for (CommitPair<Double> pair : pairs) {
                csvWriter.append(pair.commit.getName());
                csvWriter.append(",").append(Integer.toString(pair.commit.getCommitTime()));
                csvWriter.append(",").append(Double.toString(pair.data));
                csvWriter.append("\n");
            }

            logger.info("Data for {} saved to {}", repoName, output.getAbsolutePath());
        } catch (IOException e) {
            logger.fatal("Error writing {} data: {}", repoName, e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }
}
