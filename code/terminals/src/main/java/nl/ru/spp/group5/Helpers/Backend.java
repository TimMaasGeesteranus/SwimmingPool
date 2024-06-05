package nl.ru.spp.group5.Helpers;

import java.util.HashMap;
import java.util.Map;

public class Backend {
    private static Map<String, Integer> cardEntries = new HashMap<>();
    private static Map<String, Boolean> cardValidity = new HashMap<>();
    private static Map<String, String> cardExpiryDates = new HashMap<>();
    private static final String employeeCode = "123456";

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

    public static void blockCard(String cardId) {
        cardValidity.put(cardId, false);
    }

    public static String getEmployeeCode() {
        return employeeCode;
    }

    public static void setCardExpiryDate(String cardId, String expiryDate) {
        cardExpiryDates.put(cardId, expiryDate);
    }

    public static String getCardExpiryDate(String cardId) {
        return cardExpiryDates.get(cardId);
    }
}
