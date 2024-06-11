package nl.ru.spp.group5;

import javacard.framework.*;
import javacard.security.*;

public class Card extends Applet {

    // Lifecycle states
    private static final byte STATE_INITIAL = (byte) 0x00;
    private static final byte STATE_ISSUED = (byte) 0x01;
    private static final byte STATE_BLOCKED = (byte) 0x02;
    private static final byte STATE_END_OF_LIFE = (byte) 0x03;
    private byte cardState;

    private static final byte INS_GET_DATA = (byte) 0x00;
    private static final byte INS_SET_DATA = (byte) 0x01;
    private static final byte INS_ISSUE_SEASON_TICKET = (byte) 0x02;
    private static final byte INS_ISSUE_ENTRY_TICKET = (byte) 0x03;
    private static final byte INS_USE_ENTRY = (byte) 0x04;
    private static final byte INS_CHECK_VALIDITY = (byte) 0x05;
    private static final byte INS_GET_REMAINING_ENTRIES = (byte) 0x06;
    private static final byte INS_BLOCK_CARD = (byte) 0x07;
    private static final byte INS_CHECK_ENTRIES = (byte) 0x0B;
    private static final byte INS_ISSUE_CARD = (byte) 0x0D;
    private static final byte INS_SAVE_CERTIFICATE = (byte) 0x0E;

    // MUTUAL AUTHENTICATION
    private final Auth auth;
    private static final byte INS_RETURN_CARD_CERTIFICATE = (byte) 0x08;
    private static final byte INS_GET_CARD_ID = (byte) 0x10;
    private static final byte INS_GET_CARD_EXPIRATION_DATE = (byte) 0x11;
    private static final byte INS_GET_CARD_PUB_KEY = (byte) 0x12;
    private static final byte INS_CALCULATE_X1 = (byte) 0x13;
    private static final byte INS_GET_NONCE2 = (byte) 0x14;
    private static final byte INS_AUTHENTICATE_TERMINAL_FIRST_HALF = (byte) 0x15;
    private static final byte INS_AUTHENTICATE_TERMINAL_SECOND_HALF = (byte) 0x16;
    protected byte[] x2;
    protected byte[] nonce2;

    // ISSUE CARD
    private final Init init;
    private static final byte INS_ISSUE_GENERATEKEYS = (byte) 0x0F;
    private static final byte INS_ISSUE_SAVE_CERT_FIRST_HALF = (byte) 0x22;
    private static final byte INS_ISSUE_SAVE_CERT_SECOND_HALF = (byte) 0x24;
    private static final byte INS_ISSUE_SAVE_PUB_KEY_FIRST_HALF = (byte) 0x23;
    private static final byte INS_ISSUE_SAVE_PUB_KEY_SECOND_HALF = (byte) 0x25;
    protected RSAPrivateKey privKeyCard;
    protected RSAPublicKey pubKeyCard;
    protected byte[] cardID;
    protected byte[] cardExpirationDate;
    protected boolean isIssued;
    protected byte[] cardCertificate;
    protected byte[] pubKeyVendingBytes;
    protected RSAPublicKey pubKeyVending;

    // ACCESS POOL
    private final Access access;
    private static final byte INS_ACCESS_GET_SEASON_CERT = (byte) 0x26;

    // BUY TICKET
    private final BuyTicket buyTicket;
    private static final byte INS_REQUEST_SEASON_TICKET_CERTIFICATE = (byte) 0x09;
    private static final byte INS_SAVE_SEASON_TICKET_CERTIFICATE_FIRST_HALF = (byte) 0x0A;
    private static final byte INS_SAVE_SEASON_TICKET_CERTIFICATE_SECOND_HALF = (byte) 0x1A;
    private static final byte INS_SET_ENTRIES = (byte) 0x0C;
    protected byte[] seasonTicketCertificate;
    protected byte entryCounter;




    private short expirationYear;
    private byte expirationMonth;
    private byte expirationDay;
    private boolean isBlocked;
    private byte[] cardKey;
    private byte[] kCard;

    private byte[] data;

