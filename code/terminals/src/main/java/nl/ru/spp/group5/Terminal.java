package nl.ru.spp.group5;

import java.util.List;
import java.util.Scanner;
import javax.smartcardio.*;

public abstract class Terminal{

    Terminal(){

    }

    public void waitForCard() {
        TerminalFactory factory = TerminalFactory.getDefault();
        CardTerminal terminal = null;

        try {
            terminal = factory.terminals().list().get(0);
        } catch (Exception e) {
            System.err.println("Error getting card terminal: " + e.getMessage());
            return;
        }

        while (true) {
            System.out.println("Waiting for card");
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
        }
    }

    public void waitForCardReiner(){
        String READER_NAME = "cyberJack RFID standard"; // Adjust this as needed

        try {
            TerminalFactory factory = TerminalFactory.getDefault();
            List<CardTerminal> terminals = factory.terminals().list();
            System.out.println("Terminals: " + terminals);

            if (terminals.isEmpty()) {
                System.out.println("No card terminals found.");
                return;
            }

            for (CardTerminal terminal : terminals) {
                if (terminal.getName().contains(READER_NAME)) {
                    System.out.println("cyberJack RFID standard card reader is connected.");
                    return;
                }
            }

            System.out.println("cyberJack RFID standard card reader is not connected.");
            return;
        } catch (CardException e) {
            System.err.println("Error checking card readers: " + e.getMessage());
            return;
        }
    }
}
