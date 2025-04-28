package org.example.data;

import org.kohsuke.github.GHObject;

import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;

public class GitHubObject implements Serializable {
    public long id;
    public Instant createdAt;
    public Instant updatedAt;

    public GitHubObject() {}

    public GitHubObject(GHObject o) throws IOException {
        id = o.getId();
        createdAt = o.getCreatedAt();
        updatedAt = o.getUpdatedAt();
    }
}
