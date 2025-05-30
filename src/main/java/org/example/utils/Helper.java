package org.example.utils;

import static org.example.Main.logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
}
