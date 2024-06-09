package nl.ru.spp.group5.Helpers;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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

    public void logEvent(String eventType, String cardId, String message, String signature) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String logEntry = "Timestamp: " + timestamp + ", Event Type: " + eventType + ", Card ID: " + cardId + ", Message: " + message + ", Signature: " + signature;
        eventLogs.add(logEntry);

        // Log the event using java.util.logging
        logger.log(Level.INFO, logEntry);
    }

    public void auditLogs() {
        for (String log : eventLogs) {
            logger.log(Level.INFO, "Auditing log entry: " + log);
        }
        logger.log(Level.INFO, "Audit completed");
    }

    public void synchronizeLogs() {
        String dbUrl = "jdbc:mysql://your-database-url:3306/your-database";
        String dbUser = "your-username";
        String dbPassword = "your-password";

        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            String insertLogSQL = "INSERT INTO logs (log_entry) VALUES (?)";
            PreparedStatement preparedStatement = connection.prepareStatement(insertLogSQL);

            for (String log : eventLogs) {
                try {
                    preparedStatement.setString(1, log);
                    preparedStatement.executeUpdate();
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Failed to insert log entry.", e);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to synchronize logs with the central database.", e);
        }
    }
}
