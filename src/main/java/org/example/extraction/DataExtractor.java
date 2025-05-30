package org.example.extraction;

import org.apache.commons.lang3.tuple.Pair;
import org.example.data.*;
import org.example.utils.Helper;
import org.kohsuke.github.*;
import org.kohsuke.github.GHCommit.File;
import org.kohsuke.github.GHIssueQueryBuilder.Sort;

import io.github.cdimascio.dotenv.Dotenv;

import static org.example.Main.logger;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataExtractor {
    // .emv example: DATE_CUTOFF=2024-01-01T00:00:00.00Z
    public static Instant dateCutoff = Instant.parse(Dotenv.load().get("DATE_CUTOFF"));

    public static List<PullRequest> extractPullRequests(GHRepository repo) throws IOException {
        List<PullRequest> prs = new ArrayList<>();
        for (GHPullRequest p : repo.queryPullRequests().state(GHIssueState.ALL).list()) {
            prs.add(new PullRequest(p));
        }

        return prs;
    }

    public static List<Issue> extractIssues(GHRepository repo) throws IOException {
        List<Issue> issues = new ArrayList<>();
        for (GHIssue i : repo.queryIssues().since(dateCutoff).state(GHIssueState.ALL).sort(Sort.CREATED)
                .direction(GHDirection.DESC).list()) {
            issues.add(new Issue(i));
        }

        return issues;
    }

    public static List<Release> extractReleases(GHRepository repo, Commit initCommit) throws IOException {
        List<Release> releases = new ArrayList<>();
        List<GHRelease> ghReleases_imm = repo.listReleases().toList();
        ArrayList<GHRelease> ghReleases = new ArrayList<>(ghReleases_imm);

        for (int i = 0; i < ghReleases.size() - 1; i++) {
            releases.add(new Release(ghReleases.get(i), ghReleases.get(i + 1).getTagName()));
        }

        if(!ghReleases.isEmpty()) {
            releases.add(new Release(ghReleases.getLast(), initCommit.sha1));
        }

        return releases;
    }

    public static List<Commit> extractCommits(GHRepository repo) throws IOException {
        List<Commit> commits = new ArrayList<>();
        for (GHCommit c : repo.queryCommits().since(dateCutoff).list()) {
            commits.add(new Commit(c));
        }

        return commits;
    }

    public static List<IssueComment> extractIssueComments(GHIssue issue) throws IOException {
        List<IssueComment> comments = new ArrayList<>();

        for (GHIssueComment comment : issue.queryComments().since(dateCutoff).list()) {
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

    public static void putAllInMap(Map<String, Pair<String, GHTreeEntry>> map, GHTree tree, String folder) {
        tree.getTree().stream().forEach(e -> {
                map.put(e.getPath(), Pair.of(folder + e.getPath(), e));
            });
    }

    public static void fillSearchEntries(Map<String, Pair<String, GHTreeEntry>> map, GHCommit commit) throws IOException {
        GHTree rootTree = commit.getTree();

        GHTreeEntry docsTreeEntry = rootTree.getEntry("docs");
        if (docsTreeEntry != null && docsTreeEntry.asTree() != null) {
            GHTree docsTree = docsTreeEntry.asTree();
            putAllInMap(map, docsTree, "docs/");

            docsTree.getTree().forEach(entry -> {
                String[] split = entry.getPath().split("/");
                String entryName = split[split.length - 1];

                if (entryName.equals("PULL_REQUEST_TEMPLATE")) {
                    try {
                        putAllInMap(map, entry.asTree(), "docs/PULL_REQUEST_TEMPLATE/");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        putAllInMap(map, rootTree, "");

        GHTreeEntry prTreeEntry = rootTree.getEntry("PULL_REQUEST_TEMPLATE");
        if (prTreeEntry != null && prTreeEntry.asTree() != null) {
            putAllInMap(map, prTreeEntry.asTree(), "PULL_REQUEST_TEMPLATE/");
        }
        
        GHTreeEntry ghTreeEntry = rootTree.getEntry(".github");
        if (ghTreeEntry != null && ghTreeEntry.asTree() != null) {
            GHTree ghTree = ghTreeEntry.asTree();
            putAllInMap(map, ghTree, ".github/");

            ghTree.getTree().forEach(entry -> {
                String[] split = entry.getPath().split("/");
                String entryName = split[split.length - 1];

                if (entryName.equals("ISSUE_TEMPLATE")) {
                    try {
                        putAllInMap(map, entry.asTree(), ".github/ISSUE_TEMPLATE/");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (entryName.equals("PULL_REQUEST_TEMPLATE")) {
                    try {
                        putAllInMap(map, entry.asTree(), ".github/PULL_REQUEST_TEMPLATE/");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public static void fillSearchFiles(Map<String, File> map, GHCommit commit) throws IOException {
        Map<String, Integer> folderPrecedence = new HashMap<>();
        folderPrecedence.put("docs", 1); // Lowest precedence
        folderPrecedence.put("", 2); // Middle precedence (root folder)
        folderPrecedence.put(".github", 3); // Highest precedence

        Map<String, Integer> currentFilePrecedenceInMap = new HashMap<>();

        List<File> allFiles = commit.listFiles().toList();

        for (File file : allFiles) {
            String filePath = file.getFileName();
            String[] split = filePath.split("/");

            String folder = split.length > 1 ? split[split.length - 2] : "";
            String fileName = split[split.length - 1];

            Integer currentPrecedence = folderPrecedence.get(folder);

            if (currentPrecedence != null) {
                Integer existingPrecedence = currentFilePrecedenceInMap.get(fileName);

                if (existingPrecedence == null || currentPrecedence > existingPrecedence) {
                    map.put(fileName, file);
                    currentFilePrecedenceInMap.put(fileName, currentPrecedence);
                }
            }
        }
    }

    public static DocumentationStats extractDocumentationStats(GHCommit commit) throws IOException {
        DocumentationStats stats = new DocumentationStats();
        int n = DocumentationStats.DOC_FILE_LIST.length;

        Map<String, Pair<String, GHTreeEntry>> searchEntries = new HashMap<>();
        // fillSearchEntries(searchEntries, commit);

        searchEntries.values().forEach(pair -> {
            String[] split = pair.getKey().split("/");
            String folder = split.length > 1 ? split[split.length - 2] : "";
            String fileName = split[split.length - 1];

            GHTreeEntry entry = pair.getValue();
            
            if (DocumentationStats.DOC_FILE_MAP.containsKey(fileName)) {
                int ind = DocumentationStats.DOC_FILE_MAP.get(fileName);
                stats.documentationFiles[ind].exists = true;
                try {
                    stats.documentationFiles[ind].size = (int) Helper.countLines(entry.readAsBlob());
                } catch (IOException e) {
                    logger.info("[DataExtractor] Error reading file entry: " + e.getMessage());
                    throw new RuntimeException(e);
                }
            }

            if (folder.equals("ISSUE_TEMPLATE") && (fileName.endsWith(".md") || fileName.endsWith(".yml"))) {
                stats.documentationFiles[n].exists = true;
                try {
                    stats.documentationFiles[n].size += (int) Helper.countLines(entry.readAsBlob());
                } catch (IOException e) {
                    logger.info("[DataExtractor] Error reading issue template entry: " + e.getMessage());
                    throw new RuntimeException(e);
                }
            } else if (fileName.equalsIgnoreCase("pull_request_template.md") || (folder.equals("PULL_REQUEST_TEMPLATE") && fileName.endsWith(".md"))) {
                stats.documentationFiles[n + 1].exists = true;
                try {
                    stats.documentationFiles[n + 1].size += (int) Helper.countLines(entry.readAsBlob());
                } catch (IOException e) {
                    logger.info("[DataExtractor] Error reading pull request template entry: " + e.getMessage());
                    throw new RuntimeException(e);
                }
            }
        });

        Map<String, File> searchFiles = new HashMap<>();
        fillSearchFiles(searchFiles, commit);

        searchFiles.values().forEach(file -> {
            String[] split = file.getFileName().split("/");
            String folder = split.length > 1 ? split[split.length - 2] : "";
            String fileName = split[split.length - 1];
            
            if (DocumentationStats.DOC_FILE_MAP.containsKey(fileName)) {
                int ind = DocumentationStats.DOC_FILE_MAP.get(fileName);
                stats.documentationFiles[ind].additions = (int) file.getLinesAdded();
                stats.documentationFiles[ind].deletions = (int) file.getLinesDeleted();
            }

            if (folder.equals("ISSUE_TEMPLATE") && (fileName.endsWith(".md") || fileName.endsWith(".yml"))) {
                stats.documentationFiles[n].additions += (int) file.getLinesAdded();
                stats.documentationFiles[n].deletions += (int) file.getLinesDeleted();
            } else if (fileName.equalsIgnoreCase("pull_request_template.md") || (folder.equals("PULL_REQUEST_TEMPLATE") && fileName.endsWith(".md"))) {
                stats.documentationFiles[n + 1].additions += (int) file.getLinesAdded();
                stats.documentationFiles[n + 1].deletions += (int) file.getLinesDeleted();
            }
        });

        return stats;
    }
}
