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

    void getCardID(APDU apdu){
        // Prepare data
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(card.cardID, (short) 0, buffer, (short) 0, (short) Consts.CARD_ID_LENGTH);

        // Send certificate
        apdu.setOutgoingAndSend((short)0, (short) Consts.CARD_ID_LENGTH);    }
}
