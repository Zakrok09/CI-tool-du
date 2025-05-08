package org.example.computation;

import static org.example.Main.logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.example.data.Repository;
import org.example.utils.QuadFunction;

public class DataSaver {

    public static <T> void saveData(String kpi, Instant windowEnd, Duration intervalSize, int intervalCount, List<Repository> repos,
            QuadFunction<Repository, Instant, Duration, Integer, T[]> kpiComputor) throws IOException {
        logger.info("Computing {} for {} repositories with window end {}, interval size {}, and count {}.", kpi, repos.size(),
                windowEnd, intervalSize, intervalCount);
        if (intervalCount < 1) {
            logger.error("Interval count must be at least 1.");
            throw new IllegalArgumentException("Interval count must be at least 1.");
        }

        String fileName = kpi + "_" + windowEnd.toString() + "_" + intervalSize.toString() + "_" + intervalCount + ".csv";
        File output = new File("kpis", fileName);

        if (!output.getParentFile().exists() && !output.getParentFile().mkdirs()) {
            logger.error("Failed to create kpis directory for saving data.");
            throw new IOException("Error `.mkdirs()`. Directory not created.");
        }

        try (FileWriter csvWriter = new FileWriter(output)) {
            csvWriter.append("repoName");
            for (int i = 0; i < intervalCount; i++) {
                csvWriter.append(",").append(String.valueOf(i));
            }
            csvWriter.append("\n");

            for (Repository repo : repos) {
                T[] kpiValues = kpiComputor.apply(repo, windowEnd, intervalSize, intervalCount);
                csvWriter.append(repo.fullName);
                for (T value : kpiValues) {
                    csvWriter.append(",").append(String.valueOf(value));
                }
                csvWriter.append("\n");
            }

            logger.info("Data for {} saved to {}", kpi, output.getAbsolutePath());
        } catch (IOException e) {
            logger.fatal("Error writing {} data: {}", kpi, e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }
}