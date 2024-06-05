package nl.ru.spp.group5.Helpers;

import javax.smartcardio.*;
import java.security.*;
import java.util.Base64;

public class Card_Managment {

    // Dummy private key for signing the certificate
    private static final PrivateKey PRIVATE_KEY = generatePrivateKey();

    private static PrivateKey generatePrivateKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            return keyPair.getPrivate();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate private key", e);
        }
    }

    public Card_Managment() {
        // Initialization code here
    }

    public void initializeCard(String cardId, byte[] cardKey) {
        Backend.setCardEntries(cardId, 0);
        Backend.setCardValidity(cardId, true);
        Backend.setCardTicketType(cardId, "none");
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
            Backend.setCardTicketType(cardId, "none"); // Set initial ticket type as none

            card.disconnect(false);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void rechargeCard(String cardId, String type) {
        if (!Backend.isCardValid(cardId)) {
            System.out.println("Card is not valid.");
            return;
        }

        if (!mutualAuthenticate(cardId)) {
            System.out.println("Authentication failed.");
            return;
        }

        if (type.equals("season")) {
            byte[] newCertificate = generateSeasonTicketCertificate(cardId);
            boolean success = sendSeasonTicketCertificate(cardId, newCertificate);
            if (success) {
                System.out.println("Season ticket recharged successfully.");
                Backend.setCardTicketType(cardId, "season");
            } else {
                System.out.println("Failed to recharge the season ticket.");
            }
        } else if (type.equals("entry")) {
            int currentEntries = Backend.getCardEntries(cardId);
            int newEntries = currentEntries + 10;
            if (newEntries > 999) {
                System.out.println("Cannot recharge: entry limit exceeded.");
                return;
            }
            boolean success = setEntries(cardId, newEntries);
            if (success) {
                System.out.println("10-entry ticket recharged successfully.");
                Backend.setCardTicketType(cardId, "entry");
            } else {
                System.out.println("Failed to recharge the 10-entry ticket.");
            }
        } else {
            System.out.println("Unknown card type.");
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

    public static int checkEntries(String cardId) {
        return Backend.getCardEntries(cardId);
    }

    public static boolean setEntries(String cardId, int entries) {
        try {
            TerminalFactory factory = TerminalFactory.getDefault();
            CardTerminals terminals = factory.terminals();
            CardTerminal terminal = terminals.list().get(0);
            Card card = terminal.connect("*");
            CardChannel channel = card.getBasicChannel();

            CommandAPDU setEntriesCommand = new CommandAPDU(0x00, 0x0C, 0x00, 0x00, new byte[]{(byte) entries});

            ResponseAPDU response = channel.transmit(setEntriesCommand);
            if (response.getSW() != 0x9000) {
                throw new CardException("Failed to set entries. Response: " + Integer.toHexString(response.getSW()));
            }

            Backend.setCardEntries(cardId, entries);

            card.disconnect(false);
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

    public static byte[] requestSeasonTicketCertificate(String cardId) {
        try {
            System.out.println("Initializing terminal and card connection...");

            TerminalFactory factory = TerminalFactory.getDefault();
            CardTerminals terminals = factory.terminals();
            CardTerminal terminal = terminals.list().get(0);

            System.out.println("Connecting to the card...");
            Card card = terminal.connect("*");
            CardChannel channel = card.getBasicChannel();

            System.out.println("Sending APDU command to request season ticket certificate...");
            CommandAPDU requestCommand = new CommandAPDU(new byte[]{
                (byte) 0x00, // CLA
                (byte) 0x09, // INS (custom instruction for requesting season ticket certificate)
                (byte) 0x00, // P1
                (byte) 0x00, // P2
                (byte) 0x00  // Lc
            });

            System.out.println("APDU command sent: " + bytesToHex(requestCommand.getBytes()));
            
            ResponseAPDU response = channel.transmit(requestCommand);

            System.out.println("Response received. Status Word (SW): " + Integer.toHexString(response.getSW()));
            
            if (response.getSW() != 0x9000) {
                throw new CardException("Failed to request the season ticket certificate. Response: " + Integer.toHexString(response.getSW()));
            }

            byte[] certificate = response.getData();
            System.out.println("Certificate data received: " + bytesToHex(certificate));

            card.disconnect(false);

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

    public static byte[] generateSeasonTicketCertificate(String cardId) {
        try {
            String expiryDate = "2025-12-31"; // Example expiry date
            String data = "CardID:" + cardId + ";ExpiryDate:" + expiryDate;
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(PRIVATE_KEY);
            signature.update(data.getBytes());
            byte[] signedData = signature.sign();
            System.out.println("Data signed successfully. Signature length: " + signedData.length);

            // Save expiry date and ticket type in the backend
            Backend.setCardExpiryDate(cardId, expiryDate);
            Backend.setCardTicketType(cardId, "season");

            return signedData;
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean sendSeasonTicketCertificate(String cardId, byte[] certificateBytes) {
        try {
            System.out.println("Initializing terminal and card connection...");

            TerminalFactory factory = TerminalFactory.getDefault();
            CardTerminals terminals = factory.terminals();
            CardTerminal terminal = terminals.list().get(0);

            System.out.println("Connecting to the card...");
            Card card = terminal.connect("*");
            CardChannel channel = card.getBasicChannel();

            System.out.println("Certificate bytes to send: " + bytesToHex(certificateBytes));
            System.out.println("Certificate length: " + certificateBytes.length);

            if (certificateBytes.length != 256) {
                System.err.println("Certificate length is not 256 bytes.");
                return false;
            }

            // Split certificate into two parts
            byte[] firstHalf = new byte[128];
            byte[] secondHalf = new byte[128];
            System.arraycopy(certificateBytes, 0, firstHalf, 0, 128);
            System.arraycopy(certificateBytes, 128, secondHalf, 0, 128);

            // Send first half
            CommandAPDU sendFirstHalfCommand = new CommandAPDU(0x00, 0x0A, 0x00, 0x00, firstHalf);
            System.out.println("Sending first half of certificate...");
            System.out.println("APDU command (first half): " + bytesToHex(sendFirstHalfCommand.getBytes()));
            ResponseAPDU response = channel.transmit(sendFirstHalfCommand);
            System.out.println("Response received for first half. Status Word (SW): " + Integer.toHexString(response.getSW()));

            if (response.getSW() != 0x9000) {
                throw new CardException("Failed to send the first half of the season ticket certificate. Response: " + Integer.toHexString(response.getSW()));
            }

            // Send second half
            CommandAPDU sendSecondHalfCommand = new CommandAPDU(0x00, 0x0A, 0x00, 0x01, secondHalf);
            System.out.println("Sending second half of certificate...");
            System.out.println("APDU command (second half): " + bytesToHex(sendSecondHalfCommand.getBytes()));
            response = channel.transmit(sendSecondHalfCommand);
            System.out.println("Response received for second half. Status Word (SW): " + Integer.toHexString(response.getSW()));

            if (response.getSW() != 0x9000) {
                throw new CardException("Failed to send the second half of the season ticket certificate. Response: " + Integer.toHexString(response.getSW()));
            }

            card.disconnect(false);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
