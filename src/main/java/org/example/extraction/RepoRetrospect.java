package org.example.extraction;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.example.extraction.testcounter.TestCounter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A (soon to be) data agnostic class that collects data per commit
 * by specifying the algorithm that shall be executed on collection.
 * Should have a generic T type that will be the type of data to be
 * collected per commit.
 * Stores all logic related to walking through the commits and nothing
 * else.
 * Current implementation does not uphold that, will be fixed later.
 */
public class RepoRetrospect {
    private final Git repoGit;
    private final String currentBranch;
    private final String headCommitId;

    /**
     * Generic pair holder that will store information per commit
     * @param <T> the type of the data stored pairwise with the commit
     */
    public static class CommitPair<T> {
        RevCommit commit;
        T data;

        public CommitPair(RevCommit commit, T data) {
            this.commit = commit;
            this.data = data;
        }

        @Override
        public String toString() {
            return "For commit=" + commit.getName() + " made at: " + commit.getCommitTime() + "; found: " + data;
        }
    }

    private void checkout(RevCommit commit) throws GitAPIException {
        repoGit.checkout().setName(commit.getName()).call();
    }

    /**
     * Construct a repository retrospect-or.
     *
     * @param repoGit the JGit of the repository to be analysed
     */
    public RepoRetrospect(Git repoGit) throws IOException {
        this.repoGit = repoGit;
        this.currentBranch = repoGit.getRepository().getBranch();
        this.headCommitId = repoGit.getRepository().resolve("HEAD").getName();
    }

    public void restore() throws GitAPIException {
        repoGit.checkout().setName(currentBranch).call();
        repoGit.checkout().setName(headCommitId).call();
    }

    /**
     * Walk over the sampled commits
     */
    public List<CommitPair<Integer>> walkSampledCommits(List<RevCommit> sampled, TestCounter counter) throws GitAPIException {
        List<CommitPair<Integer>> results = new ArrayList<>();
        for (RevCommit commit : sampled) {
            try {
                checkout(commit);
                int testCount = counter.countUnitTestsAtCommit(repoGit.getRepository().getDirectory().getParentFile(), commit);
                results.add(new CommitPair<>(commit, testCount));
            } catch (GitAPIException e) {
                restore();
                throw new RuntimeException();
            }
        }
        restore();
        return results;
    }
}
