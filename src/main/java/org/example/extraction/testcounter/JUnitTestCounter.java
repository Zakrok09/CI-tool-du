package org.example.extraction.testcounter;

import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.io.File;

import static java.util.Collections.singleton;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClasspathRoots;

public class JUnitTestCounter implements TestCounter {

    @Override
    public int countUnitTestsAtCommit(File repoRoot, RevCommit commit) {
        try (LauncherSession session = LauncherFactory.openSession()) {
            Launcher launcher = session.getLauncher();

            LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClasspathRoots(singleton(repoRoot.toPath())))
                .build();

            TestPlan testPlan = launcher.discover(request);

            return (int) testPlan.countTestIdentifiers(testIdentifier ->
                testIdentifier.isTest() && testIdentifier.getSource().isPresent()
            );
        }
    }
}
