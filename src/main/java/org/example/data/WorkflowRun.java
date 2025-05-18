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

    public WorkflowRun() {}

    public static WorkflowRun fromCSV(String csvLine) {
        String[] parts = csvLine.split(";");
        WorkflowRun run = new WorkflowRun();
        run.id = Long.parseLong(parts[0]);
        run.createdAt = Instant.ofEpochSecond((long) Double.parseDouble(parts[1]));
        run.updatedAt = Instant.ofEpochSecond((long) Double.parseDouble(parts[2]));
        run.start_time = Instant.ofEpochSecond((long) Double.parseDouble(parts[4]));
        run.name = parts[3];
        run.triggererId = Long.parseLong(parts[5]);
        run.commit_id = parts[6];
        run.status = GHWorkflowRun.Status.valueOf(parts[7]);
        return run;
    }

    public String toCSV() {
        return String.format("%d;%s;%s;%s;%s;%d;%s;%s\n",
                id,
                createdAt.toString(),
                updatedAt.toString(),
                name,
                start_time.toString(),
                triggererId,
                commit_id,
                status.toString());
    }
}
