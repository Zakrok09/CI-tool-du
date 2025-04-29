package org.example.utils;

import io.github.cdimascio.dotenv.Dotenv;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import java.io.IOException;

public class GitHubAPIAuthHelper {

    public static GitHub getGitHubAPI() {
        String gh_ouath = Dotenv.load().get("GITHUB_OAUTH");

        try {
            GitHub gh = new GitHubBuilder().withOAuthToken(gh_ouath).build();
            System.out.println("[GH] Logged in as: " + gh.getMyself().getName());
            return gh;
        } catch (IOException e) {
            System.out.println("[GH] Error reading GITHUB_OAUTH env key. Have you set it in .env?");
            throw new RuntimeException(e);
        }
    }
}
