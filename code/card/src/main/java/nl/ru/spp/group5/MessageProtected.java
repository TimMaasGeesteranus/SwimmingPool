package nl.ru.spp.group5;

import javacard.security.*;
import javacardx.crypto.*;
import javacard.framework.*;

public class MessageProtected {
    private final Card card;

    MessageProtected(Card card) {
        this.card = card;
    }

    void sets1FirstHalf(APDU apdu){
        // Get sign from apdu and save onto card
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, card.s1, (short) 0, (short) (Consts.KEY_LENGTH/2));

        // Send ok message
        apdu.setOutgoingAndSend((short)0, (short)0);
    }

    void sets1SecondHalf(APDU apdu){
        // Get sign from apdu and save onto card
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, card.s1, (short) (Consts.KEY_LENGTH/2), (short) (Consts.KEY_LENGTH/2));

        // Send ok message
        apdu.setOutgoingAndSend((short)0, (short)0);
    }

    void gets2(APDU apdu){
        // Fill data
        fillData(card.m1);

        // Setup signature
        card.signature.init(card.pubKeyVending, Signature.MODE_VERIFY);

        // Check signature
        if(!card.signature.verify(card.data, (short) 0, (short) card.data.length, card.s1, (short) 0, (short) card.s1.length)){
            ISOException.throwIt((short) 0x6F04); // Throw error   
            return;  
        }

        // Create s2
        fillData(card.m2);
        card.signature.init(card.privKeyCard, Signature.MODE_SIGN);
        card.signature.sign(card.data, (short) 0, (short) card.data.length, card.s2, (short) 0);


        // Send s2
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(card.s2, (short) 0, buffer, (short) 0, (short) Consts.KEY_LENGTH);

        // Send expiration date
        apdu.setOutgoingAndSend((short)0, (short) Consts.KEY_LENGTH); 
    }

    boolean isEqual(byte[] array1, byte[] array2) {
        if (array1.length != array2.length) {
            return false;
        }
        for (short i = 0; i < array1.length; i++) {
            if (array1[i] != array2[i]) {
                return false;
            }
        }
        return true;
    }

    void toZeroes(byte[] data){
        for (short i = 0; i < data.length; i++) {
            data[i] = 0;
        }   
    }

    void fillData(byte[] message){
        toZeroes(card.data);
        Util.arrayCopy(message, (short) 0, card.data, (short) 0, (short) Consts.KEY_LENGTH);
        Util.arrayCopy(card.nonce1, (short) 0, card.data, (short) Consts.KEY_LENGTH, (short) Consts.NONCE_LENGTH);
        Util.arrayCopy(card.nonce2, (short) 0, card.data, (short) (Consts.KEY_LENGTH+Consts.NONCE_LENGTH), (short) Consts.NONCE_LENGTH);
        card.data[(short) (message.length + Consts.NONCE_LENGTH + Consts.NONCE_LENGTH)] = card.counter;
    }
}

// }catch(Exception e){
//     ISOException.throwIt((short) 0x6F04); // Throw error      
// }