package org.example.data;

import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHPullRequestCommitDetail;

import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;

public class Commit implements Serializable {

    public String sha1;
    public User author;
    public Instant commitDate;
    public int linesChanged;

    public Commit() {}

    public Commit(GHCommit commit) throws IOException {
        sha1 = commit.getSHA1();
        author = new User(commit.getAuthor());
        commitDate = commit.getCommitDate();
        linesChanged = commit.getLinesChanged();
    }
}
