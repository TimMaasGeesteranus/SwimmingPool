package nl.ru.spp.group5.Helpers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Backend {
    private static final String EMPLOYEE_CODE = "123456";
    private static Set<String> blockedCards = new HashSet<>();

    public static String getEmployeeCode() {
        return EMPLOYEE_CODE;
    }

    public static void blockCard(String cardID) {
        blockedCards.add(cardID);
    }

    public static boolean isCardBlocked(String cardID) {
        return blockedCards.contains(cardID);
    }
}
