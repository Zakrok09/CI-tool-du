package org.example.extraction;

import org.example.data.*;
import org.kohsuke.github.*;
import org.kohsuke.github.GHIssueQueryBuilder.Sort;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DataExtractor {
    // .emv example: DATE_CUTOFF=2024-01-01T00:00:00.00Z
    public static Instant dateCutoff = LocalDateTime.parse(
            Dotenv.load().get("DATE_CUTOFF"), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SS'Z'"))
            .atZone(ZoneId.of("Europe/Amsterdam")).toInstant();

    public static List<PullRequest> extractPullRequests(GHRepository repo) throws IOException {
        List<PullRequest> prs = new ArrayList<>();
        for (GHPullRequest p : repo.queryPullRequests().state(GHIssueState.ALL).list()) {
            prs.add(new PullRequest(p));
        }

        return prs;
    }

    public static List<Issue> extractIssues(GHRepository repo) throws IOException {
        List<Issue> issues = new ArrayList<>();
        for  (GHIssue i : repo.queryIssues().since(dateCutoff).state(GHIssueState.ALL).sort(Sort.CREATED).direction(GHDirection.DESC).list()) {
            issues.add(new Issue(i));
        }

        return issues;
    }

    public static List<Release> extractReleases(GHRepository repo, Commit initCommit) throws IOException {
        List<Release> releases = new ArrayList<>();
        List<GHRelease> ghReleases_imm = repo.listReleases().toList();
        ArrayList<GHRelease> ghReleases = new ArrayList<>(ghReleases_imm);

        ghReleases.sort((r1, r2) -> {
            try {
                return r2.getCreatedAt().compareTo(r1.getCreatedAt());
            } catch (IOException e) {
                e.printStackTrace();
                return 0;
            }
        });

        for  (int i = 0; i < ghReleases.size() - 1; i++) {
            releases.add(new Release(ghReleases.get(i), ghReleases.get(i + 1).getTagName()));
        }

        if(!ghReleases.isEmpty()) {
            releases.add(new Release(ghReleases.getLast(), initCommit.sha1));
        }

        return releases;
    }

    public static List<Commit> extractCommits(GHRepository repo) throws IOException {
        List<Commit> commits = new ArrayList<>();
        for  (GHCommit c : repo.queryCommits().since(dateCutoff).list()) {
            commits.add(new Commit(c));
        }

        return commits;
    }

    public static List<IssueComment> extractIssueComments(GHIssue issue) throws IOException {
        List<IssueComment> comments = new ArrayList<>();

        for(GHIssueComment comment : issue.queryComments().since(dateCutoff).list()) {
            comments.add(new IssueComment(comment));
        }

        return comments;
    }

    public static Object[] extractDeploymentData(GHDeployment d) throws IOException {
        Object[] data = new Object[2];
        
        for (GHDeploymentStatus status : d.listStatuses()) {
            Instant curr = status.getCreatedAt();
            if (data[1] == null || curr.isAfter((Instant) data[1])) {
                data[0] = status.getState().toString();
                data[1] = curr;
            }
        }

        return data;
    }

    public static List<Deployment> extractDeployments(GHRepository repo) throws IOException {
        List<Deployment> deployments = new ArrayList<>();
        
        for (GHDeployment d : repo.listDeployments(null, null, null, null)) {
            deployments.add(new Deployment(d));
        }

        return deployments;
    }
}
