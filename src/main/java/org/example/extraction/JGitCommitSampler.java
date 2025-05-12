package org.example.extraction;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class JGitCommitSampler {
    private final RevWalk start;
    private List<RevCommit> sampledCommits;

    public JGitCommitSampler(RevWalk start) {
        this.start = start;
        this.sampledCommits = new ArrayList<>();
    }

    /**
     * Create a jgit commit sampler by starting at the head of the repository
     *
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
     *
     * @param step duration between jgit commits. Will gather every "step" jgit commit.
     */
    public void sampleCommitsWithDuration(Duration step, Duration limit) {
        List<RevCommit> sampled = new ArrayList<>();
        Instant cutoff = Instant.now().minus(limit);
        try (RevWalk walk = start) {
            Instant lastRecorded = null;
            for (RevCommit commit : walk) {
                Instant commitTime = Instant.ofEpochSecond(commit.getCommitTime());
                if (commitTime.isBefore(cutoff)) break;
                if (lastRecorded == null || Duration.between(commitTime, lastRecorded).abs().compareTo(step) >= 0) {
                    sampled.add(commit);
                    lastRecorded = commitTime;
                }
            }
        }
        this.sampledCommits = sampled;
    }

    /**
     * Sample all commits starting from the set start.
     *
     * @return a list of all jgit commits
     */
    public List<RevCommit> sampleAllCommits() {
        List<RevCommit> results = new ArrayList<>();
        start.forEach(results::add);
        this.sampledCommits = results;
        return results;
    }

    public void printSamplesToCSV(String fileName) throws IOException {
        if (this.sampledCommits.isEmpty()) throw new IllegalStateException("can't save empty samples");

        Path dir = Paths.get("sampled_commits");
        if (Files.notExists(dir)) Files.createDirectory(dir);

        Path file = dir.resolve(fileName + ".csv");
        Files.write(
                file,
                sampledCommits.stream().map(c -> c.getName() + " " + Instant.ofEpochSecond(c.getCommitTime())).toList(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    public List<RevCommit> getSampledCommits() {
        return this.sampledCommits;
    }
}
