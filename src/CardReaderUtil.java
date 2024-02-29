import javax.smartcardio.*;


import java.util.List;

public class CardReaderUtil {

    private static final String READER_NAME = "cyberJack RFID standard"; // Adjust this as needed

    public static boolean isCardReaderConnected() {
        try {
            TerminalFactory factory = TerminalFactory.getDefault();
            List<CardTerminal> terminals = factory.terminals().list();
            System.out.println("Terminals: " + terminals);

            if (terminals.isEmpty()) {
                System.out.println("No card terminals found.");
                return false;
            }

            for (CardTerminal terminal : terminals) {
                if (terminal.getName().contains(READER_NAME)) {
                    System.out.println("cyberJack RFID standard card reader is connected.");
                    return true;
                }
            }

            System.out.println("cyberJack RFID standard card reader is not connected.");
            return false;
        } catch (CardException e) {
            System.err.println("Error checking card readers: " + e.getMessage());
            return false;
        }
    }

    public static void main(String[] args) {
        boolean isReaderConnected = isCardReaderConnected();
        System.out.println("Is cyberJack RFID standard card reader connected: " + isReaderConnected);
    }
}
