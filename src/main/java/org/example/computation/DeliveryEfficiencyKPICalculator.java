package org.example.computation;

import org.example.data.Commit;
import org.example.data.Release;
import org.example.data.Repository;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.time.*;
import java.util.*;

import static org.example.Main.logger;

public class DeliveryEfficiencyKPICalculator {

    public static void storeDeliveryFrequencies(Repository repo,
                                                Instant windowStart,
                                                Instant windowEnd,
                                                Duration intervalSize) throws IOException {
        logger.info("Computing delivery frequencies for repository {} with interval size {}.", repo.fullName, intervalSize);

        String repoFileName = repo.fullName.replace('/', '_');
        String fileName = "deliveryFrequency_" + intervalSize.toString() + "_" + repoFileName + ".csv";
        File output = new File("kpis", fileName);

        if(!output.getParentFile().exists() && !output.getParentFile().mkdirs()) {
            logger.error("Failed to create directories, necessary to save defect counts.");
            throw new RemoteException("Error `.mkdirs()`. Directories not created.");
        }

        if(output.exists()) {
            logger.info("File with delivery frequencies for repository {} with interval size {} already exists.",
                    repo.fullName, intervalSize);
            return;
        }

        int intervalCnt = (int) Duration.between(windowStart, windowEnd).dividedBy(intervalSize) + 1;
        int[] numReleasesPerInterval = computeDeliveryFrequency(repo, windowStart, windowEnd, intervalSize);

        try (FileWriter csvWriter = new FileWriter(output)) {
            csvWriter.append("IntervalStart,NumReleases\n");
            for (int i = 0; i < intervalCnt; i++) {
                LocalDate intervalStart = windowStart.plus(intervalSize.multipliedBy(i))
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
                csvWriter.append(String.valueOf(intervalStart))
                        .append(",")
                        .append(String.valueOf(numReleasesPerInterval[i]))
                        .append("\n");
            }
            logger.info("Delivery frequencies for {} saved to {}", repoFileName, output.getAbsolutePath());
        } catch (IOException e) {
            logger.fatal("Error writing delivery frequencies for {}: {}", repoFileName, e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

    public static void storeDeliverySizes(Repository repo,
                                          Instant windowStart,
                                          Instant windowEnd,
                                          Duration intervalSize) throws IOException {
        logger.info("Computing delivery sizes for repository {} with interval size {}.", repo.fullName, intervalSize);

        String repoFileName = repo.fullName.replace('/', '_');
        String fileName = "deliverySize_" + intervalSize.toString() + "_" + repoFileName + ".csv";
        File output = new File("kpis", fileName);

        if(!output.getParentFile().exists() && !output.getParentFile().mkdirs()) {
            logger.error("Failed to create directories, necessary to save defect counts.");
            throw new RemoteException("Error `.mkdirs()`. Directories not created.");
        }

        if(output.exists()) {
            logger.info("File with delivery sizes for repository {} with interval size {} already exists.",
                    repo.fullName, intervalSize);
            return;
        }

        int intervalCnt = (int) Duration.between(windowStart, windowEnd).dividedBy(intervalSize) + 1;
        int[] deliverySizePerInterval = new int[intervalCnt];

        for(Release r : repo.releases) {
            int index = (int) Duration.between(windowStart, r.publishedAt).dividedBy(intervalSize);
            deliverySizePerInterval[index] += r.changesFromPrevRelease;
        }

        try (FileWriter csvWriter = new FileWriter(output)) {
            csvWriter.append("IntervalStart,DeliverySize\n");
            for (int i = 0; i < intervalCnt; i++) {
                LocalDate intervalStart = windowStart.plus(intervalSize.multipliedBy(i))
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
                csvWriter.append(String.valueOf(intervalStart))
                        .append(",")
                        .append(String.valueOf(deliverySizePerInterval[i]))
                        .append("\n");
            }
            logger.info("Delivery sizes for {} saved to {}", repoFileName, output.getAbsolutePath());
        } catch (IOException e) {
            logger.fatal("Error writing delivery sizes for {}: {}", repoFileName, e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

    public static void storeChangeLeadTimes(Repository repo,
                                            Instant windowStart,
                                            Instant windowEnd,
                                            Duration intervalSize) throws IOException {
        logger.info("Computing change lead times for repository {} with interval size {}.", repo.fullName, intervalSize);

        String repoFileName = repo.fullName.replace('/', '_');
        String fileName = "changeLeadTime_" + intervalSize.toString() + "_" + repoFileName + ".csv";
        File output = new File("kpis", fileName);

        if(!output.getParentFile().exists() && !output.getParentFile().mkdirs()) {
            logger.error("Failed to create directories, necessary to save defect counts.");
            throw new RemoteException("Error `.mkdirs()`. Directories not created.");
        }

        if(output.exists()) {
            logger.info("File with change lead times for repository {} with interval size {} already exists.",
                    repo.fullName, intervalSize);
            return;
        }

        Map<Release, Long> totalCltPerRelease = new HashMap<>();
        Map<Release, Integer> numCommitsPerRelease = new HashMap<>();
        List<Commit> commitsSorted = new ArrayList<>(repo.commits);
        Collections.reverse(commitsSorted);
        List<Release> releasesSorted = new ArrayList<>(repo.releases);
        Collections.reverse(releasesSorted);
        int currReleaseIdx = 0;

        for(Commit c : commitsSorted) {
            while(currReleaseIdx < releasesSorted.size()
                    && c.commitDate.isAfter(releasesSorted.get(currReleaseIdx).publishedAt)) {
                currReleaseIdx++;
            }

            // Unreleased commits
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

        int intervalCnt = (int) Duration.between(windowStart, windowEnd).dividedBy(intervalSize) + 1;
        double[] avgCltPerInterval = new double[intervalCnt];
        int[] numReleasesPerInterval = computeDeliveryFrequency(repo, windowStart, windowEnd, intervalSize);

        for(Release r : repo.releases) {
            int index = (int) Duration.between(windowStart, r.publishedAt).dividedBy(intervalSize);
            long avgClt = totalCltPerRelease.get(r) / numCommitsPerRelease.get(r);
            avgCltPerInterval[index] += avgClt;
        }

        for(int i = 0; i < intervalCnt; i++) {
            avgCltPerInterval[i] /= (numReleasesPerInterval[i] > 0) ? numReleasesPerInterval[i] : 1;
        }

        try (FileWriter csvWriter = new FileWriter(output)) {
            csvWriter.append("IntervalStart,CLT\n");
            for (int i = 0; i < intervalCnt; i++) {
                LocalDate intervalStart = windowStart.plus(intervalSize.multipliedBy(i))
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
                csvWriter.append(String.valueOf(intervalStart))
                        .append(",")
                        .append(String.valueOf(avgCltPerInterval[i]))
                        .append("\n");
            }
            logger.info("CLTs for {} saved to {}", repoFileName, output.getAbsolutePath());
        } catch (IOException e) {
            logger.fatal("Error writing CLTs for {}: {}", repoFileName, e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

    private static int[] computeDeliveryFrequency(Repository repo,
                                                  Instant windowStart,
                                                  Instant windowEnd,
                                                  Duration intervalSize) {
        int intervalCnt = (int) Duration.between(windowStart, windowEnd).dividedBy(intervalSize) + 1;
        int[] numReleasesPerInterval = new int[intervalCnt];

        for(Release r : repo.releases) {
            int index = (int) Duration.between(windowStart, r.publishedAt).dividedBy(intervalSize);
            numReleasesPerInterval[index]++;
        }

        return numReleasesPerInterval;
    }
}
