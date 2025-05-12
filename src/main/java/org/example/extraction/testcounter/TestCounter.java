package org.example.extraction.testcounter;

import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;

public interface TestCounter {
    int countUnitTestsAtCommit(File repoRoot, RevCommit commit);
}
