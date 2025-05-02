package org.example.extraction;

import org.example.data.*;
import org.kohsuke.github.*;

import java.io.IOException;
import java.time.Instant;
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

    public static List<Issue> extractIssues(GHRepository repo, Instant dateCutoff) throws IOException {
        List<Issue> issues = new ArrayList<>();
        for  (GHIssue i : repo.queryIssues().state(GHIssueState.ALL).list()) {
            issues.add(new Issue(i));
        }

        return issues;
    }

    public static List<User> extractContributors(GHRepository repo) throws IOException {
        List<User> users = new ArrayList<>();
        for  (GHUser u : repo.listContributors()) {
            users.add(new User(u));
        }

        return users;
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
}
