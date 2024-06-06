package nl.ru.spp.group5.Helpers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Backend {
    private static final String EMPLOYEE_CODE = "123456";
    private static Set<String> blockedCards = new HashSet<>();
    private static Map<String, Integer> cardEntries = new HashMap<>();
    private static Map<String, Boolean> cardValidity = new HashMap<>();
    private static Map<String, String> cardTicketType = new HashMap<>(); // new map to store ticket type
    private static Map<String, String> cardExpiryDate = new HashMap<>(); // new map to store expiry date

    public static String getEmployeeCode() {
        return EMPLOYEE_CODE;
    }

    public static void blockCard(String cardID) {
        blockedCards.add(cardID);
    }

    public static boolean isCardBlocked(String cardID) {
        return blockedCards.contains(cardID);
    }

    public static void setCardEntries(String cardId, int entries) {
        cardEntries.put(cardId, entries);
    }

    public static int getCardEntries(String cardId) {
        return cardEntries.getOrDefault(cardId, 0);
    }

    public static void setCardValidity(String cardId, boolean isValid) {
        cardValidity.put(cardId, isValid);
    }

    public static boolean isCardValid(String cardId) {
        return cardValidity.getOrDefault(cardId, false);
    }

    public static void setCardTicketType(String cardId, String type) {
        cardTicketType.put(cardId, type);
    }

    public static String getCardTicketType(String cardId) {
        return cardTicketType.get(cardId);
    }

    public static void setCardExpiryDate(String cardId, String expiryDate) {
        cardExpiryDate.put(cardId, expiryDate);
    }

    public static String getCardExpiryDate(String cardId) {
        return cardExpiryDate.get(cardId);
    }
}
