package org.example.extraction.projects;

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

    public static void main(String[] args) throws IOException {
        GitHub gh = GitHubAPIAuthHelper.getGitHubAPI();

        PagedSearchIterable<GHRepository> iterable = gh.searchRepositories()
                .language("java").language("javascript").language("python")     // only those languages
                .fork(GHFork.PARENT_ONLY)                                               // no forks
                .visibility(GHRepository.Visibility.PUBLIC)                             // only public
                .sort(GHRepositorySearchBuilder.Sort.STARS)
                .order(GHDirection.DESC)
                .list();

        List<GHRepository> repos = getFirstN(iterable, 200, 100);
        logger.info("Fetched {} repositories", repos.size());
        logger.info("Filtering on label usage");
        List<GHRepository> filtered = filterOnLabelUsage(repos);
        logger.info("Filtered in {} repositories", filtered.size());

        filtered.forEach(System.out::println);

        writeToFile(Path.of("projects-new.csv"), filtered);
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
        if (repo.isArchived()) return false;
        logger.info("Checking repo {}", repo.getName());

        int issues = 0;
        int labelled = 0;

        for (GHIssue issue : repo.queryIssues().list()) {
            issues++;
            if (!issue.getLabels().isEmpty()) labelled++;
        }

        logger.debug(repo.getFullName() + " has {} issues and {} labelled", issues, labelled);

        return issues > 100 && (labelled / (double) issues) > 0.75;
    }

    private static void writeToFile(Path file, List<GHRepository> repos) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardOpenOption.CREATE)) {
            writer.write("project;");

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
