package org.example.extraction;

import org.example.Main;
import org.example.data.*;
import org.kohsuke.github.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.example.Main.logger;

public class DataExtractor {
    private static final int MAX_PAGE_SIZE = 100;

    public static List<PullRequest> extractPullRequests(GHRepository repo) throws IOException {
        List<PullRequest> prs = new ArrayList<>();
        List<GHPullRequest> ghPullRequests = repo.queryPullRequests()
                .state(GHIssueState.ALL)
                .list()
                .withPageSize(MAX_PAGE_SIZE)
                .toList();

        logger.info("Extracting {} PRs.", ghPullRequests.size());

        int cnt = 1;

        for (GHPullRequest p : ghPullRequests) {
            if(cnt % 100 == 0) {
                logger.info("Progress: {} / {}", cnt, ghPullRequests.size());
            }
            cnt++;
            prs.add(new PullRequest(p));
        }

        return prs;
    }

    public static List<Issue> extractIssues(GHRepository repo) throws IOException {
        List<Issue> issues = new ArrayList<>();
        List<GHIssue> ghIssues = repo.queryIssues().state(GHIssueState.ALL)
                .list()
                .withPageSize(MAX_PAGE_SIZE)
                .toList();

        logger.info("Extracting {} issues.", ghIssues.size());
        int cnt = 1;

        for  (GHIssue i : ghIssues) {
            if(cnt % 50 == 0) {
                logger.info("Progress: {} / {}", cnt, ghIssues.size());
            }
            cnt++;
            issues.add(new Issue(i));
        }

        return issues;
    }

    public static List<Release> extractReleases(GHRepository repo, Commit initCommit) throws IOException {
        List<Release> releases = new ArrayList<>();
        List<GHRelease> ghReleases = repo.listReleases().withPageSize(MAX_PAGE_SIZE).toList();

        logger.info("Extracting {} releases.", ghReleases.size());

        int cnt = 1;

        for  (int i = 0; i < ghReleases.size() - 1; i++) {
            if(cnt % 10 == 0) {
                logger.info("Progress: {} / {}", cnt, ghReleases.size());
            }
            cnt++;
            releases.add(new Release(ghReleases.get(i), ghReleases.get(i + 1).getTagName()));
        }

        if(!ghReleases.isEmpty()) {
            releases.add(new Release(ghReleases.get(ghReleases.size() - 1), initCommit.sha1));
        }

        return releases;
    }

    public static List<Commit> extractCommits(GHRepository repo) throws IOException {
        List<Commit> commits = new ArrayList<>();
        List<GHCommit> ghCommits = repo.listCommits().withPageSize(MAX_PAGE_SIZE).toList();

        logger.info("Extracting {} commits.", ghCommits.size());

        int cnt = 1;

        for  (GHCommit c : ghCommits) {
            try {
                if(cnt % 100 == 0) {
                    logger.info("Progress: {} / {}", cnt, ghCommits.size());
                }
                cnt++;
                commits.add(new Commit(c));
            } catch (Exception ignored) {}
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
