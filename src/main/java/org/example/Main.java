package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.fetching.FetchSettings;
import org.example.scripts.Daniel;
import org.example.scripts.Serban;

import java.io.IOException;

public class Main {
    public static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
        logger.info("Starting script");
        FetchSettings.Daniel();
        Daniel.danielDocumentationStats("final_for_repo_data_1_docs-0-75", 30, 6);
    }
}