package org.example.utils;

import static org.example.Main.logger;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;

import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.extras.okhttp3.OkHttpGitHubConnector;

import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.Authenticator;
import okhttp3.Cache;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

public class GitHubAPIAuthHelper {
    String[] tokens = null;
    String[] proxies = null;
    String proxyHost = null;
    String proxyUser = null;
    String proxyPass = null;
    int step = 0;

    public static GitHub getGitHubAPI() {
        String gh_ouath = Dotenv.load().get("GITHUB_OAUTH");

        try {
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .cache(new Cache(new File("cache"), 100 * 1024 * 1024)) // 100 MB cache
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
        boolean useProxy = false;

        if (this.tokens == null) {
            try {
                this.tokens = Dotenv.load().get("TOKEN_POOL").split(",");
                this.proxies = Dotenv.load().get("PROXY_POOL").split(",");
                this.proxyHost = Dotenv.load().get("PROXY_HOST");
                this.proxyUser = Dotenv.load().get("PROXY_USER");
                this.proxyPass = Dotenv.load().get("PROXY_PASS");
                useProxy = Dotenv.load().get("USE_PROXY").equals("true");
                logger.info("{} tokens loaded.", tokens.length);
            } catch (Exception e) {
                this.tokens = new String[] { Dotenv.load().get("GITHUB_OAUTH") };
            }
        }

        try {
            Authenticator proxyAuthenticator = new Authenticator() {
                @Override
                public Request authenticate(Route route, Response response) throws IOException {
                    String credential = Credentials.basic(proxyUser, proxyPass);
                    return response.request().newBuilder()
                            .header("Proxy-Authorization", credential)
                            .build();
                }
            };


            OkHttpClient okHttpClient;
            
            if (useProxy) {
                int port = Integer.parseInt(proxies[(step++) % proxies.length]);
                okHttpClient = new OkHttpClient.Builder()
                    .cache(new Cache(new File("cache"), 100 * 1024 * 1024)) // 100 MB cache
                    .proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, port)))
                    .proxyAuthenticator(proxyAuthenticator)
                    .build();
            } else { 
                okHttpClient = new OkHttpClient.Builder()
                    .cache(new Cache(new File("cache"), 100 * 1024 * 1024)) // 100 MB cache
                    .build();
            }

            return new GitHubBuilder()
                    .withConnector(new OkHttpGitHubConnector(okHttpClient))
                    .withOAuthToken(tokens[(step++) % tokens.length])
                    .build();
        } catch (IOException e) {
            logger.info("[GH] Error reading TOKEN_POOL env key. Have you set it in .env?");
            throw new RuntimeException(e);
        }
    }
}
