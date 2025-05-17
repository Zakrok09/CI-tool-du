package org.example.extraction.ci;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.*;
import java.util.regex.Pattern;

public class CIContentParser {
    private final String content;

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

    private static final
    Map<String, Pattern> testPatterns = Map.of(
            "javascript", Pattern.compile("\\b(pnpm|npm|yarn)\\s+(run\\s+)?test\\b", Pattern.CASE_INSENSITIVE),
            "python", Pattern.compile("\\b(pytest|python\\s+-m\\s+unittest)\\b", Pattern.CASE_INSENSITIVE),
            "java", Pattern.compile("\\b(mvn|gradle|gradlew)\\s+test\\b", Pattern.CASE_INSENSITIVE)
    );

    private final Yaml yaml;

    public CIContentParser(String content) {
        this.content = content;
        LoaderOptions options = new LoaderOptions();
        options.setProcessComments(false);
        yaml = new Yaml(options);
    }

    public CIContentParser(String content, Yaml yaml) {
        this.content = content;
        this.yaml = yaml;
    }

    public Map<String, Integer> parseWorkflow() {
        String audited = content.replace("on:", "triggers:");
        Map<String, Object> data = yaml.load(audited);
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

    public boolean isExecutingTests() {
        Map<String, Object> data = yaml.load(content);

        Map<String, Object> jobs = (Map<String, Object>) data.get("jobs");

        for (Map.Entry<String, Object> jobEntry : jobs.entrySet()) {
            Map<String, Object> job = (Map<String, Object>) jobEntry.getValue();
            List<Map<String, Object>> steps = (List<Map<String, Object>>) job.get("steps");

            if (steps == null) continue;

            if (steps.stream().anyMatch(this::isStepRunningTests)) return true;
        }

        return false;
    }

    private boolean isStepRunningTests(Map<String, Object> step) {
        Object run = step.get("run");
        if (!(run instanceof String command)) return false;

        return testPatterns.entrySet()
                .stream().anyMatch(
                        pattern -> pattern.getValue().matcher(command).find()
                );
    }
}
