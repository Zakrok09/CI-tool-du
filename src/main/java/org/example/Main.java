package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.fetching.FetchSettings;
import org.example.scripts.Daniel;
import org.example.scripts.Serban;

import org.example.fetching.CachedGitCloner;
import org.example.extraction.JGitCommitSampler;
import org.example.extraction.ci.CIExtractorMain;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.io.IOException;

import io.github.cdimascio.dotenv.Dotenv;
public class Main {
    public static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
        logger.info("Starting script");
        FetchSettings.Daniel();
        Daniel.danielComments("missing_comments.csv", 30, 5);
    }
}