package nl.ru.spp.group5.Helpers;

import javax.smartcardio.*;
import java.util.HashMap;
import java.util.Map;

public class Card_Managment {
    private static Map<String, Integer> cardEntries = new HashMap<>();
    private static Map<String, Boolean> cardValidity = new HashMap<>();

    public Card_Managment() {
        // Initialization code here
    }

    public void initializeCard(String cardId, byte[] cardKey) {
        // Example implementation to initialize a card with a unique ID and symmetric key
        cardEntries.put(cardId, 0);
        cardValidity.put(cardId, true);
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

            cardEntries.put(cardId, 0); // Initialize entry count for the card
            cardValidity.put(cardId, true); // Mark the card as valid

            card.disconnect(false);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void rechargeCard(String cardId, String type) {
        if (!cardValidity.getOrDefault(cardId, false)) {
            System.out.println("Card is not valid.");
            return;
        }
        if (type.equals("season")) {
            // Example logic to recharge a season ticket
            System.out.println("Recharging season ticket for card: " + cardId);
        } else if (type.equals("entry")) {
            // Example logic to recharge a 10-entry ticket
            System.out.println("Recharging 10-entry ticket for card: " + cardId);
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

            cardValidity.put(cardId, false); // Mark the card as invalid

            card.disconnect(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unblockCard(String cardId) {
        cardValidity.put(cardId, true); // Mark the card as valid
    }

    public boolean checkCardValidity(String cardId) {
        return cardValidity.getOrDefault(cardId, false);
    }

    public void updateCardEntries(String cardId, int entries) {
        cardEntries.put(cardId, entries); // Update the number of entries on the card
    }

    public static boolean mutualAuthenticate(String cardId) {
        // Implement mutual authentication logic here
        return true;
    }

    public static int checkEntries(String cardId) {
        return cardEntries.getOrDefault(cardId, 0);
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

            cardEntries.put(cardId, entries);

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

    public static String requestSeasonTicketCertificate(String cardId) {
        try {
            TerminalFactory factory = TerminalFactory.getDefault();
            CardTerminals terminals = factory.terminals();
            CardTerminal terminal = terminals.list().get(0);
            Card card = terminal.connect("*");
            CardChannel channel = card.getBasicChannel();

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
            card.disconnect(false);
            return new String(certificate); // Assuming the certificate is a string
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String generateSeasonTicketCertificate(String cardId) {
        // Implement logic to generate a new season ticket certificate
        return "2025-12-31"; // Example return value
    }

    public static boolean sendSeasonTicketCertificate(String cardId, String certificate) {
        try {
            TerminalFactory factory = TerminalFactory.getDefault();
            CardTerminals terminals = factory.terminals();
            CardTerminal terminal = terminals.list().get(0);
            Card card = terminal.connect("*");
            CardChannel channel = card.getBasicChannel();

            byte[] certificateBytes = certificate.getBytes();
            CommandAPDU sendCertificateCommand = new CommandAPDU(0x00, 0x0A, 0x00, 0x00, certificateBytes);

            ResponseAPDU response = channel.transmit(sendCertificateCommand);
            if (response.getSW() != 0x9000) {
                throw new CardException("Failed to send the season ticket certificate. Response: " + Integer.toHexString(response.getSW()));
            }

            card.disconnect(false);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
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

            cardEntries.put(cardId, newEntries);

            card.disconnect(false);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
