package nl.ru.spp.group5;

import javacard.security.*;
import nl.ru.spp.group5.Consts;

import java.math.BigInteger;

import javacard.framework.*;

public class BuyTicket {
    private final Card card;

    BuyTicket(Card card) {
        this.card = card;
    }

    void requestSeasonTicketCertificate(APDU apdu){
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(card.seasonTicketCertificate, (short) 0, buffer, ISO7816.OFFSET_CDATA, Consts.CERT_LENGTH);

        apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, Consts.CERT_LENGTH);
    }

    void sendSeasonTicketCertificate(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short length = apdu.setIncomingAndReceive();
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, card.seasonTicketCertificate, (short) 0, length);
        apdu.setOutgoingAndSend((short) 0, (short) 0);
    }

    void setEntries(APDU apdu) {
        card.entryCounter = 10;
        apdu.setOutgoingAndSend((short) 0, (short) 0);
    }

}