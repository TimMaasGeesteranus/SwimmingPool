package nl.ru.spp.group5;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;

import nl.ru.spp.group5.Helpers.Backend;
import nl.ru.spp.group5.Helpers.Card_Managment;
import nl.ru.spp.group5.Helpers.SecurityProtocols;
import nl.ru.spp.group5.Helpers.Utils;

public class VendingMachineTerminal extends Terminal {

    public static void main(String[] args) throws FileNotFoundException, IOException, NoSuchAlgorithmException, InvalidKeySpecException{
        System.out.println("This is the vending machine terminal");
        VendingMachineTerminal vendingMachineTerminal = new VendingMachineTerminal();
        vendingMachineTerminal.waitForCard();
    }

    public VendingMachineTerminal() throws FileNotFoundException, IOException, NoSuchAlgorithmException, InvalidKeySpecException{

    }

    @Override
    public void handleCard(CardChannel channel) throws BadPaddingException, NoSuchPaddingException, IllegalBlockSizeException, InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, CardException {
        Scanner scanner = new Scanner(System.in);
        Utils.clearScreen();

        while (true) {
            System.out.println("Welcome to the vending machine. What do you want to do?");
            System.out.println("1: Buy a new card");
            System.out.println("2: Buy a season ticket");
            System.out.println("3: Buy a 10-entry ticket");
            System.out.println("4: Block a card (employees only) \n");

            String userInput = scanner.nextLine();

            switch (userInput) {
                case "1":
                    buyNewCard();
                    break;
                case "2":
                    buySeasonTicket();
                    break;
                case "3":
                    buyTenEntryTicket();
                    break;
                case "4":
                    blockCard("0");
                    break;
                case "5":
                    System.out.println(SecurityProtocols.mutualAuthentication(channel, false, TERMINAL_PUB_KEY, TERMINAL_PRIV_KEY));
                    break;
                default:
                    Utils.clearScreen();
                    System.out.println("\n!! Invalid input. Please enter a number between 1 and 4 !! \n");
                    break;
            }
        }
    }

    public static void buySeasonTicket() {
        Scanner scanner = new Scanner(System.in);
        Utils.clearScreen();

        System.out.println("Requesting season ticket...");
        String cardId = "0"; // Example card ID, replace with actual logic to get card ID

        boolean authenticated = Card_Managment.mutualAuthenticate(cardId);
        if (!authenticated) {
            System.out.println("Authentication failed. Returning to the menu.");
            return;
        }

        byte[] currentCertificate = Card_Managment.requestSeasonTicketCertificate(cardId);
        if (currentCertificate != null) {
            System.out.println("A season ticket already exists on this card and you cannot buy another one.");
            System.out.println("Current season ticket expires on: " + currentCertificate);
            System.out.println("Press enter to return to the menu");
            scanner.nextLine();
            Utils.clearScreen();
            return;
        }

        System.out.println("Confirm purchase of new season ticket? (yes/no)");
        String confirmation = scanner.nextLine();
        if (!confirmation.equalsIgnoreCase("yes")) {
            System.out.println("Purchase cancelled. Returning to the menu.");
            return;
        }

        byte[] newCertificate = Card_Managment.generateSeasonTicketCertificate(cardId);
        if (newCertificate == null) {
            System.out.println("Failed to generate new season ticket certificate.");
            return;
        }

        System.out.println("Length of new certificate: " + newCertificate.length);

        boolean success = Card_Managment.sendSeasonTicketCertificate(cardId, newCertificate);
        if (success) {
            System.out.println("Season ticket purchased successfully.");
        } else {
            System.out.println("Failed to update the season ticket. Please try again.");
        }

        System.out.println("Press enter to return to the menu");
        scanner.nextLine();
        Utils.clearScreen();
    }


    public static void buyTenEntryTicket() {
        Scanner scanner = new Scanner(System.in);
        Utils.clearScreen();

        System.out.println("Requesting 10-entry ticket...");
        String cardId = "0"; // Example card ID, replace with actual logic to get card ID

        if (Backend.isCardBlocked(cardId)) {
            System.out.println("This card is blocked. Returning to the menu.");
            return;
        }

        boolean authenticated = Card_Managment.mutualAuthenticate(cardId);
        if (!authenticated) {
            System.out.println("Authentication failed. Returning to the menu.");
            return;
        }

        int currentEntries = Card_Managment.checkEntries(cardId);
        if (currentEntries != 0) {
            System.out.println("Card already has entries. Cannot issue a new 10-entry ticket.");
            Utils.clearScreen();
            return;
        }

        System.out.println("Confirm purchase of 10-entry ticket? (yes/no)");
        String confirmation = scanner.nextLine();
        if (!confirmation.equalsIgnoreCase("yes")) {
            System.out.println("Purchase cancelled. Returning to the menu.");
            return;
        }

        boolean success = Card_Managment.setEntries(cardId, 10);
        if (success) {
            System.out.println("10-entry ticket purchased successfully.");
        } else {
            System.out.println("Failed to issue 10-entry ticket. Please try again.");
        }

        System.out.println("Press enter to return to the menu");
        scanner.nextLine();
        Utils.clearScreen();
    }

    public static void buyNewCard() {
        Scanner scanner = new Scanner(System.in);
        Utils.clearScreen();

        System.out.println("Issuing new card...");
        String cardId = "0"; // Example card ID, replace with actual logic to get card ID

        // Step 1: Generate card keys
        byte[] kCard = generateRandomKey();
        byte[] cardKey = generateRandomKey();

        boolean issued = Card_Managment.issueCard(cardId, kCard, cardKey);
        if (!issued) {
            System.out.println("Failed to issue the card. Returning to the menu.");
            return;
        }

        // Step 2: Generate and save the certificate
        byte[] certificate = Card_Managment.generateSeasonTicketCertificate(cardId);
        if (certificate == null) {
            System.out.println("Failed to generate the certificate. Returning to the menu.");
            return;
        }

        boolean saved = Card_Managment.saveCertificate(cardId, certificate);
        if (!saved) {
            System.out.println("Failed to save the certificate. Returning to the menu.");
            return;
        }

        System.out.println("New card issued successfully.");
        System.out.println("Card ID: " + cardId);
        System.out.println("Expiry Date: 2024-12-31");

        System.out.println("Press enter to return to the menu");
        scanner.nextLine();
        Utils.clearScreen();
    }

    public static byte[] generateRandomKey() {
        try {
            SecureRandom secureRandom = SecureRandom.getInstanceStrong();
            byte[] key = new byte[16];
            secureRandom.nextBytes(key);
            return key;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to generate key");
        }
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

    public static boolean buyTenEntryTicket(String cardID) {
        if (Backend.isCardBlocked(cardID)) {
            System.out.println("This card is blocked. Returning to the menu.");
            return false;
        }

        int currentEntries = getNumberOfEntries(cardID);
        if (currentEntries > 999) {
            return false;
        }

        int newEntries = currentEntries + 10;
        updateEntryCount(cardID, newEntries);
        return true;
    }

    private static int getNumberOfEntries(String cardID) {
        // Example implementation, replace with actual logic to get the current number of entries
        return Card_Managment.checkEntries(cardID);
    }

    private static void updateEntryCount(String cardID, int newEntries) {
        // Implement this method
        if (Backend.isCardBlocked(cardID)) {
            System.out.println("This card is blocked. Cannot update entries.");
            return;
        }
        Card_Managment.updateEntryCount(cardID, newEntries);
    }
}
