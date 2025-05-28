package org.example.data;

import java.io.Serializable;

import org.example.utils.Helper;

public class DocumentationStats implements Serializable {
    public DocumentationFile[] documentationFiles;

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
        documentationFiles = new DocumentationFile[Helper.FILES_TO_CHECK.size() + 2];
        for (String fileName : Helper.FILES_TO_CHECK.keySet()) {
            int ind = Helper.FILES_TO_CHECK.get(fileName);
            documentationFiles[ind] = new DocumentationFile(fileName);
        }

        documentationFiles[Helper.FILES_TO_CHECK.size()] = new DocumentationFile("issue templates");
        documentationFiles[Helper.FILES_TO_CHECK.size() + 1] = new DocumentationFile("pull request templates");
    }
}
