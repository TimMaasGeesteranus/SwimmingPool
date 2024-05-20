package nl.ru.spp.group5;

import java.security.Security;
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
            System.out.println(factory.terminals().list());
            terminal = factory.terminals().list().get(0);
        } catch (Exception e) {
            System.err.println("Error getting card terminal: " + e.getMessage());
            return;
        }

        while (true) {
            System.out.println("Waiting for card");
            try {
                if (terminal.isCardPresent()) {
                    CardChannel channel = terminal.connect("*").getBasicChannel();
                    handleCard(channel);
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

    abstract public void handleCard(CardChannel channel) throws CardException;
}
