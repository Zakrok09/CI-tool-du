package org.example.extraction;

import org.example.data.*;
import org.kohsuke.github.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DataExtractor {
    public static List<PullRequest> extractPullRequests(GHRepository repo) throws IOException {
        List<PullRequest> prs = new ArrayList<>();
        for (GHPullRequest p : repo.queryPullRequests().state(GHIssueState.ALL).list()) {
            prs.add(new PullRequest(p));
        }

        return prs;
    }

    public static List<Issue> extractIssues(GHRepository repo) throws IOException {
        List<Issue> issues = new ArrayList<>();
        for  (GHIssue i : repo.queryIssues().state(GHIssueState.ALL).list()) {
            issues.add(new Issue(i));
        }

        return issues;
    }

    public static List<Release> extractReleases(GHRepository repo) throws IOException {
        List<Release> releases = new ArrayList<>();
        List<GHRelease> ghReleases = repo.listReleases().toList();

        for  (int i = 0; i < ghReleases.size() - 1; i++) {
            releases.add(new Release(ghReleases.get(i), ghReleases.get(i + 1)));
        }

        if(!ghReleases.isEmpty()) {
            releases.add(new Release(ghReleases.get(ghReleases.size() - 1), null));
        }

        return releases;
    }

    public static List<Commit> extractCommits(GHRepository repo) throws IOException {
        List<Commit> commits = new ArrayList<>();
        for  (GHCommit c : repo.listCommits()) {
            commits.add(new Commit(c));
        }

        return commits;
    }

    public static List<IssueComment> extractIssueComments(GHIssue issue) throws IOException {
        List<IssueComment> comments = new ArrayList<>();

        for(GHIssueComment comment : issue.getComments()) {
            comments.add(new IssueComment(comment));
        }

        return comments;
    }
}
