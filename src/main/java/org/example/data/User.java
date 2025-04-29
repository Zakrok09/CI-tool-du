package org.example.data;

import org.kohsuke.github.GHUser;

import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;

public class User implements Serializable {

    public String name;
    public Instant accountCreatedAt;

    public User() {}

    public User(GHUser user) throws IOException {
        accountCreatedAt = user.getCreatedAt();
    }
}
