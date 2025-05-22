package org.example.data;

import org.kohsuke.github.GHIssueComment;

import java.io.IOException;
import java.io.Serializable;

public class IssueComment extends GitHubObject implements Serializable {

    public String body;

    public IssueComment() {}

    public IssueComment(GHIssueComment issueComment) throws IOException {
        super(issueComment);

        try {
            body = issueComment.getBody();
        } catch (Exception e) {
            body = null;
        }
    }
}
