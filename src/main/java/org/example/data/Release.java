package org.example.data;

import org.example.utils.Helper;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCompare;
import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GHRepository;

import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;

public class Release extends GitHubObject implements Serializable {

    public Instant publishedAt;
    public String tagName;
    public int changesFromPrevRelease;
    public int documentationSize;

    public Release() {}

    public Release(GHRelease release, String prevTag) throws IOException {
        super(release);
        try {
            publishedAt = release.getPublishedAt();
            tagName = release.getTagName();
            changesFromPrevRelease = getNumChangesFromPrevRelease(release.getOwner(), prevTag);
            documentationSize = Helper.countLines(release.getBody());
        } catch (Exception e) {
            publishedAt = null;
            tagName = null;
            changesFromPrevRelease = 0;
        }
    }

    private int getNumChangesFromPrevRelease(GHRepository repo, String prevTag) throws IOException {
        GHCompare compare = repo.getCompare(prevTag, tagName);

        return Arrays.stream(compare.getFiles())
                .mapToInt(GHCommit.File::getLinesChanged)
                .sum();
    }
}
