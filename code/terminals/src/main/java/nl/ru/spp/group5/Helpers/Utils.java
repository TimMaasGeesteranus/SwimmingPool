package nl.ru.spp.group5.Helpers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

public class Utils {
    public final static int CARD_ID_LENGTH = 16;
    public final static int CARD_EXP_DATE_LENGTH = 10;
    public final static int KEY_LENGTH = 256;
    public final static int NONCE_LENGTH = 16;
    public final static int CERT_LENGTH = KEY_LENGTH;

    // Method to generate a nonce for cryptographic protocols
    // TODO: make this a counter bc thats more safe than using random
    public static byte[] generateNonce() {
        SecureRandom random = new SecureRandom();
        byte[] nonce = new byte[NONCE_LENGTH]; // 16 bytes nonce
        random.nextBytes(nonce);
        return nonce;
    }
    
    // TODO: maybe not do this random bc duplicates?
    public static byte[] generateCardID(){
        byte[] cardID = new byte[CARD_ID_LENGTH];
        Random random = new Random();

        // Fill the byte array with random numbers
        for (int i = 0; i < CARD_ID_LENGTH; i++) {
            cardID[i] = (byte) (random.nextInt(10) + 48);     
        }
        
        return cardID;
    }

    // Method to get the current date for checking season ticket validity
    public static String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormat.format(new Date());
    }

    public static void clearScreen() {  
        System.out.print("\033[H\033[2J");  
        System.out.flush();  
    }  

    public static byte[] stringToBytes(String input){
        return input.getBytes();
    }

    public static String bytesToString(byte[] input){
        return new String(input);
    }

    public static byte[] intToBytes(int input){
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(input);
        return buffer.array();
    }

    public static byte[] getExpirationDate(int yearsFromNow){
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, yearsFromNow);
        Date expirationDate = calendar.getTime();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormat.format(expirationDate).getBytes();
    }

    public static byte[] getExpirationDateUsingMonths(int monthsFromNow){
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, monthsFromNow);
        Date expirationDate = calendar.getTime();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormat.format(expirationDate).getBytes();
    }

    public static byte[] sign(byte[] data, RSAPrivateKey key) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException{
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(key);
        signature.update(data);

        return signature.sign();
    }

    public static RSAPublicKey readPubKey() throws FileNotFoundException, IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        File file = new File("terminal_pubkey.pem");

        try(PemReader pemReader = new PemReader(new FileReader(file));){
            PemObject pemObject = pemReader.readPemObject();
            byte[] pemContent = pemObject.getContent();
            pemReader.close();

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(pemContent);
            return (RSAPublicKey) keyFactory.generatePublic(keySpec);
        }
    }

    public static RSAPrivateKey readPrivKey() throws FileNotFoundException, IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        File file = new File("terminal_privkey.pem");

        try(PemReader pemReader = new PemReader(new FileReader(file));){
            PemObject pemObject = pemReader.readPemObject();
            byte[] pemContent = pemObject.getContent();
            pemReader.close();

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pemContent);
            return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
        }
    }
}
