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
        // save m1
        toZeroes(card.m1);
        card.m1[0] = 0x09;

        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(card.seasonTicketCertificate, (short) 0, buffer, ISO7816.OFFSET_CDATA, Consts.CERT_LENGTH);

        // save m2
        Util.arrayCopy(card.seasonTicketCertificate, (short) 0, card.m2, (short) 0, (short) Consts.CERT_LENGTH);

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
        byte[] buffer = apdu.getBuffer();
        card.entryCounter = buffer[ISO7816.OFFSET_CDATA];

        apdu.setOutgoingAndSend((short) 0, (short) 0);
    }

    void getEntries(APDU apdu){
        // save m1
        toZeroes(card.m1);
        card.m1[0] = 0x1B;

        // Prepare data
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(new byte[] {card.entryCounter}, (short) 0, buffer, (short) 0, (short) 1);

        // save m2
        toZeroes(card.m2);
        Util.arrayCopy(new byte[] {card.entryCounter}, (short) 0, card.m2, (short) 0, (short) 1);

        // Send ID
        apdu.setOutgoingAndSend((short)0, (short) 1);    
    }

    void saveSeasonExpiryDate(APDU apdu){
        // save m1
        toZeroes(card.m1);
        card.m1[0] = 0x2A;

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

    void toZeroes(byte[] data){
        for (short i = 0; i < data.length; i++) {
            data[i] = 0;
        }   
    }
}