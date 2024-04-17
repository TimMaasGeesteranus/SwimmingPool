import java.util.Scanner;
import javax.smartcardio.*;

public class Main {

    public static void main(String[] args) {

    checking();
    }
    public static void connection() {

        Scanner scanner = new Scanner(System.in);

        // Check if the card reader is connected
        if (CardReaderUtil.isCardReaderConnected()) {
            try {
                TerminalFactory factory = TerminalFactory.getDefault();
                CardTerminal terminal = factory.terminals().list().get(0);

                if (terminal.isCardPresent()) {
                    System.out.print("Enter PIN: ");
                    String enteredPin = scanner.nextLine();

                    if ("1234".equals(enteredPin)) {
                        // Correct PIN, proceed with card communication
                        Card card = terminal.connect("*");
                        CardChannel channel = card.getBasicChannel();

                        // Example command - replace with actual command for your card
                        byte[] commandBytes = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
                        CommandAPDU command = new CommandAPDU(commandBytes);
                        ResponseAPDU response = channel.transmit(command);

                        System.out.println("Response: " + response.toString());
                        card.disconnect(false);

                        // Send "Hello World" message to the card reader
                        System.out.println("Hello World");
                    } else {
                        System.out.println("Incorrect PIN.");
                    }
                } else {
                    System.out.println("No card is present in the reader.");
                }
            } catch (CardException e) {
                System.err.println("Error accessing card: " + e.getMessage());
            }
        } else {
            System.out.println("cyberJack RFID standard card reader is not connected.");
        }

        scanner.close();
    }
    public static void checking() {
        TerminalFactory factory = TerminalFactory.getDefault();
        CardTerminal terminal = null;

        try {
            terminal = factory.terminals().list().get(0);
        } catch (Exception e) {
            System.err.println("Error getting card terminal: " + e.getMessage());
            return;
        }

        while (true) {
            try {
                if (terminal.isCardPresent()) {
                    System.out.println("ahhh cumming");
                    // Wait for card to be removed before checking again
                    while (terminal.isCardPresent()) {
                        Thread.sleep(1000);
                    }
                }
                Thread.sleep(1000); // Check every second
            } catch (Exception e) {
                System.err.println("Error checking card presence: " + e.getMessage());
            }
        }}
    public static void button() {
        TerminalFactory factory = TerminalFactory.getDefault();
        CardTerminal terminal = null;

        try {
            terminal = factory.terminals().list().get(0);
        } catch (Exception e) {
            System.err.println("Error getting card terminal: " + e.getMessage());
            return;
        }

        try {
            if (terminal.isCardPresent()) {
                System.out.println("Input pressed on physical card reader.");
                // Additional processing can be added here
            } else {
                System.out.println("No card detected. Exiting function.");
                return; // Breaks the execution as no card is found
            }
        } catch (CardException e) {
            System.err.println("Error checking card presence: " + e.getMessage());
        }
    }
}
