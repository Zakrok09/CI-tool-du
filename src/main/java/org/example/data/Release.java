package org.example.data;

import org.kohsuke.github.GHRelease;

import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;

public class Release extends GitHubObject implements Serializable {

    public Instant publishedAt;
    public String tagName;

    public Release() {}

    public Release(GHRelease release) throws IOException {
        super(release);

        publishedAt = release.getPublishedAt();
        tagName = release.getTagName();
    }
}
