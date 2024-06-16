package nl.ru.spp.group5.Helpers;

import javax.smartcardio.*;

import static nl.ru.spp.group5.Helpers.Utils.*;

import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.util.Base64;

public class Card_Managment {

    public Card_Managment() {
        // Initialization code here
    }

    public static void blockCard(String cardId) {
        try {
            TerminalFactory factory = TerminalFactory.getDefault();
            CardTerminals terminals = factory.terminals();
            CardTerminal terminal = terminals.list().get(0);
            Card card = terminal.connect("*");
            CardChannel channel = card.getBasicChannel();

            CommandAPDU blockCommand = new CommandAPDU(new byte[]{
                (byte) 0x00, // CLA
                (byte) 0x07, // INS (custom instruction for blocking the card)
                (byte) 0x00, // P1
                (byte) 0x00, // P2
                (byte) 0x00  // Lc
            });

            ResponseAPDU response = channel.transmit(blockCommand);
            if (response.getSW() != 0x9000) {
                throw new CardException("Failed to block the card. Response: " + Integer.toHexString(response.getSW()));
            }

            Backend.blockCard(cardId); // Mark the card as invalid

            card.disconnect(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getEntriesFromCard(CardChannel channel) {
        try {
            CommandAPDU apdu = new CommandAPDU(0x00, 0x1B, 0x00, 0x00);

            ResponseAPDU response = channel.transmit(apdu);
            if (response.getSW() != 0x9000) {
                throw new CardException("Failed to set entries. Response: " + Integer.toHexString(response.getSW()));
            }

            return (response.getData()[0] & 0xFF);
        } catch (Exception e) {
            e.printStackTrace();
            return 10;
        } 
    }

    public static boolean setEntries(CardChannel channel, String cardId, int entries) {
        try {
            CommandAPDU setEntriesCommand = new CommandAPDU(0x00, 0x0C, 0x00, 0x00, new byte[]{(byte) entries});

            ResponseAPDU response = channel.transmit(setEntriesCommand);
            if (response.getSW() != 0x9000) {
                throw new CardException("Failed to set entries. Response: " + Integer.toHexString(response.getSW()));
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static byte[] requestSeasonTicketCertificate(CardChannel channel) {
        try {
            CommandAPDU requestCommand = new CommandAPDU(new byte[]{
                (byte) 0x00, // CLA
                (byte) 0x09, // INS (custom instruction for requesting season ticket certificate)
                (byte) 0x00, // P1
                (byte) 0x00, // P2
                (byte) 0x00  // Lc
            });
            
            ResponseAPDU response = channel.transmit(requestCommand);            
            if (response.getSW() != 0x9000) {
                throw new CardException("Failed to request the season ticket certificate. Response: " + Integer.toHexString(response.getSW()));
            }

            byte[] certificate = response.getData();
            return certificate;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] generateSeasonTicketCertificate(byte[] cardID, byte[] seasonExpiryDate, RSAPrivateKey terminalPrivKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException{ 
        // Concatenate cardID, cardExpirationdate
        byte[] dataToSign = new byte[CARD_ID_LENGTH + CARD_EXP_DATE_LENGTH];
        System.arraycopy(cardID, 0, dataToSign, 0, CARD_ID_LENGTH);
        System.arraycopy(seasonExpiryDate, 0, dataToSign, CARD_ID_LENGTH, CARD_EXP_DATE_LENGTH);
        // Sign and return
        return sign(dataToSign, terminalPrivKey);
    }

    public static void sendSeasonExpiryDateToCard(CardChannel channel, byte[] seasonExpiryDate){
        try {
            CommandAPDU command = new CommandAPDU(0x00, 0x2A, 0x00, 0x00, seasonExpiryDate);

            ResponseAPDU response = channel.transmit(command);
            if (response.getSW() != 0x9000) {
                throw new CardException("Failed to set season ticket expiry date. Response: " + Integer.toHexString(response.getSW()));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }    

    }

    public static boolean sendSeasonTicketCertificate(CardChannel channel, byte[] certificate) {
        try {
            if (certificate.length != CERT_LENGTH) {
                System.err.println("Certificate length is not 256 bytes.");
                return false;
            }

            // Split certificate into two parts
            byte[] firstHalf = new byte[CERT_LENGTH/2];
            byte[] secondHalf = new byte[CERT_LENGTH/2];
            System.arraycopy(certificate, 0, firstHalf, 0, CERT_LENGTH/2);
            System.arraycopy(certificate, CERT_LENGTH/2, secondHalf, 0, CERT_LENGTH/2);

            // Send first half
            CommandAPDU sendFirstHalfCommand = new CommandAPDU(0x00, 0x0A, 0x00, 0x00, firstHalf);
            ResponseAPDU response = channel.transmit(sendFirstHalfCommand);

            if (response.getSW() != 0x9000) {
                throw new CardException("Failed to send the first half of the season ticket certificate. Response: " + Integer.toHexString(response.getSW()));
            }

            // Send second half
            CommandAPDU sendSecondHalfCommand = new CommandAPDU(0x00, 0x1A, 0x00, 0x00, secondHalf);
            response = channel.transmit(sendSecondHalfCommand);

            if (response.getSW() != 0x9000) {
                throw new CardException("Failed to send the second half of the season ticket certificate. Response: " + Integer.toHexString(response.getSW()));
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void signMessageAndSend(CardChannel channel, byte[] message, byte[] nonce1, byte[] nonce2, byte counter, RSAPrivateKey privKey) throws SignatureException, InvalidKeyException, NoSuchAlgorithmException, CardException{
        byte[] signature = signMessage(message, nonce1, nonce2, counter, privKey);

        byte[] signatureFirstHalf = new byte[signature.length/2];
        byte[] signatureSecondHalf = new byte[signature.length/2];
        System.arraycopy(signature, 0, signatureFirstHalf, 0, signature.length/2);
        System.arraycopy(signature, signature.length/2, signatureSecondHalf, 0, signature.length/2);

        sendSignedMessageFirstHalf(channel, signatureFirstHalf);
        sendSignedMessageSecondHalf(channel, signatureSecondHalf);
    }

    public static byte[] signMessage(byte[] message, byte[] nonce1, byte[] nonce2, byte counter, RSAPrivateKey privKey) throws SignatureException, InvalidKeyException, NoSuchAlgorithmException, CardException{
        byte[] data = new byte[message.length + nonce1.length + nonce2.length + 1];
        System.arraycopy(message, 0, data, 0, message.length);
        System.arraycopy(nonce1, 0, data, message.length, nonce1.length);
        System.arraycopy(nonce2, 0, data, message.length+nonce1.length, nonce2.length);
        data[message.length + nonce1.length + nonce2.length] = counter;

        return Utils.signWithSHA1(data, privKey);
    }

    public static void sendSignedMessageFirstHalf(CardChannel channel, byte[] signature) throws CardException{
        // Sending signature
        CommandAPDU apdu = new CommandAPDU(0x00, (byte)0x1C, 0x00, 0x00, signature);
        ResponseAPDU response = channel.transmit(apdu);

        // Verifying response
        if (response.getSW() != 0x9000){
            throw new CardException("something went wrong with sending the signed message");
        }
    }

    public static void sendSignedMessageSecondHalf(CardChannel channel, byte[] signature) throws CardException{
        // Sending signature
        CommandAPDU apdu = new CommandAPDU(0x00, (byte)0x1D, 0x00, 0x00, signature);
        ResponseAPDU response = channel.transmit(apdu);

        // Verifying response
        if (response.getSW() != 0x9000){
            throw new CardException("something went wrong with sending the signed message");
        }
    }

    public static byte[] getSignedResponse(CardChannel channel) throws CardException{
        // Sending request for signed response
        CommandAPDU apdu = new CommandAPDU(0x00, (byte)0x1E, 0x00, 0x00);
        ResponseAPDU response = channel.transmit(apdu);

        // Verifying response
        if (response.getSW() != 0x9000){
            System.out.println(Integer.toHexString(response.getSW()));
            throw new CardException("something went wrong with getting the signed response OOF");
        }

        return response.getData();
    }
}
