package org.example.computation;

import org.example.data.Commit;
import org.example.data.Repository;
import org.example.data.User;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static org.example.Main.logger;

public class CollaborationMetricsCalculator {

    private static final int MIN_COMMITS_THRESHOLD = 5;
    private final static double CORE_CONTRIBUTORS_FRACTION_THRESHOLD = 0.8;

    public static void storeTeamStructureMetrics(Repository repo,
                                                Instant windowStart,
                                                Instant windowEnd,
                                                Duration intervalSize) throws IOException {
        logger.info("Computing team structure metrics for repository {} with interval size {}.", repo.fullName, intervalSize);

        String repoFileName = repo.fullName.replace('/', '_');
        String fileName = "teamMetrics" + intervalSize.toString() + "_" + repoFileName + ".csv";
        File output = new File("metrics", fileName);

        if(!output.getParentFile().exists() && !output.getParentFile().mkdirs()) {
            logger.error("Failed to create directories, necessary to save defect counts.");
            throw new RemoteException("Error `.mkdirs()`. Directories not created.");
        }

        if(output.exists()) {
            logger.info("File with team metrics for repository {} with interval size {} already exists.",
                    repo.fullName, intervalSize);
            return;
        }

        int intervalCnt = (int) Duration.between(windowStart, windowEnd).dividedBy(intervalSize) + 1;
        int[] teamSizePerInterval = new int[intervalCnt];
        int[] teamExperiencePerInterval = new int[intervalCnt];
        calculateTeamSizeExperiencePerInterval(repo, windowStart, windowEnd, intervalSize, intervalCnt,
                teamSizePerInterval, teamExperiencePerInterval);

        double[] fractionCollaboratorCommitsPerInterval = calculateCoreCommitFractions(repo,
                windowStart, windowEnd, intervalSize, intervalCnt);

        try (FileWriter csvWriter = new FileWriter(output)) {
            csvWriter.append("IntervalStart,TeamSize,TeamExperience,CollaboratorCommits\n");
            for (int i = 0; i < intervalCnt; i++) {
                LocalDate intervalStart = windowStart.plus(intervalSize.multipliedBy(i))
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
                csvWriter.append(String.valueOf(intervalStart))
                        .append(",")
                        .append(String.valueOf(teamSizePerInterval[i]))
                        .append(",")
                        .append(String.valueOf(teamExperiencePerInterval[i]))
                        .append(",")
                        .append(String.valueOf(fractionCollaboratorCommitsPerInterval[i]))
                        .append("\n");
            }
            logger.info("Team metrics for {} saved to {}", repoFileName, output.getAbsolutePath());
        } catch (IOException e) {
            logger.fatal("Error writing delivery frequencies for {}: {}", repoFileName, e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

    private static void calculateTeamSizeExperiencePerInterval(Repository repo,
                                                Instant windowStart,
                                                Instant windowEnd,
                                                Duration intervalSize,
                                                int intervalCnt,
                                                int[] teamSizePerInterval,
                                                int[] teamExperiencePerInterval) {
        Map<User, Integer>[] developersCommitsPerInterval = new Map[intervalCnt];

        for(int i = 0; i < intervalCnt; i++) {
            developersCommitsPerInterval[i] = new HashMap<>();
        }

        for(Commit c : repo.commits) {
            if(c.commitDate.isBefore(windowStart) || c.commitDate.isAfter(windowEnd)) {
                continue;
            }

            int index = (int) Duration.between(windowStart, c.commitDate).dividedBy(intervalSize);

            developersCommitsPerInterval[index].merge(c.author, 1, Integer::sum);
        }

        for(int i = 0; i < intervalCnt; i++) {
            Instant currWindowEnd = windowStart.plus(intervalSize.multipliedBy(i + 1));

            for (User user : developersCommitsPerInterval[i].keySet()) {
                if (developersCommitsPerInterval[i].get(user) >= MIN_COMMITS_THRESHOLD) {
                    teamSizePerInterval[i]++;
                    teamExperiencePerInterval[i] += (int) Duration.between(user.accountCreatedAt, currWindowEnd).toDays();
                }
            }

            if (teamSizePerInterval[i] > 0) {
                teamExperiencePerInterval[i] /= teamSizePerInterval[i];
            }
        }
    }

    private static double[] calculateCoreCommitFractions(Repository repo,
                                                                 Instant windowStart,
                                                                 Instant windowEnd,
                                                                 Duration intervalSize,
                                                                 int intervalCnt) {
        int[] totalCommitsPerInterval = new int[intervalCnt];
        int[] coreCommitsPerInterval = new int[intervalCnt];
        double[] fractionCoreCommitsPerInterval = new double[intervalCnt];

        Set<String> coreContributors = getCoreContributors(repo);

        for(Commit c : repo.commits) {
            if(c.commitDate.isBefore(windowStart) || c.commitDate.isAfter(windowEnd)) {
                continue;
            }

            int index = (int) Duration.between(windowStart, c.commitDate).dividedBy(intervalSize);
            totalCommitsPerInterval[index]++;

            if(coreContributors.contains(c.author.name)) {
                coreCommitsPerInterval[index]++;
            }
        }

        for(int i = 0; i < intervalCnt; i++) {
            if(totalCommitsPerInterval[i] > 0) {
                fractionCoreCommitsPerInterval[i] = (double) coreCommitsPerInterval[i] / totalCommitsPerInterval[i];
            }
        }

        return fractionCoreCommitsPerInterval;
    }

    private static Set<String> getCoreContributors(Repository repo) {
        Map<String, Integer> commitsPerUser = new HashMap<>();

        for(Commit c : repo.commits) {
            commitsPerUser.merge(c.author.name, 1, Integer::sum);
        }

        List<Map.Entry<String, Integer>> commitsPerUserSorted = new ArrayList<>(commitsPerUser.entrySet());
        commitsPerUserSorted.sort(Map.Entry.<String, Integer>comparingByValue().reversed());

        int totalCommits = repo.commits.size();
        int currCommits = 0;
        Set<String> coreContributors = new HashSet<>();

        for(Map.Entry<String, Integer> entry : commitsPerUserSorted) {
            currCommits += entry.getValue();
            // If we reach the 80% threshold, the next developers are no longer core
            if(((double) currCommits / totalCommits) > CORE_CONTRIBUTORS_FRACTION_THRESHOLD) {
                coreContributors.add(entry.getKey());
                break;
            }

            coreContributors.add(entry.getKey());
        }

        return coreContributors;
    }
}
