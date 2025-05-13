package org.example.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

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
        }
        return lines;
    }
}
