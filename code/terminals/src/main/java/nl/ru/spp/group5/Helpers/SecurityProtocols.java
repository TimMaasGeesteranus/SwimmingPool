package nl.ru.spp.group5.Helpers;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;


import java.security.Key;

public class SecurityProtocols {

    // Method for mutual authentication between card and terminal/vending machine
    public static boolean mutualAuthentication(CardChannel channel) throws CardException{
        // Ask card to send its certificate
        byte[] certCard = getCertFromCard(channel);

        // Check validity of certificate and if card is not blocked
        if(!certCardValid(certCard) || Backend.isCardBlocked("123")){
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

    private static boolean certCardValid(byte[] certCard){
        return true;
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
