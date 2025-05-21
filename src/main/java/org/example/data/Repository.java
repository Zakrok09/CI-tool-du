package org.example.data;

import org.eclipse.jgit.api.Git;
import org.example.extraction.DataExtractor;
import org.example.fetching.FetchSettings;
import org.kohsuke.github.*;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Repository extends GitHubObject implements Serializable {
    public String defaultBranch;
    public String description;
    public String fullName;
    public List<String> branches;

    public List<Commit> commits;
    public List<PullRequest> pullRequests;
    public List<Issue> issues;

    public List<Release> releases;
    public List<Deployment> deployments;

    public Repository() {}

    public Repository(GHRepository repo) throws IOException {
        super(repo);

        if (!FetchSettings.set)
            FetchSettings.All();

        fullName = repo.getFullName();
        description = repo.getDescription();
        defaultBranch = repo.getDefaultBranch();

        commits = FetchSettings.commits ? DataExtractor.extractCommits(repo) :  new ArrayList<>();
        releases = FetchSettings.releases ? DataExtractor.extractReleases(repo, commits.getLast()) : new ArrayList<>();
        branches = FetchSettings.branches ? repo.getBranches().keySet().stream().toList() : new ArrayList<>();

        pullRequests = FetchSettings.pullRequests ? DataExtractor.extractPullRequests(repo) : new ArrayList<>();
        issues = FetchSettings.issues ? DataExtractor.extractIssues(repo) : new ArrayList<>();

        // TODO: Should we extract check runs for all repositories the same way we do for PRs, issues, ....
        // OR Do we create a method getCheckRuns() which will use APICatcher and we call it for each repository when calculating CFR?
        deployments = FetchSettings.deployments ? DataExtractor.extractDeployments(repo) :  new ArrayList<>();
    }

    public Git getGit() throws IOException {
        File output = new File("clones", fullName.replace("/", "_"));

        if (output.exists()) return Git.open(output);
        else throw new RuntimeException("Getting git of a not cloned repo");
    }
}
