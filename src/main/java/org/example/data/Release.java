package org.example.data;

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

    public Release() {}

    public Release(GHRelease release, GHRelease prevRelease) throws IOException {
        super(release);

        publishedAt = release.getPublishedAt();
        tagName = release.getTagName();
        changesFromPrevRelease = getNumChangesFromPrevRelease(prevRelease);
    }

    private int getNumChangesFromPrevRelease(GHRelease prev) throws IOException {
        if(prev == null) {
            return 0;
        }

        GHCompare compare = prev.getOwner().getCompare(prev.getTagName(), tagName);

        return Arrays.stream(compare.getFiles())
                .mapToInt(GHCommit.File::getLinesChanged)
                .sum();
    }
}
