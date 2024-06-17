package nl.ru.spp.group5;

import static nl.ru.spp.group5.Helpers.Utils.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.logging.*;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.smartcardio.*;

import nl.ru.spp.group5.Helpers.Card_Managment;
import nl.ru.spp.group5.Helpers.SecurityProtocols;
import nl.ru.spp.group5.Helpers.Utils;

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

    public static void main(String[] args) throws FileNotFoundException, IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        logger.info("Starting AccessGateTerminal...");
        AccessGateTerminal accessGateTerminal = new AccessGateTerminal();
        accessGateTerminal.waitForCard();
    }

    public AccessGateTerminal() throws FileNotFoundException, IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        logger.info("AccessGateTerminal initialized.");
    }

    @Override
    public void handleCard(CardChannel channel) throws InterruptedException, BadPaddingException, NoSuchPaddingException, IllegalBlockSizeException, InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, CardException {
        Utils.clearScreen();
        System.out.println("Loading...");
        logger.info("Card detected, handling card...");

        // Mutual authentication
        if(!SecurityProtocols.mutualAuthentication(channel, true, TERMINAL_PUB_KEY, TERMINAL_PRIV_KEY)){
            denyAccess();
        }

        try{
            // If card has season ticket, check certificate
            byte[] cardSeasonCert = getSeasonCertFromCard(channel);
            if(cardSeasonCert != null){
                if(isCertValid(channel, cardSeasonCert)){
                    openGate();
                    return;
                }
                else if(cardHasEntry(channel)){
                    openGate();
                    return;
                }
            }
            else if(cardHasEntry(channel)){
                openGate();
                return;
            }
            denyAccess();
        } catch (Exception e) {
            e.printStackTrace();
        }       
    }

    private static byte[] getSeasonCertFromCard(CardChannel channel) throws CardException{  
        // Sending cert request
        CommandAPDU apdu = new CommandAPDU(0x00, (byte)0x26, 0x00, 0x00);
        ResponseAPDU response = channel.transmit(apdu);

        // Verifying response
        if (response.getSW() != 0x9000){
            System.out.println(response.getSW());
            throw new CardException("something went wrong when requesting season ticket");
        }

        return response.getData();
    }

    private boolean isCertValid(CardChannel channel, byte[] cardSeasonCert) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, CardException{        
        // Concatenate cardID, cardExpirationdate and pubKeyCard so cert can be validated
        byte[] data = new byte[CARD_ID_LENGTH + CARD_EXP_DATE_LENGTH];
        byte[] cardID = SecurityProtocols.getCardID(channel);
        byte[] seasonCertExpirationDate = getSeasonCertExpirationDate(channel);

        // Check if expirationDate is valid
        boolean valid = Utils.isValidDate(seasonCertExpirationDate);
        if(!valid){
            return false;
        }

        System.arraycopy(cardID, 0, data, 0, CARD_ID_LENGTH);
        System.arraycopy(seasonCertExpirationDate, 0, data, CARD_ID_LENGTH, CARD_EXP_DATE_LENGTH);

        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(TERMINAL_PUB_KEY);
        sig.update(data);

        return sig.verify(cardSeasonCert);
    }

    private boolean cardHasEntry(CardChannel channel){
        int entries = Card_Managment.getEntriesFromCard(channel);

        // Delete entry from card
        if(entries > 0){
            Card_Managment.setEntries(channel, "123", entries-1);
            return true;
        }

        return false;
    }

    public void openGate() {
        logger.info("Access granted, opening the gate...");
        Utils.clearScreen();
        System.out.println("Welcome to the swimming pool!");
        System.out.println("...Opening the gate...");
    }

    public void denyAccess() {
        Utils.clearScreen();
        logger.warning("Access denied.");
        System.out.println("Access denied...");
    }

    public byte[] getSeasonCertExpirationDate(CardChannel channel) throws CardException{
        // Sending ID request
        CommandAPDU apdu = new CommandAPDU(0x00, (byte)0x2B, 0x00, 0x00);
        ResponseAPDU response = channel.transmit(apdu);

        // Verifying response
        if (response.getSW() != 0x9000){
            throw new CardException("something went wrong with getting season cert expirydate");
        }

        return response.getData();
    }
}
