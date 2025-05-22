package org.example.fetching;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.data.Repository;
import org.kohsuke.github.GitHub;

import java.io.*;
import java.rmi.RemoteException;

import static org.example.Main.logger;

public class CachedDataRepoFetcher {
    /** Checks local file system in repos/**. If a repo is found there, it is returned.
     * If not, it's collected using the API object, and saved for future use.
     * TODO: Save a timestamp as well? For updates/data safety.
     * @param gh GitHub API object
     * @param repoName name of repo to fetch
     * @param forceUpdate will update the local file even if found
     * @return repository object
     * @throws IOException on failed read of saved data
     */
    public static Repository getRepoData(GitHub gh, String repoName, boolean forceUpdate) throws IOException {
        logger.info("Getting repo data: {}", repoName);

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.registerModule(new com.fasterxml.jackson.datatype.jdk8.Jdk8Module());

        String repoFileName = repoName.replace('/', '_') + ".json";
        File output = new File("repos", repoFileName);
        if (output.exists() && !forceUpdate) {
            logger.debug("{} API data found locally.", repoName);
            return mapper.readValue(output, Repository.class);
        }

        if(!output.getParentFile().exists() && !output.getParentFile().mkdirs()) {
            logger.error("Failed to create directories, necessary to save repo data.");
            throw new RemoteException("Error `.mkdirs()`. Directories not created.");
        }

        logger.debug("{} not found or forced update, getting from GitHub.", repoName);
        Repository repo = new Repository(gh.getRepository(repoName));

        logger.info("Done reading from API, writing to file.");

        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(output, repo);

        return repo;
    }

    public static Repository getRepoData(GitHub gh, String repoName) throws IOException {
        return getRepoData(gh, repoName, false);
    }
}
