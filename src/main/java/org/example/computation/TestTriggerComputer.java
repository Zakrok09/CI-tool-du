package org.example.computation;

import org.example.data.WorkflowRun;
import org.example.extraction.ci.KnownEvent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.example.Main.logger;

public class TestTriggerComputer {


    /**
     * Calculates the frequency of trigger events seen in collected workflows.
     * @return a KnownEvent indexed array of integers, where the index is the event's enum value
     */
    public int[] frequencyOfTriggers() {
        int[] frequencies = new int[KnownEvent.values().length];

        Path dir = Path.of("sampled_workflows");
        if (!dir.toFile().exists()) throw new RuntimeException("sampled_workflows folder does not exist!");

        for (File file : Objects.requireNonNull(dir.toFile().listFiles())) {
            if (file.getName().endsWith(".csv")) {
                logger.debug("Parsing file: {}", file.getName());

                try (var lines = Files.lines(file.toPath())) {
                    lines.skip(1)
                            .map(this::extractTriggersFromLine)
                            .flatMap(triggers -> triggers.stream())
                            .forEach(trigger -> {
                                KnownEvent event = KnownEvent.valueOf(trigger.toUpperCase());
                                frequencies[event.ordinal()]++;
                            });
                } catch (IOException e) {
                    logger.error("Error reading file: {}, skipping...", file.getName(), e);
                }
            }
        }

        // Print the frequencies for debugging
        for (int i = 0; i < frequencies.length; i++) {
            logger.debug("Frequency of {}: {}", KnownEvent.values()[i], frequencies[i]);
        }

        return frequencies;
    }

    public int countWorkflows() {
        Path dir = Path.of("sampled_workflows");
        if (!dir.toFile().exists()) throw new RuntimeException("sampled_workflows folder does not exist!");
        int count = 0;

        for (File file : Objects.requireNonNull(dir.toFile().listFiles())) {
            if (file.getName().endsWith(".csv")) {
                logger.debug("Parsing file: {}", file.getName());

                try (var lines = Files.lines(file.toPath())) {
                    count += lines.skip(1).filter(trigger -> !trigger.trim().isEmpty()).count();
                } catch (IOException e) {
                    logger.error("Error reading file: {}, skipping...", file.getName(), e);
                }
            }
        }
        return count;
    }

    private List<String> extractTriggersFromLine(String line) {
        String[] parts = line.split(";");
        return List.of(parts[3].split(","));
    }

}
