package org.example.extraction.ci;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CIContentParser {

    /**
     * @link <a href="https://docs.github.com/en/actions/writing-workflows/choosing-when-your-workflow-runs/events-that-trigger-workflows">Workflow triggers from GitHub</a>
     */
    private static final Set<String> KNOWN_EVENTS = Set.of(
            "branch_protection_rule", "check_run", "check_suite", "create", "delete",
            "deployment", "deployment_status", "discussion", "discussion_comment", "fork",
            "gollum", "issue_comment", "issues", "label", "merge_group", "milestone",
            "page_build", "public", "pull_request", "pull_request_review",
            "pull_request_review_comment", "pull_request_target", "push", "registry_package",
            "release", "repository_dispatch", "schedule", "status", "watch", "workflow_call",
            "workflow_dispatch", "workflow_run"
    );

    private final Yaml yaml;

    public CIContentParser() {
        LoaderOptions options = new LoaderOptions();
        options.setProcessComments(false);
        yaml = new Yaml(options);
    }

    public CIContentParser(Yaml yaml) {
        this.yaml = yaml;
    }

    public Map<String, Integer> parseWorkflow(String content) {
        content = content.replace("on:", "triggers:");
        Map<String, Object> data = yaml.load(content);
        Map<String, Integer> res = new HashMap<>();
        for (String event : KNOWN_EVENTS) res.put(event, 0);

        Object onSection = data.get("triggers");
        switch (onSection) {
            case String s -> {
                s = s.trim();
                if (KNOWN_EVENTS.contains(s)) {
                    res.put(s, res.get(s) + 1);
                }
            }
            case List<?> list -> {
                for (Object event : list) {
                    if (event instanceof String s && KNOWN_EVENTS.contains(s)) {
                        res.put(s, res.get(s) + 1);
                    }
                }
            }
            case Map<?, ?> map -> {
                for (Object key : map.keySet()) {
                    if (key instanceof String s) {
                        s = s.trim();
                        if (KNOWN_EVENTS.contains(s)) {
                            res.put(s, res.get(s) + 1);
                        }
                    }
                }
            }
            case null, default -> System.out.println("hui");
        }

        return res;
    }

}
