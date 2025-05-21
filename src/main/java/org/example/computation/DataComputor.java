package org.example.computation;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import org.example.data.Commit;
import org.example.data.Issue;
import org.example.data.Release;
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

        List<Release> releases = repo.releases.stream()
                .filter(r -> r.publishedAt.isAfter(windowStart) && r.publishedAt.isBefore(windowEnd))
                .toList();
        int[] failures_per_release = new int[releases.size()];
        int r_pointer = 0;

        List<Issue> issues = repo.issues;

        // assumes issues are sorted on created date descendingly -> from newest to oldest
        for (Issue issue : issues) {
            Instant created_at = issue.createdAt;

            // Keep walking backwards until we find a release which is published before the issue is created
            while (created_at.isBefore(releases.get(r_pointer).publishedAt)) r_pointer++;

            if (issue.isBug) failures_per_release[r_pointer]++; // TODO: Possibly add time cutoff since CFR could be too big
        }

        Double[] cfr_per_interval = new Double[intervalCount];
        int[] total_releases_per_interval = new int[intervalCount];
        int[] failed_releases_per_interval = new int[intervalCount];

        Instant walk = windowEnd;
        int interval_pointer = 0;

        // assuming releases are sorted from newest to oldest
        for (int r = 0; r < releases.size(); r++) {
            Instant interval_start = walk.minus(intervalSize);

            while (releases.get(r).publishedAt.isBefore(interval_start)) {
                walk = interval_start;
                interval_pointer++;
            }

            // if release is in this interval
            total_releases_per_interval[interval_pointer]++;

            // if failed, mark
            if (failures_per_release[r] > 0) failed_releases_per_interval[interval_pointer]++;

        }

        for (int i = 0; i < intervalCount; i++) {
            if (total_releases_per_interval[i] == 0) {
                cfr_per_interval[i] = 0.0;
            } else {
                cfr_per_interval[i] = total_releases_per_interval[i] / ((double) failed_releases_per_interval[i]);
            }
        }

        return cfr_per_interval;
    }

    /**
     * Computes the delivery frequency per interval, i.e., the number of releases within each interval.
     * @param repo The repository to compute delivery frequencies for.
     * @param windowEnd The end of the time window to compute defect counts for.
     * @param intervalSize Size of each interval.
     * @param intervalCount Number of intervals to compute.
     * @return An array containing the delivery frequencies for all intervals.
     */
    public static Integer[] computeDeliveryFrequency(Repository repo, Instant windowEnd, Duration intervalSize, int intervalCount) {
        Instant windowStart = windowEnd.minus(intervalSize.multipliedBy(intervalCount));
        System.out.println(windowStart);

        int[] numReleasesPerInterval = new int[intervalCount];

        if (repo.releases == null) {
            return Arrays.stream(numReleasesPerInterval).boxed().toArray(Integer[]::new);
        }

        for(Release r : repo.releases) {
            System.out.println(r.publishedAt);
            int index = (int) Duration.between(windowStart, r.publishedAt).dividedBy(intervalSize);
            // Only consider releases published after the start of the window and until end
            if (index >= 0 && index < intervalCount) {
                numReleasesPerInterval[index]++;
            }
        }

        return Arrays.stream(numReleasesPerInterval).boxed().toArray(Integer[]::new);
    }

    /**
     * Computes the delivery sizes per interval, i.e., the changed LOCs between successive intervals.
     * @param repo The repository to compute delivery sizes for.
     * @param windowEnd The end of the time window to compute defect counts for.
     * @param intervalSize Size of each interval.
     * @param intervalCount Number of intervals to compute.
     * @return An array containing the delivery sizes for all intervals.
     */
    public static Integer[] computeDeliverySize(Repository repo, Instant windowEnd, Duration intervalSize, int intervalCount) {
        Instant windowStart = windowEnd.minus(intervalSize.multipliedBy(intervalCount));

        int[] deliverySizePerInterval = new int[intervalCount];

        if (repo.releases == null) {
            return Arrays.stream(deliverySizePerInterval).boxed().toArray(Integer[]::new);
        }
        for(Release r : repo.releases) {
            int index = (int) Duration.between(windowStart, r.publishedAt).dividedBy(intervalSize);
            // Only consider releases published after the start of the window
            if (index >= 0 && index < intervalCount) {
                deliverySizePerInterval[index] += r.changesFromPrevRelease;
            }
        }

        return Arrays.stream(deliverySizePerInterval).boxed().toArray(Integer[]::new);
    }

    /**
     * Computes the change lead time (CLT) per interval, i.e., the average duration between commit date and the date of the
     * first release that includes it.
     * @param repo The repository to compute CLTs for.
     * @param windowEnd The end of the time window to compute defect counts for.
     * @param intervalSize Size of each interval.
     * @param intervalCount Number of intervals to compute.
     * @return An array containing the CLTs for all intervals.
     */
    public static Double[] computeCLT(Repository repo, Instant windowEnd, Duration intervalSize, int intervalCount) {
        Map<Release, Long> totalCltPerRelease = new HashMap<>(); // Store CLT per release in minutes
        Map<Release, Integer> numCommitsPerRelease = new HashMap<>();
        List<Commit> commitsSorted = new ArrayList<>(repo.commits); // Commits sorted by date in ASC order
        Collections.reverse(commitsSorted);
        List<Release> releasesSorted = new ArrayList<>(repo.releases); // Release sorted by date in ASC order
        Collections.reverse(releasesSorted);
        int currReleaseIdx = 0;

        for(Commit c : commitsSorted) {
            // Get first release after the current commit
            while(currReleaseIdx < releasesSorted.size()
                    && c.commitDate.isAfter(releasesSorted.get(currReleaseIdx).publishedAt)) {
                currReleaseIdx++;
            }

            // All subsequent commits are unreleased
            if(currReleaseIdx == releasesSorted.size()) {
                break;
            }

            // Add duration between commit and release to current release CLT
            totalCltPerRelease.merge(releasesSorted.get(currReleaseIdx),
                    Duration.between(c.commitDate, releasesSorted.get(currReleaseIdx).publishedAt).toMinutes(),
                    Long::sum);
            numCommitsPerRelease.merge(releasesSorted.get(currReleaseIdx),
                    1,
                    Integer::sum);
        }

        Instant windowStart = windowEnd.minus(intervalSize.multipliedBy(intervalCount));

        double[] avgCltPerInterval = new double[intervalCount];
        Integer[] numReleasesPerInterval = computeDeliveryFrequency(repo, windowEnd, intervalSize, intervalCount);

        // Compute CLTs per release
        for(Release r : repo.releases) {
            if(totalCltPerRelease.containsKey(r) && numCommitsPerRelease.containsKey(r)) {
                int index = (int) Duration.between(windowStart, r.publishedAt).dividedBy(intervalSize);
                // Only consider releases published after the start of the window
                if (index >= 0 && index < numReleasesPerInterval.length) {
                    double avgClt = (double) totalCltPerRelease.get(r) / numCommitsPerRelease.get(r);
                    // Add the per-release CLT to the current per-interval CLT
                    avgCltPerInterval[index] += avgClt;
                }
            }
        }

        // Compute CLTs per interval
        for(int i = 0; i < intervalCount; i++) {
            avgCltPerInterval[i] /= (numReleasesPerInterval[i] > 0) ? numReleasesPerInterval[i] : 1;
        }

        return Arrays.stream(avgCltPerInterval).boxed().toArray(Double[]::new);
    }
}
