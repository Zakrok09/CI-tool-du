package org.example.fetching;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.example.data.Repository;

import java.io.File;
import java.io.IOException;

public class CachedGitCloner {
    public static Git getGit(String repoName, boolean forceUpdate) throws IOException {
        File output = new File("clones", repoName.replace("/", "_"));
        if (output.exists() && !forceUpdate) {
            System.out.println(repoName + " found locally, getting from cache.");
            return Git.open(output);
        }

        System.out.println(repoName + " not available locally, cloning.");
        String repoUrl = "https://github.com/" + repoName;
        Git git = null;
        try {
            git = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(output)
                    .call();
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Cloned " + repoName + "repository to: " + output);
        return git;
    }

    public static Git getGit(String repoName) throws IOException {
        return getGit(repoName, false);
    }
}
