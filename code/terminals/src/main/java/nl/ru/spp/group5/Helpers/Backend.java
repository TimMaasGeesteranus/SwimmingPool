package nl.ru.spp.group5.Helpers;

import java.util.HashMap;
import java.util.Map;

public class Backend {
    private static final String EMPLOYEE_CODE = "123456";
    private static Set<String> blockedCards = new HashSet<>();

    private static Map<String, Integer> cardEntries = new HashMap<>();
    private static Map<String, Boolean> cardValidity = new HashMap<>();

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
}
