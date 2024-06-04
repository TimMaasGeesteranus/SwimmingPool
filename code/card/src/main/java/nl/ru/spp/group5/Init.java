package nl.ru.spp.group5;

import javacard.security.*;

import javacard.framework.*;

public class Init {
    private final Card card;

    Init(Card card) {
        this.card = card;
    }

    void generateKeys(APDU apdu){
        if(card.isIssued){
            ISOException.throwIt(ISO7816.SW_COMMAND_NOT_ALLOWED);
        }

        // Get cardID and expirationDate from apdu and save onto card
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, card.cardID, (short) 0, (short) Consts.CARD_ID_LENGTH );
        Util.arrayCopy(buffer, (short) (ISO7816.OFFSET_CDATA + Consts.CARD_ID_LENGTH), card.cardExpirationDate, (short) 0, (short) Consts.CARD_EXP_DATE_LENGTH);

        // Generate keys for the card
        KeyPair keyPair = new KeyPair(KeyPair.ALG_RSA, KeyBuilder.LENGTH_RSA_2048);
        keyPair.genKeyPair();
        card.pubKeyCard = (RSAPublicKey) keyPair.getPublic();
        card.privKeyCard = (RSAPrivateKey) keyPair.getPrivate();

        // Send public key
        card.pubKeyCard.getModulus(buffer, (short) 0);
        apdu.setOutgoingAndSend((short)0, (short) 256);
    }

    void saveCertFirstHalf(APDU apdu){
        if(card.isIssued){
            ISOException.throwIt(ISO7816.SW_COMMAND_NOT_ALLOWED);
        }

        // Get card certificate from apdu and save onto card
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, card.cardCertificate, (short) 0, (short) (Consts.CERT_LENGTH / 2));

        // Send ok message
        apdu.setOutgoingAndSend((short)0, (short)0);
    }

    void saveCertSecondHalf(APDU apdu){
        if(card.isIssued){
            ISOException.throwIt(ISO7816.SW_COMMAND_NOT_ALLOWED);
        }

        // Get card certificate from apdu and save onto card
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, card.cardCertificate, (short) (Consts.CERT_LENGTH / 2), (short) (Consts.CERT_LENGTH / 2));

        // Send ok message
        apdu.setOutgoingAndSend((short)0, (short)0);
    }

    void savePubKeyVendingFirstHalf(APDU apdu){
        if(card.isIssued){
            ISOException.throwIt(ISO7816.SW_COMMAND_NOT_ALLOWED);
        }

        // Get first half publick key and save onto card
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, card.pubKeyVendingBytes, (short) 0, (short) (Consts.KEY_LENGTH/2));

        apdu.setOutgoingAndSend((short)0, (short)0);
    }

    void savePubKeyVendingSecondHalf(APDU apdu){
        if(card.isIssued){
            ISOException.throwIt(ISO7816.SW_COMMAND_NOT_ALLOWED);
        }

        // Get second half publick key and save onto card
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, card.pubKeyVendingBytes, (short) (Consts.CERT_LENGTH/2), (short) (Consts.KEY_LENGTH/2));

        card.pubKeyVending = convertBytesToKey(card.pubKeyVendingBytes);

        // Block initialization
        card.isIssued = true;

        apdu.setOutgoingAndSend((short)0, (short)0);
    }

    RSAPublicKey convertBytesToKey(byte[] modulus){
        // Create exponent
        byte[] exponent = new byte[] {0x01, 0x00, 0x01};

        // Create RSA key
        RSAPublicKey pubkey = (RSAPublicKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC, KeyBuilder.LENGTH_RSA_2048, false);
        pubkey.setModulus(modulus, (short) 0, (short) modulus.length);
        pubkey.setExponent(exponent, (short) 0, (short) exponent.length);

        return pubkey;
    }
}
