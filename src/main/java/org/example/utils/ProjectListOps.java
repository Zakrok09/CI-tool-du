package org.example.utils;

import java.nio.file.Files;
import java.util.List;

public class ProjectListOps {

    public static List<String> getProjectListFromFile(String filename, int skipLines) {
        try {;
            return Files.readAllLines(java.nio.file.Paths.get(filename))
                    .stream()
                    .skip(skipLines)
                    .collect(java.util.stream.Collectors.toList());
        } catch (java.io.IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            return List.of();
        }
    }

    public static List<String> getProjectListFromFile(String filename) {
        return getProjectListFromFile(filename, 0);
    }

    public static void saveProjectListToFile(List<String> projects, String filename) {
        try (java.io.BufferedWriter writer = Files.newBufferedWriter(java.nio.file.Paths.get(filename))) {
            for (String project : projects) {
                writer.write(project);
                writer.newLine();
            }
        } catch (java.io.IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }

    public static void printAll(List<String> projects) {
        for (String project : projects) {
            System.out.println(project);
        }
    }

    public static void setMinus(List<String> list1, List<String> list2) {
        list1.removeAll(list2);
    }

}
