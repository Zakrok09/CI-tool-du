package org.example;

import org.example.data.Repository;
import org.example.fetching.CachedDataRepoFetcher;
import org.example.utils.GitHubAPIAuthHelper;
import org.kohsuke.github.GitHub;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        GitHub gh = GitHubAPIAuthHelper.getGitHubAPI();

        Repository geit = CachedDataRepoFetcher.getRepo(gh, "kelhaji/geit", true);
    }
}