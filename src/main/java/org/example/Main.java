package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Cache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.example.computation.DataComputor;
import org.example.computation.DataSaver;
import org.example.data.Repository;
import org.example.fetching.CachedDataRepoFetcher;
import org.example.fetching.CachedGitCloner;
import org.example.fetching.FetchFromJSON;
import org.example.utils.GitHubAPIAuthHelper;
import org.example.utils.QuadFunction;
import org.kohsuke.github.GitHub;

import java.io.File;
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

import javax.xml.crypto.Data;

public class Main {
    public static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
        logger.info("Starting script");

//        serbanLoad();

//        serbanKPI(null, "main");
    }

    private static void serbanLoad() throws IOException {
        String path = "intake/small20.json";
        String json = Files.readString(Paths.get(path));

        ObjectMapper mapper = new ObjectMapper();
        List<List<Object>> rawItems = mapper.readValue(json, new TypeReference<>() {});

        List<String> items = rawItems.stream()
                .map(inner -> (String) inner.getFirst())
                .toList();

        GitHubAPIAuthHelper ghHelper = new GitHubAPIAuthHelper();

        long start = System.nanoTime();
        ForkJoinPool customPool = new ForkJoinPool(2);
        customPool.submit(() -> {
            items.parallelStream().forEach(project -> {
                try {
                    CachedGitCloner.getGit(project);
                    CachedDataRepoFetcher.getRepoData(ghHelper.getNextGH(), project);
                    logger.info("{} downloaded successfully.", project);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            });
        }).join();
        long end = System.nanoTime();
        long elapsedSeconds = (end - start) / 1_000_000_000;
        long minutes = elapsedSeconds / 60;
        long seconds = elapsedSeconds % 60;
        logger.info(String.format("Downloaded 20 projects in %dmin%02dsec.", minutes, seconds));
    }

    private static void serbanKPI(GitHub gh, String group) throws IOException {
        String path = "intake/" + group + ".json";
        String json = Files.readString(Paths.get(path));
        ObjectMapper mapper = new ObjectMapper();

        List<String> items = mapper.readValue(json, new TypeReference<>() {});
        List<Repository> repos = items.stream().map(x -> {
            try {
                return CachedDataRepoFetcher.getRepoData(gh, x);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).toList();


        Instant instant = LocalDate.of(2025, 5, 1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Duration duration =  Duration.ofDays(30);
        DataSaver.saveData(group + "-clt", instant, duration, 12, repos, DataComputor::computeCLT);
    }

    private static void exampleGet10MergesToMainGeit(GitHub gh) throws IOException {
        // Small example usage
        // Use repo data to identify default branch, then from the Git object
        // traverse commits to the default branch
        // and print the first 10
        // TL;DR get last 10 merges to main
        Repository geitRepo = CachedDataRepoFetcher.getRepoData(gh, "aipotheosis-labs/aci");
        // Git geit = CachedGitCloner.getGit("kelhaji/geit");


        // ObjectId mainBranch = geit.getRepository().resolve(geitRepo.defaultBranch);

        // try (RevWalk walk = new RevWalk(geit.getRepository())) {
        //     RevCommit headCommit = walk.parseCommit(mainBranch);

        //     walk.markStart(headCommit);

        //     int count = 0;
        //     for (RevCommit commit : walk) {
        //         if (commit.getParentCount() > 1) {
        //             logger.debug("Merge commit: {} ({})", commit.getShortMessage(), commit.getName());
        //             count++;
        //             if (count >= 10) {
        //                 break;
        //             }
        //         }
        //     }
        // }

        // Example defect counts
        // DataSaver.<Integer>saveData("defectCount", Instant.now(), Duration.ofDays(365), 5, List.of(geitRepo), DataComputor::computeDefectCount);

        // Example MTTRs
        // DataSaver.<Double>saveData("MTTR", Instant.now(), Duration.ofDays(365), 5, List.of(geitRepo), DataComputor::computeMTTR);

        // Example CFRs
        DataSaver.<Double>saveData("CFR", Instant.now(), Duration.ofDays(365), 5, List.of(geitRepo), DataComputor::computeCFR);
    }
}