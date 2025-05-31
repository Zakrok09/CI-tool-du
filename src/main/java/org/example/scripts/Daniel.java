package org.example.scripts;

import static org.example.Main.logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.cdimascio.dotenv.Dotenv;

import org.example.Main;
import org.example.computation.DataComputor;
import org.example.computation.DataSaver;
import org.example.data.Repository;
import org.example.extraction.JGitCommitSampler;
import org.example.extraction.RepoRetrospect;
import org.example.fetching.CachedDataRepoFetcher;
import org.example.fetching.CachedGitCloner;
import org.example.utils.GitHubAPIAuthHelper;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.FileWriter;
import java.io.File;
import java.util.concurrent.CompletableFuture;

import org.example.extraction.RepoRetrospect.CommitPair;
import org.example.utils.ProjectListOps;

public class Daniel {

    public static void danielLoad(String group) throws IOException {
        List<String> input = ProjectListOps.getProjectListFromFile("intake/" + group + ".txt");

        int totalTokens = Dotenv.load().get("TOKEN_POOL").split(",").length;
        int threadsPerToken = 2;
        int batchSize = input.size() / totalTokens / threadsPerToken;
        
        List<Pair<Integer, List<String>>> searchPairs = new ArrayList<>();
        for (int i = 0; i < totalTokens; ++i) {
            for (int j = 0; j < threadsPerToken; ++j) {
                int index = i * threadsPerToken + j;
                List<String> repos = new ArrayList<>();

                for (int k = 0; k < batchSize; ++k) {
                    repos.add(input.get(index * batchSize + k));
                }
                searchPairs.add(Pair.of(index, repos));
            }
        }

        long start = System.nanoTime();
        GitHubAPIAuthHelper ghHelper = new GitHubAPIAuthHelper();
        searchPairs.parallelStream().forEach(pair -> {
            int index = pair.getKey();
            List<String> repos = pair.getValue();

            int startIndex = index * batchSize;
            int endIndex = (index != totalTokens * threadsPerToken - 1) ? startIndex + batchSize : input.size();

            logger.info("Starting thread for token index {} with start {} and end {}",
                    index, startIndex, endIndex);
            repos.forEach(project -> {
                try {
                        CachedGitCloner.getGit(project);
                        CachedDataRepoFetcher.getRepoData(ghHelper.getNextGH(), project);
                        Main.logger.info("{} downloaded successfully.", project);
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
            });
        });

        long end = System.nanoTime();
        long elapsedSeconds = (end - start) / 1_000_000_000;
        long minutes = elapsedSeconds / 60;
        long seconds = elapsedSeconds
                % 60;
        Main.logger.info(String.format("Downloaded %d projects in %dmin%02dsec.", input.size(), minutes, seconds));
    }

    public static void danielKPI(String group) throws IOException {
        String path = "intake/" + group + ".txt";     
        List<String> items = ProjectListOps.getProjectListFromFile(path);

        GitHubAPIAuthHelper ghHelper = new GitHubAPIAuthHelper();
        List<Repository> repos = items.stream().map(project -> {
            try {
                return CachedDataRepoFetcher.getRepoData(ghHelper.getNextGH(), project);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).toList();

        Instant instant = LocalDate.of(2025, 5, 1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Duration duration = Duration.ofDays(30);
        int count = 12;

        DataSaver.saveData(group + "-df", instant, duration, count, repos, DataComputor::computeDeliveryFrequency);
        DataSaver.saveData(group + "-clt", instant, duration, count, repos, DataComputor::computeCLT);
        DataSaver.saveData(group + "-ds", instant, duration, count, repos, DataComputor::computeDeliverySize);
        DataSaver.saveData(group + "-mttr", instant, duration, count, repos, DataComputor::computeMTTR);
        DataSaver.saveData(group + "-dc", instant, duration, count, repos, DataComputor::computeDefectCount);
    }

    public static void danielComments(String group, int stepInDays, int threads) throws IOException {
        List<String> items = ProjectListOps.getProjectListFromFile("intake/" + group + ".txt");

        Instant dateCutoff = Instant.parse(Dotenv.load().get("DATE_CUTOFF"));

        try (ForkJoinPool customPool = new ForkJoinPool(threads)) {
            List<CompletableFuture<Void>> futures = items.stream()
                    .map(project -> CompletableFuture.runAsync(() -> {
                        try {
                            Git repoGit = CachedGitCloner.getGit(project);
                            JGitCommitSampler sampler = new JGitCommitSampler(repoGit.getRepository());
                            sampler.sampleCommitsWithDuration(Duration.ofDays(stepInDays), dateCutoff);
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
