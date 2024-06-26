package nl.ru.spp.group5;

import static nl.ru.spp.group5.Helpers.Utils.bytesToHex;
import static nl.ru.spp.group5.Helpers.Utils.bytesToString;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;

import nl.ru.spp.group5.Helpers.Backend;
import nl.ru.spp.group5.Helpers.Card_Managment;
import nl.ru.spp.group5.Helpers.SecurityProtocols;
import nl.ru.spp.group5.Helpers.Utils;
import nl.ru.spp.group5.Helpers.Init;

public class VendingMachineTerminal extends Terminal {

    public static void main(String[] args) throws FileNotFoundException, IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        System.out.println("This is the vending machine terminal");
        VendingMachineTerminal vendingMachineTerminal = new VendingMachineTerminal();
        vendingMachineTerminal.waitForCard();
    }

    public VendingMachineTerminal() throws FileNotFoundException, IOException, NoSuchAlgorithmException, InvalidKeySpecException {

    }

    @Override
    public void handleCard(CardChannel channel, CardTerminal terminal) throws InterruptedException, BadPaddingException, NoSuchPaddingException, IllegalBlockSizeException, InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, CardException {
        Scanner scanner = new Scanner(System.in);
        Utils.clearScreen();

        while (terminal.isCardPresent()) {
            System.out.println("Welcome to the vending machine. What do you want to do?");
            System.out.println("1: Buy a new card");
            System.out.println("2: Buy a season ticket");
            System.out.println("3: Buy a 10-entry ticket");
            System.out.println("4: Block a card (employees only) \n");

            String userInput = scanner.nextLine();

            switch (userInput) {
                case "1":
                    try{
                        buyNewCard(channel, TERMINAL_PUB_KEY, TERMINAL_PRIV_KEY);
                    } catch(CardException e){
                        System.out.println(e.getMessage());
                        System.out.println("");
                        break;
                    }
                    break;
                case "2":
                    buySeasonTicket(channel, TERMINAL_PUB_KEY, TERMINAL_PRIV_KEY);
                    break;
                case "3":
                    buyTenEntryTicket(channel, TERMINAL_PUB_KEY, TERMINAL_PRIV_KEY);
                    break;
                case "4":
                    blockCard("0");
                    break;
                default:
                    Utils.clearScreen();
                    System.out.println("\n!! Invalid input. Please enter a number between 1 and 4 !! \n");
                    break;
            }
        }
    }

    public static void buySeasonTicket(CardChannel channel, RSAPublicKey terminalPubKey, RSAPrivateKey terminalPrivKey) throws SignatureException, InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, CardException {
        Scanner scanner = new Scanner(System.in);
        Utils.clearScreen();
        System.out.println("loading...");

        byte[] cardID = SecurityProtocols.getCardID(channel);

        // Check if card was issued
        if(Utils.isAllZeros(cardID)){
            Utils.clearScreen();
            System.out.println("Purchased cancelled. Cannot buy a season ticket without buying the card first.");
            System.out.println("");
            return;
        }

        String cardIDString = new String(cardID);

        if (Backend.isCardBlocked(cardIDString)) {
            System.out.println("This card is blocked. Returning to the menu.");
            return;
        }

        boolean authenticated = SecurityProtocols.mutualAuthentication(channel, false, terminalPubKey, terminalPrivKey);
        if (!authenticated) {
            return;
        }

        //byte[] currentCertificate = Card_Managment.requestSeasonTicketCertificate(channel);
        byte[] currentCertificate = SecurityProtocols.requestSeasonTicketCertificateProtected(channel, terminalPrivKey, SecurityProtocols.getCardPubKey(channel), nonce1, nonce2);

        // Check if the certificate is valid (not all zeros)
        boolean isCertificateValid = !Utils.isAllZeros(currentCertificate);
        Utils.clearScreen();
        if (isCertificateValid) {
            System.out.println("A season ticket already exists on this card.");
            System.out.println("");
            System.out.println("Buying a new season ticket will override the old one and you will lose the remaining days.");
            System.out.println("");
            System.out.println("Do you still want to proceed? (yes/no)");
            String confirmation = scanner.nextLine();
            if (!confirmation.equalsIgnoreCase("yes")) {
                System.out.println("Purchase cancelled. Returning to the menu.");
                return;
            }
        } else {
            System.out.println("Confirm purchase of new season ticket? (yes/no)");
            String confirmation = scanner.nextLine();
            if (!confirmation.equalsIgnoreCase("yes")) {
                Utils.clearScreen();
                System.out.println("Purchase cancelled. Returning to the menu.");
                System.out.println("");
                return;
            }
        }

        // Generate new seasonExpiryDate
        byte[] seasonExpiryDate = Utils.getExpirationDateUsingMonths(3);


        byte[] newCertificate = Card_Managment.generateSeasonTicketCertificate(cardID, seasonExpiryDate, terminalPrivKey);
        if (newCertificate == null) {
            System.out.println("Failed to generate new season ticket certificate.");
            return;
        }
        SecurityProtocols.sendSeasonExpiryDateToCardProtected(channel, terminalPrivKey, SecurityProtocols.getCardPubKey(channel), nonce1, nonce2, seasonExpiryDate);

        boolean success = SecurityProtocols.sendSeasonTicketCertificateProtected(channel, terminalPrivKey, SecurityProtocols.getCardPubKey(channel), nonce1, nonce2, newCertificate);

        if (success) {
            System.out.println("Season ticket purchased successfully.");
        } else {
            System.out.println("Failed to update the season ticket. Please try again.");
        }

        System.out.println("Press enter to return to the menu");
        scanner.nextLine();
        Utils.clearScreen();
    }

    public static void buyTenEntryTicket(CardChannel channel, RSAPublicKey terminalPubKey, RSAPrivateKey terminalPrivKey) throws SignatureException, InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, CardException {
        Scanner scanner = new Scanner(System.in);
        Utils.clearScreen();

        System.out.println("Requesting 10-entry ticket...");
        byte[] cardID = SecurityProtocols.getCardID(channel);

        // Check if card was issued
        if(Utils.isAllZeros(cardID)){
            Utils.clearScreen();
            System.out.println("Purchased cancelled. Cannot buy a 10-entry ticket without buying the card first.");
            System.out.println("");
            return;
        }

        String cardId = new String(cardID);

        if (Backend.isCardBlocked(cardId)) {
            System.out.println("This card is blocked. Returning to the menu.");
            return;
        }

        boolean authenticated = SecurityProtocols.mutualAuthentication(channel, false, terminalPubKey, terminalPrivKey);
        if (!authenticated) {
            return;
        }

        int currentEntries = SecurityProtocols.getEntriesFromCardProtected(channel, terminalPrivKey, SecurityProtocols.getCardPubKey(channel), nonce1, nonce2);
        if (currentEntries != 0) {
            System.out.println("Card still has " + currentEntries + " entries. Cannot issue a new 10-entry ticket.");
            System.out.println("");
            return;
        }

        System.out.println("Confirm purchase of 10-entry ticket? (yes/no)");
        String confirmation = scanner.nextLine();
        if (!confirmation.equalsIgnoreCase("yes")) {
            System.out.println("Purchase cancelled. Returning to the menu.");
            return;
        }

        boolean success = SecurityProtocols.setEntriesProtected(channel, terminalPrivKey, SecurityProtocols.getCardPubKey(channel), nonce1, nonce2, cardId, 10);
        if (success) {
            System.out.println("10-entry ticket purchased successfully.");
        } else {
            System.out.println("Failed to issue 10-entry ticket. Please try again.");
        }

        System.out.println("Press enter to return to the menu");
        scanner.nextLine();
        Utils.clearScreen();
    }

    public static void buyNewCard(CardChannel channel, RSAPublicKey terminalPubKey, RSAPrivateKey terminalPrivKey) throws InterruptedException, SignatureException, InvalidKeyException, NoSuchAlgorithmException, CardException{
        Utils.clearScreen();
        Scanner scanner = new Scanner(System.in);
        System.out.println("Confirm purchase of new card (yes/no)");
        String confirmation = scanner.nextLine();
        if (!confirmation.equalsIgnoreCase("yes")) {
            System.out.println("Purchase cancelled. Returning to the menu.");
            System.out.println("");
            return;
        }

        Utils.clearScreen();
        System.out.println("Issueing card. This might take a while...");

        if(!Init.initCard(channel, terminalPubKey, terminalPrivKey)){
            System.out.println("Something went wrong while issueing the card. Pleae try again");
        }

        System.out.println("Press enter to return to the menu");
        scanner.nextLine();
        Utils.clearScreen();
    }

    public static void blockCard(String cardID) {
        Scanner scanner = new Scanner(System.in);

        Utils.clearScreen();
        System.out.println("Please enter the secret employee code: ");
        String employeeCodeInput = scanner.nextLine();

        if (!employeeCodeInput.equals(Backend.getEmployeeCode())) {
            System.out.println("Wrong code. Returning to the menu. \n");
            return;
        }

        Utils.clearScreen();
        System.out.println("Please enter the ID of the card you want to block:");
        String cardIDInput = scanner.nextLine();

        Backend.blockCard(cardIDInput);
        Card_Managment.blockCard(cardIDInput);

        Utils.clearScreen();
        System.out.println("Card with ID " + cardIDInput + " has been blocked successfully. \n");

        System.out.println("Press enter to return to the menu");
        scanner.nextLine();
        Utils.clearScreen();
    }
}
