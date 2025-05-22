package org.example.extraction;

import org.eclipse.jgit.api.Git;
import org.example.computation.TestFrequencyComputer;
import org.example.computation.TestTriggerComputer;
import org.example.extraction.ci.CIContentParser;
import org.example.extraction.ci.CIWorkflowExtractor;
import org.example.extraction.ci.KnownEvent;
import org.example.extraction.testcounter.JUnitTestCounter;
import org.example.extraction.testcounter.TestCounter;
import org.example.fetching.CachedGitCloner;
import org.example.utils.GitHubAPIAuthHelper;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

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
            System.out.println("Frequency of " + KnownEvent.values()[i] + ": " + freqs[i]);
        }
    }

    @Test
    public void test4() {
        TestFrequencyComputer tfc = new TestFrequencyComputer(Duration.ofDays(30L), Duration.ofDays(365L));
        Map<String, List<Integer>> freqs = tfc.calculateFrequency();

        for (Map.Entry<String, List<Integer>> entry : freqs.entrySet()) {
            System.out.println("Project: " + entry.getKey());
            System.out.println("Frequencies: " + entry.getValue());
        }
    }
}
