package nl.ru.spp.group5;

import javacard.framework.*;

public class Card extends Applet {

    private short expirationYear;
    private byte expirationMonth;
    private byte expirationDay;
    private byte entryCounter;

    // Instruction codes
    private static final byte INS_GET_DATA = (byte) 0x00;
    private static final byte INS_SET_DATA = (byte) 0x01;

    // Data buffer
    private byte[] data;

    // Constructor
    private Card() {
        data = new byte[256];
        entryCounter = 10;
        register();
    }

    // Install method for Java Card
    public static void install(byte[] buffer, short offset, byte length) {
        new Card();
    }

    // Process incoming APDUs
    public void process(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        if (selectingApplet()) {
            return;
        }

        byte ins = buffer[ISO7816.OFFSET_INS];
        switch (ins) {
            case INS_GET_DATA:
                getData(apdu);
                break;
            case INS_SET_DATA:
                setData(apdu);
                break;
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }

    // Get data from the card
    private void getData(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short length = (short) data.length;
        Util.arrayCopy(data, (short) 0, buffer, ISO7816.OFFSET_CDATA, length);
        apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, length);
    }

    // Set data to the card
    private void setData(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short length = apdu.setIncomingAndReceive();
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, data, (short) 0, length);
    }

    // Simulate setting the current date
    private void setCurrentDate() {
        this.expirationYear = 2023; // Example year
        this.expirationMonth = 10;  // Example month
        this.expirationDay = 15;    // Example day
    }

    // Add days to the expiration date
    private void addDaysToExpiration(int days) {
        this.expirationDay += days;
        // This is a simplistic method; a real implementation should adjust month and year as needed
    }

    // Issue a season ticket
    public void issueSeasonTicket(int duration) {
        setCurrentDate();
        addDaysToExpiration(duration);
        System.out.println("Issuing a season ticket valid until " + expirationYear + "-" + expirationMonth + "-" + expirationDay + ".");
    }

    // Validate the season ticket
    public boolean isSeasonTicketValid() {
        short currentYear = 2023;
        byte currentMonth = 10;
        byte currentDay = 16; // Assume the next day for validity check

        // Simple date comparison for validity
        if (currentYear < expirationYear) return true;
        else if (currentYear == expirationYear && currentMonth < expirationMonth) return true;
        else if (currentYear == expirationYear && currentMonth == expirationMonth && currentDay <= expirationDay) return true;
        return false;
    }

    // Issue a 10-entry ticket
    public void issueEntryTicket() {
        entryCounter = 10;
        System.out.println("Issuing a 10-entry ticket with 10 entries.");
    }

    // Use an entry
    public void useEntry() {
        if (entryCounter > 0) {
            entryCounter--;
            System.out.println("One entry used, " + entryCounter + " entries remaining.");
        } else {
            System.out.println("No entries remaining. Please reload or issue a new ticket.");
        }
    }

    // Check remaining entries
    public byte getRemainingEntries() {
        return entryCounter;
    }
}
