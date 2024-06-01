package nl.ru.spp.group5;

import javacard.security.*;
import javacard.framework.*;

public class Init {
    private final Card applet;

    Init(Card applet) {
        this.applet = applet;
    }

    void generateKeys(APDU apdu){
        // Get cardID and expirationDate from apdu and save onto card
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, applet.cardID, (short) 0, (short) Consts.CARD_ID_LENGTH );
        Util.arrayCopy(buffer, (short) (ISO7816.OFFSET_CDATA + Consts.CARD_ID_LENGTH), applet.cardExpirationDate, (short) 0, (short) Consts.CARD_EXP_DATE_LENGTH);

        // Generate keys for the card
        KeyPair keyPair = new KeyPair(KeyPair.ALG_RSA, KeyBuilder.LENGTH_RSA_2048);
        keyPair.genKeyPair();
        applet.pubKeyCard = (RSAPublicKey) keyPair.getPublic();
        applet.privKeyCard = (RSAPrivateKey) keyPair.getPrivate();

        // Send public key
        applet.pubKeyCard.getModulus(buffer, (short) 0);
        apdu.setOutgoingAndSend((short)0, (short) 256);
    }
}
