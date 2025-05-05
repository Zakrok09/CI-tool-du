package org.example.extraction;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.example.data.Repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.example.Main.logger;

/**
 * A (soon to be) data agnostic class that collects data per commit
 * by specifying the algorithm that shall be executed on collection.
 * Should have a generic T type that will be the type of data to be
 * collected per commit.
 * Stores all logic related to walking through the commits and nothing
 * else.
 * Current implementation does not uphold that, will be fixed later.
 */
public class RepoRetrospect {
    private final Git repoGit;

    /**
     * Generic pair holder that will store information per commit
     * @param <T> the type of the data stored pairwise with the commit
     */
    public static class CommitPair<T> {
        RevCommit commit;
        T data;

        public CommitPair(RevCommit commit, T data) {
            this.commit = commit;
            this.data = data;
        }
    }

    private RevWalk beginAtHead() throws IOException {
        RevWalk walk = new RevWalk(repoGit.getRepository());
        ObjectId head = repoGit.getRepository().resolve("refs/heads/main");
        walk.markStart(walk.parseCommit(head));
        return walk;
    }

    /**
     * Construct a repository retrospect-or.
     * @param repository the repository to be analysed
     * @throws IOException can throw when acquiring git due to File reading
     */
    public RepoRetrospect(Repository repository) throws IOException {
        this.repoGit = repository.getGit();
    }

    /**
     * Walk through the files of a commit
     * todo: SHOULD make this data and strategy agnostic
     * @param revCommit the commit to be walked
     * @return a commit pair with the data for this commit
     */
    public CommitPair<Integer> walkCommit(RevCommit revCommit) {
        int totalTests = 0;
        try (TreeWalk treeWalk = new TreeWalk(repoGit.getRepository())) {
            treeWalk.addTree(revCommit.getTree());
            treeWalk.setRecursive(true);

            while (treeWalk.next()) {
                String path = treeWalk.getPathString();
                if (path.contains("src/test")) {
                    String content = readBlob(repoGit, treeWalk.getObjectId(0));
                    totalTests += countTests(content);
                }
            }
            return new CommitPair<>(revCommit, totalTests);
        } catch (Exception e) {
            logger.fatal("Tree walk of commit {} : {}", revCommit.getName(), e.getLocalizedMessage());
            throw new RuntimeException("");
        }
    }

    /**
     * Walk through all the commits
     * TODO: introduce limit
     * @return a list of commit pairs
     * @throws IOException may throw due to File reading
     */
    public List<CommitPair<Integer>> walkAllCommits() throws IOException {
        List<CommitPair<Integer>> results = new ArrayList<>();
        try (RevWalk walk = beginAtHead()) {
            walk.forEach(rc -> results.add(walkCommit(rc)));
        }
        return results;
    }

    /**
     * Read a blob from a tree
     * @deprecated shall be reworked into own class
     */
    public static String readBlob(Git repoGit, ObjectId blobId) throws IOException {
        try (ObjectReader reader = repoGit.getRepository().newObjectReader()) {
            ObjectLoader loader = reader.open(blobId);
            byte[] bytes = loader.getBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    // TODO: use the JUnit engine to count the number of tests
    // TODO: separate unit from integration tests
    int countTests(String code) {
        Pattern pattern = Pattern.compile("@Test");
        Matcher matcher = pattern.matcher(code);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }


}
