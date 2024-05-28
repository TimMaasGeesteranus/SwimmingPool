package nl.ru.spp.group5;

import java.io.IOException;
import java.util.logging.*;
import javax.smartcardio.*;

public class AccessGateTerminal extends Terminal {
    private static final Logger logger = Logger.getLogger(AccessGateTerminal.class.getName());

    static {
        try {
            LogManager.getLogManager().reset();
            FileHandler fileHandler = new FileHandler("access_gate_terminal.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            logger.setLevel(Level.ALL);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to initialize logger handler.", e);
        }
    }

    public static void main(String[] args) {
        logger.info("Starting AccessGateTerminal...");
        AccessGateTerminal accessGateTerminal = new AccessGateTerminal();
        accessGateTerminal.waitForCard();
    }

    public AccessGateTerminal() {
        logger.info("AccessGateTerminal initialized.");
    }

    @Override
    public void handleCard(CardChannel channel) throws CardException {
        logger.info("Card detected, handling card...");

        // TODO: Mutual authentication
        CommandAPDU apdu = new CommandAPDU((byte) 0x02, (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00); // TODO: what bytes are sent when?
        ResponseAPDU response = channel.transmit(apdu);

        byte[] responseBytes = response.getData();
        String responseString = new String(responseBytes);
        logger.info("Received response from card: " + responseString);

        switch (responseString) {
            case "true":
                openGate();
                break;
            case "false":
                denyAccess();
                break;
            default:
                denyAccess();
                break;
        }
    }

    public void openGate() {
        logger.info("Access granted, opening the gate...");
        System.out.println("Welcome to the swimming pool!");
        System.out.println("...Opening the gate...");
    }

    public void denyAccess() {
        logger.warning("Access denied.");
        System.out.println("Access denied...");
    }
}
