package org.example.data;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;

import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;

public class User implements Serializable {

    public String name;
    public Instant accountCreatedAt;
    public boolean isCollaborator;

    public User() {}

    public User(GHUser user, GHRepository repo) throws IOException {
        name = user.getName();
        accountCreatedAt = user.getCreatedAt();
        isCollaborator = repo.isCollaborator(user);
    }
}
