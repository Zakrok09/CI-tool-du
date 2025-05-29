package org.example.extraction.projects;

import io.github.cdimascio.dotenv.Dotenv;

import org.apache.commons.lang3.tuple.Pair;
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
    //     GitHub gh = GitHubAPIAuthHelper.getGitHubAPI();

    //     PagedSearchIterable<GHRepository> iterable = gh.searchRepositories()
    //             .language("java").language("javascript").language("python")     // only those languages
    //             .fork(GHFork.PARENT_ONLY)                                       // no forks
    //             .visibility(GHRepository.Visibility.PUBLIC)                     // only public
    //             .stars(">49")                                                   // at least 50 stars
    //             .list();

    //     List<GHRepository> repos = new ArrayList<>();
    //     PagedIterator<GHRepository> iterator = iterable.iterator();
    //     while (iterator.hasNext()) {
    //         repos.add(iterator.next());
    //     }

    //     logger.info("Fetched {} repositories", repos.size());
    //     writeToFile(Path.of("projects-all.csv"), repos);
    // }

    // public static void main(String[] args) throws IOException {
    //     int firstN = 1000;
    //     int skip = 950;
    //     int total = firstN - skip;

    //     int totalTokens = Dotenv.load().get("TOKEN_POOL").split(",").length;
    //     int threadsPerToken = 1;
    //     int batchSize = total / totalTokens / threadsPerToken;

    //     GitHubAPIAuthHelper ghHelper = new GitHubAPIAuthHelper();
    //     List<Pair<Integer, PagedSearchIterable<GHRepository>>> searchPairs = new ArrayList<>();
    //     for (int i = 0; i < totalTokens; ++i) {
    //         GitHub gh = ghHelper.getNextGH();
    //         for (int j = 0; j < threadsPerToken; ++j) {
    //             int index = i * threadsPerToken + j;
    //             PagedSearchIterable<GHRepository> iterable = getReposIterable(gh);
    //             searchPairs.add(Pair.of(index, iterable));
    //         }
    //     }

    //     searchPairs.parallelStream().forEach(pair -> {
    //         int index = pair.getKey();
    //         PagedSearchIterable<GHRepository> iterable = pair.getValue();

    //         int start = skip + index * batchSize;
    //         int end = (index != totalTokens * threadsPerToken - 1) ? start + batchSize : firstN;

    //         logger.info("Starting thread for token index {} with start {} and end {}", index, start, end);

    //         List<GHRepository> repos = getFirstN(iterable, end, start);
    //         logger.info("Fetched {} repositories", repos.size());
            
    //         logger.info("Filtering on label usage");
    //         List<GHRepository> filtered = filterOnLabelUsage(repos);
            
    //         logger.info("Filtered in {} repositories", filtered.size());
            
    //         String fileName = "projects-new-" + start + "-" + end + ".csv";
            
    //         try {
    //             writeToFile(Path.of(fileName), filtered);
    //         } catch (IOException e) {
    //             e.printStackTrace();
    //         }
    //     });
    // }

    public static void main(String[] args) throws IOException {
        String[] input = new String[] { "elastic/elasticsearch" , "louislam/uptime-kuma" };

        int totalTokens = Dotenv.load().get("TOKEN_POOL").split(",").length;
        int threadsPerToken = 1;
        int batchSize = input.length / totalTokens / threadsPerToken;

        GitHubAPIAuthHelper ghHelper = new GitHubAPIAuthHelper();
        List<Pair<Integer, List<GHRepository>>> searchPairs = new ArrayList<>();
        for (int i = 0; i < totalTokens; ++i) {
            GitHub gh = ghHelper.getNextGH();
            for (int j = 0; j < threadsPerToken; ++j) {
                int index = i * threadsPerToken + j;
                List<GHRepository> repos = new ArrayList<>();

                for (int k = 0; k < batchSize; ++k) {
                    repos.add(gh.getRepository(input[index * batchSize + k]));
                }

                searchPairs.add(Pair.of(index, repos));
            }
        }

        searchPairs.parallelStream().forEach(pair -> {
            int index = pair.getKey();
            List<GHRepository> repos = pair.getValue();

            int start = index * batchSize;
            int end = (index != totalTokens * threadsPerToken - 1) ? start + batchSize : input.length;

            logger.info("Starting thread for token index {} with start {} and end {}", index, start, end);
            
            logger.info("Filtering on label usage");
            List<GHRepository> filtered = filterOnLabelUsage(repos);
            
            logger.info("Filtered in {} repositories", filtered.size());
            
            String fileName = "projects-new-selected-" + start + "-" + end + ".csv";
            
            try {
                writeToFile(Path.of(fileName), filtered);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static PagedSearchIterable<GHRepository> getReposIterable(GitHub gh) {
        return gh.searchRepositories()
                .language("java").language("javascript").language("python")     // only those languages
                .fork(GHFork.PARENT_ONLY)                                             // no forks
                .visibility(GHRepository.Visibility.PUBLIC)                           // only public
                .stars(">49")                                                       // at least 50 stars
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
     * Filters repositories which have at least 100 issues and 75% of them are labelled.
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
            if (issue.isPullRequest()) continue;                               // skip pull requests
            issues++;
            if (!issue.getLabels().isEmpty()) labelled++;
        }

        logger.info(repo.getFullName() + " has {} issues and {} labelled", issues, labelled);

        return issues > 100 && (labelled / (double) issues) > 0.75;
    }

    private static void writeToFile(Path file, List<GHRepository> repos) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardOpenOption.CREATE)) {
            writer.write("project;\n");

            for (GHRepository repo : repos)
                try {
                    writer.write(repo.getOwnerName() + "/" + repo.getName() + "\n");
                } catch (Exception e) {
                    System.err.println("Error processing run " + repo.getOwnerName() + "/" + repo.getName() + ": " + e.getMessage());
                }

            writer.flush();
        }
    }
}
