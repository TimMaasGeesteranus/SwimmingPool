package nl.ru.spp.group5.Helpers;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import static nl.ru.spp.group5.Helpers.Utils.*;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

public class SecurityProtocols {

    // Method for mutual authentication between card and terminal/vending machine
    public static boolean mutualAuthentication(CardChannel channel, boolean isGate, RSAPublicKey pubKeyVending, RSAPrivateKey privKeyVending) throws BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException, InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, CardException{
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

        // Generate nonce 1
        byte[] nonce1 = new byte[NONCE_LENGTH];
        System.arraycopy(generateNonce(), 0, nonce1, 0, NONCE_LENGTH);

        // Send nonce1, retreive x1 and nonce2
        byte[] x1 = sendNonce1AndGetx1(channel, nonce1);
        byte[] nonce2 = getNonce2(channel);

        // Authenticate card
        if(!verifyEncryption(x1, nonce1, cardPubKey)){
            return false;
        }

        calculatex2AndSend(channel, privKeyVending, nonce2);

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

    private static byte[] sendNonce1AndGetx1(CardChannel channel, byte[] nonce1) throws CardException{
        //TODO: generate cert if gate and send to card

        // Sending nonce
        CommandAPDU apdu = new CommandAPDU(0x00, (byte)0x13, 0x00, 0x00, nonce1);

        // Verifying response
        ResponseAPDU response = channel.transmit(apdu);
        if (response.getSW() != 0x9000){
            System.out.println("something went wrong");
            System.exit(1);
        }

        return response.getData();
    }

    private static byte[] getNonce2(CardChannel channel) throws CardException{
        // Sending request for nonce2
        CommandAPDU apdu = new CommandAPDU(0x00, (byte)0x14, 0x00, 0x00);

        // Verifying response
        ResponseAPDU response = channel.transmit(apdu);
        if (response.getSW() != 0x9000){
            System.out.println("something went wrong");
            System.exit(1);
        }

        // saving x1
        return response.getData();
    }

    private static boolean verifyEncryption(byte[] x1, byte[] nonce1, byte[] cardPubKey) throws BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, InvalidKeySpecException, NoSuchAlgorithmException{         
        // Convert key to be used with Cipher
        RSAPublicKeySpec spec = new RSAPublicKeySpec(new BigInteger(1, cardPubKey), BigInteger.valueOf(65537));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(spec);

        // Setup cipher
        Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, publicKey);

        // Decrypt x1
        byte[] decryptedX1 = cipher.doFinal(x1);

        // Pad nonce1 with zeroes
        byte[] paddedNonce = new byte[KEY_LENGTH];
        System.arraycopy(nonce1, 0, paddedNonce, 0, NONCE_LENGTH);

        // Compare decrypted x1 and nonce1
        return Arrays.equals(decryptedX1, paddedNonce);
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }

    private static void calculatex2AndSend(CardChannel channel, RSAPrivateKey privKeyVending, byte[] nonce2) throws CardException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, NoSuchPaddingException, BadPaddingException{
        // Setup cipher
        Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, privKeyVending);    

        // Encrypt
        byte[] x2 = cipher.doFinal(nonce2);

        // Divide into two halfs because x2 is too big to send
        byte[] firstHalf = new byte[x2.length/2];
        byte[] secondHalf = new byte[x2.length/2];
        System.arraycopy(x2, 0, firstHalf, 0, x2.length/2);
        System.arraycopy(x2, x2.length/2, secondHalf, 0, x2.length/2);

        // Sending first half
        CommandAPDU apdu = new CommandAPDU(0x00, (byte)0x15, 0x00, 0x00, firstHalf);

        // Verifying response
        ResponseAPDU response = channel.transmit(apdu);
        if (response.getSW() != 0x9000){
            System.out.println("something went wrong 1");
            System.exit(1);
        }

        // Sending second half
        apdu = new CommandAPDU(0x00, (byte)0x16, 0x00, 0x00, secondHalf);

        // Verifying response
        response = channel.transmit(apdu);
        System.out.println(response.getSW());
        System.out.println(response.getData());

        if (response.getSW() != 0x9000){
            System.out.println("something went wrong 2");
            System.exit(1);
        }
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
