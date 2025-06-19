package org.example.fetching;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.utils.GitHubAPIAuthHelper;
import org.kohsuke.github.GitHub;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.example.Main.logger;

public class FetchFromJSON {
    public static List<String> loadProjectsFromFile() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(new File("intake/results.json"));

        List<String> urls = new ArrayList<>();
        for (JsonNode item : root.get("items")) {
            urls.add(item.get("name").asText());
        }

        return urls;
    }

    public List<String> fetch(int limit, int offset) throws IOException {
        long start = System.nanoTime();
        logger.info("Fetching {} projects.", limit);
        List<String> projects = loadProjectsFromFile().stream().skip(offset).limit(limit).toList();
        projects.forEach(logger::debug);
        GitHubAPIAuthHelper ghHelper = new GitHubAPIAuthHelper();

        projects.parallelStream().forEach(project -> {
            try {
                // it might not be a great idea to parallelize API stuff.
                // what could possibly go wrong
                CachedGitCloner.getGit(project);
                CachedDataRepoFetcher.getRepoData(ghHelper.getNextGH(), project);
                logger.info("{} downloaded successfully.", project);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        long end = System.nanoTime();
        long elapsedSeconds = (end - start) / 1_000_000_000;
        long minutes = elapsedSeconds / 60;
        long seconds = elapsedSeconds % 60;

        logger.info(String.format("Downloaded %s projects in %dmin%02dsec.", limit, minutes, seconds));


        return projects;
    }
}
