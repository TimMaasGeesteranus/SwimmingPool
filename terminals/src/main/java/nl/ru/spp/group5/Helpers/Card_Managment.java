package nl.ru.spp.group5.Helpers;

public class Card_Managment {

    // Constructor
    public Card_Managment() {
        // Initialization code here
    }

    // Initializes a new card
    public void initializeCard() {
        // Code to initialize a new card with a unique ID and symmetric key
    }

    // Issues a new card with specified type (season or 10-entry ticket) and duration for season tickets
    public void issueCard(String type, int duration) {
        // Code to issue a new card with the specified type and duration
    }

    // Recharges an existing card with the specified ticket type
    public void rechargeCard(String cardId, String type) {
        // Code to recharge an existing card
    }

    // Blocks a card from further use
    public static void blockCard(String cardId) {
        // TODO: disable symmetric key linked to cardID
    }

    // Unblocks a previously blocked card
    public void unblockCard(String cardId) {
        // Code to unblock a card
    }

    // Checks whether the card is valid for entry
    public boolean checkCardValidity(String cardId) {
        // Code to check if the card is valid (not blocked, has entries left, or valid season ticket)
        return false; // Placeholder return value
    }

    // Updates the number of entries on a 10-entry ticket card
    public void updateCardEntries(String cardId, int entries) {
        // Code to update the number of entries on a card
    }

    // Additional methods as needed for card management

}
