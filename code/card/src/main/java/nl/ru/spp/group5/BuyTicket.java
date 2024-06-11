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

    void saveSeasonTicketCertificateFirstHalf(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, card.seasonTicketCertificate, (short) 0, (short) (Consts.CERT_LENGTH/2));
        
        apdu.setOutgoingAndSend((short) 0, (short) 0);
    }

    void saveSeasonTicketCertificateSecondHalf(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, card.seasonTicketCertificate, (short) (Consts.CERT_LENGTH/2), (short) (Consts.CERT_LENGTH/2));
        
        apdu.setOutgoingAndSend((short) 0, (short) 0);
    }

    void setEntries(APDU apdu) {
        card.entryCounter = 10;
        apdu.setOutgoingAndSend((short) 0, (short) 0);
    }

    void getEntries(APDU apdu){
        // Prepare data
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(new byte[] {card.entryCounter}, (short) 0, buffer, (short) 0, (short) 1);

        // Send ID
        apdu.setOutgoingAndSend((short)0, (short) 1);    
    }

    void saveSeasonExpiryDate(APDU apdu){
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, card.seasonTicketExpirationDate, (short) 0, (short) Consts.CARD_EXP_DATE_LENGTH);
        
        apdu.setOutgoingAndSend((short) 0, (short) 0);       
    }

    void getSeasonExpiryDate(APDU apdu){
        // Prepare data
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(card.seasonTicketExpirationDate, (short) 0, buffer, (short) 0, (short) Consts.CARD_EXP_DATE_LENGTH);

        // Send ID
        apdu.setOutgoingAndSend((short)0, (short) Consts.CARD_EXP_DATE_LENGTH);    
    }

}