package org.example.extraction;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.example.extraction.ci.CIContentParser;
import org.example.extraction.ci.CIWorkflowExtractor;
import org.example.extraction.testcounter.JUnitTestCounter;
import org.example.extraction.testcounter.TestCounter;
import org.example.fetching.CachedGitCloner;
import org.example.utils.GitHubAPIAuthHelper;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.time.Duration;

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
    public void test3() throws IOException {
        GitHub gh = GitHubAPIAuthHelper.getGitHubAPI();

        CIWorkflowExtractor ciFileExtractor = new CIWorkflowExtractor(gh, "withastro/astro");
        ciFileExtractor.getValidWorkflows().forEach(w -> {
            System.out.println(w.getName());
            System.out.println(new CIContentParser(w.getFileContent()).isExecutingTests());
            w.getTriggers().forEach(a -> System.out.println("\t" + a));
            System.out.println("\n\n");
        });
    }
}
