package org.example.data;

import org.example.extraction.DataExtractor;
import org.kohsuke.github.*;

import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class Repository extends GitHubObject implements Serializable {
    public String defaultBranch;
    public String description;
    public String fullName;

    public List<Commit> commits;
    public List<PullRequest> pullRequests;
    public List<Issue> issues;

    public List<Release> releases;

    public Repository() {}

    public Repository(GHRepository repo) throws IOException {
        super(repo);

        fullName = repo.getFullName();
        description = repo.getDescription();
        defaultBranch = repo.getDefaultBranch();

        commits = DataExtractor.extractCommits(repo);

        pullRequests = DataExtractor.extractPullRequests(repo);
        issues = DataExtractor.extractIssues(repo);

        releases = DataExtractor.extractReleases(repo, commits.get(commits.size() - 1));
    }
}
