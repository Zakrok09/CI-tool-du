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
import java.util.ArrayList;
import java.util.List;

import static org.example.Main.logger;

public class ProjectSampler {

    // public static void main(String[] args) throws IOException {
    // GitHub gh = GitHubAPIAuthHelper.getGitHubAPI();

    // PagedSearchIterable<GHRepository> iterable = gh.searchRepositories()
    // .language("java").language("javascript").language("python") // only those
    // languages
    // .fork(GHFork.PARENT_ONLY) // no forks
    // .visibility(GHRepository.Visibility.PUBLIC) // only public
    // .stars(">49") // at least 50 stars
    // .list();

    // List<GHRepository> repos = new ArrayList<>();
    // PagedIterator<GHRepository> iterator = iterable.iterator();
    // while (iterator.hasNext()) {
    // repos.add(iterator.next());
    // }

    // logger.info("Fetched {} repositories", repos.size());
    // writeToFile(Path.of("projects-all.csv"), repos);
    // }

    // public static void main(String[] args) throws IOException {
    // int firstN = 1000;
    // int skip = 950;
    // int total = firstN - skip;

    // int totalTokens = Dotenv.load().get("TOKEN_POOL").split(",").length;
    // int threadsPerToken = 1;
    // int batchSize = total / totalTokens / threadsPerToken;

    // GitHubAPIAuthHelper ghHelper = new GitHubAPIAuthHelper();
    // List<Pair<Integer, PagedSearchIterable<GHRepository>>> searchPairs = new
    // ArrayList<>();
    // for (int i = 0; i < totalTokens; ++i) {
    // GitHub gh = ghHelper.getNextGH();
    // for (int j = 0; j < threadsPerToken; ++j) {
    // int index = i * threadsPerToken + j;
    // PagedSearchIterable<GHRepository> iterable = getReposIterable(gh);
    // searchPairs.add(Pair.of(index, iterable));
    // }
    // }

    // searchPairs.parallelStream().forEach(pair -> {
    // int index = pair.getKey();
    // PagedSearchIterable<GHRepository> iterable = pair.getValue();

    // int start = skip + index * batchSize;
    // int end = (index != totalTokens * threadsPerToken - 1) ? start + batchSize :
    // firstN;

    // logger.info("Starting thread for token index {} with start {} and end {}",
    // index, start, end);

    // List<GHRepository> repos = getFirstN(iterable, end, start);
    // logger.info("Fetched {} repositories", repos.size());

    // logger.info("Filtering on label usage");
    // List<GHRepository> filtered = filterOnLabelUsage(repos);

    // logger.info("Filtered in {} repositories", filtered.size());

    // String fileName = "projects-new-" + start + "-" + end + ".csv";

    // try {
    // writeToFile(Path.of(fileName), filtered);
    // } catch (IOException e) {
    // e.printStackTrace();
    // }
    // });
    // }

    // public static void main(String[] args) throws IOException {
    // String[] input = new String[] { "elastic/elasticsearch" ,
    // "louislam/uptime-kuma" };

    // int totalTokens = Dotenv.load().get("TOKEN_POOL").split(",").length;
    // int threadsPerToken = 1;
    // int batchSize = input.length / totalTokens / threadsPerToken;

    // GitHubAPIAuthHelper ghHelper = new GitHubAPIAuthHelper();
    // List<Pair<Integer, List<GHRepository>>> searchPairs = new ArrayList<>();
    // for (int i = 0; i < totalTokens; ++i) {
    // GitHub gh = ghHelper.getNextGH();
    // for (int j = 0; j < threadsPerToken; ++j) {
    // int index = i * threadsPerToken + j;
    // List<GHRepository> repos = new ArrayList<>();

    // for (int k = 0; k < batchSize; ++k) {
    // repos.add(gh.getRepository(input[index * batchSize + k]));
    // }

    // searchPairs.add(Pair.of(index, repos));
    // }
    // }

    // searchPairs.parallelStream().forEach(pair -> {
    // int index = pair.getKey();
    // List<GHRepository> repos = pair.getValue();

    // int start = index * batchSize;
    // int end = (index != totalTokens * threadsPerToken - 1) ? start + batchSize :
    // input.length;

    // logger.info("Starting thread for token index {} with start {} and end {}",
    // index, start, end);

    // logger.info("Filtering on label usage");
    // List<GHRepository> filtered = filterOnLabelUsage(repos);

    // logger.info("Filtered in {} repositories", filtered.size());

    // String fileName = "projects-new-selected-" + start + "-" + end + ".csv";

    // try {
    // writeToFile(Path.of(fileName), filtered);
    // } catch (IOException e) {
    // e.printStackTrace();
    // }
    // });
    // }

    // public static void main(String[] args) throws IOException {
    //     List<String> projects = CIExtractorMain.getProjectsFromCSV("projects-all.csv");

    //     String token = Dotenv.load().get("GITHUB_OAUTH");
    //     OkHttpClient client = new OkHttpClient();

    //     String since = "2024-05-01T00:00:00Z";
    //     String until = "2025-05-01T00:00:00Z";

    //     String fileName = "projects-all-commit-data.csv";
    //     try (BufferedWriter writer = Files.newBufferedWriter(Path.of(fileName), StandardOpenOption.CREATE)) {
    //         writer.write("project;\n");

    //         projects.stream().forEach(project -> {
    //             logger.info("Getting commit count since {} until {} for {}", since, until, project);
    //             int commitCount = getCommitCountInWindow(client, project, token, since, until);
    //             logger.info("Got {} commit count for project: {}", commitCount, project);

    //             try {
    //                 writer.write(project + ";" + commitCount + "\n");
    //             } catch (Exception e) {
    //                 logger.error("Error processing run " + project + ", " + commitCount + ": "
    //                         + e.getMessage());
    //             }
    //         });

    //         writer.flush();
    //     }
    // }

    public static void main(String[] args) throws IOException {
        List<String> projects = CIExtractorMain.getProjectsFromCSV("projects-all.csv");

        String token = Dotenv.load().get("GITHUB_OAUTH");
        OkHttpClient client = new OkHttpClient();

        String fileName = "projects-all-release-data.csv";
        try (BufferedWriter writer = Files.newBufferedWriter(Path.of(fileName), StandardOpenOption.CREATE)) {
            writer.write("project;\n");

            projects.stream().forEach(project -> {
                logger.info("Getting release count for {}", project);
                int releaseCount = getReleaseCount(client, project, token);
                logger.info("Got {} release count for project: {}", releaseCount, project);

                try {
                    writer.write(project + ";" + releaseCount + "\n");
                } catch (Exception e) {
                    logger.error("Error processing run " + project + ", " + releaseCount + ": "
                            + e.getMessage());
                }
            });

            writer.flush();
        }
    }

    public static PagedSearchIterable<GHRepository> getReposIterable(GitHub gh) {
        return gh.searchRepositories()
                .language("java").language("javascript").language("python") // only those languages
                .fork(GHFork.PARENT_ONLY) // no forks
                .visibility(GHRepository.Visibility.PUBLIC) // only public
                .stars(">49") // at least 50 stars
                .list();
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
            writer.write("project;\n");

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
}
