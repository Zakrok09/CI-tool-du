package org.example.computation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.Duration;
import java.util.List;

import org.example.data.*;

public class DataComputor {

    // TODO: Should this method save to file or return values?
    public static void computeDefectCount(List<Repository> repos, Duration intervalSize, int intervalCount) {
        if (intervalCount < 1) {
            return;
        }

        // TODO: Create a key for the file name based on the parameters
        File output = new File("kpis", "defects_per_interval_defects.csv");

        if(!output.getParentFile().exists() && !output.getParentFile().mkdirs()) {
            return;
        }

        try (FileWriter csvWriter = new FileWriter(output)) {
            csvWriter.append("repoName");
            for (int i = 0; i < intervalCount; i++) {
                csvWriter.append(",").append(String.valueOf(i));
            }
            csvWriter.append("\n");

            for (Repository repo : repos) {
                Instant windowEnd = repo.updatedAt;
                Instant windowStart = windowEnd.minus(intervalSize.multipliedBy(intervalCount));
                int[] delta = new int[intervalCount + 1];

                for (Issue issue : repo.issues) {
                    Instant createdAt = issue.createdAt;
                    Instant closedAt = issue.closedAt;
                    if (!issue.isBug) continue;

                    int startIndex = (int) Duration.between(windowStart, createdAt).dividedBy(intervalSize);
                    int endIndex = (closedAt != null) ? endIndex = (int) Duration.between(windowStart, closedAt).dividedBy(intervalSize) : (intervalCount - 1);

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

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
