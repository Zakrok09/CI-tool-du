package org.example.data;

import org.kohsuke.github.GHTag;

import java.io.IOException;
import java.io.Serializable;

public class Tag implements Serializable {

    public Commit commit;
    public String name;

    public Tag() {}

    public Tag(GHTag tag) throws IOException {
        commit = new Commit(tag.getCommit());
        name = tag.getName();
    }
}
