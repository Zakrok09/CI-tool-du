package org.example.extraction;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class JGitCommitSampler {
    private final RevWalk start;

    public JGitCommitSampler(RevWalk start) {
        this.start = start;
    }

    /**
     * Create a jgit commit sampler by starting at the head of the repository
     * @param repo the repository to sample from
     * @throws IOException may throw when reading files
     */
    public JGitCommitSampler(Repository repo) throws IOException {
        RevWalk walk = new RevWalk(repo);
        ObjectId head = repo.resolve("HEAD");
        walk.markStart(walk.parseCommit(head));
        this.start = walk;
    }

    /**
     * Sample jgit commits from
     * @param step duration between jgit commits. Will gather every "step" jgit commit.
     * @return a list of all sampled jgit commits
     */
    public List<RevCommit> sampleCommitsWithDuration(Duration step) {
        List<RevCommit> sampled = new ArrayList<>();
        try (RevWalk walk = start) {
            Instant lastRecorded = null;
            for (RevCommit commit : walk) {
                Instant commitTime = Instant.ofEpochSecond(commit.getCommitTime());
                if (lastRecorded == null || Duration.between(commitTime, lastRecorded).abs().compareTo(step) >= 0) {
                    sampled.add(commit);
                    lastRecorded = commitTime;
                }
            }
        }
        return sampled;
    }

    /**
     * Sample all commits starting from the set start.
     * @return a list of all jgit commits
     */
    public List<RevCommit> sampleAllCommits() {
        List<RevCommit> results = new ArrayList<>();
        start.forEach(results::add);
        return results;
    }
}
