package nl.ru.spp.group5;

import javacard.security.*;
import javacard.framework.*;
import javacardx.crypto.Cipher;
import nl.ru.spp.group5.Consts;

public class Access {
    private final Card card;

    Access(Card card) {
        this.card = card;
    }

    void returnSeasonCert(APDU apdu){
        apdu.setOutgoingAndSend((short) 0, (short) 0);
        // Prepare data
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(card.seasonTicketCertificate, (short) 0, buffer, (short) 0, (short) Consts.CERT_LENGTH);

        // Send certificate
        apdu.setOutgoingAndSend((short)0, (short) Consts.CERT_LENGTH);
    }    
}
