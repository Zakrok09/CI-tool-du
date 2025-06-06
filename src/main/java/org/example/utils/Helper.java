package org.example.utils;

import static org.example.Main.logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Helper {
    public static int countLines(String input) throws IOException {
        if (input == null || input.isEmpty()) {
            return 0;
        }

        int lines = 0;
        try (BufferedReader reader = new BufferedReader(new StringReader(input))) {
            while (reader.readLine() != null) {
                lines++;
            }
        } catch (IOException e) {
            logger.info("[Helper] Error reading input string: " + e.getMessage());
            throw new RuntimeException(e);
        }

        return lines;
    }

    public static int countLines(InputStream input) throws IOException {
        if (input == null) {
            return 0;
        }

        int lines = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
            while (reader.readLine() != null) {
                lines++;
            }
        } catch (IOException e) {
            logger.info("[Helper] Error reading input string: " + e.getMessage());
            throw new RuntimeException(e);
        }

        return lines;
    }

    public static List<String> getFileLinesSafe(String filePath) {
        return getFileLinesSafe(Paths.get(filePath));
    }

    public static List<String> getFileLinesSafe(Path filePath) {
        try {
            return java.nio.file.Files.readAllLines(filePath);
        } catch (IOException e) {
            logger.error("[Helper] Error reading file: " + filePath, e);
            throw new RuntimeException("Error reading file: " + filePath, e);
        }
    }

}
