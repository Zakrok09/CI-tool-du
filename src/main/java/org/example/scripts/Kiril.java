package org.example.scripts;

import org.example.Main;
import org.example.fetching.CachedDataRepoFetcher;
import org.example.fetching.CachedGitCloner;
import org.example.utils.GitHubAPIAuthHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

public class Kiril {

    public static void pullDataFromFile(String filename) throws IOException {


        List<String> items = Files.readAllLines(Paths.get(filename)).stream().skip(1).toList();

        GitHubAPIAuthHelper ghHelper = new GitHubAPIAuthHelper();

        long start = System.nanoTime();
        try (ForkJoinPool customPool = new ForkJoinPool(1)) {
            customPool.submit(() -> {
                items.parallelStream().forEach(project -> {
                    try {
                        CachedGitCloner.getGit(project);
                        CachedDataRepoFetcher.getRepoData(ghHelper.getNextGH(), project);
                        Main.logger.info("{} downloaded successfully.", project);
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                });
            }).join();
        }
        long end = System.nanoTime();
        long elapsedSeconds = (end - start) / 1_000_000_000;
        long minutes = elapsedSeconds / 60;
        long seconds = elapsedSeconds % 60;
        Main.logger.info(String.format("Downloaded %d projects in %dmin%02dsec.", items.size(), minutes, seconds));
    }
}
