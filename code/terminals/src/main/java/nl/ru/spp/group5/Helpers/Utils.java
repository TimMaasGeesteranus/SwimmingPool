package nl.ru.spp.group5.Helpers;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {

    // Method to generate a nonce for cryptographic protocols
    public static byte[] generateNonce() {
        SecureRandom random = new SecureRandom();
        byte[] nonce = new byte[16]; // 16 bytes nonce
        random.nextBytes(nonce);
        return nonce;
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
}
