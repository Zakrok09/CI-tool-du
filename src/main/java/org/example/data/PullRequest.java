package org.example.data;

import org.kohsuke.github.GHPullRequest;

import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;

public class PullRequest extends GitHubObject implements Serializable {
    public boolean isMerged;
    public Instant mergedAt;
    public int commitCount;
    public int reviewCount;
    public int commentCount;

    public PullRequest() {}

    public PullRequest(GHPullRequest pr) throws IOException {
        super(pr);
        isMerged = pr.isMerged();
        mergedAt = pr.getMergedAt();
        commitCount = pr.getCommits();
        reviewCount = pr.listReviews().toList().size();
        commentCount = pr.getCommentsCount();
    }
}
