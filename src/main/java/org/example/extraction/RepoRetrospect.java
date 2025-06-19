package org.example.extraction;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.example.extraction.testcounter.JUnitTestCounter;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

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
    private final String currentBranch;
    private final String headCommitId;

    /**
     * Generic pair holder that will store information per commit
     * 
     * @param <T> the type of the data stored pairwise with the commit
     */
    public static class CommitPair<T> {
        public RevCommit commit;
        public T data;

        public CommitPair(RevCommit commit, T data) {
            this.commit = commit;
            this.data = data;
        }

        @Override
        public String toString() {
            return "For commit=" + commit.getName() + " made at: " + commit.getCommitTime() + "; found: " + data;
        }
    }

    private void checkout(RevCommit commit) throws GitAPIException {
        repoGit.checkout().setName(commit.getName()).call();
    }

    /**
     * Construct a repository retrospect-or.
     *
     * @param repoGit the JGit of the repository to be analysed
     */
    public RepoRetrospect(Git repoGit) throws IOException {
        this.repoGit = repoGit;
        this.currentBranch = repoGit.getRepository().getBranch();
        this.headCommitId = repoGit.getRepository().resolve("HEAD").getName();
    }

    public void restore() throws GitAPIException {
        repoGit.checkout().setName(currentBranch).call();
        repoGit.checkout().setName(headCommitId).call();
    }

    /**
     * Walk over the sampled commits
     */
    public List<CommitPair<String>> walkAndTestSampledCommits(List<RevCommit> sampled, String project) throws GitAPIException {
        List<CommitPair<String>> results = new ArrayList<>();
        for (RevCommit commit : sampled) {
            System.out.println("Walking " + commit.getName() + " " + commit.getId());
            try {
                checkout(commit);
                System.out.println("checked out");

                JUnitTestCounter jutc = new JUnitTestCounter();

                System.out.println("getting tests");
                String output_mvn_test = jutc.getTestOutputAtCommit(repoGit.getRepository().getDirectory());
                appendToFile(project, output_mvn_test);

                results.add(new CommitPair<>(commit, output_mvn_test));
            } catch (GitAPIException e) {
                restore();
                System.out.println("MOVING ERROR " + e);
            }
        }
        restore();
        return results;
    }

    private void appendToFile(String project, String output) {
        String file = "outputs/outputs-" + project.replace("/", "_") + ".txt";
        try (FileWriter fileWriter = new FileWriter(file, true)) {
            System.out.println(output);
            fileWriter.write(output);
        } catch (IOException e) {
            System.out.println("FAILED READING!!!");
            System.out.println("OUTPUT FOR PROJECT: " + project + "\n\n\n\n\n\n\n\n");
            System.out.println(output);
            System.out.println("\n\n\n\n\n\n\n\n\n\n");
        }
    }

    // TODO: Change this when collection algorithm is generic.
    /**
     * Walk over the sampled commits and calculate code comment percentage
     */
    public List<CommitPair<Double>> commentPecentageWalk(List<RevCommit> sampled) throws GitAPIException {
        List<CommitPair<Double>> results = new ArrayList<>();
        for (RevCommit commit : sampled) {
            try {
                checkout(commit);

                ProcessBuilder pb = new ProcessBuilder("cloc", "--quiet", "--exclude-dir=doc,docs,test,tests",
                        "--exclude-lang=JSON,Markdown,Text", repoGit.getRepository().getWorkTree().getAbsolutePath());
                Process process = pb.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;

                pb.redirectErrorStream(true);

                double codeCommentPercentage = 0.0;

                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 5 && parts[0].trim().equalsIgnoreCase("SUM:")) {
                        int commentLines = Integer.parseInt(parts[3].trim());
                        int codeLines = Integer.parseInt(parts[4].trim());
                        if (codeLines > 0) {
                            codeCommentPercentage = (double) commentLines / codeLines;
                        }
                        break;
                    }
                }

                reader.close();
                process.waitFor();

                results.add(new CommitPair<>(commit, codeCommentPercentage));
            } catch (GitAPIException e) {
                logger.error("Git API exception while processing commit {}: {}", commit.getName(), e.getMessage());
            } catch (IOException | InterruptedException e) {
                restore();
                e.printStackTrace();
                throw new RuntimeException(e.getMessage());
            }
        }
        restore();
        return results;
    }
}
