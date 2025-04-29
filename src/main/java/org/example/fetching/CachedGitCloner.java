package org.example.fetching;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;

import static org.example.Main.logger;

public class CachedGitCloner {
    public static Git getGit(String repoName, boolean forceUpdate) throws IOException {
        File output = new File("clones", repoName.replace("/", "_"));

        if (output.exists() && !forceUpdate) {
            logger.debug("{} found locally, getting from cache.", repoName);
            return Git.open(output);
        }

        logger.debug("{} not available locally, cloning.", repoName);
        String repoUrl = "https://github.com/" + repoName;
        Git git;
        try {
            git = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(output)
                    .call();
        } catch (GitAPIException e) {
            logger.fatal("Error cloning repository: {}", e.getLocalizedMessage());
            throw new RuntimeException(e);
        }

        logger.debug("Cloned {}repository to: {}", repoName, output);
        return git;
    }

    public static Git getGit(String repoName) throws IOException {
        return getGit(repoName, false);
    }
}
