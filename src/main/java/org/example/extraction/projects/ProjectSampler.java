package org.example.extraction.projects;

import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.apache.commons.lang3.tuple.Pair;
import org.example.extraction.ci.CIExtractorMain;
import org.example.utils.GitHubAPIAuthHelper;
import org.kohsuke.github.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.example.Main.logger;

public class ProjectSampler {

    public static Instant dateCutoff = Instant.parse(Dotenv.load().get("DATE_CUTOFF"));

    public static void main(String[] args) throws IOException {
        // preliminary_filtering("intake/2-projects-preliminary.csv");
        // secondary_filtering("intake/2-projects-preliminary.csv",
        // "intake/2-projects-secondary.csv");

        // Extract data for selection criteria - All projects
        // extractCommitCounts("intake/2-projects-preliminary-skip-5000.csv",
        // "intake/2-projects-all-skip-5000-commit-data.csv");
        // extractReleaseCounts("intake/2-projects-preliminary.csv",
        // "intake/2-projects-all-release-data.csv");

        // Filter based on issue usage
        final_filtering("intake/2-projects-tertiary-0-1200.csv", "intake/2-projects-final-0-1200.csv");
    }

    public static PagedSearchIterable<GHRepository> getReposIterable(GitHub gh) {
        return gh.searchRepositories()
                .language("python").language("javascript").language("typescript") // only these languages
                .language("java").language("c#").language("c++").language("php")
                .fork(GHFork.PARENT_ONLY) // no forks
                .visibility(GHRepository.Visibility.PUBLIC) // only public
                .stars(">49") // at least 50 stars
                .list();
    }

    public static void preliminary_filtering(String output) throws IOException {
        GitHub gh = GitHubAPIAuthHelper.getGitHubAPI();

        List<GHRepository> repos = new ArrayList<>();
        String[] languages = { "Python", "JavaScript", "TypeScript", "Java", "C#", "C++", "PHP" };

        for (String language : languages) {
            PagedSearchIterable<GHRepository> iterable = gh.searchRepositories()
                    .language(language)
                    .fork(GHFork.PARENT_ONLY) // no forks
                    .visibility(GHRepository.Visibility.PUBLIC) // only public
                    .stars(">49")
                    .list();

            PagedIterator<GHRepository> iterator = iterable.iterator();
            while (iterator.hasNext()) {
                GHRepository curr = iterator.next();
                if (!curr.isArchived()) {
                    repos.add(curr);
                }
            }
        }

        logger.info("Fetched {} repositories", repos.size());
        writeToFile(Path.of(output), repos);
    }

