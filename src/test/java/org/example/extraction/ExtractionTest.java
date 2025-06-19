package org.example.extraction;

import io.github.cdimascio.dotenv.Dotenv;
import kotlin.Pair;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.example.computation.TestFrequencyComputer;
import org.example.computation.TestTriggerComputer;
import org.example.computation.TriggerExecFreqComputer;
import org.example.extraction.ci.KnownEvent;
import org.example.fetching.CachedGitCloner;
import org.example.utils.ProjectListOps;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.example.Main.logger;

public class ExtractionTest {

    @Test
    public void test1() throws IOException {
        logger.info("STARTING");
        List<String> projects = ProjectListOps.getProjectListFromFile("intake/maven-projects.csv");

        for (String project : projects) {
            Git gitRepo = CachedGitCloner.getGit(project);RepoRetrospect repoRetrospect = new RepoRetrospect(gitRepo);

            JGitCommitSampler sampler = new JGitCommitSampler(gitRepo.getRepository());

            Dotenv dotenv = Dotenv.load();
            String cutoffStr = dotenv.get("DATE_CUTOFF");
            sampler.sampleCommitsWithDuration(Duration.ofDays(30), Instant.parse(cutoffStr));

            try {
                logger.info("Starting to list for {}", project);
                repoRetrospect.walkAndTestSampledCommits(sampler.getSampledCommits(), project);
            } catch (GitAPIException e) {
                logger.error("Problem with project: ", e);
                System.err.println("something bad happened: " + e.getMessage());
            }
        }
    }

    @Test
    public void test2() throws IOException {
        Git gitRepo = CachedGitCloner.getGit("withastro/astro");
        JGitCommitSampler sampler = new JGitCommitSampler(gitRepo.getRepository());

        sampler.sampleCommitsWithDuration(Duration.ofDays(30L), Duration.ofDays(730));

        sampler.printSamplesToCSV("kafka-ops_julie");
    }

    @Test
    public void test3() {
        TestTriggerComputer ttc = new TestTriggerComputer();
        int[] freqs = ttc.frequencyOfTriggers();

        List<Pair<String, Integer>> freqers = new ArrayList<>();

        for (KnownEvent event : KnownEvent.values())
            freqers.add(new Pair<>(event.name(), freqs[event.ordinal()]));

        freqers.sort(Comparator.comparing(Pair::component1));

        System.out.println("total tests: " + ttc.countWorkflows());

        for (var freq : freqers) {
            if (freq.component2() == 0) continue;
            System.out.println("Frequency of " + freq.component1() + ": " + freq.component2());
        }
    }

    @Test
    public void test4() {
        var tfc = new TestFrequencyComputer(Duration.ofDays(30L), Duration.ofDays(365L), "intake/pr-wf.csv");
        Map<String, List<Integer>> freqs = tfc.calculateFrequency();

        // alphabetically sort the map by key
        List<Map.Entry<String, List<Integer>>> sortedEntries = new ArrayList<>(freqs.entrySet());

        sortedEntries.sort(Map.Entry.comparingByKey());

        for (Map.Entry<String, List<Integer>> entry : sortedEntries) {
            System.out.println(entry.getKey() + "," + entry.getValue().stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse(""));
        }
    }

    @Test
    public void test5() throws IOException {
        Path dir = Path.of("sampled_workflow_runs");
        int total_runs = 0;
        if (!dir.toFile().exists()) throw new RuntimeException("sampled_workflow_runs folder does not exist!");
        for (var file : Objects.requireNonNull(dir.toFile().listFiles())) {
            if (file.getName().endsWith(".csv")) {
                try (var lines = Files.lines(file.toPath())) {
                    total_runs += (int) lines.skip(1).count();
                } catch (IOException e) {
                    System.err.println("Error reading file: " + file.getName() + ", skipping...");
                }
            }
        }
        System.out.println("Total workflow runs: " + total_runs);
    }

    @Test
    public void test6() {
        TriggerExecFreqComputer tefc = new TriggerExecFreqComputer("intake/pr-wf.csv");
        int[] frequencies = tefc.frequencyOfTriggerExecutions();
        List<Pair<String, Integer>> freqs = new ArrayList<>();

        for (KnownEvent knownEvent : KnownEvent.values()) {
            int frequency = frequencies[knownEvent.ordinal()];
            if (frequency > 0) {
                freqs.add(new Pair<>(knownEvent.name(), frequency));
            }
        }

        freqs.sort(Comparator.comparing(Pair::component1));

        System.out.println("Trigger execution frequencies:");
        for (Pair<String, Integer> pair : freqs) {
            System.out.println(pair.getFirst() + ";" + pair.getSecond());
        }
    }

    @Test
    public void test7() {
        TriggerExecFreqComputer tefc = new TriggerExecFreqComputer("intake/pr-wf.csv");
        tefc.statistics2();
    }
}
