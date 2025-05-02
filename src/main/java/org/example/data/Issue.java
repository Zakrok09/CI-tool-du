package org.example.data;

import org.example.extraction.DataExtractor;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;

import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Issue extends GitHubObject implements Serializable {

    public List<IssueComment> comments;
    public Instant createdAt;
    public Instant closedAt;

    public Issue() {}

    public Issue(GHIssue issue) throws IOException {
        super(issue);

        comments = DataExtractor.extractComments(issue);
        createdAt = issue.getCreatedAt();
        closedAt = issue.getClosedAt();
    }


}