    public static void secondary_filtering(String input, String output) throws IOException {
        List<String> repos = CIExtractorMain.getProjectsFromCSV(input);

        GitHubAPIAuthHelper ghHelper = new GitHubAPIAuthHelper();

        List<String> filtered = repos.parallelStream()
                .map(repoName -> {
                    try {
                        GHRepository curr = ghHelper.getNextGH().getRepository(repoName);
                        if (hasWorkflowBeforeWindow(curr)) {
                            logger.info("Filtered in {}", repoName);
                            return repoName;
                        }
                    } catch (IOException e) {
                        logger.error("Error filtering {} on CI usage, error: {}", repoName, e.getMessage());
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();

        logger.info("Filtered {} repositories from {} total", filtered.size(), repos.size());
        writeStringNamesToFile(Path.of(output), filtered);
    }

    public static void final_filtering(String input, String output) throws IOException {
        List<String> repos = CIExtractorMain.getProjectsFromCSV(input);

        GitHubAPIAuthHelper ghHelper = new GitHubAPIAuthHelper();
        List<String> filtered = repos.parallelStream()
                .map(repoName -> {
                    logger.info("Filtering {} on label usage", repoName);
                    try {
                        GHRepository ghRepo = ghHelper.getNextGH().getRepository(repoName);
                        if (uses(ghRepo)) {
                            logger.info("Filtered in {}", repoName);
                            return repoName;
                        } else {
                            logger.info("Excluded {}", repoName);
                            return null;
                        }
                    } catch (IOException e) {
                        logger.error("Error filtering {} on label usage", repoName, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        try {
            logger.info("Filtered in {} repositories", filtered.size());
            writeStringNamesToFile(Path.of(output), filtered);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void extractCommitCounts(String input, String output) throws IOException {
        List<String> projects = CIExtractorMain.getProjectsFromCSV(input);

        String token = Dotenv.load().get("GITHUB_OAUTH");
        OkHttpClient client = new OkHttpClient();

        String since = "2024-05-15T00:00:00Z";
        String until = "2025-05-15T00:00:00Z";

        try (BufferedWriter writer = Files.newBufferedWriter(Path.of(output), StandardOpenOption.CREATE)) {
            writer.write("project;\n");

            projects.stream().forEach(project -> {
                logger.info("Getting commit count since {} until {} for {}", since, until, project);
                int commitCount = getCommitCountInWindow(client, project, token, since, until);
                logger.info("Got {} commit count for project: {}", commitCount, project);

                try {
                    writer.write(project + ";" + commitCount + "\n");
                } catch (Exception e) {
                    logger.error("Error processing run " + project + ", " + commitCount + ": " + e.getMessage());
                }
            });

            writer.flush();
        }
    }

    public static void extractReleaseCounts(String input, String output) throws IOException {
        List<String> projects = CIExtractorMain.getProjectsFromCSV(input);

        String token = Dotenv.load().get("GITHUB_OAUTH");
        OkHttpClient client = new OkHttpClient();

        try (BufferedWriter writer = Files.newBufferedWriter(Path.of(output),
                StandardOpenOption.CREATE)) {
            writer.write("project;\n");

            projects.stream().forEach(project -> {
                logger.info("Getting release count for {}", project);
                int releaseCount = getReleaseCount(client, project, token);
                logger.info("Got {} release count for project: {}", releaseCount, project);

                try {
                    writer.write(project + ";" + releaseCount + "\n");
                } catch (Exception e) {
                    logger.error("Error processing run " + project + ", " + releaseCount + ": " + e.getMessage());
                }
            });

            writer.flush();
        }
    }

    public static List<GHRepository> getFirstN(PagedSearchIterable<GHRepository> iterable, int n, int skip) {
        int i = 0;

        PagedIterator<GHRepository> iterator = iterable.iterator();
        while (i < skip && iterator.hasNext()) {
            i++;
            iterator.next();
        }

        ArrayList<GHRepository> res = new ArrayList<>();
        while (i < n && iterator.hasNext()) {
            i++;
            res.add(iterator.next());
        }
        return res;
    }

    public static List<GHRepository> getFirstN(PagedSearchIterable<GHRepository> iterable, int n) {
        return getFirstN(iterable, n, 0);
    }

    public static List<GHRepository> filterOnLabelUsage(List<GHRepository> repos) {
        return repos.stream().filter(ProjectSampler::uses).toList();
    }

    /**
     * Filters repositories which have at least 100 issues and 75% of them are
     * labelled.
     */
    public static boolean uses(GHRepository repo) {
        if (repo.isArchived()) { // not archived
            logger.info("Skipping archived repo {}", repo.getName());
            return false;
        }
        logger.info("Checking repo {}", repo.getName());

        int issues = 0;
        int labelled = 0;

        for (GHIssue issue : repo.queryIssues().state(GHIssueState.ALL).list()) {
            if (issue.isPullRequest())
                continue; // skip pull requests
            issues++;
            if (!issue.getLabels().isEmpty())
                labelled++;
        }

        logger.info(repo.getFullName() + " has {} issues and {} labelled", issues, labelled);

        return issues > 100 && (labelled / (double) issues) > 0.75;
    }

    public static boolean hasWorkflowBeforeWindow(GHRepository repo) throws IOException {
        for (GHWorkflow workflow : repo.listWorkflows()) {
            if (workflow.getCreatedAt().isBefore(dateCutoff)) {
                return true;
            }
        }

        return false;
    }

    public static int getReleaseCount(OkHttpClient client, String repo, String token) {
        String[] parts = repo.split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Repository must be in the format 'owner/repo'");
        }
        String owner = parts[0];
        repo = parts[1];

        String query = String.format("{ repository(owner: \"%s\", name: \"%s\") { releases { totalCount } } }", owner,
                repo);
        String json = "{\"query\": \"" + query.replace("\"", "\\\"") + "\"}";

        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url("https://api.github.com/graphql")
                .addHeader("Authorization", "Bearer " + token)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            assert response.body() != null;
            String respBody = response.body().string();
            int idx = respBody.indexOf("\"totalCount\":");
            if (idx == -1)
                throw new RuntimeException("totalCount not found in response");
            int start = idx + "\"totalCount\":".length();
            int end = respBody.indexOf("}", start);
            String countStr = respBody.substring(start, end).replaceAll("[^0-9]", "");
            return Integer.parseInt(countStr);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static int getCommitCountInWindow(OkHttpClient client, String repo, String token, String since,
            String until) {
        String[] parts = repo.split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Repository must be in the format 'owner/repo'");
        }
        String owner = parts[0];
        repo = parts[1];

        String query = String.format(
                "{ repository(owner: \"%s\", name: \"%s\") { defaultBranchRef { target { ... on Commit { history(since: \"%s\", until: \"%s\") { totalCount } } } } } }",
                owner, repo, since, until);
        String json = "{\"query\": \"" + query.replace("\"", "\\\"") + "\"}";

        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url("https://api.github.com/graphql")
                .addHeader("Authorization", "Bearer " + token)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            assert response.body() != null;
            String respBody = response.body().string();
            int idx = respBody.indexOf("\"totalCount\":");
            if (idx == -1)
                throw new RuntimeException("totalCount not found in response");
            int start = idx + "\"totalCount\":".length();
            int end = respBody.indexOf("}", start);
            String countStr = respBody.substring(start, end).replaceAll("[^0-9]", "");
            return Integer.parseInt(countStr);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeToFile(Path file, List<GHRepository> repos) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardOpenOption.CREATE)) {
            for (GHRepository repo : repos)
                try {
                    writer.write(repo.getOwnerName() + "/" + repo.getName() + "\n");
                } catch (Exception e) {
                    System.err.println("Error processing run " + repo.getOwnerName() + "/" + repo.getName() + ": "
                            + e.getMessage());
                }

            writer.flush();
        }
    }

    private static void writeStringNamesToFile(Path file, List<String> repos) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardOpenOption.CREATE)) {
            for (String repoName : repos)
                try {
                    writer.write(repoName + "\n");
                } catch (Exception e) {
                    System.err.println("Error processing run " + repoName + ": " + e.getMessage());
                }

            writer.flush();
        }
    }
}
