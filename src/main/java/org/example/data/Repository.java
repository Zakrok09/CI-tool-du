package org.example.data;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;

import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class Repository extends GitHubObject implements Serializable {
    // modify this to change how far into the past to get issues
    // set low for developing and debugging, high for actual data collection
    private final Instant yesterday = Instant.now().minus(1, ChronoUnit.DAYS);
    private final Instant oneMonthAgo = Instant.now().minus(30, ChronoUnit.DAYS);
    private final Instant all = Instant.MIN;
    private final Instant dateCutoff =  oneMonthAgo;

    public String defaultBranch;
    public String description;
    public String fullName;
    public List<String> branches;
    public List<PullRequest> pullRequests;
    public List<Issue> issues;

    public Repository() {}

    public Repository(GHRepository repo) throws IOException {
        super(repo);

        fullName = repo.getFullName();
        description = repo.getDescription();
        defaultBranch = repo.getDefaultBranch();
        branches = repo.getBranches().keySet().stream().toList();

        pullRequests = new ArrayList<>();
        for (GHPullRequest p : repo.queryPullRequests().list()) {
            pullRequests.add(new PullRequest(p));
        }

        issues = new ArrayList<>();
        for  (GHIssue i : repo.queryIssues().since(dateCutoff).list()) {
            issues.add(new Issue(i));
        }
    }
}
