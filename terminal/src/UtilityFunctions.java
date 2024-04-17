import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UtilityFunctions {

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
}