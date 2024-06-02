package nl.ru.spp.group5.Helpers;

import javax.smartcardio.*;
import java.security.*;
import java.util.Base64;
import java.util.Date;

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
            String newCertificate = generateSeasonTicketCertificate(cardId);
            boolean success = sendSeasonTicketCertificate(cardId, newCertificate);
            if (success) {
                System.out.println("Season ticket recharged successfully. New expiry date: " + newCertificate);
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
        try {
            String data = "CardID:" + cardId + ";ExpiryDate:2025-12-31";
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(PRIVATE_KEY);
            signature.update(data.getBytes());
            byte[] signedData = signature.sign();
            return data + ";Signature:" + Base64.getEncoder().encodeToString(signedData);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
            return null;
        }
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
}
