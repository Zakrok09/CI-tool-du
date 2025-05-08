package org.example.computation;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import org.example.data.Deployment;
import org.example.data.Issue;
import org.example.data.Repository;

public class DataComputor {
    
    /**
     * Compute defect counts for a single repository given an end point for a time window
     * going back a specified number of intervals with a given size.
     * Defects are issues with the isBug flag set to true.
     * Issues which are created before the start of the first interval are still
     * counted.
     * Issues which are closed before the window start are not counted.
     * 
     * @param repo         The repository to compute defect counts for.
     * @param windowEnd     The end of the time window to compute defect counts for.
     * @param intervalSize  Size of each interval.
     * @param intervalCount Number of intervals to compute.
     * 
     * @throws IllegalArgumentException if intervalCount is less than 1.
     * @throws IOException              on failed write of data.
     * 
     * @apiNote Example: computeDefectCount(repo, Instant.now(), Duration.ofDays(7), 4) will
     *          compute defect counts going back from today for 4 intervals of 7 days each.
     */
    public static Integer[] computeDefectCount(Repository repo, Instant windowEnd, Duration intervalSize, int intervalCount) {
        Instant windowStart = windowEnd.minus(intervalSize.multipliedBy(intervalCount));
        int[] delta = new int[intervalCount + 1];

        for (Issue issue : repo.issues) {
            Instant createdAt = issue.createdAt;
            Instant closedAt = issue.closedAt;
            if (!issue.isBug)
                continue;

            int startIndex = (int) (Duration.between(windowStart, createdAt).dividedBy(intervalSize));
            int endIndex = (closedAt != null)
                    ? ((int) (Duration.between(windowStart, closedAt).dividedBy(intervalSize)))
                    : (intervalCount - 1);

            // For now, issues which were created before the window start are still counted.
            // Issues which are closed before the window start are not counted.
            if (startIndex < 0)
                startIndex = 0;
            if (endIndex < 0)
                continue;

            ++delta[startIndex];
            --delta[endIndex + 1];
        }

        Integer[] defectsPerInterval = new Integer[intervalCount];
        int currentCount = 0;
        for (int i = 0; i < intervalCount; i++) {
            currentCount += delta[i];
            defectsPerInterval[i] = currentCount;
        }

        return defectsPerInterval;
    }

    // TODO: Consider CI construction failure?
    /**
     * Compute mean time to recovery (MTTR) from failures for a single repository.
     * Given an endpoint for a time window, goes back a specified number of intervals with a given size.
     * Failures are issues with the isBug flag set to true.
     * Solve time is calculated as the difference between the closedAt and createdAt
     * timestamps of the issue.
     * Issues which were created before the window start are still counted.
     * Issues which are closed before the window start are not counted.
     * MTTR is calculated per interval.
     * 
     * @param repo          The repository to compute MTTR for.
     * @param windowEnd     The end of the time window to compute MTTR for.
     * @param intervalSize  Size of each interval.
     * @param intervalCount Number of intervals to compute.
     * 
     * @apiNote Example: computeMTTR(repo, Instant.now(), Duration.ofDays(7), 4) will
     *          compute MTTR going back from today for 4 intervals of 7 days each.
     */
    public static Double[] computeMTTR(Repository repo, Instant windowEnd, Duration intervalSize, int intervalCount) {
        Instant windowStart = windowEnd.minus(intervalSize.multipliedBy(intervalCount));
        int[] countBucket = new int[intervalCount + 1];
        double[] timeBucket = new double[intervalCount + 1];

        for (Issue issue : repo.issues) {
            Instant createdAt = issue.createdAt;
            Instant closedAt = issue.closedAt;
            // Open issues are not counted.
            if (closedAt == null || !issue.isBug)
                continue;

            double solveTime = ((double) Duration.between(createdAt, closedAt).toMillis()) / 1000.0; // TODO: Should we
                                                                                                     // use seconds or
                                                                                                     // minutes?
            int startIndex = (int) (Duration.between(windowStart, createdAt).dividedBy(intervalSize));
            int endIndex = (closedAt != null)
                    ? ((int) (Duration.between(windowStart, closedAt).dividedBy(intervalSize)))
                    : (intervalCount - 1);

            // For now, issues which were created before the window start are still counted.
            // Issues which are closed before the window start are not counted.
            if (startIndex < 0)
                startIndex = 0;
            if (endIndex < 0)
                continue;

            ++countBucket[endIndex];
            timeBucket[endIndex] += solveTime;
        }

        Double[] mttrPerInterval = new Double[intervalCount];
        for (int i = 0; i < intervalCount; i++) {
            if (countBucket[i] == 0) {
                mttrPerInterval[i] = 0.0;
            } else {
                mttrPerInterval[i] = timeBucket[i] / ((double) countBucket[i]);
            }
        }

        return mttrPerInterval;
    }
    
    /**
     * Compute change failure rate (CFR) for a single repository given an end point for a time window
     * going back a specified number of intervals with a given size.
     * Failures are deployments with final state "failure".
     * Only deployments which were completed after the window start are counted.
     * 
     * @param repo          The repository to compute CFR for.
     * @param windowEnd     The end of the time window to compute CFR for.
     * @param intervalSize  Size of each interval.
     * @param intervalCount Number of intervals to compute.
     * 
     * @apiNote Example: computeCFR(repo, Instant.now(), Duration.ofDays(7), 4) will
     *          compute CFR going back from today for 4 intervals of 7 days each.
     */
    public static Double[] computeCFR(Repository repo, Instant windowEnd, Duration intervalSize, int intervalCount) {
        Instant windowStart = windowEnd.minus(intervalSize.multipliedBy(intervalCount));
        int[] failureBucket = new int[intervalCount + 1];
        int[] totalBucket = new int[intervalCount + 1];

        for (Deployment deployment : repo.deployments) {
            String conclusion = deployment.finalState;
            Instant completedAt = deployment.completedAt;

            // TODO: Maybe consider deployments with "error".   
            if (!conclusion.equals("FAILURE") && !conclusion.equals("SUCCESS"))
                continue;

            int endIndex = (int) (Duration.between(windowStart, completedAt).dividedBy(intervalSize));

            // For now, only deployments which were completed after the window start are counted.
            if (endIndex < 0)
                continue;

            if (conclusion.equals("FAILURE")) {
                ++failureBucket[endIndex];
            }
            ++totalBucket[endIndex];
        }

        Double[] cfrPerInterval = new Double[intervalCount];
        for (int i = 0; i < intervalCount; i++) {
            if (totalBucket[i] == 0) {
                cfrPerInterval[i] = 0.0;
            } else {
                cfrPerInterval[i] = ((double) failureBucket[i]) / ((double) totalBucket[i]);
            }
        }

        return cfrPerInterval;
    }
}
