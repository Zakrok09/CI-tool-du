package org.example.data;

import org.example.extraction.DataExtractor;
import org.example.fetching.FetchSettings;
import org.kohsuke.github.GHCommit;

import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;

public class Commit implements Serializable {

    public String sha1;
    public User author;
    public Instant commitDate;
    public int linesChanged;
    public DocumentationStats documentationStats;

    public Commit() {}

    public Commit(GHCommit commit) throws IOException {
        try {
            sha1 = commit.getSHA1();
            author = FetchSettings.users ? new User(commit.getAuthor()) : null;
            commitDate = commit.getCommitDate();
            linesChanged = commit.getLinesChanged();
            documentationStats = DataExtractor.extractDocumentationStats(commit);
        } catch (Exception e) {
            sha1 = null;
            author = null;
            commitDate = null;
            linesChanged = 0;
        }
    }
}
