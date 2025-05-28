package org.example.data;

import org.example.extraction.DataExtractor;
import org.example.fetching.FetchSettings;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHLabel;

import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Issue extends GitHubObject implements Serializable {

    public boolean isBug;
    public Instant closedAt;
    public List<IssueComment> comments;

    public Issue() {
    }

    public Issue(GHIssue issue) throws IOException {
        super(issue);

        // Could also try some regex where the label contains "bug" but does not contain "fix"
        // TODO: Should we include a field to mark if the issue is a pull request? i.e., do we consider open PR with label "bug" for defect count
        try {
            isBug = isBug(issue);
            closedAt = issue.getClosedAt();

            comments = FetchSettings.issueComments ? DataExtractor.extractIssueComments(issue) : new ArrayList<>();
        } catch (Exception e) {
            isBug = false;
            closedAt = null;
            comments = null;
        }
    }

    public boolean isBug(GHIssue issue) {
        String bugPattern = "(?i)\\b(defect|error|bug|issue|mistake|incorrect|fault|flaw)\\b";
        String resolvedPattern = "(?i)\\b(fix|resolve)\\b";

        for (GHLabel label : issue.getLabels()) {
            String name = label.getName().toLowerCase();
            String desc = label.getDescription() != null ? label.getDescription().toLowerCase() : "";

            boolean nameHasBug = name.matches(".*" + bugPattern + ".*");
            boolean nameHasResolved = name.matches(".*" + resolvedPattern + ".*");
            boolean descHasBug = desc.matches(".*" + bugPattern + ".*");
            boolean descHasResolved = desc.matches(".*" + resolvedPattern + ".*");

            if ((nameHasBug && nameHasResolved) || (descHasBug && descHasResolved))
                return false;

            if (nameHasBug || descHasBug) return true;
        }

        return false;
    }


}
