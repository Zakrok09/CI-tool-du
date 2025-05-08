package org.example.data;

import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;

import org.example.extraction.DataExtractor;
import org.kohsuke.github.GHDeployment;

public class Deployment extends GitHubObject implements Serializable {
    // GitHub API: https://docs.github.com/en/rest/deployments/deployments?apiVersion=2022-11-28
    public String finalState; // Possible values: error, failure, pending, in_progress, queued, or success
    public Instant completedAt;

    public Deployment() {}

    public Deployment(GHDeployment d) throws IOException {
        super(d);

        Object[] data = DataExtractor.extractDeploymentData(d);
        finalState = (String) data[0];
        completedAt = data[1] != null ? (Instant) data[1] : null;
    }
}
