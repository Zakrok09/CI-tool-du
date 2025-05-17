package org.example.extraction.ci;

import org.kohsuke.github.GHWorkflow;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

public class CIWorkflow {
    private final String name;
    private final String file_name;
    private final String file_content;
    private final Instant created_at;
    private final Instant updated_at;

    /* todo: make this use an enum */
    private final List<String> triggers;

    public CIWorkflow(GHWorkflow workflow, String file_content, List<String> triggers) throws IOException {
        name = workflow.getName();
        this.file_name = workflow.getPath();
        this.file_content = file_content;
        created_at = workflow.getCreatedAt();
        updated_at = workflow.getUpdatedAt();
        this.triggers = triggers;
    }

    public String getName() {
        return name;
    }

    public String getFilename() {
        return file_name;
    }

    public String getFileContent() {
        return file_content;
    }

    public Instant getCreatedAt() {
        return created_at;
    }

    public Instant getUpdatedAt() {
        return updated_at;
    }

    public List<String> getTriggers() {
        return triggers
                ;
    }
}
