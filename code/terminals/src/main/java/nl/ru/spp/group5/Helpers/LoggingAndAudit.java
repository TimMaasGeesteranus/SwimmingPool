package nl.ru.spp.group5.Helpers;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.*;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.util.Base64;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class LoggingAndAudit {

    private List<String> eventLogs = new ArrayList<>();
    private static final Logger logger = Logger.getLogger(LoggingAndAudit.class.getName());
    private static SecretKey secretKey;

    static {
        try {
            LogManager.getLogManager().reset();
            FileHandler fileHandler = new FileHandler("logging_and_audit.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            logger.setLevel(Level.ALL);

            // Initialize encryption key
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            secretKey = keyGen.generateKey();
        } catch (IOException | NoSuchAlgorithmException e) {
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
            String insertLogSQL = "INSERT INTO logs (encrypted_log) VALUES (?)";
            PreparedStatement preparedStatement = connection.prepareStatement(insertLogSQL);

            for (String log : eventLogs) {
                try {
                    String encryptedLog = encrypt(log);
                    preparedStatement.setString(1, encryptedLog);
                    preparedStatement.executeUpdate();
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Failed to encrypt log entry.", e);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to synchronize logs with the central database.", e);
        }
    }

    private String encrypt(String data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    private String decrypt(String encryptedData) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(decryptedBytes);
    }
}
