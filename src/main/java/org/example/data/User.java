package org.example.data;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;

import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public class User implements Serializable {

    public String name;
    public Instant accountCreatedAt;

    public User() {}

    public User(GHUser user) throws IOException {
        name = user.getLogin();
        accountCreatedAt = user.getCreatedAt();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(name, user.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
