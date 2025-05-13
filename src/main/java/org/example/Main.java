package org.example;

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
import org.kohsuke.github.GitHub;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import javax.xml.crypto.Data;

public class Main {
    public static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
        GitHub gh = GitHubAPIAuthHelper.getGitHubAPI();
        logger.info("Starting script");

        // exampleGet10MergesToMainGeit(gh);

        // edit these numbers before running
        // FetchFromJSON.fetch(gh, 10, 3);
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