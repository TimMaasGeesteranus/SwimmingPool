package nl.ru.spp.group5;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.logging.*;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.smartcardio.*;

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
        logger.info("Card detected, handling card...");

        // Mutual authentication
        // if(!SecurityProtocols.mutualAuthentication(channel, true, TERMINAL_PUB_KEY, TERMINAL_PRIV_KEY)){
        //     denyAccess();
        // }

        try{
            // If card has season ticket, check certificate
            byte[] cardSeasonCert = getSeasonCertFromCard(channel);
            if(cardSeasonCert != null){
                if(isCertValid(cardSeasonCert)){
                    openGate();
                }
            }

            // Check if card has entry
            if(cardHasEntry()){
                openGate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }       
        denyAccess();
    }

    //TODO seasoncert on card is probably incorrect because it wont return like it should
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

    private boolean isCertValid(byte[] cardSeasonCert){
        return true;
    }

    private boolean cardHasEntry(){
        return true;
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
