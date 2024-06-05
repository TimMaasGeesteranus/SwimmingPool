package nl.ru.spp.group5;

import javacard.security.*;
import javacard.framework.*;
import javacardx.crypto.Cipher;

public class Auth {
    private final Card card;

    Auth(Card card) {
        this.card = card;
    }

    void returnCertificate(APDU apdu){
        // Prepare data
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(card.cardCertificate, (short) 0, buffer, (short) 0, (short) Consts.CERT_LENGTH);

        // Send certificate
        apdu.setOutgoingAndSend((short)0, (short) Consts.CERT_LENGTH);
    }

    void returnID(APDU apdu){
        // Prepare data
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(card.cardID, (short) 0, buffer, (short) 0, (short) Consts.CARD_ID_LENGTH);

        // Send ID
        apdu.setOutgoingAndSend((short)0, (short) Consts.CARD_ID_LENGTH);    
    }

    void returnExpirationDate(APDU apdu){
        // Prepare data
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(card.cardExpirationDate, (short) 0, buffer, (short) 0, (short) Consts.CARD_EXP_DATE_LENGTH);

        // Send expiration date
        apdu.setOutgoingAndSend((short)0, (short) Consts.CARD_EXP_DATE_LENGTH); 
    }

    void returnPubKey(APDU apdu){
        // Prepare data
        byte[] buffer = apdu.getBuffer();
        card.pubKeyCard.getModulus(buffer, (short) 0);

        // Send public key card
        apdu.setOutgoingAndSend((short)0, (short) Consts.KEY_LENGTH); 
    }

    void calculatex1(APDU apdu){
        byte[] nonce1 = new byte[Consts.KEY_LENGTH]; // nonce will be padded with zeroes to match key_length

        // Get nonce from apdu
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, nonce1, (short) 0, (short) Consts.NONCE_LENGTH);

        // Encrypt nonce using key
        Cipher cipher = Cipher.getInstance(Cipher.ALG_RSA_NOPAD, false);
        cipher.init(card.privKeyCard, Cipher.MODE_ENCRYPT);
        cipher.doFinal(nonce1, (short) 0, (short) Consts.KEY_LENGTH, buffer, (short) 0);

        // Send encrypted nonce
        apdu.setOutgoingAndSend((short)0, (short) Consts.KEY_LENGTH);    
    }

    void getNonce2(APDU apdu){
        card.nonce2 = generateNonce();

        //Prepare data
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(card.nonce2, (short) 0, buffer, (short) 0, (short) Consts.NONCE_LENGTH);

        apdu.setOutgoingAndSend((short) 0, (short) Consts.NONCE_LENGTH);
    }

    byte[] generateNonce(){
        byte[] nonce = new byte[Consts.NONCE_LENGTH];

        RandomData random = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);
        random.generateData(nonce, (short) 0, Consts.NONCE_LENGTH);

        return nonce;
    }

    void authenticateTerminalFirstHalf(APDU apdu){
        // Get first half of x2 onto card and save
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, card.x2, (short) 0, (short) (Consts.KEY_LENGTH/2) );

        apdu.setOutgoingAndSend((short)0, (short)0);
    }

void authenticateTerminalSecondHalf(APDU apdu) {
    // Get second half of x2 onto card and save
    byte[] buffer = apdu.getBuffer();
    Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, card.x2, (short) (Consts.KEY_LENGTH / 2), (short) (Consts.KEY_LENGTH / 2));

    // Decrypt x2 using key
    Cipher cipher = Cipher.getInstance(Cipher.ALG_RSA_NOPAD, false);
    cipher.init(card.pubKeyVending, Cipher.MODE_DECRYPT);

    byte[] n2 = new byte[Consts.KEY_LENGTH];
    cipher.doFinal(card.x2, (short) 0, (short) Consts.KEY_LENGTH, n2, (short) 0);

    // Prepare padded nonce
    byte[] paddedNonce = new byte[Consts.KEY_LENGTH];
    Util.arrayCopy(card.nonce2, (short) 0, paddedNonce, (short) 0, (short) Consts.NONCE_LENGTH);

    // Compare n2 with paddedNonce
    if (isEqual(n2, paddedNonce)) {
        buffer[0] = 1; // Indicate success
    } else {
        buffer[0] = 0; // Indicate failure
    }

    // Send back n2 and paddedNonce for debugging
    Util.arrayCopy(n2, (short) 0, buffer, (short) 1, (short) Consts.KEY_LENGTH);
    Util.arrayCopy(paddedNonce, (short) 0, buffer, (short) (Consts.KEY_LENGTH + 1), (short) Consts.KEY_LENGTH);

    apdu.setOutgoingAndSend((short) 0, (short) (Consts.KEY_LENGTH * 2 + 1));
}

boolean isEqual(byte[] array1, byte[] array2) {
    if (array1.length != array2.length) {
        return false;
    }
    for (short i = 0; i < array1.length; i++) {
        if (array1[i] != array2[i]) {
            return false;
        }
    }
    return true;
}

}