package org.example.data;

import org.kohsuke.github.GHPullRequest;

import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;

public class PullRequest extends GitHubObject implements Serializable {
    public boolean isMerged;
    public Instant mergedAt;
    public int commitCount;
    public int reviewCount;
    public int commentCount;
    public List<String> commitShas;

    public PullRequest() {}

    public PullRequest(GHPullRequest pr) throws IOException {
        super(pr);

        try {
            isMerged = pr.isMerged();
            mergedAt = pr.getMergedAt();
            commitCount = pr.getCommits();
            reviewCount = pr.listReviews().toList().size();
            commentCount = pr.getCommentsCount();
            commitShas = pr.listCommits().toList().stream().map(x -> x.getCommit().getTree().getSha()).toList();
        } catch (Exception e) {
            isMerged = false;
            mergedAt = null;
            commitCount = 0;
            reviewCount = 0;
            commentCount = 0;
            commitShas = null;
        }
    }
}