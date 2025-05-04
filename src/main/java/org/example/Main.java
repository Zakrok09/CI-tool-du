package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.example.computation.DataComputor;
import org.example.data.Repository;
import org.example.fetching.CachedDataRepoFetcher;
import org.example.fetching.CachedGitCloner;
import org.example.utils.GitHubAPIAuthHelper;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

public class Main {
    public static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
        GitHub gh = GitHubAPIAuthHelper.getGitHubAPI();
        logger.info("Starting script");

        // Small example usage
        // Use repo data to identify default branch, then from the Git object
        // traverse commits to the default branch
        // and print the first 10
        // TL;DR get last 10 merges to main
        Repository geitRepo = CachedDataRepoFetcher.getRepoData(gh, "aipotheosis-labs/aci");
        Git geit = CachedGitCloner.getGit("kelhaji/geit");

        ObjectId mainBranch = geit.getRepository().resolve(geitRepo.defaultBranch);

        try (RevWalk walk = new RevWalk(geit.getRepository())) {
            RevCommit headCommit = walk.parseCommit(mainBranch);

            walk.markStart(headCommit);

            int count = 0;
            for (RevCommit commit : walk) {
                if (commit.getParentCount() > 1) {
                    logger.debug("Merge commit: {} ({})", commit.getShortMessage(), commit.getName());
                    count++;
                    if (count >= 10) {
                        break;
                    }
                }
            }
        }

        // Example defect counts
        // DataComputor.computeDefectCount(List.of(geitRepo), Duration.ofDays(365), 5);

        // Example MTTRs
        // DataComputor.computeMTTR(List.of(geitRepo), Duration.ofDays(365), 5);

        // Example CFRs
        // DataComputor.computeCFR(List.of(geitRepo), Duration.ofDays(365), 5);
    }
}