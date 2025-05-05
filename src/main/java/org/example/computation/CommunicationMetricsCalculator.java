package org.example.computation;

import org.example.data.Issue;
import org.example.data.IssueComment;
import org.example.data.PullRequest;
import org.example.data.Repository;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.example.Main.logger;

public class CommunicationMetricsCalculator {

    public static void storeTeamStructureMetrics(Repository repo,
                                                 Instant windowStart,
                                                 Instant windowEnd,
                                                 Duration intervalSize) throws IOException {
        logger.info("Computing communication activity metrics for repository {} with interval size {}.",
                repo.fullName, intervalSize);

        String repoFileName = repo.fullName.replace('/', '_');
        String fileName = "communicationMetrics" + intervalSize.toString() + "_" + repoFileName + ".csv";
        File output = new File("metrics", fileName);

        if(!output.getParentFile().exists() && !output.getParentFile().mkdirs()) {
            logger.error("Failed to create directories, necessary to save defect counts.");
            throw new RemoteException("Error `.mkdirs()`. Directories not created.");
        }

        if(output.exists()) {
            logger.info("File with communication metrics for repository {} with interval size {} already exists.",
                    repo.fullName, intervalSize);
            return;
        }

        int intervalCnt = (int) Duration.between(windowStart, windowEnd).dividedBy(intervalSize) + 1;
        int[] issueCommentsPerInterval = new int[intervalCnt];
        double[] issueCommentLengthPerInterval = new double[intervalCnt];
        calculateIssueCommentMetricsPerInterval(repo, windowStart, windowEnd, intervalSize, intervalCnt,
                issueCommentsPerInterval, issueCommentLengthPerInterval);

        int[] prReviewsPerInterval = new int[intervalCnt];
        int[] prCommentsPerInterval = new int[intervalCnt];
        calculatePRMetricsPerInterval(repo, windowStart, windowEnd, intervalSize,
                prReviewsPerInterval, prCommentsPerInterval);


        try (FileWriter csvWriter = new FileWriter(output)) {
            csvWriter.append("IntervalStart,IssueComments,IssueCommentLength,PRReviews,PRComments\n");
            for (int i = 0; i < intervalCnt; i++) {
                LocalDate intervalStart = windowStart.plus(intervalSize.multipliedBy(i))
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
                csvWriter.append(String.valueOf(intervalStart))
                        .append(",")
                        .append(String.valueOf(issueCommentsPerInterval[i]))
                        .append(",")
                        .append(String.valueOf(issueCommentLengthPerInterval[i]))
                        .append(",")
                        .append(String.valueOf(prReviewsPerInterval[i]))
                        .append(",")
                        .append(String.valueOf(prCommentsPerInterval[i]))
                        .append("\n");
            }
            logger.info("Team metrics for {} saved to {}", repoFileName, output.getAbsolutePath());
        } catch (IOException e) {
            logger.fatal("Error writing delivery frequencies for {}: {}", repoFileName, e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

    private static void calculateIssueCommentMetricsPerInterval(Repository repo,
                                                                Instant windowStart,
                                                                Instant windowEnd,
                                                                Duration intervalSize,
                                                                int intervalCnt,
                                                                int[] issueCommentsPerInterval,
                                                                double[] issueCommentLengthPerInterval) {
        for(Issue issue : repo.issues) {
            for(IssueComment comment : issue.comments) {
                if (comment.createdAt.isBefore(windowStart) || comment.createdAt.isAfter(windowEnd)) {
                    continue;
                }

                int index = (int) Duration.between(windowStart, comment.createdAt).dividedBy(intervalSize);
                issueCommentsPerInterval[index]++;
                issueCommentLengthPerInterval[index] += comment.body.length();
            }
        }

        for(int i = 0; i < intervalCnt; i++) {
            if (issueCommentsPerInterval[i] > 0) {
                issueCommentLengthPerInterval[i] /= issueCommentsPerInterval[i];
            }
        }
    }

    private static void calculatePRMetricsPerInterval(Repository repo,
                                                                Instant windowStart,
                                                                Instant windowEnd,
                                                                Duration intervalSize,
                                                                int[] prReviewsPerInterval,
                                                                int[] prCommentsPerInterval) {
        for(PullRequest pr : repo.pullRequests) {
            if(!pr.isMerged || pr.mergedAt.isBefore(windowStart) || pr.mergedAt.isAfter(windowEnd)) {
                continue;
            }

            int index = (int) Duration.between(windowStart, pr.mergedAt).dividedBy(intervalSize);
            prReviewsPerInterval[index] += pr.reviewCount;
            prCommentsPerInterval[index] += pr.commentCount;
        }
    }
}
