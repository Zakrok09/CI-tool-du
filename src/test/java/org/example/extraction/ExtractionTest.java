package org.example.extraction;

import kotlin.Pair;
import org.eclipse.jgit.api.Git;
import org.example.computation.TestFrequencyComputer;
import org.example.computation.TestTriggerComputer;
import org.example.computation.TriggerExecFreqComputer;
import org.example.extraction.ci.CIContentParser;
import org.example.extraction.ci.CIWorkflowExtractor;
import org.example.extraction.ci.KnownEvent;
import org.example.extraction.testcounter.JUnitTestCounter;
import org.example.extraction.testcounter.TestCounter;
import org.example.fetching.CachedGitCloner;
import org.example.utils.GitHubAPIAuthHelper;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GitHub;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

public class ExtractionTest {

    @Test
    public void test1() throws IOException {
        Git gitRepo = CachedGitCloner.getGit("kafka-ops/julie");
        RepoRetrospect repoRetrospect = new RepoRetrospect(gitRepo);

        JGitCommitSampler sampler = new JGitCommitSampler(gitRepo.getRepository());

        TestCounter testCounter = new JUnitTestCounter();

        System.out.println(
                testCounter.countUnitTestsAtCommit(gitRepo.getRepository().getDirectory().getParentFile(),
                        sampler.sampleAllCommits().getFirst())
        );
//        repoRetrospect
//                .walkSampledCommits(sampler.sampleCommitsWithDuration(Duration.ofDays(1L)), testCounter)
//                .forEach(System.out::println);
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

        System.out.println("total tests: " + ttc.countWorkflows());

        for (int i = 0; i < freqs.length; i++) {
            if( freqs[i] == 0) continue;
            System.out.println("Frequency of " + KnownEvent.values()[i] + ": " + freqs[i]);
        }
    }

    @Test
    public void test4() {
        TestFrequencyComputer tfc = new TestFrequencyComputer(Duration.ofDays(30L), Duration.ofDays(365L));
        Map<String, List<Integer>> freqs = tfc.calculateFrequency();

        // alphabetically sort the map by key

        for (Map.Entry<String, List<Integer>> entry : freqs.entrySet().stream().sorted((a, b) ->
                a.getKey().charAt(0) - b.getKey().charAt(0)
        ).toList()) {
            System.out.println(entry.getKey() + ": " + entry.getValue() + " (total: " + entry.getValue().stream().mapToInt(Integer::intValue).sum() + ")");
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
    public void test6() throws IOException {
        TriggerExecFreqComputer tefc = new TriggerExecFreqComputer("final_for_repo_data.csv");
        int[] frequencies = tefc.frequencyOfTriggerExecutions();
        List<Pair<String, Integer>> freqs = new ArrayList<>();

        for (KnownEvent knownEvent : KnownEvent.values()) {
            int frequency = frequencies[knownEvent.ordinal()];
            if (frequency > 0) {
                freqs.add(new Pair<>(knownEvent.name(), frequency));
            }
        }

        freqs.sort((a, b) -> Integer.compare(b.getSecond(), a.getSecond()));

        System.out.println("Trigger execution frequencies:");
        for (Pair<String, Integer> pair : freqs) {
            System.out.println(pair.getFirst() + ": " + pair.getSecond());
        }

    }
//
//    @Test
//    public void test7() throws IOException {
//        TriggerExecFreqComputer tefc = new TriggerExecFreqComputer("final_for_repo_data.csv");
//        Map<String, Integer> frequenciesMap = tefc.frequencyOfTriggerExecutionsMAP();
//
//        System.out.println("Trigger execution frequencies (MAP) sorted:");
//
//        List<Pair<String, Integer>> freqs = new ArrayList<>();
//
//        for (Map.Entry<String, Integer> entry : frequenciesMap.entrySet()) {
//            if (entry.getValue() > 0) {
//                freqs.add(new Pair<>(entry.getKey(), entry.getValue()));
//            }
//        }
//
//        freqs.sort((a, b) -> Integer.compare(b.getSecond(), a.getSecond()));
//        for (Pair<String, Integer> pair : freqs) {
//            System.out.println(pair.getFirst() + ": " + pair.getSecond());
//        }
//    }
}
