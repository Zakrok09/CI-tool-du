package org.example.data;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Issue extends GitHubObject implements Serializable {

    public List<IssueComment> comments;

    public Issue() {}

    public Issue(GHIssue issue) throws IOException {
        super(issue);

        comments = extractComments(issue);
    }

    private List<IssueComment> extractComments(GHIssue issue) throws IOException {
        List<IssueComment> comments = new ArrayList<>();

        for(GHIssueComment comment : issue.getComments()) {
            comments.add(new IssueComment(comment));
        }

        return comments;
    }
}
