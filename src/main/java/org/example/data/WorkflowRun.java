package org.example.data;

import org.kohsuke.github.GHWorkflowRun;

import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;

public class WorkflowRun extends GitHubObject implements Serializable {
    public String name;
    public Instant start_time;
    public long id;
    public long triggererId;
    public String commit_id;
    public GHWorkflowRun.Status status;

    public WorkflowRun(GHWorkflowRun run) throws IOException {
        super(run);

        name = run.getName();
        id = run.getId();
        start_time = run.getRunStartedAt();
        triggererId = run.getTriggeringActor().getId();
        status = run.getStatus();
        commit_id = run.getHeadCommit().getId();
    }
}
