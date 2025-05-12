package org.example.extraction;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.example.extraction.testcounter.JUnitTestCounter;
import org.example.extraction.testcounter.TestCounter;
import org.example.fetching.CachedGitCloner;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExtractionTest {

    @Test
    public void test1() throws IOException, GitAPIException {
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
}
