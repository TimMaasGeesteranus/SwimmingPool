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
        byte[] nonce2 = generateNonce();

        //Prepare data
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(nonce2, (short) 0, buffer, (short) 0, (short) Consts.NONCE_LENGTH);

        apdu.setOutgoingAndSend((short) 0, (short) Consts.NONCE_LENGTH);
    }

    byte[] generateNonce(){
        byte[] nonce = new byte[Consts.NONCE_LENGTH];

        RandomData random = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);
        random.generateData(nonce, (short) 0, Consts.NONCE_LENGTH);

        return nonce;
    }
}
