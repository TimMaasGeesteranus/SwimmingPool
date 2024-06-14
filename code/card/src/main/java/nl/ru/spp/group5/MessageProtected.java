package nl.ru.spp.group5;

import javacard.security.*;
import nl.ru.spp.group5.Consts;

import java.math.BigInteger;

import javacard.framework.*;

public class MessageProtected {
    private final Card card;

    MessageProtected(Card card) {
        this.card = card;
    }

    void setTemporarySignatureFirstHalf(APDU apdu){
        // Get sign from apdu and save onto card
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, card.temp_signature, (short) 0, (short) (Consts.KEY_LENGTH/2));

        // Send ok message
        apdu.setOutgoingAndSend((short)0, (short)0);
    }

    void setTemporarySignatureSecondHalf(APDU apdu){
        // Get sign from apdu and save onto card
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, card.temp_signature, (short) (Consts.KEY_LENGTH/2), (short) (Consts.KEY_LENGTH/2));

        // Send ok message
        apdu.setOutgoingAndSend((short)0, (short)0);
    }
}