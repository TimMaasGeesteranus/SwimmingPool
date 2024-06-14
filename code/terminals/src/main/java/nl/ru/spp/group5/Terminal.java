package nl.ru.spp.group5;

import static nl.ru.spp.group5.Helpers.Utils.NONCE_LENGTH;
import static nl.ru.spp.group5.Helpers.Utils.readPrivKey;
import static nl.ru.spp.group5.Helpers.Utils.readPubKey;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.interfaces.RSAPrivateKey;
import java.util.List;
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.smartcardio.*;

import nl.ru.spp.group5.Helpers.Utils;

public abstract class Terminal{
    protected final RSAPublicKey TERMINAL_PUB_KEY;
    protected final RSAPrivateKey TERMINAL_PRIV_KEY;
    public static byte counter;
    public static byte[] nonce1;
    public static byte[] nonce2;

    Terminal() throws FileNotFoundException, IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        // Read private and public keys from files
        this.TERMINAL_PUB_KEY = readPubKey();
        this.TERMINAL_PRIV_KEY = readPrivKey();
        counter = (byte) 0x00;
        nonce1 = new byte[NONCE_LENGTH];
        nonce2 = new byte[NONCE_LENGTH];
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
            Utils.clearScreen();
            System.out.println("Waiting for card");
            try {
                if (terminal.isCardPresent()) {
                    CardChannel channel = terminal.connect("*").getBasicChannel();
                    if (appletSelectedSuccessfully(channel)){
                        handleCard(channel);
                    }
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

    abstract public void handleCard(CardChannel channel) throws BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException, InvalidKeySpecException, InterruptedException, SignatureException, InvalidKeyException, NoSuchAlgorithmException, CardException;

    private boolean appletSelectedSuccessfully(CardChannel channel) throws CardException{
        byte[] apduBytes = {
            (byte) 0x00,       // CLA (Class)
            (byte) 0xA4,       // INS (Instruction - SELECT)
            (byte) 0x04,       // P1  (Parameter 1 - SELECT by DF Name)
            (byte) 0x00,       // P2  (Parameter 2 - First or only occurrence)
            (byte) 0x06,       // Lc  (Length of data)
            (byte) 0x11,       // AID length (11 bytes for your AID)
            (byte) 0x22, (byte) 0x33, (byte) 0x44, (byte) 0x55, (byte) 0x66, // AID bytes
            (byte) 0x00        // Le  (Maximum response length)
        };
        CommandAPDU apdu = new CommandAPDU(apduBytes);
        ResponseAPDU response = channel.transmit(apdu);

        if (response.getSW() != 0x9000){
            System.out.println("Could not select the applet on the card. Please try again.");
            return false;
        }
        return true;
    }
}
