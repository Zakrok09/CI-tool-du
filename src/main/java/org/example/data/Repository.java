package org.example.data;

import org.example.extraction.DataExtractor;
import org.kohsuke.github.*;

import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

    public User owner;
    public List<User> contributors;

    public List<Release> releases;
    public List<Tag> tags;

    public Repository() {}

    public Repository(GHRepository repo) throws IOException {
        super(repo);

        fullName = repo.getFullName();
        description = repo.getDescription();
        defaultBranch = repo.getDefaultBranch();
        branches = repo.getBranches().keySet().stream().toList();

        pullRequests = DataExtractor.extractPullRequests(repo);
        issues = DataExtractor.extractIssues(repo, dateCutoff);

        owner = new User(repo.getOwner());
        contributors = DataExtractor.extractContributors(repo);

        releases = DataExtractor.extractReleases(repo);
        tags = DataExtractor.extractTags(repo);
    }
}