    private Card() {
        data = new byte[256];
        entryCounter = 0;
        isBlocked = false;
        seasonTicketCertificate = new byte[Consts.CERT_LENGTH];
        cardKey = new byte[Consts.KEY_LENGTH];
        kCard = new byte[Consts.KEY_LENGTH];
        cardID = new byte[Consts.CARD_ID_LENGTH];
        cardExpirationDate = new byte[Consts.CARD_EXP_DATE_LENGTH];
        cardCertificate = new byte[Consts.CERT_LENGTH];
        isIssued = false;
        x2 = new byte[Consts.KEY_LENGTH];
        nonce2 = new byte[Consts.NONCE_LENGTH];
        pubKeyVendingBytes = new byte[Consts.KEY_LENGTH];

        init = new Init(this);
        auth = new Auth(this);
        access = new Access(this);
        buyTicket = new BuyTicket(this);
        cardState = STATE_INITIAL;
        register();
    }

    public static void install(byte[] buffer, short offset, byte length) {
        new Card();
    }

    public void process(APDU apdu) {
        if (cardState == STATE_BLOCKED || cardState == STATE_END_OF_LIFE) {
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
            case INS_RETURN_CARD_CERTIFICATE:
                auth.returnCertificate(apdu);
                break;
            case INS_REQUEST_SEASON_TICKET_CERTIFICATE:
                buyTicket.requestSeasonTicketCertificate(apdu);
                break;
            case INS_SAVE_SEASON_TICKET_CERTIFICATE_FIRST_HALF:
                buyTicket.saveSeasonTicketCertificateFirstHalf(apdu);
                break;
            case INS_SAVE_SEASON_TICKET_CERTIFICATE_SECOND_HALF:
                buyTicket.saveSeasonTicketCertificateSecondHalf(apdu);
                break;
            case INS_CHECK_ENTRIES:
                checkEntries(apdu);
                break;
            case INS_SET_ENTRIES:
                buyTicket.setEntries(apdu);
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
            case INS_ISSUE_SAVE_CERT_FIRST_HALF:
                init.saveCertFirstHalf(apdu);
                break;
            case INS_ISSUE_SAVE_CERT_SECOND_HALF:
                init.saveCertSecondHalf(apdu);
                break;
            case INS_ISSUE_SAVE_PUB_KEY_FIRST_HALF:
                init.savePubKeyVendingFirstHalf(apdu);
                break;
            case INS_ISSUE_SAVE_PUB_KEY_SECOND_HALF:
                init.savePubKeyVendingSecondHalf(apdu);
                break;
            case INS_GET_CARD_ID:
                auth.returnID(apdu);
                break;
            case INS_GET_CARD_EXPIRATION_DATE:
                auth.returnExpirationDate(apdu);
                break;
            case INS_GET_CARD_PUB_KEY:
                auth.returnPubKey(apdu);
                break;
            case INS_CALCULATE_X1:
                auth.calculatex1(apdu);
                break;
            case INS_GET_NONCE2:
                auth.getNonce2(apdu);
                break;
            case INS_AUTHENTICATE_TERMINAL_FIRST_HALF:
                auth.authenticateTerminalFirstHalf(apdu);
                break;
            case INS_AUTHENTICATE_TERMINAL_SECOND_HALF:
                auth.authenticateTerminalSecondHalf(apdu);
                break;
            case INS_ACCESS_GET_SEASON_CERT:
                access.returnSeasonCert(apdu);
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
        if (cardState == STATE_ISSUED) {
            cardState = STATE_BLOCKED;
            isBlocked = true;
        }
    }

    private void checkEntries(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        buffer[0] = entryCounter;
        apdu.setOutgoingAndSend((short) 0, (short) 1);
    }

    private void issueCard(APDU apdu) {
        if (cardState == STATE_INITIAL) {
            byte[] buffer = apdu.getBuffer();
            short length = apdu.setIncomingAndReceive();
            Util.arrayCopy(buffer, (short) (ISO7816.OFFSET_CDATA), cardKey, (short) 0, (short) 16);
            Util.arrayCopy(buffer, (short) (ISO7816.OFFSET_CDATA + 16), kCard, (short) 0, (short) 16);
            cardState = STATE_ISSUED;
            isIssued = true;
        }
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
