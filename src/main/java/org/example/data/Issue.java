package org.example.data;

import org.kohsuke.github.GHIssue;

import java.io.IOException;
import java.io.Serializable;

public class Issue extends GitHubObject implements Serializable {
    public Issue() {}

    public Issue(GHIssue issue) throws IOException {
        super(issue);
    }
}
