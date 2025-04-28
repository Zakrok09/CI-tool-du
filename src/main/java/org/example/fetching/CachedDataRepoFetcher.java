package org.example.fetching;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.example.data.GHRepositoryMixin;
import org.example.data.Repository;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.File;
import java.io.IOException;

public class CachedDataRepoFetcher {
    /** Checks local file system in repos/**. If a repo is found there, it is returned.
     * If not, it's collected using the API object, and saved for future use.
     * TODO: Save a timestamp as well? For updates/data safety.
     * @param gh GitHub API object
     * @param repoName name of repo to fetch
     * @param forceUpdate will update the local file even if found
     * @return repository object
     * @throws IOException
     */
    public static Repository getRepo(GitHub gh, String repoName, boolean forceUpdate) throws IOException {
        System.out.println("Getting repo: " + repoName);

        System.out.println("Initializing mapper.");
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        String repoFileName = repoName.replace('/', '_') + ".json";
        File output = new File("repos", repoFileName);
        if (output.exists() && !forceUpdate) {
            System.out.println(repoName + " found locally, getting from cache.");
            return mapper.readValue(output, Repository.class);
        }

        System.out.println(repoName + " not found or forced update, getting from GitHub.");
        Repository repo = new Repository(gh.getRepository(repoName));

        System.out.println("Done reading from API, writing to file.");
        output.getParentFile().mkdirs();
        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(output, repo);
        return repo;
    }

    public static Repository getRepo(GitHub gh, String repoName) throws IOException {
        return getRepo(gh, repoName, false);
    }
}
