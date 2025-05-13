package org.example.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

public class Helper {
    // Files to check for documentation data
    public static final Map<String, Integer> FILES_TO_CHECK = Map.of(
            "README.md", 0,
            "CONTRIBUTING.md", 1,
            "INSTALL.md", 2
    );

    public static int countLines(String input) throws IOException {
        if (input == null || input.isEmpty()) {
            return 0;
        }

        int lines = 0;
        try (BufferedReader reader = new BufferedReader(new StringReader(input))) {
            while (reader.readLine() != null) {
                lines++;
            }
        }
        return lines;
    }
}
