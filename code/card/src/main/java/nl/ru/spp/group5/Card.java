package nl.ru.spp.group5;

import javacardx.crypto.Cipher;
import javacard.framework.*;
import javacard.security.*;

public class Card extends Applet {

    // Lifecycle states
    private static final byte STATE_INITIAL = (byte) 0x00;
    private static final byte STATE_ISSUED = (byte) 0x01;
    private static final byte STATE_BLOCKED = (byte) 0x02;
    private static final byte STATE_END_OF_LIFE = (byte) 0x03;
    private byte cardState;

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
    protected byte[] paddedNonce2;
    protected byte[] nonce1;
    protected byte[] n2;
    protected Cipher cipher;
    protected RandomData random;

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
    private static final byte INS_SAVE_SEASON_EXPIRY_DATE = (byte) 0x2A;
    private static final byte INS_GET_SEASON_EXPIRY_DATE = (byte) 0x2B;
    private static final byte INS_SET_ENTRIES = (byte) 0x0C;
    private static final byte INS_GET_ENTRIES = (byte) 0x1B;
    protected byte[] seasonTicketCertificate;
    protected byte entryCounter;
    protected byte[] seasonTicketExpirationDate;

    // BLOCKING
    private static final byte INS_BLOCK_CARD = (byte) 0x07;
    private boolean isBlocked;

    // PROTECTED MESSAGING
    private final MessageProtected messageProtected;
    private static final byte INS_SET_TEMPORARY_SIGNATURE_FIRST_HALF = (byte) 0x1C;
    private static final byte INS_SET_TEMPORARY_SIGNATURE_SECOND_HALF = (byte) 0x1D;
    protected byte[] temp_signature;

    private Card() {
        cardState = STATE_INITIAL;
        entryCounter = 0;
        isBlocked = false;
        seasonTicketCertificate = new byte[Consts.CERT_LENGTH];
        cardID = new byte[Consts.CARD_ID_LENGTH];
        cardExpirationDate = new byte[Consts.CARD_EXP_DATE_LENGTH];
        seasonTicketExpirationDate = new byte[Consts.CARD_EXP_DATE_LENGTH];
        cardCertificate = new byte[Consts.CERT_LENGTH];
        isIssued = false;
        x2 = new byte[Consts.KEY_LENGTH];
        nonce2 = new byte[Consts.NONCE_LENGTH];
        nonce1 = new byte[Consts.KEY_LENGTH];
        paddedNonce2 = new byte[Consts.KEY_LENGTH];
        n2 = new byte[Consts.KEY_LENGTH];
        pubKeyVendingBytes = new byte[Consts.KEY_LENGTH];
        cipher = Cipher.getInstance(Cipher.ALG_RSA_NOPAD, false);
        random = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);
        temp_signature = new byte[Consts.KEY_LENGTH];

        init = new Init(this);
        auth = new Auth(this);
        access = new Access(this);
        buyTicket = new BuyTicket(this);
        messageProtected = new MessageProtected(this);

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
            case INS_SAVE_SEASON_EXPIRY_DATE:
                buyTicket.saveSeasonExpiryDate(apdu);
                break;
            case INS_GET_SEASON_EXPIRY_DATE:
                buyTicket.getSeasonExpiryDate(apdu);
                break;
            case INS_SET_ENTRIES:
                buyTicket.setEntries(apdu);
                break;
            case INS_GET_ENTRIES:
                buyTicket.getEntries(apdu);
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
            case INS_SET_TEMPORARY_SIGNATURE_FIRST_HALF:
                messageProtected.setTemporarySignatureFirstHalf(apdu);
                break;
            case INS_SET_TEMPORARY_SIGNATURE_SECOND_HALF:
                messageProtected.setTemporarySignatureSecondHalf(apdu);
                break;
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }

    private void blockCard() {
        if (cardState == STATE_ISSUED) {
            cardState = STATE_BLOCKED;
            isBlocked = true;
        }
    }
}
