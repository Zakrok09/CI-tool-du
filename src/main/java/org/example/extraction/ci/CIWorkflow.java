package org.example.extraction.ci;

import org.kohsuke.github.GHWorkflow;

import java.io.IOException;
import java.util.List;

public class CIWorkflow {
    private final String name;
    private final GHWorkflow workflow;
    private final String file_content;

    /* todo: make this use an enum */
    private final List<String> triggers;

    public CIWorkflow(GHWorkflow workflow, String file_content, List<String> triggers) {
        name = workflow.getName();
        this.workflow = workflow;
        this.file_content = file_content;
        this.triggers = triggers;
    }

    public String toCSV() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(";").append(workflow.getId()).append(";").append(workflow.getPath()).append(";");
        for (String trigger : triggers) sb.append(trigger).append(",");
        return sb.toString();
    }

    public String getName() {
        return name;
    }

    public String getFileContent() {
        return file_content;
    }

    public GHWorkflow getWorkflow() {
        return workflow;
    }

    public List<String> getTriggers() {
        return triggers;
    }
}
