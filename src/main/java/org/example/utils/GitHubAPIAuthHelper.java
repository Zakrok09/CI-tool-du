package org.example.utils;

import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.extras.okhttp3.OkHttpGitHubConnector;

import java.io.File;
import java.io.IOException;

import static org.example.Main.logger;

public class GitHubAPIAuthHelper {
    String[] tokens = null;
    int step = 0;

    public static GitHub getGitHubAPI() {
        String gh_ouath = Dotenv.load().get("GITHUB_OAUTH");

        try {
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .cache(new Cache(new File("cache"), 100 * 1024 * 1024)) //100 MB cache
                    .build();

            GitHub gh = new GitHubBuilder()
                    .withConnector(new OkHttpGitHubConnector(okHttpClient))
                    .withOAuthToken(gh_ouath)
                    .build();
            logger.info("[GH] Logged in as: {}", gh.getMyself().getName());
            return gh;
        } catch (IOException e) {
            logger.info("[GH] Error reading GITHUB_OAUTH env key. Have you set it in .env?");
            throw new RuntimeException(e);
        }
    }

    public GitHub getNextGH() {
        if (this.tokens == null) {
            try {
                this.tokens = Dotenv.load().get("TOKEN_POOL").split(",");
            } catch (Exception e) {
                this.tokens = new String[] {Dotenv.load().get("GITHUB_OAUTH")};
            }
        }

        logger.info("{} tokens loaded.", tokens.length);

        try {
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .cache(new Cache(new File("cache"), 100 * 1024 * 1024)) //100 MB cache
                    .build();

            return new GitHubBuilder()
                    .withConnector(new OkHttpGitHubConnector(okHttpClient))
                    .withOAuthToken(tokens[(step++)%tokens.length])
                    .build();
        } catch (IOException e) {
            logger.info("[GH] Error reading TOKEN_POOL env key. Have you set it in .env?");
            throw new RuntimeException(e);
        }
    }
}
