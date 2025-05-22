package org.example.extraction.ci;

import org.kohsuke.github.GHWorkflow;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.*;
import java.util.regex.Pattern;

public class CIContentParser {
    private final String content;



    /**
     * Regex patterns to identify test execution commands.
     * Collected from various CI/CD systems.
     * To be used in a union with the step name as it could bypass custom runs.
     */
    private static final
    Map<String, Pattern> testPatterns = Map.of(
            "javascript", Pattern.compile("\\b(pnpm|npm|yarn)\\s+(run\\s+)?test\\b", Pattern.CASE_INSENSITIVE),
            "python", Pattern.compile("\\b(pytest|python\\s+-m\\s+unittest|tox|uv\\s+run.*tox)\\b", Pattern.CASE_INSENSITIVE),
            "java", Pattern.compile("\\b(mvnw?|gradle|gradlew)(\\s+[\\w\\-:.=$/{}'!]+)*\\s+(test|verify)\\b", Pattern.CASE_INSENSITIVE)
    );

    /**
     * Regex pattern to identify test execution steps.
     * To be used in a union with the command as it could bypass step names
     * that do not suggest test execution.
     */
    private static final Pattern testNamePattern =
            Pattern.compile("\\b(test|tests|spec|unit|integration)\\b", Pattern.CASE_INSENSITIVE);

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

    /**
     * Produces a CIWorkflow object by parsing the YAML file content.
     * This will provide the triggers and if the workflow is executing tests.
     * @param workflow a GHWorkflow object, to be packed into the CIWorkflow.
     * @return a CIWorkflow object containing the workflow name, content, triggers, and test execution status.
     */
    public CIWorkflow produceCIWorkflow(GHWorkflow workflow) {
        Map<String, Integer> triggers = parseAndIdentifyTriggers();
        List<String> triggersList = filterUsedTriggers(triggers);

        return new CIWorkflow(workflow, content, triggersList, isExecutingTests());
    }

    /**
     * Filters triggers that are used in the workflow at least once.
     * @param triggers a map of trigger names and their occurrence counts.
     * @return a list of the used triggers in the workflow.
     */
    private List<String> filterUsedTriggers(Map<String, Integer> triggers) {
        return triggers.entrySet()
                .stream().filter(e -> e.getValue() > 0)
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Parses the workflow file content and identifies the workflow triggers.
     * @return a map of trigger names and their occurrence counts.
     */
    private Map<String, Integer> parseAndIdentifyTriggers() {
        String audited = content.replace("on:", "triggers:");
        Map<String, Object> data = yaml.load(audited);
        Map<String, Integer> res = new HashMap<>();
        for (KnownEvent event : KnownEvent.values()) {
            res.put(event.name().toLowerCase(), 0);
        }

        Object onSection = data.get("triggers");
        switch (onSection) {
            case String s -> {
                s = s.trim();
                if (isKnownEvent(s)) {
                    res.put(s, res.get(s) + 1);
                }
            }
            case List<?> list -> {
                for (Object event : list) {
                    if (event instanceof String s && isKnownEvent(s)) {
                        res.put(s, res.get(s) + 1);
                    }
                }
            }
            case Map<?, ?> map -> {
                for (Object key : map.keySet()) {
                    if (key instanceof String s) {
                        s = s.trim();
                        if (isKnownEvent(s)) {
                            res.put(s, res.get(s) + 1);
                        }
                    }
                }
            }
            case null, default -> System.out.println("hui");
        }

        return res;
    }

    private boolean isKnownEvent(String s) {
        try {
            KnownEvent.valueOf(s.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Checks if any step within the jobs of the workflow is executing tests.
     * @return true the moment it spots such a step.
     */
    private boolean isExecutingTests() {
        Map<String, Object> data = yaml.load(content);

        Map<String, Object> jobs = (Map<String, Object>) data.get("jobs");

        for (Map.Entry<String, Object> jobEntry : jobs.entrySet()) {
            Map<String, Object> job = (Map<String, Object>) jobEntry.getValue();
            List<Map<String, Object>> steps = (List<Map<String, Object>>) job.get("steps");

            if (steps == null) continue;

            if (steps.stream().anyMatch(this::isStepConsideredTesting)) return true;
        }

        return false;
    }

    /**
     * Checks if the step is considered a test run either by name or executed command.
     * @param step the step within the CI workflow YAML file.
     * @return whether we can consider this step a test run.
     */
    private boolean isStepConsideredTesting(Map<String, Object> step) {
        return isStepCalledTests(step) || isStepRunningTests(step);
    }

    /**
     * Checks if the step has a name suggesting a test suite run.
     * @param step the step within the CI workflow YAML file.
     * @return true the moment it spots a name that looks like it triggers a test suite run.
     */
    private boolean isStepCalledTests(Map<String, Object> step) {
        Object name = step.get("name");
        if (!(name instanceof String nameStr)) return false;

        return testNamePattern.matcher(nameStr).find();
    }

    /**
     * Checks if the step runs a command resembling a test run command.
     * @param step the step within the CI workflow YAML file.
     * @return true the moment it spots a command that looks like a test run command.
     */
    private boolean isStepRunningTests(Map<String, Object> step) {
        Object run = step.get("run");
        if (!(run instanceof String command)) return false;

        return testPatterns.entrySet()
                .stream().anyMatch(
                        pattern -> pattern.getValue().matcher(command).find()
                );
    }
}
