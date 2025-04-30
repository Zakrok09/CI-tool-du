package org.example.data;

import org.kohsuke.github.*;

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

        pullRequests = extractPullRequests(repo);
        issues = extractIssues(repo);

        owner = new User(repo.getOwner());
        contributors = extractContributors(repo);

        releases = extractReleases(repo);
        tags = extractTags(repo);
    }

    private List<PullRequest> extractPullRequests(GHRepository repo) throws IOException {
        List<PullRequest> prs = new ArrayList<>();
        for (GHPullRequest p : repo.queryPullRequests().state(GHIssueState.ALL).list()) {
            prs.add(new PullRequest(p));
        }

        return prs;
    }

    private List<Issue> extractIssues(GHRepository repo) throws IOException {
        List<Issue> issues = new ArrayList<>();
        for  (GHIssue i : repo.queryIssues().since(dateCutoff).list()) {
            issues.add(new Issue(i));
        }

        return issues;
    }

    private List<User> extractContributors(GHRepository repo) throws IOException {
        List<User> users = new ArrayList<>();
        for  (GHUser u : repo.listContributors()) {
            users.add(new User(u));
        }

        return users;
    }

    private List<Release> extractReleases(GHRepository repo) throws IOException {
        List<Release> releases = new ArrayList<>();
        for  (GHRelease r : repo.listReleases()) {
            releases.add(new Release(r));
        }

        return releases;
    }

    private List<Tag> extractTags(GHRepository repo) throws IOException {
        List<Tag> tags = new ArrayList<>();
        for  (GHTag t : repo.listTags()) {
            tags.add(new Tag(t));
        }

        return tags;
    }
}
