package nl.ru.spp.group5;

import javacard.security.*;

import javacard.framework.*;


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

        // Send certificate
        apdu.setOutgoingAndSend((short)0, (short) Consts.CARD_ID_LENGTH);    
    }

    void returnExpirationDate(APDU apdu){
        // Prepare data
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(card.cardExpirationDate, (short) 0, buffer, (short) 0, (short) Consts.CARD_EXP_DATE_LENGTH);

        // Send certificate
        apdu.setOutgoingAndSend((short)0, (short) Consts.CARD_EXP_DATE_LENGTH); 
    }

    void returnPubKey(APDU apdu){
        // Prepare data
        byte[] buffer = apdu.getBuffer();
        card.pubKeyCard.getModulus(buffer, (short) 0);

        // Send certificate
        apdu.setOutgoingAndSend((short)0, (short) Consts.KEY_LENGTH); 
    }
}
