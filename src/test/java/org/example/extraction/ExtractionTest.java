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
        Git gitRepo = CachedGitCloner.getGit("FinalProjectPhotoContestOrg/PhotoContestProject");
        RepoRetrospect repoRetrospect = new RepoRetrospect(gitRepo);

        JGitCommitSampler sampler = new JGitCommitSampler(gitRepo.getRepository());

        TestCounter testCounter = new JUnitTestCounter();
        repoRetrospect
                .walkSampledCommits(sampler.sampleCommitsWithDuration(Duration.ofDays(1L)), testCounter)
                .forEach(System.out::println);

        assertEquals(5, 5);
    }
}
