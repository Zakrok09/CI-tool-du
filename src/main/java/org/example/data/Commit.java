package org.example.data;

import org.kohsuke.github.GHCommit;

import java.io.IOException;
import java.io.Serializable;

public class Commit implements Serializable {

    public String sha1;

    public Commit() {}

    public Commit(GHCommit commit) throws IOException {
        sha1 = commit.getSHA1();
    }
}
