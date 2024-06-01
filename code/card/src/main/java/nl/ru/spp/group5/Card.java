package nl.ru.spp.group5;

import javacard.framework.*;
import javacard.security.*;

public class Card extends Applet {

    private static final byte INS_GET_DATA = (byte) 0x00;
    private static final byte INS_SET_DATA = (byte) 0x01;
    private static final byte INS_ISSUE_SEASON_TICKET = (byte) 0x02;
    private static final byte INS_ISSUE_ENTRY_TICKET = (byte) 0x03;
    private static final byte INS_USE_ENTRY = (byte) 0x04;
    private static final byte INS_CHECK_VALIDITY = (byte) 0x05;
    private static final byte INS_GET_REMAINING_ENTRIES = (byte) 0x06;
    private static final byte INS_BLOCK_CARD = (byte) 0x07;
    private static final byte INS_MUTUAL_AUTHENTICATE = (byte) 0x08;
    private static final byte INS_REQUEST_SEASON_TICKET_CERTIFICATE = (byte) 0x09;
    private static final byte INS_SEND_SEASON_TICKET_CERTIFICATE = (byte) 0x0A;
    private static final byte INS_CHECK_ENTRIES = (byte) 0x0B;
    private static final byte INS_SET_ENTRIES = (byte) 0x0C;
    private static final byte INS_ISSUE_CARD = (byte) 0x0D;
    private static final byte INS_SAVE_CERTIFICATE = (byte) 0x0E;


    //ISSUE CARD
    private final Init init;

    private static final byte INS_ISSUE_GENERATEKEYS = (byte) 0x0F;
  
    protected RSAPrivateKey privKeyCard;
    protected RSAPublicKey pubKeyCard;
    protected byte[] cardID;
    protected byte[] cardExpirationDate;

    private short expirationYear;
    private byte expirationMonth;
    private byte expirationDay;
    private byte entryCounter;
    private boolean isBlocked;
    private byte[] seasonTicketCertificate;
    private byte[] cardKey;
    private byte[] kCard;

    private byte[] data;

    private Card() {
        data = new byte[256];
        entryCounter = 0;
        isBlocked = false;
        seasonTicketCertificate = new byte[Consts.KEY_LENGTH];
        cardKey = new byte[Consts.KEY_LENGTH]; 
        kCard = new byte[Consts.KEY_LENGTH]; 
        cardID = new byte[Consts.CARD_ID_LENGTH];
        cardExpirationDate = new byte[Consts.CARD_EXP_DATE_LENGTH];

        init = new Init(this);
        register();
    }

    public static void install(byte[] buffer, short offset, byte length) {
        new Card();
    }

    public void process(APDU apdu) {
        if (isBlocked) {
            ISOException.throwIt(ISO7816.SW_COMMAND_NOT_ALLOWED);
        }

        byte[] buffer = apdu.getBuffer();
        if (selectingApplet()) {
            return;
        }

        byte ins = buffer[ISO7816.OFFSET_INS];
        switch (ins) {
            case INS_GET_DATA:
                getData(apdu);
                break;
            case INS_SET_DATA:
                setData(apdu);
                break;
            case INS_ISSUE_SEASON_TICKET:
                issueSeasonTicket(apdu);
                break;
            case INS_ISSUE_ENTRY_TICKET:
                issueEntryTicket();
                break;
            case INS_USE_ENTRY:
                useEntry();
                break;
            case INS_CHECK_VALIDITY:
                checkValidity(apdu);
                break;
            case INS_GET_REMAINING_ENTRIES:
                getRemainingEntries(apdu);
                break;
            case INS_BLOCK_CARD:
                blockCard();
                break;
            case INS_MUTUAL_AUTHENTICATE:
                mutualAuthenticate(apdu);
                break;
            case INS_REQUEST_SEASON_TICKET_CERTIFICATE:
                requestSeasonTicketCertificate(apdu);
                break;
            case INS_SEND_SEASON_TICKET_CERTIFICATE:
                sendSeasonTicketCertificate(apdu);
                break;
            case INS_CHECK_ENTRIES:
                checkEntries(apdu);
                break;
            case INS_SET_ENTRIES:
                setEntries(apdu);
                break;
            case INS_ISSUE_CARD:
                issueCard(apdu);
                break;
            case INS_SAVE_CERTIFICATE:
                saveCertificate(apdu);
                break;
            case INS_ISSUE_GENERATEKEYS:
                init.generateKeys(apdu);
                break;
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }

    private void getData(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short length = (short) data.length;
        Util.arrayCopy(data, (short) 0, buffer, ISO7816.OFFSET_CDATA, length);
        apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, length);
    }

