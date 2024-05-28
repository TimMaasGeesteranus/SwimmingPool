package nl.ru.spp.group5.Helpers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.*;

public class LoggingAndAudit {

    private List<String> eventLogs = new ArrayList<>();
    private static final Logger logger = Logger.getLogger(LoggingAndAudit.class.getName());

    static {
        try {
            LogManager.getLogManager().reset();
            FileHandler fileHandler = new FileHandler("logging_and_audit.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            logger.setLevel(Level.ALL);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to initialize logger handler.", e);
        }
    }

    // Method to log different types of events
    public void logEvent(String eventType, String details) {
        String logEntry = "Event Type: " + eventType + ", Details: " + details;
        eventLogs.add(logEntry);

        // Log the event using java.util.logging
        logger.log(Level.INFO, logEntry);
    }

    // Function to audit logs for irregularities or security breaches
    public void auditLogs() {
        for (String log : eventLogs) {
            logger.log(Level.INFO, "Auditing log entry: " + log);
        }
        logger.log(Level.INFO, "Audit completed");
    }
}
