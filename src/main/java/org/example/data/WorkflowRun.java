package org.example.data;

import org.example.extraction.ci.KnownEvent;
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
    public String event;

    public WorkflowRun(GHWorkflowRun run) throws IOException {
        super(run);

        try {
            name = run.getName();
            id = run.getId();
            start_time = run.getRunStartedAt();
            triggererId = run.getTriggeringActor().getId();
            status = run.getStatus();
            commit_id = run.getHeadCommit().getId();
            event = run.getEvent().name();
        } catch (Exception e) {
            name = null;
            start_time = null;
            triggererId = -1;
            id = -1;
            status = null;
            commit_id = null;
            event = null;
        }
    }

    public WorkflowRun() {}

    public static WorkflowRun fromCSV(String csvLine) {
        String[] parts = csvLine.split(";");
        WorkflowRun run = new WorkflowRun();
        run.id = Long.parseLong(parts[0]);
        run.createdAt = Instant.parse(parts[1]);
        run.updatedAt = Instant.parse(parts[2]);
        run.start_time = Instant.parse(parts[4]);
        run.name = parts[3];
        run.triggererId = Long.parseLong(parts[5]);
        run.commit_id = parts[6];
        run.status = GHWorkflowRun.Status.valueOf(parts[7].toUpperCase());
        run.event = parts[8];
        return run;
    }

    public String toCSV() {
        return String.format("%d;%s;%s;%s;%s;%d;%s;%s;%s\n",
                id,
                createdAt.toString(),
                updatedAt.toString(),
                name,
                start_time.toString(),
                triggererId,
                commit_id,
                status.toString(),
                event);
    }
}