    private void setData(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short length = apdu.setIncomingAndReceive();
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, data, (short) 0, length);
    }

    private void issueSeasonTicket(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short length = apdu.setIncomingAndReceive();
        if (length != 2) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        short duration = Util.getShort(buffer, ISO7816.OFFSET_CDATA);
        setCurrentDate();
        addDaysToExpiration(duration);
    }

    private void checkValidity(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short currentYear = Util.getShort(buffer, ISO7816.OFFSET_CDATA);
        byte currentMonth = buffer[ISO7816.OFFSET_CDATA + 2];
        byte currentDay = buffer[ISO7816.OFFSET_CDATA + 3];

        boolean valid = false;

        if (currentYear < expirationYear) {
            valid = true;
        } else if (currentYear == expirationYear) {
            if (currentMonth < expirationMonth) {
                valid = true;
            } else if (currentMonth == expirationMonth && currentDay <= expirationDay) {
                valid = true;
            }
        }

        buffer[0] = (byte) (valid ? 1 : 0);
        apdu.setOutgoingAndSend((short) 0, (short) 1);
    }

    private void issueEntryTicket() {
        entryCounter = 10;
    }

    private void useEntry() {
        if (entryCounter > 0) {
            entryCounter--;
        } else {
            ISOException.throwIt((short) 0x6300);
        }
    }

    private void getRemainingEntries(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        buffer[0] = entryCounter;
        apdu.setOutgoingAndSend((short) 0, (short) 1);
    }

    private void blockCard() {
        isBlocked = true;
    }

    private void mutualAuthenticate(APDU apdu) {
        // Implement mutual authentication logic here
    }

    private void requestSeasonTicketCertificate(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short length = (short) seasonTicketCertificate.length;
        Util.arrayCopy(seasonTicketCertificate, (short) 0, buffer, ISO7816.OFFSET_CDATA, length);
        apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, length);
    }

    private void sendSeasonTicketCertificate(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short length = apdu.setIncomingAndReceive();
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, seasonTicketCertificate, (short) 0, length);
    }

    private void checkEntries(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        buffer[0] = entryCounter;
        apdu.setOutgoingAndSend((short) 0, (short) 1);
    }

    private void setEntries(APDU apdu) {
        entryCounter = 10;
    }

    private void issueCard(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short length = apdu.setIncomingAndReceive();
        Util.arrayCopy(buffer, (short) (ISO7816.OFFSET_CDATA), cardKey, (short) 0, (short) 16);
        Util.arrayCopy(buffer, (short) (ISO7816.OFFSET_CDATA + 16), kCard, (short) 0, (short) 16);
    }

    private void saveCertificate(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short length = apdu.setIncomingAndReceive();
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, seasonTicketCertificate, (short) 0, length);
    }

    private void setCurrentDate() {
        this.expirationYear = 2024;
        this.expirationMonth = 11;
        this.expirationDay = 30;
    }

    private void addDaysToExpiration(short days) {
        final short[] daysInMonth = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

        while (days > 0) {
            short daysInCurrentMonth = daysInMonth[(short) (expirationMonth - 1)];
            if (expirationMonth == 2 && isLeapYear(expirationYear)) {
                daysInCurrentMonth++; // Adjust for leap year
            }

            if ((short) (expirationDay + days) <= daysInCurrentMonth) {
                expirationDay += days;
                days = 0;
            } else {
                days -= (short) (daysInCurrentMonth - expirationDay + 1);
                expirationDay = 1;
                expirationMonth++;

                if (expirationMonth > 12) {
                    expirationMonth = 1;
                    expirationYear++;
                }
            }
        }
    }

    private boolean isLeapYear(short year) {
        if (year % 4 != 0) return false;
        if (year % 100 == 0 && year % 400 != 0) return false;
        return true;
    }
}
