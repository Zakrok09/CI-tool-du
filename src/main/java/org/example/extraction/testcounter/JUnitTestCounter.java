package org.example.extraction.testcounter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class JUnitTestCounter {

    public String getTestOutputAtCommit(File repoRoot) {
        // make it go to commit

        ProcessBuilder builder = new ProcessBuilder();
        builder.command("bash", "-c", "cd " + repoRoot.getParent().replace("/.git", "") + " && mvn clean test -Dmaven.test.failure.ignore=true");

        builder.redirectErrorStream(true);

        try {
            Process process = builder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                    output.append(line).append("\n");
                }
            }

            process.waitFor();
            output.append("END OF ROUND");
            return output.toString();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveOutputToFile(String output) {

    }
}
