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

    public void initializeCard(String cardId, byte[] cardKey) {
        Backend.setCardEntries(cardId, 0);
        Backend.setCardValidity(cardId, true);
    }

    public static boolean issueCard(String cardId, byte[] kCard, byte[] cardKey) {
        try {
            TerminalFactory factory = TerminalFactory.getDefault();
            CardTerminals terminals = factory.terminals();
            CardTerminal terminal = terminals.list().get(0);
            Card card = terminal.connect("*");
            CardChannel channel = card.getBasicChannel();

            byte[] data = new byte[32];
            System.arraycopy(kCard, 0, data, 0, 16);
            System.arraycopy(cardKey, 0, data, 16, 16);

            CommandAPDU issueCardCommand = new CommandAPDU(0x00, 0x0D, 0x00, 0x00, data);

            ResponseAPDU response = channel.transmit(issueCardCommand);
            if (response.getSW() != 0x9000) {
                throw new CardException("Failed to issue the card. Response: " + Integer.toHexString(response.getSW()));
            }

            Backend.setCardEntries(cardId, 0); // Initialize entry count for the card
            Backend.setCardValidity(cardId, true); // Mark the card as valid

            card.disconnect(false);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
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

            Backend.setCardValidity(cardId, false); // Mark the card as invalid

            card.disconnect(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unblockCard(String cardId) {
        Backend.setCardValidity(cardId, true); // Mark the card as valid
    }

    public boolean checkCardValidity(String cardId) {
        return Backend.isCardValid(cardId);
    }

    public static boolean updateEntryCount(String cardId, int newEntries) {
        try {
            TerminalFactory factory = TerminalFactory.getDefault();
            CardTerminals terminals = factory.terminals();
            CardTerminal terminal = terminals.list().get(0);
            Card card = terminal.connect("*");
            CardChannel channel = card.getBasicChannel();

            CommandAPDU updateEntriesCommand = new CommandAPDU(0x00, 0x0C, 0x00, 0x00, new byte[]{(byte) newEntries});

            ResponseAPDU response = channel.transmit(updateEntriesCommand);
            if (response.getSW() != 0x9000) {
                throw new CardException("Failed to update entries. Response: " + Integer.toHexString(response.getSW()));
            }

            Backend.setCardEntries(cardId, newEntries);

            card.disconnect(false);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean mutualAuthenticate(String cardId) {
        // Implement mutual authentication logic here
        return true;
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

            Backend.setCardEntries(cardId, entries);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean saveCertificate(String cardId, byte[] certificate) {
        try {
            TerminalFactory factory = TerminalFactory.getDefault();
            CardTerminals terminals = factory.terminals();
            CardTerminal terminal = terminals.list().get(0);
            Card card = terminal.connect("*");
            CardChannel channel = card.getBasicChannel();

            CommandAPDU saveCertificateCommand = new CommandAPDU(0x00, 0x0E, 0x00, 0x00, certificate);

            ResponseAPDU response = channel.transmit(saveCertificateCommand);
            if (response.getSW() != 0x9000) {
                throw new CardException("Failed to save the certificate. Response: " + Integer.toHexString(response.getSW()));
            }

            card.disconnect(false);
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

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }

    public static byte[] generateSeasonTicketCertificate(byte[] cardID, RSAPrivateKey terminalPrivKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException{
        byte[] cardExpirationDate = getExpirationDateUsingMonths(3);

        // Concatenate cardID, cardExpirationdate
        byte[] dataToSign = new byte[CARD_ID_LENGTH + CARD_EXP_DATE_LENGTH];
        System.arraycopy(cardID, 0, dataToSign, 0, CARD_ID_LENGTH);
        System.arraycopy(cardExpirationDate, 0, dataToSign, CARD_ID_LENGTH, CARD_EXP_DATE_LENGTH);
        
        // Sign and return
        return sign(dataToSign, terminalPrivKey);
    }

    // public static byte[] generateSeasonTicketCertificateOLD(String cardId) { //TODO remove
    //     try {
    //         String expiryDate = Utils.getExpirationDateUsingMonths(3);          
    //         String data = "CardID:" + cardId + ";ExpiryDate:" + expiryDate;
    //         Signature signature = Signature.getInstance("SHA256withRSA");
    //         signature.initSign(PRIVATE_KEY);
    //         signature.update(data.getBytes());
    //         byte[] signedData = signature.sign();

    //         // Save expiry date in the backend
    //         Backend.setCardExpiryDate(cardId, expiryDate);

    //         return signedData;
    //     } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
    //         e.printStackTrace();
    //         return null;
    //     }
    // }

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
}
