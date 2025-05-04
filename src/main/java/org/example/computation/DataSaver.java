package org.example.computation;

import static org.example.Main.logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

import org.apache.commons.lang3.function.TriFunction;
import org.example.data.Repository;

public class DataSaver {

    public static <T> void saveData(String kpi, Duration intervalSize, int intervalCount, List<Repository> repos,
            TriFunction<Repository, Duration, Integer, T[]> kpiComputor) throws IOException {
        logger.info("Computing {} for {} repositories with interval size {} and count {}.", kpi, repos.size(),
                intervalSize, intervalCount);
        if (intervalCount < 1) {
            logger.error("Interval count must be at least 1.");
            throw new IllegalArgumentException("Interval count must be at least 1.");
        }

        String fileName = kpi + "_" + intervalSize.toString() + "_" + intervalCount + ".csv";
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
                T[] kpiValues = kpiComputor.apply(repo, intervalSize, intervalCount);
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