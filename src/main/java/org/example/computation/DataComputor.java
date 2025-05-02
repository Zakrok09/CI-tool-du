package org.example.computation;

import static org.example.Main.logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.example.data.Issue;
import org.example.data.Repository;

public class DataComputor {

    // TODO: Should these methods save to file, or return values and another method performs saves?

    /** Compute defect counts for a list of repositories for a specified number of intervals with a given size.
     * Defects are issues with the isBug flag set to true.
     * Issues which are created before the start of the first interval are still counted.
     * The output file is named "defectCount_<intervalSize>_<intervalCount>.csv" and saved in the "kpis" directory.
     * 
     * @param repos List of repositories to compute defect counts for.
     * @param intervalSize Size of each interval.
     * @param intervalCount Number of intervals to compute.
     * 
     * @throws IllegalArgumentException if intervalCount is less than 1.
     * @throws IOException on failed write of data.
     * 
     * @apiNote Example: computeDefectCount(repos, Duration.ofDays(7), 4) will compute defect counts for 4 intervals of 7 days each.
     * The intervals will be computed from the last updatedAt of the repository, going back in time.
     */
    public static void computeDefectCount(List<Repository> repos, Duration intervalSize, int intervalCount) throws IOException{
        logger.info("Computing defect counts for {} repositories with interval size {} and count {}.", repos.size(), intervalSize, intervalCount);
        
        if (intervalCount < 1) {
            logger.error("Interval count must be at least 1.");
            throw new IllegalArgumentException("Interval count must be at least 1.");
        }

        // TODO: Save a timestamp as well? For updates/data safety.
        String fileName = "defectCount_" + intervalSize.toString() + "_" + intervalCount + ".csv";
        File output = new File("kpis", fileName);

        if(!output.getParentFile().exists() && !output.getParentFile().mkdirs()) {
            logger.error("Failed to create directories, necessary to save defect counts.");
            throw new RemoteException("Error `.mkdirs()`. Directories not created.");
        }

        try (FileWriter csvWriter = new FileWriter(output)) {
            csvWriter.append("repoName");
            for (int i = 0; i < intervalCount; i++) {
                csvWriter.append(",").append(String.valueOf(i));
            }
            csvWriter.append("\n");

            for (Repository repo : repos) {
                Instant windowEnd = repo.updatedAt; // TODO: Maybe use a different end point? e.g.: last issue updatedAt
                Instant windowStart = windowEnd.minus(intervalSize.multipliedBy(intervalCount));
                int[] delta = new int[intervalCount + 1];

                for (Issue issue : repo.issues) {
                    Instant createdAt = issue.createdAt;
                    Instant closedAt = issue.closedAt;
                    if (!issue.isBug) continue;

                    int startIndex = (int) (Duration.between(windowStart, createdAt).dividedBy(intervalSize));
                    int endIndex = (closedAt != null) ? endIndex = ((int) (Duration.between(windowStart, closedAt).dividedBy(intervalSize))) : (intervalCount - 1);

                    // TODO: These checks need to be very precise as we need to describe which issues we consider.
                    // For now, issues which were created before the window start are still counted.
                    // Issues which are closed before the window start are not counted.
                    if (startIndex < 0) startIndex = 0;
                    if (endIndex < 0) continue;

                    ++delta[startIndex];
                    --delta[endIndex + 1];
                }

                int[] defectsPerInterval = new int[intervalCount];
                int currentCount = 0;
                for (int i = 0; i < intervalCount; i++) {
                    currentCount += delta[i];
                    defectsPerInterval[i] = currentCount;
                }

                csvWriter.append(repo.fullName);
                for (int defectCount : defectsPerInterval) {
                    csvWriter.append(",").append(String.valueOf(defectCount));
                }
                csvWriter.append("\n");
            }

            logger.info("Defect counts saved to {}", output.getAbsolutePath());
        } catch (IOException e) {
            logger.fatal("Error writing defect counts: {}", e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

    // TODO: Consider CI construction failure?
    /** Compute mean time to recovery (MTTR) from failures for a list of repositories for a specified number of intervals with a given size.
     * Failures are issues with the isBug flag set to true.
     * Solve time is calculated as the difference between the closedAt and createdAt timestamps of the issue.
     * Issues which are created before the start of the first interval are not counted.
     * The output file is named "MTTR_<intervalSize>_<intervalCount>.csv" and saved in the "kpis" directory.
     * 
     * @param repos List of repositories to compute MTTR for.
     * @param intervalSize Size of each interval.
     * @param intervalCount Number of intervals to compute.
     * 
     * @throws IllegalArgumentException if intervalCount is less than 1.
     * @throws IOException on failed write of data.
     * 
     * @apiNote Example: computeMTTR(repos, Duration.ofDays(7), 4) will compute MTTR for 4 intervals of 7 days each.
     * The intervals will be computed from the last updatedAt of the repository, going back in time.
     */
    public static void computeMTTR(List<Repository> repos, Duration intervalSize, int intervalCount) throws IOException{
        logger.info("Computing MTTRs for {} repositories with interval size {} and count {}.", repos.size(), intervalSize, intervalCount);
        
        if (intervalCount < 1) {
            logger.error("Interval count must be at least 1.");
            throw new IllegalArgumentException("Interval count must be at least 1.");
        }

        // TODO: Save a timestamp as well? For updates/data safety.
        String fileName = "MTTR_" + intervalSize.toString() + "_" + intervalCount + ".csv";
        File output = new File("kpis", fileName);

        if(!output.getParentFile().exists() && !output.getParentFile().mkdirs()) {
            logger.error("Failed to create directories, necessary to save MTTRs.");
            throw new RemoteException("Error `.mkdirs()`. Directories not created.");
        }

        try (FileWriter csvWriter = new FileWriter(output)) {
            csvWriter.append("repoName");
            for (int i = 0; i < intervalCount; i++) {
                csvWriter.append(",").append(String.valueOf(i));
            }
            csvWriter.append("\n");

            for (Repository repo : repos) {
                Instant windowEnd = repo.updatedAt; // TODO: Maybe use a different end point? e.g.: last issue updatedAt
                Instant windowStart = windowEnd.minus(intervalSize.multipliedBy(intervalCount));
                int[] countPrefix = new int[intervalCount + 1];
                double[] timePrefix = new double[intervalCount + 1];

                for (Issue issue : repo.issues) {
                    Instant createdAt = issue.createdAt;
                    Instant closedAt = issue.closedAt;
                    // TODO: How to handle open issues? This is simple and ingores them, but is likely insufficient.
                    // Do we set closedAt to the current time?
                    if (closedAt == null || !issue.isBug) continue;

                    double solveTime = ((double) Duration.between(createdAt, closedAt).toMillis()) / 1000.0; // TODO: Should we use seconds or minutes?
                    int startIndex = (int) (Duration.between(windowStart, createdAt).dividedBy(intervalSize));
                    int endIndex = (closedAt != null) ? endIndex = ((int) (Duration.between(windowStart, closedAt).dividedBy(intervalSize))) : (intervalCount - 1);

                    // TODO: These checks need to be very precise as we need to describe which issues we consider.
                    // For now, issues which were created before the window start are not counted.
                    // Issues which are closed before the window start are not counted.
                    if (startIndex < 0) continue;
                    if (endIndex < 0) continue;

                    // TODO: Which interval should we attribute the solve time to?
                    ++countPrefix[endIndex];
                    timePrefix[endIndex] += solveTime;
                }

                double[] mttrPerInterval = new double[intervalCount];
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

                csvWriter.append(repo.fullName);
                for (double mttr : mttrPerInterval) {
                    csvWriter.append(",").append(String.valueOf(mttr));
                }
                csvWriter.append("\n");
            }

            logger.info("MTTRs saved to {}", output.getAbsolutePath());
        } catch (IOException e) {
            logger.fatal("Error writing MTTRs {}", e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }
}
