package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.computation.DeliveryEfficiencyKPICalculator;
import org.example.data.Repository;
import org.example.fetching.CachedDataRepoFetcher;
import org.example.utils.GitHubAPIAuthHelper;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.time.Duration;

public class Main {
    public static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
        GitHub gh = GitHubAPIAuthHelper.getGitHubAPI();
        logger.info("Starting script");

        Repository geitRepo = CachedDataRepoFetcher.getRepoData(gh, "dnbln/DiscordPanel");
        DeliveryEfficiencyKPICalculator.storeDeliveryFrequencies(geitRepo,
                geitRepo.releases.get(geitRepo.releases.size() - 1).publishedAt,
                geitRepo.releases.get(0).publishedAt,
                Duration.ofDays(30));

        DeliveryEfficiencyKPICalculator.storeDeliverySizes(geitRepo,
                geitRepo.releases.get(geitRepo.releases.size() - 1).publishedAt,
                geitRepo.releases.get(0).publishedAt,
                Duration.ofDays(30));

        DeliveryEfficiencyKPICalculator.storeChangeLeadTimes(geitRepo,
                geitRepo.releases.get(geitRepo.releases.size() - 1).publishedAt,
                geitRepo.releases.get(0).publishedAt,
                Duration.ofDays(30));

//        Git geit = CachedGitCloner.getGit("Zakrok09/ts-automata");
//
//        ObjectId mainBranch = geit.getRepository().resolve(geitRepo.defaultBranch);
//
//        try (RevWalk walk = new RevWalk(geit.getRepository())) {
//            RevCommit headCommit = walk.parseCommit(mainBranch);
//
//            walk.markStart(headCommit);
//
//            int count = 0;
//            for (RevCommit commit : walk) {
//                if (commit.getParentCount() > 1) {
//                    logger.debug("Merge commit: {} ({})", commit.getShortMessage(), commit.getName());
//                    count++;
//                    if (count >= 10) {
//                        break;
//                    }
//                }
//            }
//        }
    }
}