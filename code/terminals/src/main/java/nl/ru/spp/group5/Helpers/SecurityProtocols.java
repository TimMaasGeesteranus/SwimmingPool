package nl.ru.spp.group5.Helpers;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import static nl.ru.spp.group5.Helpers.Utils.*;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;

public class SecurityProtocols {

    // Method for mutual authentication between card and terminal/vending machine
    public static boolean mutualAuthentication(CardChannel channel, boolean isGate, RSAPublicKey pubKeyVending) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, CardException{
        // Ask card to send its data
        byte[] cardID = getCardID(channel);
        byte[] cardExpirationDate = getCardExpirationDate(channel);
        byte[] cardPubKey = getCardPubKey(channel);

        // Ask card to send its certificate
        byte[] certCard = getCertFromCard(channel);

        // Concatenate cardID, cardExpirationdate and pubKeyCard so cert can be validated
        byte[] data = new byte[CARD_ID_LENGTH + CARD_EXP_DATE_LENGTH + KEY_LENGTH];
        System.arraycopy(cardID, 0, data, 0, CARD_ID_LENGTH);
        System.arraycopy(cardExpirationDate, 0, data, CARD_ID_LENGTH, CARD_EXP_DATE_LENGTH);
        System.arraycopy(cardPubKey, 0, data, CARD_ID_LENGTH+CARD_EXP_DATE_LENGTH, KEY_LENGTH);       

        // Check validity of certificate and if card is not blocked
        if(!certCardValid(certCard, data, pubKeyVending) || Backend.isCardBlocked(new String(cardID))){
            System.out.println("Something went wrong while authenticating");
            return false;
        }

        // Send certificate and nonce1, retreive x1 and nonce2
        byte[] x1AndNonce2 = sendCertAndNonce1AndGetx1AndNonce2();

        // Authenticate card
        authenticateCard(x1AndNonce2);

        calculatex2AndSend();

        return true; 
    }

    private static byte[] getCardID(CardChannel channel) throws CardException{
        // Sending ID request
        CommandAPDU apdu = new CommandAPDU(0x00, (byte)0x10, 0x00, 0x00);
        ResponseAPDU response = channel.transmit(apdu);

        // Verifying response
        if (response.getSW() != 0x9000){
            System.out.println("something went wrong");
            System.exit(1);
        }

        return response.getData();
    }

    private static byte[] getCardExpirationDate(CardChannel channel) throws CardException{
        // Sending ID request
        CommandAPDU apdu = new CommandAPDU(0x00, (byte)0x11, 0x00, 0x00);
        ResponseAPDU response = channel.transmit(apdu);

        // Verifying response
        if (response.getSW() != 0x9000){
            System.out.println("something went wrong");
            System.exit(1);
        }

        return response.getData();
    }

    private static byte[] getCardPubKey(CardChannel channel) throws CardException{
        // Sending ID request
        CommandAPDU apdu = new CommandAPDU(0x00, (byte)0x12, 0x00, 0x00);
        ResponseAPDU response = channel.transmit(apdu);

        // Verifying response
        if (response.getSW() != 0x9000){
            System.out.println("something went wrong");
            System.exit(1);
        }

        return response.getData();
    }


    private static byte[] getCertFromCard(CardChannel channel) throws CardException{  
        // Sending cert request
        CommandAPDU apdu = new CommandAPDU(0x00, (byte)0x08, 0x00, 0x00);
        ResponseAPDU response = channel.transmit(apdu);

        // Verifying response
        if (response.getSW() != 0x9000){
            System.out.println("something went wrong");
            System.exit(1);
        }

        return response.getData();
    }

    private static boolean certCardValid(byte[] certCard, byte[] data, RSAPublicKey pubKeyVending) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException{        
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(pubKeyVending);
        sig.update(data);
        
        return sig.verify(certCard);
    }

    private static byte[] sendCertAndNonce1AndGetx1AndNonce2(){
        return new byte[4];
    }

    private static void authenticateCard(byte[] x1AndNonce2){

    }

    private static void calculatex2AndSend(){

    }

    // Method to derive a session key from the card's symmetric key and a nonce
    public byte[] deriveSessionKey(byte[] nonce) {
        // Implement logic to derive session key Ksession from Kcard and nonce
        // Placeholder for session key derivation logic
        return new byte[]{/* derived session key bytes */};
    }

    // Method to generate a Message Authentication Code (MAC) for message integrity and authenticity
    public byte[] generateMAC(byte[] message, byte[] sessionKey) throws Exception {
        // Implement MAC generation using sessionKey
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(sessionKey, "HmacSHA256");
        mac.init(keySpec);
        return mac.doFinal(message);
    }

    // Method to verify the certificate of a season ticket using the public key of the vending machine
    public boolean verifyCertificate(byte[] cert, byte[] publicKey) {
        // Placeholder for certificate verification logic
        return true; // Assume the certificate is valid for demonstration
    }

    // Method to encrypt data using the given key
    public byte[] encryptData(byte[] data, Key key) {
        // Placeholder for encryption logic
        return data; // Return unmodified data for demonstration
    }

    // Method to decrypt data using the given key
    public byte[] decryptData(byte[] data, Key key) {
        // Placeholder for decryption logic
        return data; // Return unmodified data for demonstration
    }


}
