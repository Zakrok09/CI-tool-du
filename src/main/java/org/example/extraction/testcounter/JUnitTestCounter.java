package org.example.extraction.testcounter;

import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.singleton;
import static org.example.Main.logger;
import static org.junit.platform.engine.discovery.ClassNameFilter.includeClassNamePatterns;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClasspathRoots;

public class JUnitTestCounter implements TestCounter {

    public int countUnitTestsAtCommit(File repoRoot, RevCommit commit) {
        File junitJar = new File("junit-platform-console-standalone-1.12.1.jar");
        File testDir = new File(repoRoot, "target/test-classes/com/purbon/kafka/topology");

        ProcessBuilder builder = new ProcessBuilder(
                "java", "-jar", junitJar.getAbsolutePath(),
                "discover",
                "--scan-classpath",
                "--class-path", testDir.getAbsolutePath()
        );

        builder.redirectErrorStream(true);

        try {
//            compileJavaProject(repoRoot);

            Process process = builder.start();
            process.waitFor();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                return reader.lines()
                        .filter(line -> line.contains("tests found"))
                        .mapToInt(line -> Integer.parseInt(line.replaceAll("\\D", "")))
                        .findFirst()
                        .orElse(0);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }


//    @Override
//    public int countUnitTestsAtCommit(File repoRoot, RevCommit commit) {
//        logger.debug("Sampling JUnit session");
//
//        try {

    /// /            compileJavaProject(repoRoot);
//            File classesDir = new File("/home/eccyboo/IdeaProjects/CItoLDU/clones/TEAMMATES_teammates/build/classes/java/test");
//
//            if (!classesDir.exists()) {
//                logger.error("Test classes directory does not exist: {}", classesDir.getAbsolutePath());
//                return 0;
//            }
//
//
//            logger.debug("Test classes directory exists with {} files",
//                    classesDir.listFiles() != null ? classesDir.listFiles().length : 0);
//
//            try (LauncherSession session = LauncherFactory.openSession()) {
//
//                logger.debug("Building discovery request");
//                LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
//                        .selectors(selectClasspathRoots(singleton(Path.of("/home/eccyboo/IdeaProjects/CItoLDU/clones/TEAMMATES_teammates/build/classes/java/test"))))
//                        .configurationParameter("")
//                        .build();
//
//                logger.debug("Sending discovery request");
//                TestPlan testPlan = session.getLauncher().discover(request);
//                logger.debug("Discovery request sent!");
//
//                AtomicInteger testCount = new AtomicInteger(0);
//                testPlan.getRoots().forEach(root -> {
//                    logger.debug("Found root: {}", root.getDisplayName());
//                    testPlan.getChildren(root).forEach(child -> {
//                        logger.debug("  Child: {} (isTest={})", child.getDisplayName(), child.isTest());
//                        if (child.isTest()) {
//                            testCount.incrementAndGet();
//                        }
//                    });
//                });
//
//                int methodCount = (int) testPlan.countTestIdentifiers(testIdentifier -> {
//                    boolean isTest = testIdentifier.isTest() && testIdentifier.getSource().isPresent();
//                    if (isTest) {
//                        logger.debug("Found test: {}", testIdentifier.getDisplayName());
//                    }
//                    return isTest;
//                });
//
//                logger.debug("Found {} test classes and {} test methods", testCount.get(), methodCount);
//                return methodCount;
//            }
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//
//    }
    private void compileJavaProject(File repoRoot) throws IOException, InterruptedException {
        ProcessBuilder builder = getCompileProcessBuilder(repoRoot);


        builder.directory(repoRoot);
        builder.inheritIO();
        Process process = builder.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) throw new RuntimeException("Failed to build: " + exitCode);
    }

    private static ProcessBuilder getCompileProcessBuilder(File repoRoot) {
        File gradle = new File(repoRoot, "build.gradle");
        File gradleKts = new File(repoRoot, "build.gradle.kts");
        File maven = new File(repoRoot, "pom.xml");

        ProcessBuilder builder;
        if (gradle.exists() || gradleKts.exists()) {
            builder = new ProcessBuilder("./gradlew", "clean", "testClasses");
        } else if (maven.exists()) {
            builder = new ProcessBuilder("mvn", "clean", "test-compile");
        } else {
            throw new IllegalStateException("Unknown build system: no Gradle or Maven file found");
        }
        return builder;
    }
}
