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

import org.example.utils.ProjectListOps;

import io.github.cdimascio.dotenv.Dotenv;
public class Main {
    public static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
        logger.info("Starting script");

        List<String> projects = ProjectListOps.getProjectListFromFile("intake/missing_code_comments.csv");
        Instant dateCutoff = Instant.parse(Dotenv.load().get("DATE_CUTOFF"));

        for (String project : projects) {
            Git gitRepo = CachedGitCloner.getGit(project);
            JGitCommitSampler sampler = new JGitCommitSampler(gitRepo.getRepository());

            sampler.sampleCommitsWithDuration(Duration.ofDays(30L), dateCutoff);

            sampler.printSamplesToCSV(project);
        }
    }
}