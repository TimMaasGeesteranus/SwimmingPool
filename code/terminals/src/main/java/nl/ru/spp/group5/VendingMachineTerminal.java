package nl.ru.spp.group5;

import java.util.Scanner;
import javax.smartcardio.*;

import nl.ru.spp.group5.Helpers.Card_Managment;
import nl.ru.spp.group5.Helpers.Utils;

public class VendingMachineTerminal {
    // Private key of the vending machine (Placeholder)
    private final byte[] kvending = {/* ... */};

    // Public key for verifying certificates (Placeholder)
    private final byte[] Kvending = {/* ... */};

    private static final String employeeCode = "123456";
    
    public static void main(String[] args){
        Scanner scanner = new Scanner(System.in);
        Utils.clearScreen();

        while (true){
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
                    buySeasonTicket("0",  "testdate", "testdate");
                    break;
                case "3":
                    buyTenEntryTicket("0");
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

    // Method to block a card
    public static void blockCard(String cardID){
        Scanner scanner = new Scanner(System.in);

        Utils.clearScreen();
        System.out.println("Please enter the secret employee code: (123456)");
        String employeeCodeInput = scanner.nextLine();

        if (!employeeCodeInput.equals(employeeCode)){
            System.out.println("Wrong code. Returning to the menu. \n");
            return;
        }

        Utils.clearScreen();
        System.out.println("Please enter the ID of the card you want to block:");
        String cardIDInput = scanner.nextLine();


        Card_Managment.blockCard(cardIDInput);

        Utils.clearScreen();
        System.out.println("Card with ID " + cardIDInput + " has been blocked successfully. \n");

        System.out.println("Press enter to return to the menu");
        scanner.nextLine();
        Utils.clearScreen();

        return;
    }

    // Method to buy a new card
    public static void buyNewCard(){
        
    }


    // Method to buy a season ticket
    public static String buySeasonTicket(String cardID, String startDate, String endDate) {
        // Generate certificate for season ticket
        String certIDcardseason = generateCertificate(cardID, startDate, endDate);
        // Logic to store or transmit the certificate goes here
        return certIDcardseason;
    }

    // Method to generate a certificate for a season ticket
    private static String generateCertificate(String cardID, String startDate, String endDate) {
        // Placeholder for certificate generation logic
        // This would involve using kvending private key to sign the certificate
        return "Cert_" + cardID + "_" + startDate + "_" + endDate;
    }

    // Method to buy a 10-entry ticket
    public static boolean buyTenEntryTicket(String cardID) {
        int currentEntries = getNumberOfEntries(cardID); // Placeholder method to get current entries

        if (currentEntries > 999/* TODO maximum allowed entries */) {
            return false; // Sale prevented
        }

        int newEntries = currentEntries + 10;
        updateEntryCount(cardID, newEntries); // Placeholder method to update entries on card
        return true;
    }

    // Placeholder method to get the current number of entries for a card
    private static int getNumberOfEntries(String cardID) {
        // Logic to retrieve current entry count from card or database
        return 0; // Default value
    }

    // Placeholder method to update the entry count on a card
    private static void updateEntryCount(String cardID, int newEntries) {
        // Logic to update the entry count on the card or in the database
    }
}
