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
        for  (GHRelease r : repo.listReleases()) {
            releases.add(new Release(r));
        }

        return releases;
    }

    public static List<Tag> extractTags(GHRepository repo) throws IOException {
        List<Tag> tags = new ArrayList<>();
        for  (GHTag t : repo.listTags()) {
            tags.add(new Tag(t));
        }

        return tags;
    }

    public static List<Commit> extractCommits(GHRepository repo) throws IOException {
        List<Commit> commits = new ArrayList<>();
        for  (GHCommit c : repo.listCommits()) {
            commits.add(new Commit(c));
        }

        return commits;
    }

    public static List<IssueComment> extractComments(GHIssue issue) throws IOException {
        List<IssueComment> comments = new ArrayList<>();

        for(GHIssueComment comment : issue.getComments()) {
            comments.add(new IssueComment(comment));
        }

        return comments;
    }
}
