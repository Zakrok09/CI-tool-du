package org.example.data;

import java.io.Serializable;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class DocumentationStats implements Serializable {
    public DocumentationFile[] documentationFiles;

    @JsonIgnore
    public static final String[] DOC_FILE_LIST = {
            "README",
            "CODE_OF_CONDUCT",
            "CONTRIBUTING",
            "LICENSE",
            "SECURITY"
    };
    @JsonIgnore
    public static final Map<String, Integer> DOC_FILE_MAP = Map.of(
            "README.md", 0,
            "README", 0,
            "CODE_OF_CONDUCT.md", 1,
            "CODE_OF_CONDUCT", 1,
            "CONTRIBUTING.md", 2,
            "CONTRIBUTING", 2,
            "LICENSE.md", 3,
            "LICENSE", 3,
            "SECURITY.md", 4,
            "SECURITY", 4
    );

    public static class DocumentationFile implements Serializable {
        public String name;
        public int size;
        public boolean exists;
        public int additions;
        public int deletions;

        public DocumentationFile() {}

        public DocumentationFile(String name) {
            this.name = name;
            this.size = 0;
            this.exists = false;
            this.additions = 0;
            this.deletions = 0;
        }
    }

    public DocumentationStats() {
        documentationFiles = new DocumentationFile[DOC_FILE_LIST.length + 2];
        for (String fileName : DOC_FILE_LIST) {
            int ind = DOC_FILE_MAP.get(fileName);
            documentationFiles[ind] = new DocumentationFile(fileName);
        }

        documentationFiles[DOC_FILE_LIST.length] = new DocumentationFile("issue templates");
        documentationFiles[DOC_FILE_LIST.length + 1] = new DocumentationFile("pull request templates");
    }
}
