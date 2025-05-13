package org.example.data;

import org.example.extraction.DataExtractor;
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
        sha1 = commit.getSHA1();
        author = new User(commit.getAuthor());
        commitDate = commit.getCommitDate();
        linesChanged = commit.getLinesChanged();
        documentationStats = DataExtractor.extractDocumentationStats(commit);
    }
}
