package org.example.computation;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import org.example.data.CheckRun;
import org.example.data.Issue;
import org.example.data.Repository;

public class DataComputor {
    
    /**
     * Compute defect counts for a signle repository for a specified number of
     * intervals with a given size.
     * Defects are issues with the isBug flag set to true.
     * Issues which are created before the start of the first interval are still
     * counted.
     * The output file is named "defectCount_<intervalSize>_<intervalCount>.csv" and
     * saved in the "kpis" directory.
     * 
     * @param repos         The repository to compute defect counts for.
     * @param intervalSize  Size of each interval.
     * @param intervalCount Number of intervals to compute.
     * 
     * @throws IllegalArgumentException if intervalCount is less than 1.
     * @throws IOException              on failed write of data.
     * 
     * @apiNote Example: computeDefectCount(repo, Duration.ofDays(7), 4) will
     *          compute defect counts for 4 intervals of 7 days each.
     *          The intervals will be computed from the last updatedAt of the
     *          repository, going back in time.
     */
    public static Integer[] computeDefectCount(Repository repo, Duration intervalSize, int intervalCount) {
        Instant windowEnd = repo.updatedAt; // TODO: Maybe use a different end point? e.g.: last issue updatedAt
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

            // TODO: These checks need to be very precise as we need to describe which
            // issues we consider.
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
     * Compute mean time to recovery (MTTR) from failures for a single repository
     * for a specified number of intervals with a given size.
     * Failures are issues with the isBug flag set to true.
     * Solve time is calculated as the difference between the closedAt and createdAt
     * timestamps of the issue.
     * Issues which are created before the start of the first interval are not
     * counted.
     * 
     * @param repo          The repository to compute MTTR for.
     * @param intervalSize  Size of each interval.
     * @param intervalCount Number of intervals to compute.
     * 
     * @apiNote Example: computeMTTR(repo, Duration.ofDays(7), 4) will compute MTTR
     *          for 4 intervals of 7 days each.
     *          The intervals will be computed from the last updatedAt of the
     *          repository, going back in time.
     */
    public static Double[] computeMTTR(Repository repo, Duration intervalSize, int intervalCount) {
        Instant windowEnd = repo.updatedAt; // TODO: Maybe use a different end point? e.g.: last issue updatedAt
        Instant windowStart = windowEnd.minus(intervalSize.multipliedBy(intervalCount));
        int[] countPrefix = new int[intervalCount + 1];
        double[] timePrefix = new double[intervalCount + 1];

        for (Issue issue : repo.issues) {
            Instant createdAt = issue.createdAt;
            Instant closedAt = issue.closedAt;
            // TODO: How to handle open issues? This is simple and ingores them, but is
            // likely insufficient.
            // Do we set closedAt to the current time?
            if (closedAt == null || !issue.isBug)
                continue;

            double solveTime = ((double) Duration.between(createdAt, closedAt).toMillis()) / 1000.0; // TODO: Should we
                                                                                                     // use seconds or
                                                                                                     // minutes?
            int startIndex = (int) (Duration.between(windowStart, createdAt).dividedBy(intervalSize));
            int endIndex = (closedAt != null)
                    ? ((int) (Duration.between(windowStart, closedAt).dividedBy(intervalSize)))
                    : (intervalCount - 1);

            // TODO: These checks need to be very precise as we need to describe which
            // issues we consider.
            // For now, issues which were created before the window start are not counted.
            // Issues which are closed before the window start are not counted.
            if (startIndex < 0)
                continue;
            if (endIndex < 0)
                continue;

            // TODO: Which interval should we attribute the solve time to?
            ++countPrefix[endIndex];
            timePrefix[endIndex] += solveTime;
        }

        Double[] mttrPerInterval = new Double[intervalCount];
        int currentCount = 0;
        double currentTime = 0.0;
        for (int i = 0; i < intervalCount; i++) {
            currentCount += countPrefix[i];
            currentTime += timePrefix[i];
            if (currentCount == 0) {
                mttrPerInterval[i] = 0.0;
            } else {
                mttrPerInterval[i] = currentTime / ((double) currentCount);
            }
        }

        return mttrPerInterval;
    }

    // TODO: Should we take a look at revert commits as well?
    // TODO: Is it correct to look at check runs? Or should it be check suites /
    // deployments?
    /**
     * Compute change failure rate (CFR) for a single repository
     * for a specified number of intervals with a given size.
     * Failures are check runs with conclusion "failure".
     * Only check runs which were completed after the window start are counted.
     * 
     * @param repo          The repository to compute CFR for.
     * @param intervalSize  Size of each interval.
     * @param intervalCount Number of intervals to compute.
     * 
     * @apiNote Example: computeCFR(repo, Duration.ofDays(7), 4) will compute CFR
     *          for 4 intervals of 7 days each.
     *          The intervals will be computed from the last updatedAt of the
     *          repository, going back in time.
     */
    public static Double[] computeCFR(Repository repo, Duration intervalSize, int intervalCount) {
        Instant windowEnd = repo.updatedAt; // TODO: Maybe use a different end point? e.g.: last issue updatedAt
        Instant windowStart = windowEnd.minus(intervalSize.multipliedBy(intervalCount));
        int[] failurePrefix = new int[intervalCount + 1];
        int[] totalPrefix = new int[intervalCount + 1];

        for (CheckRun checkRun : repo.checkRuns) {
            Instant completed_at = checkRun.completed_at;
            if (completed_at == null)
                continue;

            int endIndex = (int) (Duration.between(windowStart, completed_at).dividedBy(intervalSize));

            // TODO: These checks need to be very precise as we need to describe which
            // checkruns we consider.
            // For now, check runs which were completed after the window start are counted.
            if (endIndex < 0)
                continue;

            // TODO: Which interval should we attribute the solve time to?
            // TODO: Do we count all check runs towards total? There are also conclusions
            // like "skipped" and "neutral".
            if (checkRun.conclusion.equals("failure")) {
                ++failurePrefix[endIndex];
            }
            ++totalPrefix[endIndex];
        }

        Double[] cfrPerInterval = new Double[intervalCount];
        int failureCount = 0;
        int totalCount = 0;
        for (int i = 0; i < intervalCount; i++) {
            failureCount += failurePrefix[i];
            totalCount += totalPrefix[i];
            if (totalCount == 0) {
                cfrPerInterval[i] = 0.0;
            } else {
                cfrPerInterval[i] = ((double) failureCount) / ((double) totalCount);
            }
        }

        return cfrPerInterval;
    }
}
