package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.scripts.Daniel;
import org.example.scripts.Serban;

import java.io.IOException;

public class Main {
    public static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
        logger.info("Starting script");
        // Daniel.danielLoad("additional_preliminary");
    }
}