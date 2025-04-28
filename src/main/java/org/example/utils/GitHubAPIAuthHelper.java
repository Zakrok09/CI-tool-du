package org.example.utils;

import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class GitHubAPIAuthHelper {
    public static GitHub getGitHubAPI() throws IOException {
        Properties props = new Properties();

        try (FileInputStream fis = new FileInputStream("config.properties")) {
            props.load(fis);
        } catch (IOException e) {
            System.out.println("[GH] Error reading config file. Have you created config.properties?");
            e.printStackTrace();
        }

        String gh_ouath = props.getProperty("GITHUB_OAUTH");
        GitHub gh = new GitHubBuilder().withOAuthToken(gh_ouath).build();

        System.out.println("[GH] Logged in as: " + gh.getMyself().getName());
        return gh;
    }
}
