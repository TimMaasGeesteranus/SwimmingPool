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

import nl.ru.spp.group5.Terminal;

import static nl.ru.spp.group5.Helpers.Utils.*;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
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
        try{
            return authenticate(channel, isGate, pubKeyVending, privKeyVending);
        } catch (Exception e){
            try{
                return authenticate(channel, isGate, pubKeyVending, privKeyVending);
            } catch (Exception e2){
                try{
                    return authenticate(channel, isGate, pubKeyVending, privKeyVending);
                } catch (Exception e3){
                    System.out.println("Could not authenticate: " + e3.getMessage());
                    System.out.println("");
                    return false;
                }
            }
        }
    }

    private static boolean authenticate(CardChannel channel, boolean isGate, RSAPublicKey pubKeyVending, RSAPrivateKey privKeyVending) throws BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException, InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, CardException{
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
            throw new CardException("Something went wrong while authenticating");
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

        Terminal.nonce1 = nonce1;
        Terminal.nonce2 = nonce2;
        Terminal.counter = 0;
        return true; 
    }


    public static byte[] getCardIDProtected(CardChannel channel, RSAPrivateKey terminalPrivKey, byte[] cardPubKey, byte[] nonce1, byte[] nonce2) throws SignatureException, CardException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, InvalidKeySpecException, NoSuchAlgorithmException{         
        doFirstHalfProtected(new byte[] {(byte) 0x10}, channel, nonce1, nonce2, terminalPrivKey);
        byte[] cardID = getCardID(channel);
        return doSecondHalfProtected(channel, cardID, nonce1, nonce2, cardPubKey);        
    }

    public static byte[] requestSeasonTicketCertificateProtected(CardChannel channel, RSAPrivateKey terminalPrivKey, byte[] cardPubKey, byte[] nonce1, byte[] nonce2) throws SignatureException, CardException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, InvalidKeySpecException, NoSuchAlgorithmException{         
        doFirstHalfProtected(new byte[] {(byte) 0x09}, channel, nonce1, nonce2, terminalPrivKey);
        byte[] cert = Card_Managment.requestSeasonTicketCertificate(channel);
        return doSecondHalfProtected(channel, cert, nonce1, nonce2, cardPubKey);        
    }

    private static void doFirstHalfProtected(byte[] ins, CardChannel channel, byte[] nonce1, byte[] nonce2, RSAPrivateKey terminalPrivKey) throws SignatureException, CardException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, InvalidKeySpecException, NoSuchAlgorithmException{         
        // Create message
        byte[] m1 = new byte[KEY_LENGTH];
        System.arraycopy(ins, 0, m1, 0, ins.length);

        // Sign and send message
        Card_Managment.signMessageAndSend(channel, m1, nonce1, nonce2, Terminal.counter, terminalPrivKey);

        // Update counter
        Terminal.counter++;
    }

    private static byte[] doSecondHalfProtected(CardChannel channel, byte[] message, byte[] nonce1, byte[] nonce2, byte[] cardPubKey) throws SignatureException, CardException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, InvalidKeySpecException, NoSuchAlgorithmException{         
        byte[] m2 = new byte[KEY_LENGTH];
        System.arraycopy(message, 0, m2, 0, message.length);

        // Get signed message from card
        byte[] s2 = Card_Managment.getSignedResponse(channel);

        // Create data object from m2
        byte[] data = new byte[289];
        System.arraycopy(m2, 0, data, 0, m2.length);
        System.arraycopy(nonce1, 0, data, m2.length, nonce1.length);
        System.arraycopy(nonce2, 0, data, m2.length+nonce1.length, nonce2.length);
        data[m2.length + nonce1.length + nonce2.length] = Terminal.counter;
        
        // Compare data and signed data
        if(!signatureValid(data, s2, cardPubKey)){
            throw new CardException("IF STATEMENT something went wrong with checking the signature for the protected message");
        }

        // Update counter
        Terminal.counter++;

        return message;
    }

    public static boolean signatureValid(byte[] data, byte[] s2, byte[] cardPubKey) throws SignatureException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, InvalidKeySpecException, NoSuchAlgorithmException{         
        // Convert key to be used with Cipher
        RSAPublicKeySpec spec = new RSAPublicKeySpec(new BigInteger(1, cardPubKey), BigInteger.valueOf(65537));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(spec);

        // Setup Signature
        //Signature signature = Signature.getInstance("SHA256withRSA");
        Signature signature = Signature.getInstance("SHA1withRSA");

        signature.initVerify(publicKey);
        signature.update(data);

        return signature.verify(s2);
    }

    public static boolean signatureValidOLD(byte[] m2, byte[] s2, byte[] cardPubKey) throws BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, InvalidKeySpecException, NoSuchAlgorithmException{         
        // Convert key to be used with Cipher
        RSAPublicKeySpec spec = new RSAPublicKeySpec(new BigInteger(1, cardPubKey), BigInteger.valueOf(65537));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(spec);

        // Setup cipher
        Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, publicKey);

        // Decrypt s2
        byte[] decrypteds2 = cipher.doFinal(s2);

        // Compare decrypted x1 and nonce1
        return Arrays.equals(m2, decrypteds2);
    }

    public static byte[] getCardID(CardChannel channel) throws CardException{
        // Sending ID request
        CommandAPDU apdu = new CommandAPDU(0x00, (byte)0x10, 0x00, 0x00);
        ResponseAPDU response = channel.transmit(apdu);

        // Verifying response
        if (response.getSW() != 0x9000){
            throw new CardException("something went wrong with getting the cardID");
        }

        return response.getData();
    }

    public static byte[] getCardExpirationDate(CardChannel channel) throws CardException{
        // Sending ID request
        CommandAPDU apdu = new CommandAPDU(0x00, (byte)0x11, 0x00, 0x00);
        ResponseAPDU response = channel.transmit(apdu);

        // Verifying response
        if (response.getSW() != 0x9000){
            throw new CardException("something went wrong with getting card expiration date");
        }

        return response.getData();
    }

    public static byte[] getCardPubKey(CardChannel channel) throws CardException{
        // Sending ID request
        CommandAPDU apdu = new CommandAPDU(0x00, (byte)0x12, 0x00, 0x00);
        ResponseAPDU response = channel.transmit(apdu);

        // Verifying response
        if (response.getSW() != 0x9000){
            throw new CardException("something went wrong with getting card public key");
        }

        return response.getData();
    }


    private static byte[] getCertFromCard(CardChannel channel) throws CardException{  
        // Sending cert request
        CommandAPDU apdu = new CommandAPDU(0x00, (byte)0x08, 0x00, 0x00);
        ResponseAPDU response = channel.transmit(apdu);

        // Verifying response
        if (response.getSW() != 0x9000){
            throw new CardException("something went wrong with getting certificate from card");
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
            throw new CardException("something went wrong when sending nonce1");
        }

        return response.getData();
    }

    private static byte[] getNonce2(CardChannel channel) throws CardException{
        // Sending request for nonce2
        CommandAPDU apdu = new CommandAPDU(0x00, (byte)0x14, 0x00, 0x00);

        // Verifying response
        ResponseAPDU response = channel.transmit(apdu);
        if (response.getSW() != 0x9000){
            throw new CardException("something went wrong when receiving nonce2");
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

        // Pad nonce
        byte[] paddedNonce2 = new byte[KEY_LENGTH];
        System.arraycopy(nonce2, 0, paddedNonce2, 0, NONCE_LENGTH);

        // Encrypt
        byte[] x2 = cipher.doFinal(paddedNonce2);

        // Divide into two halfs because x2 is too big to send
        int half = KEY_LENGTH/2;
        byte[] firstHalf = new byte[half];
        byte[] secondHalf = new byte[half];
        System.arraycopy(x2, 0, firstHalf, 0, half);
        System.arraycopy(x2, half, secondHalf, 0, half);


        // Sending first half
        CommandAPDU apdu = new CommandAPDU(0x00, (byte)0x15, 0x00, 0x00, firstHalf);

        // Verifying response
        ResponseAPDU response = channel.transmit(apdu);
        if (response.getSW() != 0x9000){
            throw new CardException("something went wrong when sending first half x2");
        }

        // Sending second half
        CommandAPDU secondApdu = new CommandAPDU(0x00, (byte)0x16, 0x00, 0x00, secondHalf);

        // Verifying response
        ResponseAPDU secondResponse = channel.transmit(secondApdu);

        if (secondResponse.getSW() != 0x9000){
            throw new CardException("something went wrong when sending second half x2");
        }
    }
}
